package com.proyecto.acis.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.proyecto.acis.data.Usuario
import com.proyecto.acis.domain.PermisosOt
import com.proyecto.acis.data.EstadoOt
import com.proyecto.acis.data.OrdenTrabajo
import com.proyecto.acis.data.OrdenesRepository
import com.proyecto.acis.data.Perfil
import com.proyecto.acis.data.PrioridadOt
import com.proyecto.acis.data.TipoOt
import com.proyecto.acis.domain.DatosCrearOt
import com.proyecto.acis.domain.ErroresCrearOt
import com.proyecto.acis.domain.ErroresInforme
import com.proyecto.acis.domain.ValidadorOt

/**
 * ViewModel que gestiona el estado de la UI y coordina las acciones del usuario
 * con la capa de repositorio y validación de dominio.
 */
class OrdenesViewModel(application: Application) : AndroidViewModel(application) {
    // Repositorio que maneja la persistencia de las OTs
    private val repository = OrdenesRepository(application)
    val ordenes = repository.ordenes

    // El perfil procede de la cuenta autenticada.
    val perfil: Perfil get() = usuario?.perfil ?: Perfil.PLANIFICADOR

    // Estado con los datos ingresados en el formulario de creación o edición
    var datosCrear by mutableStateOf(DatosCrearOt())
        private set

    // Indica si ocurrió un error al intentar guardar la OT
    var errorGuardarCrear by mutableStateOf(false)
        private set

    // Activa mensajes después de intentar guardar; los errores de detalle se vinculan al ID de la OT.
    private var mostrarErroresCrear by mutableStateOf(false)
    private var ordenInformeConErrores by mutableStateOf<String?>(null)
    private var ordenObservacionConError by mutableStateOf<String?>(null)
    private var ordenRespuestaConError by mutableStateOf<String?>(null)
    private var mensajeDetalle by mutableStateOf<Pair<String, String>?>(null)

    // Almacena los borradores de formularios de detalle por cada OT
    private val formulariosDetalle = mutableStateMapOf<String, FormularioDetalle>()

    /**
     * Estructura temporal para los datos del informe técnico y observaciones.
     */
    data class FormularioDetalle(
        val diagnostico: String = "",
        val actividad: String = "",
        val materiales: String = "",
        val observacion: String = "",
        val respuesta: String = "",
    )

    // Obtiene o crea el borrador local para una OT específica
    fun formularioDetalle(orden: OrdenTrabajo): FormularioDetalle =
        formulariosDetalle.getOrPut(orden.id) {
            FormularioDetalle(
                diagnostico = orden.diagnostico,
                actividad = orden.actividad,
                materiales = orden.materiales,
            )
        }

    // Funciones para actualizar los campos del informe técnico
    fun cambiarDiagnostico(id: String, valor: String) = cambiarDetalle(id) { it.copy(diagnostico = valor) }
    fun cambiarActividad(id: String, valor: String) = cambiarDetalle(id) { it.copy(actividad = valor) }
    fun cambiarMateriales(id: String, valor: String) = cambiarDetalle(id) { it.copy(materiales = valor) }
    fun cambiarObservacion(id: String, valor: String) = cambiarDetalle(id) { it.copy(observacion = valor) }
    fun cambiarRespuesta(id: String, valor: String) = cambiarDetalle(id) { it.copy(respuesta = valor) }

    // Actualiza solo un borrador ya creado, aplicando la transformación recibida como función.
    private fun cambiarDetalle(id: String, cambio: (FormularioDetalle) -> FormularioDetalle) {
        formulariosDetalle[id]?.let { formulariosDetalle[id] = cambio(it) }
    }

    // Obtiene los errores de validación del formulario de creación
    val erroresCrear: ErroresCrearOt
        get() = if (mostrarErroresCrear) ValidadorOt.crear(datosCrear) else ErroresCrearOt()

    // Obtiene errores de validación del informe técnico
    fun erroresInforme(id: String, diagnostico: String, actividad: String): ErroresInforme =
        if (ordenInformeConErrores == id) ValidadorOt.informe(diagnostico, actividad) else ErroresInforme()

    // Obtiene mensaje de error para observaciones del cliente
    fun errorObservacion(id: String, texto: String): String? =
        if (ordenObservacionConError == id) ValidadorOt.observacion(texto) else null

    // Obtiene mensaje de error para respuestas del técnico
    fun errorRespuesta(id: String, texto: String): String? =
        if (ordenRespuestaConError == id) ValidadorOt.respuesta(texto) else null

