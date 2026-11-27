package com.proyecto.acis.data.remote


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val auth: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val ordenes: OrdenesApi by lazy { retrofit.create(OrdenesApi::class.java) }
    val intervenciones: IntervencionesApi by lazy { retrofit.create(IntervencionesApi::class.java) }
}
