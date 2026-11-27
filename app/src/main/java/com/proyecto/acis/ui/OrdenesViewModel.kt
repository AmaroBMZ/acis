package com.proyecto.acis.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.proyecto.acis.session.SesionActual
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.proyecto.acis.data.Usuario
import com.proyecto.acis.data.OrdenesRemotoRepository
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
import com.proyecto.acis.domain.UsuariosRepository
import com.proyecto.acis.data.remote.ApiClient
import com.proyecto.acis.data.remote.CheckInApiRequest
import com.proyecto.acis.data.remote.InformeApiRequest
import com.proyecto.acis.data.remote.aDominio
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

/**
 * ViewModel que gestiona el estado de la UI y coordina las acciones del usuario
 * con la capa de repositorio y validación de dominio.
 */
class OrdenesViewModel(
    private val repository: OrdenesRepository,
    private val sesion: SesionActual,
    private val usuariosRemotos: UsuariosRepository? = null,
    private val ordenesRemotas: OrdenesRemotoRepository? = null,
) : ViewModel() {
    // Consulta la identidad compartida; este ViewModel no autentica ni conserva otra copia.
    val usuario: Usuario? get() = sesion.usuario.value
    val ordenes = repository.ordenes
    private val _usuarios = MutableStateFlow<List<Usuario>>(repository.usuarios.value)
    val usuarios = _usuarios.asStateFlow()

    init {
        // SQLite conserva usuarios demo como respaldo; el catálogo remoto se usa cuando está disponible.
        viewModelScope.launch {
            repository.cargar()
            repository.usuarios.collect { locales ->
                if (_usuarios.value.isEmpty()) _usuarios.value = locales
            }
        }
        usuariosRemotos?.let { remoto ->
            viewModelScope.launch {
                remoto.usuarios.collect { cuentas -> if (cuentas.isNotEmpty()) _usuarios.value = cuentas }
            }
            viewModelScope.launch {
                sesion.token.collect { token ->
                    if (!token.isNullOrBlank()) {
                        try {
                            // Carga SQLite antes de reemplazarla; evita que la carga inicial local
                            // termine después y sobrescriba la lista que acaba de llegar del servidor.
                            repository.cargar()
                            remoto.cargar(token)
                            val lista = ordenesRemotas?.listar(token)?.map { it.aDominio() }
                            if (lista != null) repository.reemplazarDesdeServidor(lista)
                        } catch (cancelada: CancellationException) {
                            throw cancelada
                        } catch (_: Exception) {
                            // La lista local de respaldo queda disponible si el servicio aún no responde.
                        }
                    }
                }
            }
        }
    }
    // El formulario de OT usa el catálogo actual de cuentas; no permite elegir clientes desactivados.
    val clientesActivos get() = usuarios.value.filter { it.activo && it.perfil == Perfil.CLIENTE }
    val tecnicosActivos get() = usuarios.value.filter { it.activo && it.perfil == Perfil.TECNICO }
    fun etiquetaTecnico(identificador: String): String =
        usuarios.value.firstOrNull {
            it.perfil == Perfil.TECNICO &&
                (it.correo.equals(identificador, ignoreCase = true) || it.nombre.equals(identificador, ignoreCase = true))
        }?.nombre ?: identificador

    // El valor de respaldo no inicia sesión: los permisos comprueban usuario y rechazan null.
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
        // getOrPut toma los datos guardados solo la primera vez; después conserva lo que se está escribiendo.
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
        get() = if (mostrarErroresCrear) ValidadorOt.crear(
            datosCrear,
            clientesActivos.map { it.correo },
            tecnicosActivos.map { it.correo },
        ) else ErroresCrearOt()

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
    fun cambiarTecnico(valor: String) { datosCrear = datosCrear.copy(tecnicoCorreo = valor) }

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
            tecnicoCorreo = usuarios.value.firstOrNull {
                it.perfil == Perfil.TECNICO &&
                    (it.correo.equals(orden.tecnico, ignoreCase = true) || it.nombre.equals(orden.tecnico, ignoreCase = true))
            }?.correo.orEmpty(),
        )
        mostrarErroresCrear = false
    }

    // Actualiza los datos de una OT existente (CRUD - Update)
    private suspend fun actualizarOtInterno(id: String): Boolean {
        mostrarErroresCrear = true
        val datos = datosCrear
        if (ValidadorOt.crear(datos, clientesActivos.map { it.correo }, tecnicosActivos.map { it.correo }).hayErrores) return false
        val tipo = datos.tipo ?: return false
        val prioridad = datos.prioridad ?: return false
        val tecnico = tecnicosActivos.firstOrNull { it.correo == datos.tecnicoCorreo } ?: return false
        val actual = ordenes.value.firstOrNull { it.id == id } ?: return false
        val token = sesion.token.value
        val actualizado = if (ordenesRemotas != null && !token.isNullOrBlank()) {
            val respuesta = ordenesRemotas.actualizar(
                token,
                actual.copy(
                    local = datos.local,
                    equipo = datos.equipo,
                    descripcion = datos.descripcion,
                    fechaProgramada = "${datos.fecha} ${datos.hora}",
                    tipo = tipo,
                    prioridad = prioridad,
                    clienteCorreo = datos.clienteCorreo,
                    tecnico = tecnico.correo,
                ),
                tecnico.correo,
            )
            repository.guardarDesdeServidor(respuesta.aDominio())
        } else {
            repository.actualizarDatos(
                id, datos.local, datos.equipo, datos.descripcion,
                "${datos.fecha} ${datos.hora}", tipo, prioridad, datos.clienteCorreo, tecnico.nombre,
            )
        }
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
        if (ValidadorOt.crear(datos, clientesActivos.map { it.correo }, tecnicosActivos.map { it.correo }).hayErrores) return null
        val tipo = datos.tipo ?: return null
        val prioridad = datos.prioridad ?: return null
        val tecnico = tecnicosActivos.firstOrNull { it.correo == datos.tecnicoCorreo } ?: return null
        val token = sesion.token.value
        val id = if (ordenesRemotas != null) {
            if (token.isNullOrBlank()) return null
            val creada = ordenesRemotas.crear(
                token, datos.clienteCorreo, datos.local, datos.equipo, datos.descripcion,
                "${datos.fecha} ${datos.hora}", tipo, prioridad, tecnico.correo,
            )
            if (!repository.guardarDesdeServidor(creada.aDominio())) return null
            creada.codigoOt
        } else {
            repository.crear(
                datos.local, datos.equipo, datos.descripcion,
                "${datos.fecha} ${datos.hora}", tipo, prioridad, datos.clienteCorreo, tecnico.nombre,
            )
        }
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

    private suspend fun refrescarDesdeServidor(id: String) {
        val token = sesion.token.value ?: return
        val remote = ordenesRemotas ?: return
        repository.guardarDesdeServidor(remote.buscar(token, id).aDominio())
    }

    // Acciones del flujo de vida de la OT
    private suspend fun aceptarInterno(id: String): Boolean {
        val token = sesion.token.value ?: return false
        val remote = ordenesRemotas ?: return repository.aceptar(id)
        return repository.guardarDesdeServidor(remote.cambiarEstado(token, id, EstadoOt.ACEPTADA).aDominio())
    }

    // Acepta una OT asignada si hace falta y luego registra la llegada con el microservicio de terreno.
    private suspend fun iniciarVisitaInterno(id: String, ubicacion: String?) {
        val token = sesion.token.value ?: throw IllegalStateException("Sesión vencida")
        val orden = ordenes.value.firstOrNull { it.id == id } ?: throw IllegalStateException("OT no disponible")
        if (orden.estado == EstadoOt.ASIGNADA) {
            val remote = ordenesRemotas
            if (remote != null) {
                repository.guardarDesdeServidor(remote.cambiarEstado(token, id, EstadoOt.ACEPTADA).aDominio())
            } else if (!repository.aceptar(id)) {
                throw IllegalStateException("No se pudo aceptar la OT")
            }
        } else if (orden.estado != EstadoOt.ACEPTADA) {
            throw IllegalStateException("La OT no está asignada para iniciar")
        }
        ApiClient.intervenciones.checkIn("Bearer $token", CheckInApiRequest(id, ubicacion))
        if (ordenesRemotas != null) refrescarDesdeServidor(id)
        else repository.iniciarVisita(id, ubicacion)
        val guardado = ordenes.value.firstOrNull { it.id == id }?.estado == EstadoOt.EN_VISITA
        mensajeDetalle = id to when {
            !guardado -> "No se pudo iniciar la visita."
            ubicacion == null -> "Check-in registrado sin ubicación."
            else -> "Check-in registrado con ubicación."
        }
    }

    private suspend fun aprobarInterno(id: String): Boolean {
        val token = sesion.token.value ?: return false
        val remote = ordenesRemotas ?: return repository.aprobar(id)
        return repository.guardarDesdeServidor(remote.cambiarEstado(token, id, EstadoOt.CERRADA).aDominio())
    }

    // Elimina la orden de trabajo seleccionada (CRUD - Delete)
    private suspend fun borrarInterno(id: String): Boolean {
        val token = sesion.token.value ?: return false
        ordenesRemotas?.borrar(token, id)
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
        val token = sesion.token.value ?: throw IllegalStateException("Sesión vencida")
        val archivo = repository.archivoFoto(nombre) ?: return false
        val parte = MultipartBody.Part.createFormData(
            "file", archivo.name, archivo.asRequestBody("image/jpeg".toMediaType()),
        )
        ApiClient.intervenciones.subirFoto("Bearer $token", id, parte)
        val guardado = repository.guardarFoto(id, nombre)
        if (guardado && ordenesRemotas != null) refrescarDesdeServidor(id)
        mensajeDetalle = id to if (guardado) "Foto enviada al servidor y guardada en esta OT."
        else "No se pudo guardar la foto."
        return guardado
    }

    // Valida y finaliza la visita técnica
    private suspend fun finalizarVisitaInterno(id: String, diagnostico: String, actividad: String, materiales: String): Boolean {
        ordenInformeConErrores = id
        if (ValidadorOt.informe(diagnostico, actividad).hayErrores) {
            mensajeDetalle = null
            return false
        }
        val token = sesion.token.value ?: throw IllegalStateException("Sesión vencida")
        ApiClient.intervenciones.informe(
            "Bearer $token", InformeApiRequest(id, diagnostico, actividad, materiales),
        )
        val guardado = if (ordenesRemotas != null) {
            refrescarDesdeServidor(id)
            ordenes.value.firstOrNull { it.id == id }?.estado == EstadoOt.PENDIENTE_CONFORMIDAD
        } else repository.enviarInforme(id, diagnostico, actividad, materiales)
        mensajeDetalle = id to if (guardado) "Informe enviado al servidor para revisión."
        else "No se pudo finalizar la visita."
        if (guardado) ordenInformeConErrores = null
        return guardado
    }

    // Registra una observación formulada por el cliente
    private suspend fun observarInterno(id: String, observacion: String): Boolean {
        ordenObservacionConError = id
        if (ValidadorOt.observacion(observacion) != null) {
            mensajeDetalle = null
            return false
        }
        val token = sesion.token.value ?: return false
        val guardado = if (ordenesRemotas != null) {
            repository.guardarDesdeServidor(
                ordenesRemotas.cambiarEstado(token, id, EstadoOt.OBSERVADA,
                    observacionCliente = observacion.trim()).aDominio(),
            )
        } else repository.observar(id, observacion)
        mensajeDetalle = id to if (guardado) "Observación registrada." else "No se pudo registrar la observación."
        if (guardado) ordenObservacionConError = null
        return guardado
    }

    // Envía la respuesta técnica a una observación del cliente
    private suspend fun responderObservacionInterno(id: String, respuesta: String): Boolean {
        ordenRespuestaConError = id
        if (ValidadorOt.respuesta(respuesta) != null) {
            mensajeDetalle = null
            return false
        }
        val token = sesion.token.value ?: return false
        val guardado = if (ordenesRemotas != null) {
            repository.guardarDesdeServidor(
                ordenesRemotas.cambiarEstado(token, id, EstadoOt.PENDIENTE_CONFORMIDAD,
                    respuestaTecnico = respuesta.trim()).aDominio(),
            )
        } else repository.responderObservacion(id, respuesta)
        mensajeDetalle = id to if (guardado) "Respuesta registrada; el cliente puede revisar nuevamente."
        else "No se pudo enviar la respuesta."
        if (guardado) ordenRespuestaConError = null
        return guardado
    }
    // Evita lanzar otra operación mientras una acción del ViewModel sigue en curso.
    var ocupado by mutableStateOf(false)
        private set
    // Mensaje para fallos de las operaciones de órdenes y administración aún no separada.
    var errorGeneral by mutableStateOf<String?>(null)
        private set

    // Descarta solo estado de trabajo; la identidad la borra SesionViewModel después de esta limpieza.
    fun limpiarBorradores() {
        if (ocupado) return
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
        // Una llamada mientras está ocupado se descarta y no ejecuta su callback.
        if (ocupado) return
        ocupado = true
        errorGeneral = null
        viewModelScope.launch {
            try { accion() }
            // La cancelación del ciclo de vida se propaga; no se convierte en un error de negocio.
            catch (cancelada: CancellationException) { throw cancelada }
            // Una excepción interrumpe accion: no se garantiza llegar a onResultado.
            // Se publica el error general y finally habilita las acciones para reintentar.
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
    fun aceptar(id: String) = ejecutar {
        if (!autorizado(id, Perfil.TECNICO)) return@ejecutar
        try {
            if (!aceptarInterno(id)) errorGeneral = "No se pudo aceptar la orden."
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (error: HttpException) {
            errorGeneral = when (error.code()) {
                401 -> "La sesión venció. Cierra sesión e inicia nuevamente."
                403 -> "El servidor no reconoce esta OT asignada a tu cuenta. Revisa el correo del técnico asignado."
                409 -> "El servicio rechazó aceptar esta OT (HTTP 409). Reinicia ot-gestion para cargar la transición actualizada."
                else -> "El servidor rechazó aceptar la orden (HTTP ${error.code()})."
            }
        } catch (_: IOException) {
            errorGeneral = "No se pudo conectar con ot-gestion. Revisa que el servicio y el gateway estén activos."
        }
    }
    fun iniciarVisita(id: String, ubicacion: String?) = ejecutar {
        if (autorizado(id, Perfil.TECNICO)) iniciarVisitaInterno(id, ubicacion)
    }
    fun aprobar(id: String, onResultado: (Boolean) -> Unit = {}) = ejecutar {
        // Solo comunica éxito cuando la cuenta tiene permiso y el repositorio confirma el cambio.
        val ok = autorizado(id, Perfil.CLIENTE) && aprobarInterno(id)
        if (!ok) errorGeneral = "No se pudo aprobar la orden."
        onResultado(ok)
    }
    fun guardarBorrador(id: String, diagnostico: String, actividad: String, materiales: String) = ejecutar {
        if (autorizado(id, Perfil.TECNICO)) guardarBorradorInterno(id, diagnostico, actividad, materiales)
    }
    fun guardarFoto(id: String, nombre: String, onResultado: (Boolean) -> Unit) = ejecutar {
        val ok = if (!autorizado(id, Perfil.TECNICO)) false else try {
            guardarFotoInterno(id, nombre)
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (error: HttpException) {
            errorGeneral = "El servidor rechazó la foto (HTTP ${error.code()}). Comprueba que la OT esté asignada y en visita."
            false
        } catch (_: IOException) {
            errorGeneral = "No se pudo conectar con el servicio para guardar la foto."
            false
        } catch (_: Exception) {
            errorGeneral = "No se pudo guardar la foto. Inténtalo nuevamente."
            false
        }
        onResultado(ok)
    }
    fun finalizarVisita(id: String, diagnostico: String, actividad: String, materiales: String, onResultado: (Boolean) -> Unit = {}) = ejecutar {
        // Propaga tanto errores del formulario como fallos de guardado; ninguno debe cerrar la pantalla.
        val ok = if (!autorizado(id, Perfil.TECNICO)) false else try {
            finalizarVisitaInterno(id, diagnostico, actividad, materiales)
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (error: HttpException) {
            errorGeneral = "El servidor rechazó el informe (HTTP ${error.code()})."
            false
        } catch (_: IOException) {
            errorGeneral = "No se pudo conectar con el servicio para guardar el informe."
            false
        } catch (_: Exception) {
            errorGeneral = "No se pudo guardar el informe. Inténtalo nuevamente."
            false
        }
        onResultado(ok)
    }
    fun observar(id: String, observacion: String, onResultado: (Boolean) -> Unit = {}) = ejecutar {
        // El callback evita navegar antes de que se registre la observación del cliente.
        onResultado(autorizado(id, Perfil.CLIENTE) && observarInterno(id, observacion))
    }
    fun responderObservacion(id: String, respuesta: String, onResultado: (Boolean) -> Unit = {}) = ejecutar {
        // La pantalla vuelve al destino anterior únicamente cuando la respuesta quedó persistida.
        onResultado(autorizado(id, Perfil.TECNICO) && responderObservacionInterno(id, respuesta))
    }
}
