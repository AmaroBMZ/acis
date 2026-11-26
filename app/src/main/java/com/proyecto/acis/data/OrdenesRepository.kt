package com.proyecto.acis.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repositorio encargado de sincronizar el estado reactivo en memoria (StateFlow)
 * con la base de datos local SQLite y los archivos de fotos de evidencia.
 */
class OrdenesRepository(context: Context, private val database: OrdenesDb = OrdenesDb(context)) {
    // Almacén antiguo: se consulta únicamente para importar órdenes de versiones previas.
    private val preferences = context.getSharedPreferences("acis_ordenes_locales", Context.MODE_PRIVATE)
    private val fotos = File(context.filesDir, "evidencias")
    // Estado observable privado; asStateFlow expone lectura sin permitir modificaciones externas.
    private val _ordenes = MutableStateFlow<List<OrdenTrabajo>>(emptyList())
    val ordenes = _ordenes.asStateFlow()
    private val _usuarios = MutableStateFlow<List<Usuario>>(emptyList())
    // Publica identidades y accesos de solo lectura; la UI no modifica directamente esta lista.
    val usuarios = _usuarios.asStateFlow()

    // Las entradas suspendidas al final de esta clase ejecutan estas operaciones en Dispatchers.IO.
    private fun cargarInterno() {
        // Primero abre/inicializa la base; después publica las cuentas, incluida la migración del administrador.
        _ordenes.value = cargarOrdenes()
        _usuarios.value = database.leerUsuarios()
    }
    private fun autenticarInterno(correo: String, clave: String): Usuario? = database.autenticar(correo, clave)
    // Libera la conexión cuando termina el ViewModel y ya no quedan sus tareas pendientes.
    fun cerrar() = database.close()

    // Crea una nueva Orden de Trabajo
    private fun crearInterno(
        local: String,
        equipo: String,
        descripcion: String,
        fecha: String,
        tipo: TipoOt,
        prioridad: PrioridadOt,
        clienteCorreo: String,
    ): String? {
        if (listOf(local, equipo, descripcion, fecha).any { it.isBlank() }) return null
        if (tipo == TipoOt.SIN_ESPECIFICAR || prioridad == PrioridadOt.SIN_ESPECIFICAR) return null
        // Calcula el próximo identificador a partir del mayor número presente en la lista actual.
        val siguiente = (_ordenes.value.mapNotNull { it.id.removePrefix("OT-").toIntOrNull() }.maxOrNull() ?: 1000) + 1
        val id = "OT-$siguiente"
        val nueva = OrdenTrabajo(
            id = id,
            local = local.trim(),
            equipo = equipo.trim(),
            descripcion = descripcion.trim(),
            fechaProgramada = fecha.trim(),
            tipo = tipo,
            prioridad = prioridad,
            ultimaActualizacion = siguienteMarca(),
            clienteCorreo = clienteCorreo,
        )
        // Primero persiste; solo publica la nueva lista si SQLite confirma la inserción.
        if (!database.insertar(nueva)) return null
        _ordenes.value = _ordenes.value + nueva
        return id
    }

    // Actualiza los datos principales de una Orden de Trabajo existente (CRUD - Update)
    private fun actualizarDatosInterno(
        id: String,
        local: String,
        equipo: String,
        descripcion: String,
        fecha: String,
        tipo: TipoOt,
        prioridad: PrioridadOt,
        clienteCorreo: String,
    ): Boolean {
        val actual = _ordenes.value.firstOrNull { it.id == id } ?: return false
        // copy crea otra instancia inmutable con los campos modificados.
        val nueva = actual.copy(
            local = local.trim(),
            equipo = equipo.trim(),
            descripcion = descripcion.trim(),
            fechaProgramada = fecha.trim(),
            tipo = tipo,
            prioridad = prioridad,
            ultimaActualizacion = siguienteMarca(),
            clienteCorreo = clienteCorreo,
        )
        if (!database.actualizar(nueva, actual.estado)) return false
        _ordenes.value = _ordenes.value.map { if (it.id == id) nueva else it }
        return true
    }

    // Asigna la OT al técnico de demostración
    private fun asignarInterno(id: String): Boolean = cambiar(id, EstadoOt.CREADA) {
        it.copy(estado = EstadoOt.ASIGNADA, tecnico = TECNICO_DEMO)
    }

