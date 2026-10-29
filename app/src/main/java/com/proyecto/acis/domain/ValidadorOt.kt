package com.proyecto.acis.domain

import com.proyecto.acis.data.PrioridadOt
import com.proyecto.acis.data.TipoOt

/**
 * Datos requeridos para registrar una nueva Orden de Trabajo.
 */
data class DatosCrearOt(
    val local: String = "",
    val equipo: String = "",
    val descripcion: String = "",
    val tipo: TipoOt? = null,
    val prioridad: PrioridadOt? = null,
    val fecha: String = "",
    val hora: String = "",
)

/**
 * Representa los mensajes de error asociados a cada campo del formulario de creación.
 */
data class ErroresCrearOt(
    val local: String? = null,
    val equipo: String? = null,
    val descripcion: String? = null,
    val tipo: String? = null,
    val prioridad: String? = null,
    val fecha: String? = null,
    val hora: String? = null,
) {
    // Retorna true si al menos un campo tiene un mensaje de error
    val hayErrores: Boolean
        get() = listOf(local, equipo, descripcion, tipo, prioridad, fecha, hora).any { it != null }
}

/**
 * Mensajes de error para los campos obligatorios del informe técnico.
 */
data class ErroresInforme(
    val diagnostico: String? = null,
    val actividad: String? = null,
) {
    // Indica si falta completar diagnóstico o actividad
    val hayErrores: Boolean get() = diagnostico != null || actividad != null
}

/**
 * Objeto de dominio que contiene las reglas de validación para los formularios de ACIS.
 */
object ValidadorOt {
    // Valida que todos los campos del formulario de creación de OT estén completos
    fun crear(datos: DatosCrearOt) = ErroresCrearOt(
        local = if (datos.local.isBlank()) "Ingresa el local" else null,
        equipo = if (datos.equipo.isBlank()) "Ingresa el equipo" else null,
        descripcion = if (datos.descripcion.isBlank()) "Describe el trabajo" else null,
        tipo = if (datos.tipo == null || datos.tipo == TipoOt.SIN_ESPECIFICAR) "Selecciona un tipo" else null,
        prioridad = if (datos.prioridad == null || datos.prioridad == PrioridadOt.SIN_ESPECIFICAR) "Selecciona una prioridad" else null,
        fecha = if (datos.fecha.isBlank()) "Selecciona una fecha" else null,
        hora = if (datos.hora.isBlank()) "Selecciona una hora" else null,
    )

    // Valida que el informe técnico contenga diagnóstico y actividad realizada
    fun informe(diagnostico: String, actividad: String) = ErroresInforme(
        diagnostico = if (diagnostico.isBlank()) "Ingresa el diagnóstico" else null,
        actividad = if (actividad.isBlank()) "Ingresa la actividad" else null,
    )

    // Valida que la observación del cliente no esté vacía
    fun observacion(texto: String): String? = if (texto.isBlank()) "Escribe una observación" else null

    // Valida que la respuesta del técnico a una observación no esté vacía
    fun respuesta(texto: String): String? = if (texto.isBlank()) "Escribe una respuesta" else null
}
