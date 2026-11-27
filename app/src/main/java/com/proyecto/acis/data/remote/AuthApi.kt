package com.proyecto.acis.data.remote


import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class LoginRequest(
    val identificador: String,
    val clave: String,
)

data class LoginResponse(
    val token: String,
    val email: String,
    val nombre: String,
    val rol: String,
)

data class UsuarioApiDto(
    val id: Long,
    val rut: String,
    val nombre: String,
    val email: String,
    val rol: String,
    val activo: Boolean,
)

data class RegistroUsuarioRequest(
    val rut: String,
    val nombre: String,
    val email: String,
    val clave: String,
    val rol: String,
)

data class ActualizarUsuarioRequest(
    val rut: String,
    val nombre: String,
    val email: String,
    val rol: String,
    val activo: Boolean,
)

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/register")
    suspend fun registrar(
        @Header("Authorization") authorization: String,
        @Body request: RegistroUsuarioRequest,
    ): UsuarioApiDto

    @GET("api/usuarios")
    suspend fun usuarios(@Header("Authorization") authorization: String): List<UsuarioApiDto>

    @PUT("api/usuarios/{id}")
    suspend fun actualizarUsuario(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body request: ActualizarUsuarioRequest,
    ): UsuarioApiDto
}
