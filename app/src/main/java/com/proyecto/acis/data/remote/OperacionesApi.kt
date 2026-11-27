package com.proyecto.acis.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Part

data class OrdenRequestApi(
    val clienteCorreo: String,
    val local: String,
    val equipo: String,
    val descripcion: String,
    val fechaProgramada: String,
    val tipo: String,
    val prioridad: String,
    val tecnicoAsignado: String?,
)

data class OrdenApiDto(
    val codigoOt: String,
    val clienteCorreo: String,
    val local: String,
    val equipo: String,
    val descripcion: String,
    val fechaProgramada: String,
    val tipo: String,
    val prioridad: String,
    val estado: String,
    val tecnicoAsignado: String? = null,
    val checkIn: String? = null,
    val ubicacionCheckIn: String? = null,
    val checkOut: String? = null,
    val diagnostico: String? = null,
    val actividad: String? = null,
    val materiales: String? = null,
    val observacionCliente: String? = null,
    val respuestaTecnico: String? = null,
    val fechaConformidad: String? = null,
    val fotoEvidencia: String? = null,
    val ultimaActualizacion: Long? = null,
)

fun OrdenApiDto.aDominio() = com.proyecto.acis.data.OrdenTrabajo(
    id = codigoOt,
    local = local,
    equipo = equipo,
    descripcion = descripcion,
    fechaProgramada = fechaProgramada,
    tipo = com.proyecto.acis.data.TipoOt.valueOf(tipo),
    prioridad = com.proyecto.acis.data.PrioridadOt.valueOf(prioridad),
    estado = com.proyecto.acis.data.EstadoOt.valueOf(estado),
    tecnico = tecnicoAsignado.orEmpty(),
    checkIn = checkIn.orEmpty(),
    ubicacionCheckIn = ubicacionCheckIn.orEmpty(),
    checkOut = checkOut.orEmpty(),
    diagnostico = diagnostico.orEmpty(),
    actividad = actividad.orEmpty(),
    materiales = materiales.orEmpty(),
    observacionCliente = observacionCliente.orEmpty(),
    respuestaTecnico = respuestaTecnico.orEmpty(),
    fechaConformidad = fechaConformidad.orEmpty(),
    fotoEvidencia = fotoEvidencia.orEmpty(),
    ultimaActualizacion = ultimaActualizacion ?: 0L,
    clienteCorreo = clienteCorreo,
)

data class EstadoOrdenRequest(
    val estado: String,
    val observacionCliente: String? = null,
    val respuestaTecnico: String? = null,
)
data class AsignacionOrdenRequest(val tecnicoAsignado: String)
data class CheckInApiRequest(val codigoOt: String, val ubicacionCheckIn: String?)
data class InformeApiRequest(
    val codigoOt: String,
    val diagnostico: String,
    val actividad: String,
    val materiales: String,
)
data class IntervencionApiResponse(val codigoOt: String, val fotoEvidencia: String? = null)

interface OrdenesApi {
    @GET("api/ordenes")
    suspend fun listar(@Header("Authorization") authorization: String): List<OrdenApiDto>

    @GET("api/ordenes/{codigoOt}")
    suspend fun buscar(
        @Header("Authorization") authorization: String,
        @Path("codigoOt") codigoOt: String,
    ): OrdenApiDto

    @POST("api/ordenes")
    suspend fun crear(
        @Header("Authorization") authorization: String,
        @Body request: OrdenRequestApi,
    ): OrdenApiDto

    @PUT("api/ordenes/{codigoOt}")
    suspend fun actualizar(
        @Header("Authorization") authorization: String,
        @Path("codigoOt") codigoOt: String,
        @Body request: OrdenRequestApi,
    ): OrdenApiDto

    @PUT("api/ordenes/{codigoOt}/asignacion")
    suspend fun asignar(
        @Header("Authorization") authorization: String,
        @Path("codigoOt") codigoOt: String,
        @Body request: AsignacionOrdenRequest,
    )

    @PATCH("api/ordenes/{codigoOt}/estado")
    suspend fun cambiarEstado(
        @Header("Authorization") authorization: String,
        @Path("codigoOt") codigoOt: String,
        @Body request: EstadoOrdenRequest,
    ): OrdenApiDto

    @DELETE("api/ordenes/{codigoOt}")
    suspend fun borrar(
        @Header("Authorization") authorization: String,
        @Path("codigoOt") codigoOt: String,
    )
}

interface IntervencionesApi {
    @POST("api/intervenciones/check-in")
    suspend fun checkIn(
        @Header("Authorization") authorization: String,
        @Body request: CheckInApiRequest,
    ): IntervencionApiResponse

    @POST("api/intervenciones/informe")
    suspend fun informe(
        @Header("Authorization") authorization: String,
        @Body request: InformeApiRequest,
    ): IntervencionApiResponse

    @Multipart
    @POST("api/intervenciones/{codigoOt}/foto")
    suspend fun subirFoto(
        @Header("Authorization") authorization: String,
        @Path("codigoOt") codigoOt: String,
        @Part file: MultipartBody.Part,
    ): IntervencionApiResponse
}
