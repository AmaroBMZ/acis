package com.proyecto.acis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.acis.domain.AutenticacionRepository
import com.proyecto.acis.session.SesionActual
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado del acceso, separado de los formularios y errores de las órdenes. */
data class EstadoAcceso(
    val cargado: Boolean = false,
    val ocupado: Boolean = false,
    val error: String? = null,
)

class SesionViewModel(
    private val repository: AutenticacionRepository,
    private val sesion: SesionActual,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoAcceso())
    val estado = _estado.asStateFlow()
    val usuario = sesion.usuario

    init { cargar() }

    fun cargar() = ejecutar {
        repository.preparar()
        _estado.value = _estado.value.copy(cargado = true)
    }

    fun iniciarSesion(correo: String, clave: String) = ejecutar {
        if (!_estado.value.cargado) {
            _estado.value = _estado.value.copy(error = "Primero carga los datos.")
        } else {
            val resultado = repository.autenticar(correo.trim(), clave)
            val cuenta = resultado?.usuario
            if (cuenta == null || !cuenta.activo) {
                _estado.value = _estado.value.copy(error = "Correo o contraseña incorrectos, o cuenta inactiva.")
            } else {
                // Solo guardar la sesión después de validar que la cuenta está activa.
                sesion.actualizar(cuenta, resultado.token)
            }
        }
    }

    // El coordinador de UI limpia primero los borradores y evita salir durante una escritura de OT.
    fun cerrarSesion() {
        if (_estado.value.ocupado) return
        sesion.actualizar(null)
        _estado.value = _estado.value.copy(error = null)
    }

    private fun ejecutar(accion: suspend () -> Unit) {
        if (_estado.value.ocupado) return
        _estado.value = _estado.value.copy(ocupado = true, error = null)
        viewModelScope.launch {
            try { accion() }
            catch (cancelada: CancellationException) { throw cancelada }
            catch (_: Exception) {
                _estado.value = _estado.value.copy(error = "No se pudo completar la operación. Inténtalo nuevamente.")
            } finally {
                _estado.value = _estado.value.copy(ocupado = false)
            }
        }
    }
}
