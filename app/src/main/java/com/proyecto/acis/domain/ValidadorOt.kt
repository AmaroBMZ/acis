package com.proyecto.acis.domain

import com.proyecto.acis.data.PrioridadOt
import com.proyecto.acis.data.TipoOt

data class DatosCrearOt(
    val local: String = "",
    val equipo: String = "",
    val descripcion: String = "",
    val tipo: TipoOt? = null,
    val prioridad: PrioridadOt? = null,
    val fecha: String = "",
    val hora: String = "",
)

data class ErroresCrearOt(
    val local: String? = null,
    val equipo: String? = null,
    val descripcion: String? = null,
    val tipo: String? = null,
    val prioridad: String? = null,
    val fecha: String? = null,
    val hora: String? = null,
) {
    val hayErrores: Boolean
        get() = listOf(local, equipo, descripcion, tipo, prioridad, fecha, hora).any { it != null }
}

data class ErroresInforme(
    val diagnostico: String? = null,
    val actividad: String? = null,
) {
    val hayErrores: Boolean get() = diagnostico != null || actividad != null
}

object ValidadorOt {
    fun crear(datos: DatosCrearOt) = ErroresCrearOt(
        local = if (datos.local.isBlank()) "Ingresa el local" else null,
        equipo = if (datos.equipo.isBlank()) "Ingresa el equipo" else null,
        descripcion = if (datos.descripcion.isBlank()) "Describe el trabajo" else null,
        tipo = if (datos.tipo == null || datos.tipo == TipoOt.SIN_ESPECIFICAR) "Selecciona un tipo" else null,
        prioridad = if (datos.prioridad == null || datos.prioridad == PrioridadOt.SIN_ESPECIFICAR) "Selecciona una prioridad" else null,
        fecha = if (datos.fecha.isBlank()) "Selecciona una fecha" else null,
        hora = if (datos.hora.isBlank()) "Selecciona una hora" else null,
    )

    fun informe(diagnostico: String, actividad: String) = ErroresInforme(
        diagnostico = if (diagnostico.isBlank()) "Ingresa el diagnóstico" else null,
        actividad = if (actividad.isBlank()) "Ingresa la actividad" else null,
    )

    fun observacion(texto: String): String? = if (texto.isBlank()) "Escribe una observación" else null

    fun respuesta(texto: String): String? = if (texto.isBlank()) "Escribe una respuesta" else null
}
