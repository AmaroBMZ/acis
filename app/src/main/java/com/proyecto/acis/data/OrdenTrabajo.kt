package com.proyecto.acis.data

/**
 * Perfiles de usuario disponibles en la demostración local de ACIS.
 */
enum class Perfil(val etiqueta: String) {
    PLANIFICADOR("Planificador"),
    TECNICO("Técnico"),
    CLIENTE("Cliente"),
}

/**
 * Estados del ciclo de vida de una Orden de Trabajo.
 */
enum class EstadoOt(val etiqueta: String) {
    CREADA("Creada"),
    ASIGNADA("Asignada"),
    ACEPTADA("Aceptada"),
    EN_VISITA("En visita"),
    PENDIENTE_CONFORMIDAD("Pendiente de conformidad"),
    OBSERVADA("Observada"),
    CERRADA("Cerrada"),
}

/**
 * Tipo de intervención requerida.
 */
enum class TipoOt(val etiqueta: String) {
    SIN_ESPECIFICAR("Sin especificar"),
    PREVENTIVA("Mantención preventiva"),
    CORRECTIVA("Reparación correctiva"),
}

/**
 * Nivel de prioridad asignado a la OT.
 */
enum class PrioridadOt(val etiqueta: String) {
    SIN_ESPECIFICAR("Sin especificar"),
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta"),
}

/**
 * Modelo de datos que representa una Orden de Trabajo completa.
 */
data class OrdenTrabajo(
    // Identidad de la OT y datos del servicio solicitado por el planificador.
    val id: String,
    val local: String,
    val equipo: String,
    val descripcion: String,
    val fechaProgramada: String,
    // Enums: restringen los valores posibles y evitan estados escritos como texto libre.
    val tipo: TipoOt = TipoOt.SIN_ESPECIFICAR,
    val prioridad: PrioridadOt = PrioridadOt.SIN_ESPECIFICAR,
    val estado: EstadoOt = EstadoOt.CREADA,
    // Asignación y registro de llegada/salida; cadenas vacías representan datos aún no registrados.
    val tecnico: String = "",
    val checkIn: String = "",
    val ubicacionCheckIn: String = "",
    val checkOut: String = "",
    // Contenido del informe técnico: causa encontrada, trabajo realizado y materiales opcionales.
    val diagnostico: String = "",
    val actividad: String = "",
    val materiales: String = "",
    // Intercambio de revisión entre cliente y técnico, y fecha de aprobación final.
    val observacionCliente: String = "",
    val respuestaTecnico: String = "",
    val fechaConformidad: String = "",
    // Nombre del archivo en filesDir/evidencias; la imagen no se almacena dentro de SQLite.
    val fotoEvidencia: String = "",
    // Marca numérica usada para ordenar por modificación más reciente.
    val ultimaActualizacion: Long = 0L,
    // Las órdenes antiguas quedan sin cliente hasta que el planificador las asigne.
    val clienteCorreo: String = "",
)