    // Retorna un mensaje informativo de éxito o fallo para la vista de detalle
    fun mensaje(ordenId: String): String = mensajeDetalle?.takeIf { it.first == ordenId }?.second.orEmpty()

    // Asocia la orden al cliente seleccionado por el planificador.
    fun cambiarCliente(valor: String) { datosCrear = datosCrear.copy(clienteCorreo = valor) }

    // Actualiza campos individuales del formulario de creación/edición
    fun cambiarLocal(valor: String) { datosCrear = datosCrear.copy(local = valor) }
    fun cambiarEquipo(valor: String) { datosCrear = datosCrear.copy(equipo = valor) }
    fun cambiarDescripcion(valor: String) { datosCrear = datosCrear.copy(descripcion = valor) }
    fun cambiarTipo(valor: TipoOt) { datosCrear = datosCrear.copy(tipo = valor) }
    fun cambiarPrioridad(valor: PrioridadOt) { datosCrear = datosCrear.copy(prioridad = valor) }
    fun cambiarFecha(valor: String) { datosCrear = datosCrear.copy(fecha = valor) }
    fun cambiarHora(valor: String) { datosCrear = datosCrear.copy(hora = valor) }

    // Carga los datos de una OT existente en el formulario para su edición (CRUD - Update)
    fun cargarParaEditar(orden: OrdenTrabajo) {
        val partesFecha = orden.fechaProgramada.split(" ")
        datosCrear = DatosCrearOt(
            local = orden.local,
            equipo = orden.equipo,
            descripcion = orden.descripcion,
            tipo = orden.tipo,
            prioridad = orden.prioridad,
            fecha = partesFecha.getOrNull(0).orEmpty(),
            hora = partesFecha.getOrNull(1).orEmpty(),
            clienteCorreo = orden.clienteCorreo,
        )
        mostrarErroresCrear = false
    }

    // Actualiza los datos de una OT existente (CRUD - Update)
    private suspend fun actualizarOtInterno(id: String): Boolean {
        mostrarErroresCrear = true
        val datos = datosCrear
        if (ValidadorOt.crear(datos).hayErrores) return false
        val tipo = datos.tipo ?: return false
        val prioridad = datos.prioridad ?: return false
        val actualizado = repository.actualizarDatos(
            id, datos.local, datos.equipo, datos.descripcion,
            "${datos.fecha} ${datos.hora}", tipo, prioridad, datos.clienteCorreo,
        )
        if (actualizado) {
            datosCrear = DatosCrearOt()
            mostrarErroresCrear = false
        }
        return actualizado
    }

    // Intenta crear y guardar una nueva Orden de Trabajo (CRUD - Create)
    private suspend fun crearInterno(): String? {
        mostrarErroresCrear = true
        errorGuardarCrear = false
        val datos = datosCrear
        if (ValidadorOt.crear(datos).hayErrores) return null
        val tipo = datos.tipo ?: return null
        val prioridad = datos.prioridad ?: return null
        val id = repository.crear(
            datos.local, datos.equipo, datos.descripcion,
            "${datos.fecha} ${datos.hora}", tipo, prioridad, datos.clienteCorreo,
        )
        if (id != null) {
            datosCrear = DatosCrearOt()
            mostrarErroresCrear = false
        } else {
            errorGuardarCrear = true
        }
        return id
    }

    // Filtra las órdenes según las reglas de visibilidad del perfil activo
    fun ordenesParaPerfil(lista: List<OrdenTrabajo>, perfilVisible: Perfil): List<OrdenTrabajo> =
        lista.filter { orden ->
            perfilVisible == usuario?.perfil && PermisosOt.puedeVer(usuario, orden)
        }.sortedWith(
            compareByDescending<OrdenTrabajo> { it.ultimaActualizacion }
                .thenByDescending { it.id.removePrefix("OT-").toIntOrNull() ?: 0 },
        )

    // Acciones del flujo de vida de la OT
    private suspend fun asignarInterno(id: String) = repository.asignar(id)
    private suspend fun aceptarInterno(id: String) = repository.aceptar(id)

    // Inicia la visita del técnico y guarda la ubicación obtenida
    private suspend fun iniciarVisitaInterno(id: String, ubicacion: String?) {
        val guardado = repository.iniciarVisita(id, ubicacion)
        mensajeDetalle = id to when {
            !guardado -> "No se pudo iniciar la visita."
            ubicacion == null -> "Check-in registrado sin ubicación."
            else -> "Check-in registrado con ubicación."
        }
    }

