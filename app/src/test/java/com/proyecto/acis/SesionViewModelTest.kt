package com.proyecto.acis

import com.proyecto.acis.data.Perfil
import com.proyecto.acis.data.Usuario
import com.proyecto.acis.domain.AutenticacionRepository
import com.proyecto.acis.session.SesionActual
import com.proyecto.acis.ui.SesionViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/** Sustituye el almacenamiento por un contrato simulado: no requiere Android ni SQLite. */
@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val cuenta = Usuario("cliente@acis.cl", "Cliente", Perfil.CLIENTE)
    private val sesion = SesionActual()
    private val fuente = FuenteSimulada()

    private class FuenteSimulada : AutenticacionRepository {
        var fallaCarga = false
        var fallaAcceso = false
        var resultado: Usuario? = null
        var llamadas = 0
        var espera: CompletableDeferred<Unit>? = null
        override suspend fun preparar() { check(!fallaCarga) }
        override suspend fun autenticar(correo: String, clave: String): Usuario? {
            llamadas++
            espera?.await()
            check(!fallaAcceso)
            return resultado
        }
    }

    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun limpiar() { Dispatchers.resetMain() }

    @Test fun cargaFallidaPermiteReintentarSinAutenticar() = runTest(dispatcher) {
        fuente.fallaCarga = true
        val vm = SesionViewModel(fuente, sesion)
        advanceUntilIdle()
        assertFalse(vm.estado.value.cargado)
        assertNotNull(vm.estado.value.error)
        vm.iniciarSesion("cliente", "clave")
        advanceUntilIdle()
        assertEquals(0, fuente.llamadas)
        fuente.fallaCarga = false
        vm.cargar()
        advanceUntilIdle()
        assertTrue(vm.estado.value.cargado)
        assertNull(vm.estado.value.error)
    }

    @Test fun credencialRechazadaPermiteReintentarYComparteIdentidad() = runTest(dispatcher) {
        val vm = SesionViewModel(fuente, sesion)
        advanceUntilIdle()
        vm.iniciarSesion("cliente", "incorrecta")
        advanceUntilIdle()
        assertNull(sesion.usuario.value)
        assertNotNull(vm.estado.value.error)
        fuente.resultado = cuenta
        vm.iniciarSesion("cliente", "correcta")
        advanceUntilIdle()
        assertEquals(cuenta, sesion.usuario.value)
        assertSame(sesion.usuario, vm.usuario)
        assertNull(vm.estado.value.error)
    }

    @Test fun cuentaInactivaNoAbreSesion() = runTest(dispatcher) {
        fuente.resultado = cuenta.copy(activo = false)
        val vm = SesionViewModel(fuente, sesion)
        advanceUntilIdle()
        vm.iniciarSesion("cliente", "clave")
        advanceUntilIdle()
        assertNull(vm.usuario.value)
        assertNotNull(vm.estado.value.error)
    }

    @Test fun excepcionLiberaAccesoParaReintentar() = runTest(dispatcher) {
        val vm = SesionViewModel(fuente, sesion)
        advanceUntilIdle()
        fuente.fallaAcceso = true
        vm.iniciarSesion("cliente", "clave")
        advanceUntilIdle()
        assertFalse(vm.estado.value.ocupado)
        assertNotNull(vm.estado.value.error)
        assertNull(vm.usuario.value)
        fuente.fallaAcceso = false
        fuente.resultado = cuenta
        vm.iniciarSesion("cliente", "clave")
        advanceUntilIdle()
        assertEquals(cuenta, vm.usuario.value)
    }

    @Test fun accesoPendienteNoAdmiteDuplicadosNiCerrarAntesDeTerminar() = runTest(dispatcher) {
        val vm = SesionViewModel(fuente, sesion)
        advanceUntilIdle()
        fuente.resultado = cuenta
        fuente.espera = CompletableDeferred()
        vm.iniciarSesion("cliente", "clave")
        runCurrent()
        vm.iniciarSesion("cliente", "clave")
        vm.cerrarSesion()
        assertTrue(vm.estado.value.ocupado)
        assertEquals(1, fuente.llamadas)
        fuente.espera!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(cuenta, vm.usuario.value)
        vm.cerrarSesion()
        assertNull(sesion.usuario.value)
        assertTrue(vm.estado.value.cargado)
        assertNull(vm.estado.value.error)
    }
}
