package com.proyecto.acis.data

enum class Perfil(val etiqueta: String) {
    PLANIFICADOR("Planificador"),
    TECNICO("Técnico"),
    CLIENTE("Cliente"),
}

enum class EstadoOt(val etiqueta: String) {
    CREADA("Creada"),
    ASIGNADA("Asignada"),
    ACEPTADA("Aceptada"),
    EN_VISITA("En visita"),
    PENDIENTE_CONFORMIDAD("Pendiente de conformidad"),
    OBSERVADA("Observada"),
    CERRADA("Cerrada"),
}

enum class TipoOt(val etiqueta: String) {
    SIN_ESPECIFICAR("Sin especificar"),
    PREVENTIVA("Mantención preventiva"),
    CORRECTIVA("Reparación correctiva"),
}

enum class PrioridadOt(val etiqueta: String) {
    SIN_ESPECIFICAR("Sin especificar"),
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta"),
}

data class OrdenTrabajo(
    val id: String,
    val local: String,
    val equipo: String,
    val descripcion: String,
    val fechaProgramada: String,
    val tipo: TipoOt = TipoOt.SIN_ESPECIFICAR,
    val prioridad: PrioridadOt = PrioridadOt.SIN_ESPECIFICAR,
    val estado: EstadoOt = EstadoOt.CREADA,
    val tecnico: String = "",
    val checkIn: String = "",
    val ubicacionCheckIn: String = "",
    val checkOut: String = "",
    val diagnostico: String = "",
    val actividad: String = "",
    val materiales: String = "",
    val observacionCliente: String = "",
    val respuestaTecnico: String = "",
    val fechaConformidad: String = "",
    val fotoEvidencia: String = "",
    val ultimaActualizacion: Long = 0L,
)