    // El técnico acepta la OT asignada
    private fun aceptarInterno(id: String): Boolean = cambiar(id, EstadoOt.ASIGNADA) {
        it.copy(estado = EstadoOt.ACEPTADA)
    }

    // Inicia la visita técnica, registra la fecha/hora de check-in y guarda la ubicación
    private fun iniciarVisitaInterno(id: String, ubicacion: String?): Boolean = cambiar(id, EstadoOt.ACEPTADA) {
        it.copy(estado = EstadoOt.EN_VISITA, checkIn = ahora(), ubicacionCheckIn = ubicacion.orEmpty())
    }

    // Guarda borrador local del trabajo realizado durante la visita
    private fun guardarBorradorInterno(id: String, diagnostico: String, actividad: String, materiales: String): Boolean =
        cambiar(id, EstadoOt.EN_VISITA) {
            it.copy(diagnostico = diagnostico.trim(), actividad = actividad.trim(), materiales = materiales.trim())
        }

    // Asocia a la OT un archivo ya creado por la cámara; aquí se guarda su nombre en SQLite.
    private fun guardarFotoInterno(id: String, nombre: String): Boolean {
        val archivo = File(fotos, nombre)
        // Acepta un nombre simple y exige un archivo existente con contenido.
        if (nombre != File(nombre).name || !archivo.isFile || archivo.length() == 0L) return false
        val anterior = _ordenes.value.firstOrNull { it.id == id }?.fotoEvidencia.orEmpty()
        val guardado = cambiar(id, EstadoOt.EN_VISITA) { it.copy(fotoEvidencia = nombre) }
        // Elimina la evidencia anterior únicamente después de guardar la referencia nueva.
        if (guardado && anterior.isNotBlank() && anterior != nombre) File(fotos, anterior).delete()
        return guardado
    }

    // Finaliza la visita técnica y registra la fecha/hora de check-out
    private fun enviarInformeInterno(id: String, diagnostico: String, actividad: String, materiales: String): Boolean {
        if (diagnostico.isBlank() || actividad.isBlank()) return false
        return cambiar(id, EstadoOt.EN_VISITA) {
            it.copy(
                estado = EstadoOt.PENDIENTE_CONFORMIDAD,
                diagnostico = diagnostico.trim(),
                actividad = actividad.trim(),
                materiales = materiales.trim(),
                checkOut = ahora(),
            )
        }
    }

    // Registra una observación formulada por el cliente
    private fun observarInterno(id: String, observacion: String): Boolean {
        if (observacion.isBlank()) return false
        return cambiar(id, EstadoOt.PENDIENTE_CONFORMIDAD) {
            it.copy(estado = EstadoOt.OBSERVADA, observacionCliente = observacion.trim())
        }
    }

    // Registra la respuesta del técnico a una observación del cliente
    private fun responderObservacionInterno(id: String, respuesta: String): Boolean {
        if (respuesta.isBlank()) return false
        return cambiar(id, EstadoOt.OBSERVADA) {
            it.copy(estado = EstadoOt.PENDIENTE_CONFORMIDAD, respuestaTecnico = respuesta.trim())
        }
    }

    // Aprueba la conformidad de la OT y la marca como CERRADA
    private fun aprobarInterno(id: String): Boolean = cambiar(id, EstadoOt.PENDIENTE_CONFORMIDAD) {
        it.copy(estado = EstadoOt.CERRADA, fechaConformidad = ahora())
    }

    // Elimina una Orden de Trabajo, borrando su archivo de foto y su registro en SQLite (CRUD - Delete)
    private fun borrarInterno(id: String): Boolean {
        val actual = _ordenes.value.firstOrNull { it.id == id } ?: return false
        if (!database.borrar(id)) return false
        if (actual.fotoEvidencia.isNotBlank()) {
            File(fotos, actual.fotoEvidencia).delete()
        }
        _ordenes.value = _ordenes.value.filterNot { it.id == id }
        return true
    }

