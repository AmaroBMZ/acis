package com.proyecto.acis

import com.proyecto.acis.data.*
import com.proyecto.acis.domain.*
import com.proyecto.acis.session.SesionActual
import com.proyecto.acis.ui.UsuariosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

/** Prueba decisiones y recuperación ante fallos sin necesitar Android ni la base real. */
@OptIn(ExperimentalCoroutinesApi::class)
class UsuariosViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val admin = Usuario("admin@acis.cl", "Admin", Perfil.ADMINISTRADOR)
    private val sesion = SesionActual()
    private val fuente = Fuente()
    private class Fuente : UsuariosRepository {
        override val usuarios = MutableStateFlow<List<Usuario>>(emptyList())
        var llamadas = 0
        var falla = false
        var correoRecibido = ""
        var resultado = ResultadoRegistro.CREADO
        override suspend fun cargar(token: String) = true
        override suspend fun registrar(actor: String, token: String, rut: String, nombre: String, correo: String, perfil: Perfil, clave: String): ResultadoRegistro {
            llamadas++; correoRecibido = correo
            check(!falla)
            return resultado
        }
        override suspend fun actualizarAcceso(actor: String, token: String, correo: String, perfil: Perfil, activo: Boolean): Boolean { llamadas++; return true }
    }
    @Before fun preparar() { Dispatchers.setMain(dispatcher); sesion.actualizar(admin, "token-demo"); fuente.usuarios.value = listOf(admin) }
    @After fun limpiar() { Dispatchers.resetMain() }
    private fun registrar(vm: UsuariosViewModel, callback: (Boolean) -> Unit = {}) =
        vm.registrar("Ana Pérez", " ANA@ACIS.CL ", "12345678-9", Perfil.CLIENTE, "Clave1234", "Clave1234", callback)

    @Test fun normalizaCorreoYEvitaDobleEnvio() = runTest(dispatcher) {
        val vm = UsuariosViewModel(fuente, sesion)
        registrar(vm); registrar(vm)
        advanceUntilIdle()
        assertEquals(1, fuente.llamadas)
        assertEquals("ana@acis.cl", fuente.correoRecibido)
        assertFalse(vm.ocupado)
    }
    @Test fun rechazaDatosInvalidosAntesDeEscribir() {
        val vm = UsuariosViewModel(fuente, sesion)
        vm.registrar("", "mal", "rut-mal", Perfil.CLIENTE, "123", "otra") { assertFalse(it) }
        assertEquals(setOf("nombre", "correo", "rut", "clave", "confirmacion"), vm.errores.keys)
        assertEquals(0, fuente.llamadas)
    }
    @Test fun soloAdminActivoPuedeRegistrar() = runTest(dispatcher) {
        for (actor in listOf(null, admin.copy(activo = false), admin.copy(perfil = Perfil.PLANIFICADOR))) {
            sesion.actualizar(actor)
            val vm = UsuariosViewModel(fuente, sesion)
            registrar(vm) { assertFalse(it) }
            advanceUntilIdle()
            assertNotNull(vm.errorGeneral)
        }
        assertEquals(0, fuente.llamadas)
    }
    @Test fun correoDuplicadoSeIndicaEnElCampo() = runTest(dispatcher) {
        fuente.resultado = ResultadoRegistro.CORREO_DUPLICADO
        val vm = UsuariosViewModel(fuente, sesion)
        registrar(vm) { assertFalse(it) }
        advanceUntilIdle()
        assertNotNull(vm.errores["correo"])
    }
    @Test fun excepcionNotificaFalloYPermiteReintentar() = runTest(dispatcher) {
        fuente.falla = true
        val vm = UsuariosViewModel(fuente, sesion)
        var resultado: Boolean? = null
        registrar(vm) { resultado = it }
        advanceUntilIdle()
        assertEquals(false, resultado)
        assertFalse(vm.ocupado)
        assertNotNull(vm.errorGeneral)
        fuente.falla = false
        registrar(vm) { resultado = it }
        advanceUntilIdle()
        assertEquals(true, resultado)
        assertNull(vm.errorGeneral)
    }
    @Test fun administradorNoPuedeQuitarseSuAcceso() = runTest(dispatcher) {
        val vm = UsuariosViewModel(fuente, sesion)
        vm.actualizarAcceso(admin.correo, Perfil.CLIENTE, true) { assertFalse(it) }
        advanceUntilIdle()
        assertEquals(0, fuente.llamadas)
    }
    @Test fun limitesDeValidacionYConfirmacion() {
        assertTrue(ValidadorUsuario.validar("Ana", "ana@acis.cl", "Abc12345", "Abc12345").isEmpty())
        assertTrue(ValidadorUsuario.validar("Ana", "ana@acis.cl", "abcdefgh", "abcdefgh").containsKey("clave"))
        assertTrue(ValidadorUsuario.validar("Ana", "ana@acis.cl", "12345678", "12345678").containsKey("clave"))
        assertTrue(ValidadorUsuario.validar("Ana", "ana@acis.cl", "Abc12345", "Abc12346").containsKey("confirmacion"))
    }
}
