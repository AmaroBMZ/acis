package com.proyecto.acis.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.acis.data.Perfil
import com.proyecto.acis.domain.*
import com.proyecto.acis.session.SesionActual
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Coordina cuentas sin depender del ViewModel de órdenes. No conserva contraseñas como estado. */
class UsuariosViewModel(private val repository: UsuariosRepository, private val sesion: SesionActual) : ViewModel() {
    val usuarios = repository.usuarios
    var ocupado by mutableStateOf(false)
        private set
    var errorGeneral by mutableStateOf<String?>(null)
        private set
    var errores by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    fun refrescar() = cargarUsuarios()

    private fun cargarUsuarios() {
        val token = sesion.token.value ?: return
        ejecutar({}) {
            if (!repository.cargar(token)) {
                errorGeneral = "No se pudo cargar el listado de usuarios del servidor."
                false
            } else true
        }
    }

    // Al abrir o cerrar un diálogo se descartan mensajes previos; nunca interrumpe una escritura activa.
    fun limpiarErrores() { if (!ocupado) { errores = emptyMap(); errorGeneral = null } }

    fun registrar(nombre: String, correo: String, rut: String, perfil: Perfil, clave: String, confirmacion: String, onResultado: (Boolean) -> Unit) {
        if (ocupado) return
        limpiarErrores()
        errores = ValidadorUsuario.validar(nombre, correo, clave, confirmacion, rut)
        if (errores.isNotEmpty()) { onResultado(false); return }
        // La UI valida primero; el microservicio vuelve a validar y autorizar antes de persistir.
        ejecutar(onResultado) {
            val actor = sesion.usuario.value
            val token = sesion.token.value
            if (!PermisosUsuarios.puedeGestionar(actor) || token.isNullOrBlank()) {
                errorGeneral = "Solo un administrador activo puede registrar cuentas."
                false
            } else {
                when (repository.registrar(actor!!.correo, token, rut.trim().replace(".", "").replace(" ", "").uppercase(), nombre.trim(), ValidadorUsuario.correoNormalizado(correo), perfil, clave)) {
                    ResultadoRegistro.CREADO -> true
                    // Errores de datos específicos se muestran junto al campo que el administrador corrige.
                    ResultadoRegistro.CORREO_DUPLICADO -> { errores = mapOf("correo" to "Ya existe una cuenta con ese correo o RUT."); false }
                    ResultadoRegistro.NOMBRE_TECNICO_DUPLICADO -> { errores = mapOf("nombre" to "Este nombre identifica a un técnico. Usa un nombre distinto."); false }
                    ResultadoRegistro.NO_AUTORIZADO -> { errorGeneral = "No tienes permiso para registrar cuentas."; false }
                    ResultadoRegistro.DATOS_INVALIDOS -> { errorGeneral = "Revisa los datos de la cuenta."; false }
                }
            }
        }
    }

    fun actualizarAcceso(correo: String, perfil: Perfil, activo: Boolean, onResultado: (Boolean) -> Unit) = ejecutar(onResultado) {
        val actor = sesion.usuario.value
        val token = sesion.token.value
        val destino = usuarios.value.firstOrNull { it.correo == correo }
        val permitido = PermisosUsuarios.puedeModificar(actor, destino, perfil, activo)
        // La comprobación local mejora el mensaje; el microservicio vuelve a verificar el permiso.
        val guardado = permitido && !token.isNullOrBlank() && repository.actualizarAcceso(actor!!.correo, token, correo, perfil, activo)
        if (!guardado) errorGeneral = "No se pudo modificar el acceso. Conserva tu propia cuenta de administrador activa."
        guardado
    }

    // Libera el bloqueo incluso ante errores; la UI conserva el formulario si el guardado falla.
    private fun ejecutar(onResultado: (Boolean) -> Unit, accion: suspend () -> Boolean) {
        if (ocupado) return
        ocupado = true
        errorGeneral = null
        viewModelScope.launch {
            // Captura fallos recuperables, libera el indicador y siempre informa a la pantalla del resultado.
            val resultado = try { accion() }
            catch (cancelada: CancellationException) { throw cancelada }
            catch (_: Exception) { errorGeneral = "No se pudo guardar. Inténtalo nuevamente."; false }
            finally { ocupado = false }
            onResultado(resultado)
        }
    }
}
