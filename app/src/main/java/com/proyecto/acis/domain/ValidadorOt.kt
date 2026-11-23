package com.proyecto.acis.domain

import com.proyecto.acis.data.PrioridadOt
import com.proyecto.acis.data.TipoOt
import com.proyecto.acis.data.CuentasPrueba
import com.proyecto.acis.data.Perfil
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Locale

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
    val clienteCorreo: String = CuentasPrueba.CLIENTE,
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
    val clienteCorreo: String? = null,
) {
    // Retorna true si al menos un campo tiene un mensaje de error
    val hayErrores: Boolean
        get() = listOf(local, equipo, descripcion, tipo, prioridad, fecha, hora, clienteCorreo).any { it != null }
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
    // Devuelve mensajes por campo; null significa que ese campo cumple su regla.
    // El ViewModel pasa los correos de clientes activos de SQLite. El valor por defecto usa las demos.
    fun crear(datos: DatosCrearOt, clientes: List<String> = CuentasPrueba.usuarios.filter { it.perfil == Perfil.CLIENTE }.map { it.correo }) = ErroresCrearOt(
        local = if (datos.local.isBlank()) "Ingresa el local" else null,
        equipo = if (datos.equipo.isBlank()) "Ingresa el equipo" else null,
        descripcion = if (datos.descripcion.isBlank()) "Describe el trabajo" else null,
        tipo = if (datos.tipo == null || datos.tipo == TipoOt.SIN_ESPECIFICAR) "Selecciona un tipo" else null,
        prioridad = if (datos.prioridad == null || datos.prioridad == PrioridadOt.SIN_ESPECIFICAR) "Selecciona una prioridad" else null,
        fecha = if (!fechaValida(datos.fecha)) "Selecciona una fecha válida (dd/MM/yyyy)" else null,
        hora = if (!Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(datos.hora)) "Selecciona una hora válida (HH:mm)" else null,
        clienteCorreo = if (datos.clienteCorreo !in clientes) "Selecciona un cliente activo" else null,
    )

    // Comprueba formato exacto y calendario real; no exige que la fecha esté en el futuro.
    private fun fechaValida(valor: String): Boolean {
        if (!Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}").matches(valor)) return false
        // isLenient = false impide ajustar fechas imposibles, como convertir 31 de febrero a marzo.
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply { isLenient = false }
        val posicion = ParsePosition(0)
        // Exige consumir todo el texto para rechazar caracteres adicionales al final.
        return formato.parse(valor, posicion) != null && posicion.index == valor.length
    }

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