    private suspend fun aprobarInterno(id: String) = repository.aprobar(id)

    // Elimina la orden de trabajo seleccionada (CRUD - Delete)
    private suspend fun borrarInterno(id: String): Boolean {
        val borrado = repository.borrar(id)
        mensajeDetalle = id to if (borrado) "Orden eliminada." else "No se pudo eliminar la orden."
        return borrado
    }

    // Guarda borrador de avance del informe técnico sin cambiar el estado
    private suspend fun guardarBorradorInterno(id: String, diagnostico: String, actividad: String, materiales: String) {
        val guardado = repository.guardarBorrador(id, diagnostico, actividad, materiales)
        mensajeDetalle = id to if (guardado) "Borrador guardado en este dispositivo."
        else "No se pudo guardar el borrador."
    }

    // Asocia la foto capturada a la OT
    private suspend fun guardarFotoInterno(id: String, nombre: String): Boolean {
        val guardado = repository.guardarFoto(id, nombre)
        mensajeDetalle = id to if (guardado) "Foto guardada en esta OT."
        else "No se pudo guardar la foto."
        return guardado
    }

    // Valida y finaliza la visita técnica
    private suspend fun finalizarVisitaInterno(id: String, diagnostico: String, actividad: String, materiales: String) {
        ordenInformeConErrores = id
        if (ValidadorOt.informe(diagnostico, actividad).hayErrores) {
            mensajeDetalle = null
            return
        }
        val guardado = repository.enviarInforme(id, diagnostico, actividad, materiales)
        mensajeDetalle = id to if (guardado) "Informe disponible para revisión en este dispositivo."
        else "No se pudo finalizar la visita."
        if (guardado) ordenInformeConErrores = null
    }

    // Registra una observación formulada por el cliente
    private suspend fun observarInterno(id: String, observacion: String) {
        ordenObservacionConError = id
        if (ValidadorOt.observacion(observacion) != null) {
            mensajeDetalle = null
            return
        }
        val guardado = repository.observar(id, observacion)
        mensajeDetalle = id to if (guardado) "Observación registrada." else "No se pudo registrar la observación."
        if (guardado) ordenObservacionConError = null
    }

    // Envía la respuesta técnica a una observación del cliente
    private suspend fun responderObservacionInterno(id: String, respuesta: String) {
        ordenRespuestaConError = id
        if (ValidadorOt.respuesta(respuesta) != null) {
            mensajeDetalle = null
            return
        }
        val guardado = repository.responderObservacion(id, respuesta)
        mensajeDetalle = id to if (guardado) "Respuesta registrada; el cliente puede revisar nuevamente."
        else "No se pudo enviar la respuesta."
        if (guardado) ordenRespuestaConError = null
    }
    // Estado de sesión observable; private set permite leer desde la UI, pero escribir solo aquí.
    var usuario by mutableStateOf<Usuario?>(null)
        private set
    // Evita lanzar otra operación mientras una acción del ViewModel sigue en curso.
    var ocupado by mutableStateOf(false)
        private set
    // Distingue una carga inicial completada de una lista legítimamente vacía.
    var cargado by mutableStateOf(false)
        private set
    // Mensaje compartido para fallos de carga, autenticación o acciones.
    var errorGeneral by mutableStateOf<String?>(null)
        private set

    init {
        // Cierra SQLite al terminar el ViewModel, después de que terminen sus corutinas.
        viewModelScope.coroutineContext[Job]?.invokeOnCompletion { repository.cerrar() }
        cargar()
    }

    // Carga SQLite y los datos iniciales antes de permitir el acceso.
    fun cargar() = ejecutar {
        repository.cargar()
        cargado = true
    }

    // Autentica contra las cuentas locales y publica el usuario para que Compose cambie de pantalla.
    fun iniciarSesion(correo: String, clave: String) = ejecutar {
        if (!cargado) { errorGeneral = "Primero carga los datos."; return@ejecutar }
        usuario = repository.autenticar(correo, clave)
        if (usuario == null) errorGeneral = "Correo o contraseña incorrectos."
    }

    // Descarta identidad, borradores y errores de la sesión; conserva las órdenes persistidas.
    fun cerrarSesion() {
        if (ocupado) return
        usuario = null
        nuevoFormulario()
        formulariosDetalle.clear()
        mensajeDetalle = null
        ordenInformeConErrores = null
        ordenObservacionConError = null
        ordenRespuestaConError = null
        errorGeneral = null
    }