    // Centraliza las transiciones: exige el estado de origen antes de aplicar la función cambio.
    private fun cambiar(id: String, estadoRequerido: EstadoOt, cambio: (OrdenTrabajo) -> OrdenTrabajo): Boolean {
        val actual = _ordenes.value.firstOrNull { it.id == id && it.estado == estadoRequerido } ?: return false
        // cambio recibe la OT actual y devuelve otra instancia; la marca permite ordenar la lista.
        val nueva = cambio(actual).copy(ultimaActualizacion = siguienteMarca())
        if (!database.actualizar(nueva, estadoRequerido)) return false
        // Publica solo después de persistir. SQLite y StateFlow no forman una única transacción.
        _ordenes.value = _ordenes.value.map { if (it.id == id) nueva else it }
        return true
    }

    // Carga las órdenes almacenadas en SQLite o las inicializa con datos de demostración
    private fun cargarOrdenes(): List<OrdenTrabajo> {
        val actuales = database.leer()
        // Si ya se inicializó, respeta incluso una base vacía tras eliminar todas las órdenes.
        if (database.estaInicializada()) return actuales
        if (actuales.isNotEmpty()) return actuales
        // Antes de crear ejemplos, intenta recuperar los registros del almacenamiento anterior.
        val anteriores = leerOrdenesAnteriores()
        if (anteriores.isNotEmpty()) {
            check(database.guardarTodas(anteriores)) { "No se pudieron importar las órdenes anteriores" }
            return anteriores
        }
        // Inicializar datos semilla para demostración interactiva
        val semillas = obtenerOrdenesSemilla()
        check(database.guardarTodas(semillas)) { "No se pudieron guardar las órdenes iniciales" }
        return semillas
    }

    // Genera una marca de tiempo secuencial para ordenamiento
    private fun siguienteMarca(): Long = maxOf(
        System.currentTimeMillis(),
        (_ordenes.value.maxOfOrNull { it.ultimaActualizacion } ?: 0L) + 1,
    )

    // Importación de una versión anterior: no es una copia de seguridad de los cambios actuales.
    private fun leerOrdenesAnteriores(): List<OrdenTrabajo> = try {
        val json = JSONArray(preferences.getString("ordenes", "[]"))
        (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            OrdenTrabajo(
                id = item.getString("id"),
                local = item.optString("local"),
                equipo = item.optString("equipo"),
                descripcion = item.optString("descripcion"),
                fechaProgramada = item.optString("fechaProgramada"),
                tipo = TipoOt.entries.firstOrNull { it.name == item.optString("tipo") }
                    ?: TipoOt.SIN_ESPECIFICAR,
                prioridad = PrioridadOt.entries.firstOrNull { it.name == item.optString("prioridad") }
                    ?: PrioridadOt.SIN_ESPECIFICAR,
                estado = EstadoOt.valueOf(item.getString("estado")),
                tecnico = item.optString("tecnico"),
                checkIn = item.optString("checkIn"),
                checkOut = item.optString("checkOut"),
                diagnostico = item.optString("diagnostico"),
                actividad = item.optString("actividad"),
                materiales = item.optString("materiales"),
                observacionCliente = item.optString("observacionCliente"),
                respuestaTecnico = item.optString("respuestaTecnico"),
                fechaConformidad = item.optString("fechaConformidad"),
            )
        }
    } catch (_: Exception) {
        // Si el JSON no se puede interpretar, la carga inicial continúa con una lista vacía.
        emptyList()
    }

