package com.proyecto.acis.session

import com.proyecto.acis.data.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Una única identidad en memoria, compartida por acceso y permisos de las órdenes. */
class SesionActual {
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario = _usuario.asStateFlow()

    // Solo la coordinación de sesión cambia la identidad; los consumidores observan el flujo.
    internal fun actualizar(usuario: Usuario?) { _usuario.value = usuario }
}
