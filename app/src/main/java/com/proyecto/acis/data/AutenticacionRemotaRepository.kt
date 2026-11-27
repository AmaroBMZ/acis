package com.proyecto.acis.data

import com.proyecto.acis.data.remote.ApiClient
import com.proyecto.acis.data.remote.AuthApi
import com.proyecto.acis.data.remote.LoginRequest
import com.proyecto.acis.domain.AutenticacionRepository
import com.proyecto.acis.domain.SesionAutenticada

/** Adaptador de autenticación contra auth-users a partir del contrato de dominio. */
class AutenticacionRemotaRepository(
    private val authApi: AuthApi = ApiClient.auth,
) : AutenticacionRepository {
    // El acceso remoto no requiere precargar usuarios ni contraseñas en el dispositivo.
    override suspend fun preparar() = Unit

    override suspend fun autenticar(correo: String, clave: String): SesionAutenticada {
        val respuesta = authApi.login(
            LoginRequest(identificador = correo.trim(), clave = clave),
        )
        return SesionAutenticada(
            usuario = Usuario(
                correo = respuesta.email,
                nombre = respuesta.nombre,
                perfil = Perfil.valueOf(respuesta.rol),
            ),
            token = respuesta.token,
        )
    }
}
