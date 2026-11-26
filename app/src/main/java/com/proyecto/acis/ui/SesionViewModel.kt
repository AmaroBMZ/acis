package com.proyecto.acis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.acis.data.Perfil
import com.proyecto.acis.data.Usuario
import com.proyecto.acis.domain.AutenticacionRepository
import com.proyecto.acis.data.remote.ApiClient
import com.proyecto.acis.data.remote.LoginRequest
import com.proyecto.acis.session.SesionActual
import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

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
            try {
                val respuesta = ApiClient.auth.login(
                    LoginRequest(identificador = correo.trim(), clave = clave),
                )
                val perfil = Perfil.valueOf(respuesta.rol)
                val cuenta = Usuario(
                    correo = respuesta.email,
                    nombre = respuesta.nombre,
                    perfil = perfil,
                )
                sesion.actualizar(cuenta, respuesta.token)
            } catch (error: HttpException) {
                _estado.value = _estado.value.copy(
                    error = if (error.code() == 401) {
                        "Correo o contraseña incorrectos."
                    } else {
                        "No se pudo completar el inicio de sesión (HTTP ${error.code()})."
                    },
                )
            } catch (_: IOException) {
                _estado.value = _estado.value.copy(
                    error = "No se pudo conectar con el gateway. Revisa que el túnel siga activo y que la URL sea la actual.",
                )
            } catch (_: JsonParseException) {
                _estado.value = _estado.value.copy(
                    error = "El gateway respondió con un formato que la app no reconoce.",
                )
            } catch (_: IllegalArgumentException) {
                _estado.value = _estado.value.copy(
                    error = "El rol recibido por la app no coincide con los roles configurados.",
                )
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
