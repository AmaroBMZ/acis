package com.proyecto.acis.data

import com.proyecto.acis.data.remote.ApiClient
import com.proyecto.acis.data.remote.EstadoOrdenRequest
import com.proyecto.acis.data.remote.OrdenApiDto
import com.proyecto.acis.data.remote.OrdenRequestApi
import com.proyecto.acis.data.remote.OrdenesApi

/** Acceso REST a ot-gestion; SQLite queda como caché local para la interfaz y el modo sin conexión. */
class OrdenesRemotoRepository(private val api: OrdenesApi = ApiClient.ordenes) {
    suspend fun listar(token: String): List<OrdenApiDto> = api.listar(bearer(token))

    suspend fun buscar(token: String, codigoOt: String): OrdenApiDto =
        api.buscar(bearer(token), codigoOt)

    suspend fun crear(
        token: String,
        clienteCorreo: String,
        local: String,
        equipo: String,
        descripcion: String,
        fechaProgramada: String,
        tipo: TipoOt,
        prioridad: PrioridadOt,
        tecnicoCorreo: String,
    ): OrdenApiDto = api.crear(
        bearer(token),
        OrdenRequestApi(clienteCorreo, local, equipo, descripcion, fechaProgramada,
            tipo.name, prioridad.name, tecnicoCorreo),
    )

    suspend fun actualizar(token: String, orden: OrdenTrabajo, tecnicoCorreo: String): OrdenApiDto =
        api.actualizar(
            bearer(token),
            orden.id,
            OrdenRequestApi(
                clienteCorreo = orden.clienteCorreo,
                local = orden.local,
                equipo = orden.equipo,
                descripcion = orden.descripcion,
                fechaProgramada = orden.fechaProgramada,
                tipo = orden.tipo.name,
                prioridad = orden.prioridad.name,
                tecnicoAsignado = tecnicoCorreo,
            ),
        )

    suspend fun cambiarEstado(
        token: String,
        codigoOt: String,
        estado: EstadoOt,
        observacionCliente: String? = null,
        respuestaTecnico: String? = null,
    ): OrdenApiDto = api.cambiarEstado(
        bearer(token), codigoOt,
        EstadoOrdenRequest(estado.name, observacionCliente, respuestaTecnico),
    )

    suspend fun borrar(token: String, codigoOt: String) = api.borrar(bearer(token), codigoOt)

    private fun bearer(token: String) = "Bearer $token"
}
