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

    // Al abrir o cerrar un diálogo se descartan mensajes previos; nunca interrumpe una escritura activa.
    fun limpiarErrores() { if (!ocupado) { errores = emptyMap(); errorGeneral = null } }

    fun registrar(nombre: String, correo: String, perfil: Perfil, clave: String, confirmacion: String, onResultado: (Boolean) -> Unit) {
        if (ocupado) return
        limpiarErrores()
        errores = ValidadorUsuario.validar(nombre, correo, clave, confirmacion)
        if (errores.isNotEmpty()) { onResultado(false); return }
        // La UI valida para dar respuesta inmediata; SQLite vuelve a validar y autorizar dentro de la transacción.
        ejecutar(onResultado) {
            val actor = sesion.usuario.value
            if (!PermisosUsuarios.puedeGestionar(actor)) {
                errorGeneral = "Solo un administrador activo puede registrar cuentas."
                false
            } else {
                when (repository.registrar(actor!!.correo, nombre.trim(), ValidadorUsuario.correoNormalizado(correo), perfil, clave)) {
                    ResultadoRegistro.CREADO -> true
                    // Errores de datos específicos se muestran junto al campo que el administrador corrige.
                    ResultadoRegistro.CORREO_DUPLICADO -> { errores = mapOf("correo" to "Ya existe una cuenta con este correo."); false }
                    ResultadoRegistro.NOMBRE_TECNICO_DUPLICADO -> { errores = mapOf("nombre" to "Este nombre identifica a un técnico. Usa un nombre distinto."); false }
                    ResultadoRegistro.NO_AUTORIZADO -> { errorGeneral = "No tienes permiso para registrar cuentas."; false }
                    ResultadoRegistro.DATOS_INVALIDOS -> { errorGeneral = "Revisa los datos de la cuenta."; false }
                }
            }
        }
    }

    fun actualizarAcceso(correo: String, perfil: Perfil, activo: Boolean, onResultado: (Boolean) -> Unit) = ejecutar(onResultado) {
        val actor = sesion.usuario.value
        val destino = usuarios.value.firstOrNull { it.correo == correo }
        val permitido = PermisosUsuarios.puedeModificar(actor, destino, perfil, activo)
        // La comprobación local mejora el mensaje; el repositorio y SQLite verifican de nuevo al persistir.
        val guardado = permitido && repository.actualizarAcceso(actor!!.correo, correo, perfil, activo)
        if (!guardado) errorGeneral = "No se pudo modificar el acceso. Conserva tu propio rol administrador activo y evita nombres de técnicos repetidos."
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