    // Restablece los campos y errores cuando se comienza una creación o se cierra la sesión.
    fun nuevoFormulario() {
        datosCrear = DatosCrearOt()
        mostrarErroresCrear = false
        errorGuardarCrear = false
    }

    // Reutiliza la regla de dominio también al abrir directamente una pantalla de detalle.
    fun puedeVer(orden: OrdenTrabajo) = PermisosOt.puedeVer(usuario, orden)

    // Solo una acción a la vez. El repositorio cambia a IO y el estado UI se actualiza en Main.
    private fun ejecutar(accion: suspend () -> Unit) {
        if (ocupado) return
        ocupado = true
        errorGeneral = null
        viewModelScope.launch {
            try { accion() }
            // La cancelación del ciclo de vida se propaga; no se convierte en un error de negocio.
            catch (cancelada: CancellationException) { throw cancelada }
            catch (_: Exception) { errorGeneral = "No se pudo completar la operación. Inténtalo nuevamente." }
            finally { ocupado = false }
        }
    }

    // Busca la OT actual y exige rol y pertenencia antes de ejecutar cualquier modificación.
    private fun autorizado(id: String, rol: Perfil): Boolean {
        val orden = ordenes.value.firstOrNull { it.id == id }
        val permitido = orden != null && PermisosOt.puedeActuar(usuario, orden, rol)
        if (!permitido) errorGeneral = "No tienes permiso para modificar esta orden."
        return permitido
    }

    // Entrada desde la UI: controla el rol y entrega el ID creado mediante un callback.
    fun crear(onResultado: (String?) -> Unit) = ejecutar {
        if (usuario?.perfil != Perfil.PLANIFICADOR) {
            errorGeneral = "Solo el planificador puede crear órdenes."
            onResultado(null)
        } else onResultado(crearInterno())
    }
    // Valida permiso y formulario; el callback permite volver solo si se guardó correctamente.
    fun actualizarOt(id: String, onResultado: (Boolean) -> Unit) = ejecutar {
        val ok = autorizado(id, Perfil.PLANIFICADOR) && actualizarOtInterno(id)
        if (!ok && !erroresCrear.hayErrores) errorGeneral = "No se pudo actualizar la orden."
        onResultado(ok)
    }
    // Devuelve el resultado a la pantalla para que no abandone el detalle si el borrado falla.
    fun borrar(id: String, onResultado: (Boolean) -> Unit) = ejecutar {
        onResultado(autorizado(id, Perfil.PLANIFICADOR) && borrarInterno(id))
    }
    // Las acciones públicas repiten la autorización aunque la UI ya oculte botones por perfil.
    fun asignar(id: String) = ejecutar {
        if (autorizado(id, Perfil.PLANIFICADOR) && !asignarInterno(id)) errorGeneral = "No se pudo asignar la orden."
    }
    fun aceptar(id: String) = ejecutar {
        if (autorizado(id, Perfil.TECNICO) && !aceptarInterno(id)) errorGeneral = "No se pudo aceptar la orden."
    }
    fun iniciarVisita(id: String, ubicacion: String?) = ejecutar {
        if (autorizado(id, Perfil.TECNICO)) iniciarVisitaInterno(id, ubicacion)
    }
    fun aprobar(id: String) = ejecutar {
        if (autorizado(id, Perfil.CLIENTE) && !aprobarInterno(id)) errorGeneral = "No se pudo aprobar la orden."
    }
    fun guardarBorrador(id: String, diagnostico: String, actividad: String, materiales: String) = ejecutar {
        if (autorizado(id, Perfil.TECNICO)) guardarBorradorInterno(id, diagnostico, actividad, materiales)
    }
    fun guardarFoto(id: String, nombre: String, onResultado: (Boolean) -> Unit) = ejecutar {
        onResultado(autorizado(id, Perfil.TECNICO) && guardarFotoInterno(id, nombre))
    }
    fun finalizarVisita(id: String, diagnostico: String, actividad: String, materiales: String) = ejecutar {
        if (autorizado(id, Perfil.TECNICO)) finalizarVisitaInterno(id, diagnostico, actividad, materiales)
    }
    fun observar(id: String, observacion: String) = ejecutar {
        if (autorizado(id, Perfil.CLIENTE)) observarInterno(id, observacion)
    }
    fun responderObservacion(id: String, respuesta: String) = ejecutar {
        if (autorizado(id, Perfil.TECNICO)) responderObservacionInterno(id, respuesta)
    }
}