    // Genera órdenes de ejemplo cubriendo cada estado del flujo de ACIS
    private fun obtenerOrdenesSemilla(): List<OrdenTrabajo> {
        val baseTime = System.currentTimeMillis()
        return listOf(
            OrdenTrabajo(
                clienteCorreo = CuentasPrueba.CLIENTE,
                id = "OT-1001",
                local = "Mall Plaza Vespucio · Local 210",
                equipo = "Rooftop Carrier 15 TR",
                descripcion = "Equipo no enfría adecuadamente la zona de cajas. Ruido inusual en compresor.",
                fechaProgramada = "28/10/2026 10:00",
                tipo = TipoOt.CORRECTIVA,
                prioridad = PrioridadOt.ALTA,
                estado = EstadoOt.CREADA,
                ultimaActualizacion = baseTime,
            ),
            OrdenTrabajo(
                clienteCorreo = CuentasPrueba.CLIENTE,
                id = "OT-1002",
                local = "Supermercado Unimarc · Providencia",
                equipo = "Cámara Frigorífica de Lácteos Bitzer",
                descripcion = "Mantención preventiva trimestral y medición de presión R404A.",
                fechaProgramada = "28/10/2026 11:30",
                tipo = TipoOt.PREVENTIVA,
                prioridad = PrioridadOt.MEDIA,
                estado = EstadoOt.ASIGNADA,
                tecnico = TECNICO_DEMO,
                ultimaActualizacion = baseTime + 1,
            ),
            OrdenTrabajo(
                clienteCorreo = CuentasPrueba.CLIENTE,
                id = "OT-1003",
                local = "Banco Chile · Ahumada 140",
                equipo = "Chiller Fancoil 30 TR",
                descripcion = "Revisión de fuga de refrigerante y limpieza de condensador.",
                fechaProgramada = "28/10/2026 14:00",
                tipo = TipoOt.CORRECTIVA,
                prioridad = PrioridadOt.ALTA,
                estado = EstadoOt.ACEPTADA,
                tecnico = TECNICO_DEMO,
                ultimaActualizacion = baseTime + 2,
            ),
            OrdenTrabajo(
                clienteCorreo = CuentasPrueba.CLIENTE,
                id = "OT-1004",
                local = "Clínica Indisa · Pabellón 3",
                equipo = "Unidad Manejadora de Aire (UMA) Trane",
                descripcion = "Calibración de termostato digital y cambio de correas de ventilador.",
                fechaProgramada = "28/10/2026 09:00",
                tipo = TipoOt.PREVENTIVA,
                prioridad = PrioridadOt.ALTA,
                estado = EstadoOt.EN_VISITA,
                tecnico = TECNICO_DEMO,
                checkIn = "28/10/2026 09:15",
                ubicacionCheckIn = "-33.4372, -70.6506",
                ultimaActualizacion = baseTime + 3,
            ),
            OrdenTrabajo(
                clienteCorreo = CuentasPrueba.CLIENTE,
                id = "OT-1005",
                local = "Restaurante La Mar · Vitacura",
                equipo = "Visicooler Industrial Mantenimiento",
                descripcion = "Carga de gas refrigerante R134a y limpieza de evaporador.",
                fechaProgramada = "27/10/2026 16:00",
                tipo = TipoOt.CORRECTIVA,
                prioridad = PrioridadOt.MEDIA,
                estado = EstadoOt.PENDIENTE_CONFORMIDAD,
                tecnico = TECNICO_DEMO,
                checkIn = "27/10/2026 16:10",
                checkOut = "27/10/2026 18:00",
                diagnostico = "Presión baja por pequeña fuga en válvula de servicio.",
                actividad = "Se reemplazó válvula Schrader, se presurizó y realizó carga completa de gas R134a.",
                materiales = "1x Válvula Schrader, 2.5 kg Gas R134a",
                ultimaActualizacion = baseTime + 4,
            ),
            OrdenTrabajo(
                clienteCorreo = CuentasPrueba.CLIENTE,
                id = "OT-1006",
                local = "Centro Logístico Pudahuel · Bodega 5",
                equipo = "Condensador Exterior Evaporativo 50 TR",
                descripcion = "Limpieza química de serpentines exteriores.",
                fechaProgramada = "26/10/2026 15:00",
                tipo = TipoOt.PREVENTIVA,
                prioridad = PrioridadOt.BAJA,
                estado = EstadoOt.OBSERVADA,
                tecnico = TECNICO_DEMO,
                checkIn = "26/10/2026 15:10",
                checkOut = "26/10/2026 17:00",
                diagnostico = "Serpentín obstruido por polvo y suciedad ambiental.",
                actividad = "Lavado con hidrolavadora y producto desincrustante.",
                materiales = "2L Limpiador alcalino",
                observacionCliente = "El equipo funciona bien, pero falta adjuntar la medición de consumo eléctrico (amperaje).",
                ultimaActualizacion = baseTime + 5,
            ),
            OrdenTrabajo(
                clienteCorreo = CuentasPrueba.CLIENTE,
                id = "OT-1007",
                local = "Oficinas Enel · Torre B Piso 4",
                equipo = "Sistema VRV Daikin Mantenimiento Anual",
                descripcion = "Mantenimiento integral anual del sistema de climatización.",
                fechaProgramada = "25/10/2026 09:00",
                tipo = TipoOt.PREVENTIVA,
                prioridad = PrioridadOt.BAJA,
                estado = EstadoOt.CERRADA,
                tecnico = TECNICO_DEMO,
                checkIn = "25/10/2026 09:10",
                checkOut = "25/10/2026 13:00",
                diagnostico = "Unidades interiores y exterior en óptimo estado operacional.",
                actividad = "Limpieza de filtros, verificación de presiones y reapriete de conexiones eléctricas.",
                materiales = "Filtros lavables, limpiador dieléctrico",
                fechaConformidad = "25/10/2026 13:15",
                ultimaActualizacion = baseTime + 6,
            ),
        )
    }

