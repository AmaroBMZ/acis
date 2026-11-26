package com.proyecto.acis.data.remote


import retrofit2.http.Body
import retrofit2.http.POST

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

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}