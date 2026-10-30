package com.proyecto.acis.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
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

    // Perfil activo seleccionado en la interfaz (Planificador, Técnico o Cliente)
    var perfil by mutableStateOf(Perfil.PLANIFICADOR)
        private set

    // Estado con los datos ingresados en el formulario de creación
    var datosCrear by mutableStateOf(DatosCrearOt())
        private set

    // Indica si ocurrió un error al intentar guardar la OT
    var errorGuardarCrear by mutableStateOf(false)
        private set

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

    // Cambia el perfil activo
    fun seleccionarPerfil(nuevo: Perfil) { perfil = nuevo }

    // Actualiza campos individuales del formulario de creación
    fun cambiarLocal(valor: String) { datosCrear = datosCrear.copy(local = valor) }
    fun cambiarEquipo(valor: String) { datosCrear = datosCrear.copy(equipo = valor) }
    fun cambiarDescripcion(valor: String) { datosCrear = datosCrear.copy(descripcion = valor) }
    fun cambiarTipo(valor: TipoOt) { datosCrear = datosCrear.copy(tipo = valor) }
    fun cambiarPrioridad(valor: PrioridadOt) { datosCrear = datosCrear.copy(prioridad = valor) }
    fun cambiarFecha(valor: String) { datosCrear = datosCrear.copy(fecha = valor) }
    fun cambiarHora(valor: String) { datosCrear = datosCrear.copy(hora = valor) }

    // Intenta crear y guardar una nueva Orden de Trabajo
    fun crear(): String? {
        mostrarErroresCrear = true
        errorGuardarCrear = false
        val datos = datosCrear
        if (ValidadorOt.crear(datos).hayErrores) return null
        val tipo = datos.tipo ?: return null
        val prioridad = datos.prioridad ?: return null
        val id = repository.crear(
            datos.local, datos.equipo, datos.descripcion,
            "${datos.fecha} ${datos.hora}", tipo, prioridad,
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
            when (perfilVisible) {
                Perfil.PLANIFICADOR -> true
                Perfil.TECNICO -> orden.tecnico == OrdenesRepository.TECNICO_DEMO
                Perfil.CLIENTE -> orden.estado in setOf(
                    EstadoOt.PENDIENTE_CONFORMIDAD, EstadoOt.OBSERVADA, EstadoOt.CERRADA,
                )
            }
        }.sortedWith(
            compareByDescending<OrdenTrabajo> { it.ultimaActualizacion }
                .thenByDescending { it.id.removePrefix("OT-").toIntOrNull() ?: 0 },
        )

    // Acciones del flujo de vida de la OT
    fun asignar(id: String) = repository.asignar(id)
    fun aceptar(id: String) = repository.aceptar(id)

    // Inicia la visita del técnico y guarda la ubicación obtenida
    fun iniciarVisita(id: String, ubicacion: String?) {
        val guardado = repository.iniciarVisita(id, ubicacion)
        mensajeDetalle = id to when {
            !guardado -> "No se pudo iniciar la visita."
            ubicacion == null -> "Check-in registrado sin ubicación."
            else -> "Check-in registrado con ubicación."
        }
    }

    fun aprobar(id: String) = repository.aprobar(id)

    // Elimina la orden de trabajo seleccionada
    fun borrar(id: String): Boolean {
        val borrado = repository.borrar(id)
        mensajeDetalle = id to if (borrado) "Orden eliminada." else "No se pudo eliminar la orden."
        return borrado
    }

    // Guarda borrador de avance del informe técnico sin cambiar el estado
    fun guardarBorrador(id: String, diagnostico: String, actividad: String, materiales: String) {
        val guardado = repository.guardarBorrador(id, diagnostico, actividad, materiales)
        mensajeDetalle = id to if (guardado) "Borrador guardado en este dispositivo."
        else "No se pudo guardar el borrador."
    }

    // Asocia la foto capturada a la OT
    fun guardarFoto(id: String, nombre: String): Boolean {
        val guardado = repository.guardarFoto(id, nombre)
        mensajeDetalle = id to if (guardado) "Foto guardada en esta OT."
        else "No se pudo guardar la foto."
        return guardado
    }

    // Valida y finaliza la visita técnica
    fun finalizarVisita(id: String, diagnostico: String, actividad: String, materiales: String) {
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
    fun observar(id: String, observacion: String) {
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
    fun responderObservacion(id: String, respuesta: String) {
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
}