    companion object {
        const val TECNICO_DEMO = "Técnico demo"

        // Formatea la fecha y hora actual en zona horaria local
        private fun ahora(): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CL"))
            .format(Date())
    }
    // IO evita bloquear Compose; Mutex serializa lecturas/modificaciones de la lista y SQLite.
    private val mutex = Mutex()
    // Función genérica: devuelve el resultado de la acción, ejecutada en IO y protegida por Mutex.
    private suspend fun <T> enIo(accion: () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock { accion() }
    }
    // API suspendida del repositorio: todas estas entradas pasan por enIo para serializar el acceso.
    suspend fun cargar() = enIo { cargarInterno() }
    suspend fun registrarUsuario(actor: String, nombre: String, correo: String, perfil: Perfil, clave: String) = enIo {
        val resultado = database.registrarUsuario(actor, nombre, correo, perfil, clave)
        if (resultado == com.proyecto.acis.domain.ResultadoRegistro.CREADO) _usuarios.value = database.leerUsuarios()
        resultado
    }
    suspend fun actualizarAcceso(actorCorreo: String, correo: String, perfil: Perfil, activo: Boolean): Boolean = enIo {
        // enIo ejecuta fuera del hilo de la interfaz y serializa el acceso mediante Mutex.
        val guardado = database.actualizarAcceso(actorCorreo, correo, perfil, activo)
        // Recarga solo tras confirmar la escritura para no mostrar permisos que no llegaron a guardarse.
        if (guardado) _usuarios.value = database.leerUsuarios()
        guardado
    }
    suspend fun autenticar(correo: String, clave: String) = enIo { autenticarInterno(correo, clave) }
    suspend fun crear(local: String, equipo: String, descripcion: String, fecha: String, tipo: TipoOt, prioridad: PrioridadOt, clienteCorreo: String) =
        enIo { crearInterno(local, equipo, descripcion, fecha, tipo, prioridad, clienteCorreo) }
    suspend fun actualizarDatos(id: String, local: String, equipo: String, descripcion: String, fecha: String, tipo: TipoOt, prioridad: PrioridadOt, clienteCorreo: String) =
        enIo { actualizarDatosInterno(id, local, equipo, descripcion, fecha, tipo, prioridad, clienteCorreo) }
    suspend fun asignar(id: String) = enIo { asignarInterno(id) }
    suspend fun aceptar(id: String) = enIo { aceptarInterno(id) }
    suspend fun iniciarVisita(id: String, ubicacion: String?) = enIo { iniciarVisitaInterno(id, ubicacion) }
    suspend fun guardarBorrador(id: String, diagnostico: String, actividad: String, materiales: String) =
        enIo { guardarBorradorInterno(id, diagnostico, actividad, materiales) }
    suspend fun guardarFoto(id: String, nombre: String) = enIo { guardarFotoInterno(id, nombre) }
    suspend fun enviarInforme(id: String, diagnostico: String, actividad: String, materiales: String) =
        enIo { enviarInformeInterno(id, diagnostico, actividad, materiales) }
    suspend fun observar(id: String, observacion: String) = enIo { observarInterno(id, observacion) }
    suspend fun responderObservacion(id: String, respuesta: String) = enIo { responderObservacionInterno(id, respuesta) }
    suspend fun aprobar(id: String) = enIo { aprobarInterno(id) }
    suspend fun borrar(id: String) = enIo { borrarInterno(id) }
}
