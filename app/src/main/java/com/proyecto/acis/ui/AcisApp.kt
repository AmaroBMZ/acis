package com.proyecto.acis.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.proyecto.acis.data.EstadoOt
import com.proyecto.acis.data.OrdenTrabajo
import com.proyecto.acis.data.Perfil
import com.proyecto.acis.data.PrioridadOt
import com.proyecto.acis.data.TipoOt
import com.proyecto.acis.data.obtenerUbicacionCheckIn
import java.util.Calendar
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

private object RutasAcis {
    const val ORDENES = "ordenes"
    const val CREAR = "crear"
    const val DETALLE = "detalle/{ordenId}"

    fun detalle(ordenId: String) = "detalle/$ordenId"
}

@Composable
fun AcisApp(viewModel: OrdenesViewModel, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val ordenes by viewModel.ordenes.collectAsState()
    val perfil = viewModel.perfil

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("ACIS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Gestión de Órdenes de Trabajo · ACIS", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Perfil.entries.forEach { opcion ->
                        FilterChip(
                            selected = perfil == opcion,
                            onClick = {
                                viewModel.seleccionarPerfil(opcion)
                                navController.popBackStack(RutasAcis.ORDENES, inclusive = false)
                            },
                            label = { Text(opcion.etiqueta) },
                        )
                    }
                }
            }
        }
        HorizontalDivider()
        NavHost(
            navController = navController,
            startDestination = RutasAcis.ORDENES,
            modifier = Modifier.weight(1f),
            enterTransition = {
                fadeIn(tween(420)) + slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(420),
                )
            },
            exitTransition = {
                fadeOut(tween(350)) + slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(350),
                )
            },
            popEnterTransition = {
                fadeIn(tween(420)) + slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(420),
                )
            },
            popExitTransition = {
                fadeOut(tween(350)) + slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(350),
                )
            },
        ) {
            composable(RutasAcis.ORDENES) {
                Crossfade(targetState = perfil, animationSpec = tween(420), label = "cambio de perfil") { perfilVisible ->
                    ListaOrdenes(
                        ordenes = viewModel.ordenesParaPerfil(ordenes, perfilVisible),
                        perfil = perfilVisible,
                        onCrear = { navController.navigate(RutasAcis.CREAR) },
                        onSeleccionar = { navController.navigate(RutasAcis.detalle(it)) },
                    )
                }
            }
            composable(RutasAcis.CREAR) {
                CrearOrden(
                    viewModel = viewModel,
                    onVolver = { navController.popBackStack() },
                    onCrear = {
                        val id = viewModel.crear()
                        if (id != null) {
                            navController.navigate(RutasAcis.detalle(id)) {
                                popUpTo(RutasAcis.ORDENES) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
            composable(
                route = RutasAcis.DETALLE,
                arguments = listOf(navArgument("ordenId") { type = NavType.StringType }),
            ) { entrada ->
                val ordenId = entrada.arguments?.getString("ordenId")
                val orden = ordenes.firstOrNull { it.id == ordenId }
                if (orden != null) {
                    DetalleOrden(
                        orden = orden,
                        perfil = perfil,
                        viewModel = viewModel,
                        onVolver = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ListaOrdenes(
    ordenes: List<OrdenTrabajo>,
    perfil: Perfil,
    onCrear: () -> Unit,
    onSeleccionar: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (perfil) {
                        Perfil.PLANIFICADOR -> "Órdenes de trabajo"
                        Perfil.TECNICO -> "Mis órdenes asignadas"
                        Perfil.CLIENTE -> "Órdenes para revisión"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (perfil == Perfil.PLANIFICADOR) Button(onClick = onCrear) { Text("Crear OT") }
                Text("Registro local con soporte para perfiles, ubicación y fotos de evidencia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (ordenes.isEmpty()) {
            item {
                Text(
                    when (perfil) {
                        Perfil.PLANIFICADOR -> "Todavía no hay órdenes. Crea la primera OT."
                        Perfil.TECNICO -> "No tienes órdenes asignadas. Cambia a Planificador y asigna una OT."
                        Perfil.CLIENTE -> "No hay informes pendientes de conformidad."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        items(ordenes, key = { it.id }) { orden ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSeleccionar(orden.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(orden.id, style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    AnimatedContent(targetState = orden.estado, label = "estado de OT") { estado ->
                        EstadoBadge(estado)
                    }
                    Text(orden.local, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(orden.equipo, style = MaterialTheme.typography.bodyMedium)
                    Text(orden.tipo.etiqueta, style = MaterialTheme.typography.bodySmall)
                    PrioridadBadge(orden.prioridad)
                    Text(orden.fechaProgramada, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CrearOrden(
    viewModel: OrdenesViewModel,
    onVolver: () -> Unit,
    onCrear: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val ahora = remember { Calendar.getInstance() }
    val datos = viewModel.datosCrear
    val errores = viewModel.erroresCrear
    var dia by rememberSaveable { mutableStateOf(ahora.get(Calendar.DAY_OF_MONTH)) }
    var mes by rememberSaveable { mutableStateOf(ahora.get(Calendar.MONTH)) }
    var anio by rememberSaveable { mutableStateOf(ahora.get(Calendar.YEAR)) }
    var hora by rememberSaveable { mutableStateOf(ahora.get(Calendar.HOUR_OF_DAY)) }
    var minuto by rememberSaveable { mutableStateOf(ahora.get(Calendar.MINUTE)) }

    LazyColumn(modifier = Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = onVolver) { Text("← Volver") } }
        item { Text("Nueva orden de trabajo", style = MaterialTheme.typography.headlineSmall) }
        item { CampoFormulario(datos.local, viewModel::cambiarLocal, "Local", errores.local) }
        item { CampoFormulario(datos.equipo, viewModel::cambiarEquipo, "Equipo", errores.equipo) }
        item { CampoFormulario(datos.descripcion, viewModel::cambiarDescripcion,
            "Descripción del trabajo", errores.descripcion, minLines = 3) }
        item { Text("Tipo de trabajo", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TipoOt.entries.filter { it != TipoOt.SIN_ESPECIFICAR }.forEach { tipo ->
                    FilterChip(selected = datos.tipo == tipo,
                        onClick = { viewModel.cambiarTipo(tipo) }, label = { Text(tipo.etiqueta) })
                }
            }
        }
        if (errores.tipo != null) item { ErrorSeleccion(errores.tipo) }
        item { Text("Prioridad", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())) {
                PrioridadOt.entries.filter { it != PrioridadOt.SIN_ESPECIFICAR }.forEach { prioridad ->
                    FilterChip(selected = datos.prioridad == prioridad,
                        onClick = { viewModel.cambiarPrioridad(prioridad) }, label = { Text(prioridad.etiqueta) })
                }
            }
        }
        if (errores.prioridad != null) item { ErrorSeleccion(errores.prioridad) }
        item { Text("Fecha y hora programada", style = MaterialTheme.typography.titleMedium) }
        item {
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    DatePickerDialog(context, { _, nuevoAnio, nuevoMes, nuevoDia ->
                        anio = nuevoAnio
                        mes = nuevoMes
                        dia = nuevoDia
                        viewModel.cambiarFecha(String.format(
                            Locale.forLanguageTag("es-CL"), "%02d/%02d/%04d", dia, mes + 1, anio,
                        ))
                    }, anio, mes, dia).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (datos.fecha.isNotBlank()) "Fecha: ${datos.fecha}" else "Seleccionar fecha") }
        }
        if (errores.fecha != null) item { ErrorSeleccion(errores.fecha) }
        item {
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    TimePickerDialog(context, { _, nuevaHora, nuevoMinuto ->
                        hora = nuevaHora
                        minuto = nuevoMinuto
                        viewModel.cambiarHora(String.format(
                            Locale.forLanguageTag("es-CL"), "%02d:%02d", hora, minuto,
                        ))
                    }, hora, minuto, true).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (datos.hora.isNotBlank()) "Hora: ${datos.hora}" else "Seleccionar hora") }
        }
        if (errores.hora != null) item { ErrorSeleccion(errores.hora) }
        if (viewModel.errorGuardarCrear) item {
            Text("No se pudo guardar la OT.", color = MaterialTheme.colorScheme.error)
        }
        item {
            Button(onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                onCrear()
            }) { Text("Guardar OT") }
        }
    }
}

@Composable
private fun DetalleOrden(
    orden: OrdenTrabajo,
    perfil: Perfil,
    viewModel: OrdenesViewModel,
    onVolver: () -> Unit,
) {
    val context = LocalContext.current
    val scopeUbicacion = rememberCoroutineScope()
    var buscandoUbicacion by rememberSaveable(orden.id) { mutableStateOf(false) }
    val iniciarConUbicacion: () -> Unit = {
        buscandoUbicacion = true
        scopeUbicacion.launch {
            val ubicacion = obtenerUbicacionCheckIn(context)
            viewModel.iniciarVisita(orden.id, ubicacion)
            buscandoUbicacion = false
        }
    }
    val permisoUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) iniciarConUbicacion()
        else viewModel.iniciarVisita(orden.id, null)
    }
    var fotoPendiente by rememberSaveable(orden.id) { mutableStateOf("") }
    var errorCamara by rememberSaveable(orden.id) { mutableStateOf("") }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
        val nombre = fotoPendiente
        fotoPendiente = ""
        if (nombre.isNotBlank()) {
            val archivo = File(File(context.filesDir, "evidencias"), nombre)
            if (guardada && viewModel.guardarFoto(orden.id, nombre)) {
                errorCamara = ""
            } else {
                archivo.delete()
                if (guardada) errorCamara = "No se pudo guardar la foto."
            }
        }
    }
    val formulario = viewModel.formularioDetalle(orden)
    val erroresInforme = viewModel.erroresInforme(orden.id, formulario.diagnostico, formulario.actividad)
    val errorObservacion = viewModel.errorObservacion(orden.id, formulario.observacion)
    val errorRespuesta = viewModel.errorRespuesta(orden.id, formulario.respuesta)
    val mensaje = viewModel.mensaje(orden.id)

    LazyColumn(modifier = Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onVolver) { Text("← Volver a órdenes") } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(orden.id, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                AnimatedContent(targetState = orden.estado, label = "estado del detalle") { estado ->
                    EstadoBadge(estado)
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoDetalle("Local", orden.local)
                    CampoDetalle("Equipo", orden.equipo)
                    CampoDetalle("Tipo de trabajo", orden.tipo.etiqueta)
                    PrioridadBadge(orden.prioridad)
                    CampoDetalle("Fecha programada", orden.fechaProgramada)
                    CampoDetalle("Trabajo solicitado", orden.descripcion)
                    if (orden.tecnico.isNotBlank()) CampoDetalle("Técnico", orden.tecnico)
                    if (orden.checkIn.isNotBlank()) CampoDetalle("Check-in", orden.checkIn)
                    if (orden.ubicacionCheckIn.isNotBlank()) CampoDetalle("Ubicación del check-in", orden.ubicacionCheckIn)
                    if (orden.checkOut.isNotBlank()) CampoDetalle("Check-out", orden.checkOut)
                    if (orden.diagnostico.isNotBlank()) CampoDetalle("Diagnóstico", orden.diagnostico)
                    if (orden.actividad.isNotBlank()) CampoDetalle("Actividad", orden.actividad)
                    if (orden.materiales.isNotBlank()) CampoDetalle("Materiales", orden.materiales)
                    if (orden.observacionCliente.isNotBlank()) CampoDetalle("Observación del cliente", orden.observacionCliente)
                    if (orden.respuestaTecnico.isNotBlank()) CampoDetalle("Respuesta del técnico", orden.respuestaTecnico)
                    if (orden.fechaConformidad.isNotBlank()) CampoDetalle("Conformidad", orden.fechaConformidad)
                    if (orden.fotoEvidencia.isNotBlank()) {
                        val archivo = File(File(context.filesDir, "evidencias"), orden.fotoEvidencia)
                        val foto = remember(orden.fotoEvidencia) {
                            val opciones = BitmapFactory.Options().apply { inSampleSize = 4 }
                            BitmapFactory.decodeFile(archivo.path, opciones)?.asImageBitmap()
                        }
                        if (foto != null) {
                            Text("Foto de evidencia", style = MaterialTheme.typography.titleSmall)
                            Image(foto, contentDescription = "Evidencia de ${orden.id}",
                                modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop)
                        } else {
                            Text("Foto de evidencia no disponible", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        if (perfil == Perfil.PLANIFICADOR && orden.estado == EstadoOt.CREADA) {
            item { Button(onClick = { viewModel.asignar(orden.id) }) { Text("Asignar a técnico demo") } }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.ASIGNADA) {
            item { Button(onClick = { viewModel.aceptar(orden.id) }) { Text("Aceptar OT") } }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.ACEPTADA) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Se pedirá ubicación aproximada para el check-in. Puedes continuar si no la permites.",
                        style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) iniciarConUbicacion()
                        else permisoUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }, enabled = !buscandoUbicacion) {
                        Text(if (buscandoUbicacion) "Buscando ubicación..." else "Iniciar visita y registrar check-in")
                    }
                }
            }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.EN_VISITA) {
            item { Text("Informe técnico", style = MaterialTheme.typography.titleLarge) }
            item {
                OutlinedButton(onClick = {
                    var archivoNuevo: File? = null
                    try {
                        val carpeta = File(context.filesDir, "evidencias")
                        if (!carpeta.exists()) carpeta.mkdirs()
                        val archivo = File(carpeta, "${orden.id}_${UUID.randomUUID()}.jpg")
                        archivo.createNewFile()
                        archivoNuevo = archivo
                        fotoPendiente = archivo.name
                        errorCamara = ""
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", archivo,
                        )
                        camara.launch(uri)
                    } catch (_: ActivityNotFoundException) {
                        archivoNuevo?.delete()
                        fotoPendiente = ""
                        errorCamara = "No hay una cámara disponible en este dispositivo."
                    } catch (_: Exception) {
                        archivoNuevo?.delete()
                        fotoPendiente = ""
                        errorCamara = "No se pudo abrir la cámara."
                    }
                }) { Text(if (orden.fotoEvidencia.isBlank()) "Tomar foto de evidencia" else "Cambiar foto de evidencia") }
            }
            if (errorCamara.isNotBlank()) item { Text(errorCamara, color = MaterialTheme.colorScheme.error) }
            item { CampoFormulario(formulario.diagnostico, { viewModel.cambiarDiagnostico(orden.id, it) }, "Diagnóstico",
                erroresInforme.diagnostico, minLines = 2) }
            item { CampoFormulario(formulario.actividad, { viewModel.cambiarActividad(orden.id, it) }, "Actividad realizada",
                erroresInforme.actividad, minLines = 2) }
            item { CampoFormulario(formulario.materiales, { viewModel.cambiarMateriales(orden.id, it) }, "Materiales usados (opcional)") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        viewModel.guardarBorrador(orden.id, formulario.diagnostico, formulario.actividad, formulario.materiales)
                    }) { Text("Guardar borrador") }
                    Button(onClick = {
                        viewModel.finalizarVisita(orden.id, formulario.diagnostico, formulario.actividad, formulario.materiales)
                    }) { Text("Finalizar visita") }
                }
            }
        }
        if (perfil == Perfil.CLIENTE && orden.estado == EstadoOt.PENDIENTE_CONFORMIDAD) {
            item { Text("Revisar conformidad", style = MaterialTheme.typography.titleLarge) }
            item { CampoFormulario(formulario.observacion, { viewModel.cambiarObservacion(orden.id, it) },
                "Observación si no estás conforme", errorObservacion) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.aprobar(orden.id) }) { Text("Aprobar") }
                    OutlinedButton(onClick = {
                        viewModel.observar(orden.id, formulario.observacion)
                    }) { Text("Observar") }
                }
            }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.OBSERVADA) {
            item { Text("Responder observación", style = MaterialTheme.typography.titleLarge) }
            item { CampoFormulario(formulario.respuesta, { viewModel.cambiarRespuesta(orden.id, it) }, "Respuesta al cliente",
                errorRespuesta, minLines = 2) }
            item { Button(onClick = {
                viewModel.responderObservacion(orden.id, formulario.respuesta)
            }) { Text("Enviar respuesta") } }
        }
        if (mensaje.isNotBlank()) item { Text(mensaje, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun CampoFormulario(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String? = null,
    minLines: Int = 1,
) {
    val traerAlFrente = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    OutlinedTextField(
        value = valor,
        onValueChange = { onCambio(capitalizarOraciones(it)) },
        label = { Text(etiqueta) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        isError = error != null,
        trailingIcon = if (error != null) {{
            Icon(painterResource(android.R.drawable.ic_dialog_alert), contentDescription = "Error en $etiqueta",
                tint = MaterialTheme.colorScheme.error)
        }} else null,
        supportingText = error?.let { mensaje -> { Text(mensaje) } },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth()
            .bringIntoViewRequester(traerAlFrente)
            .onFocusChanged { foco ->
                if (foco.isFocused) scope.launch { traerAlFrente.bringIntoView() }
            },
    )
}

private fun capitalizarOraciones(texto: String): String = buildString {
    var inicio = true
    texto.forEach { letra ->
        append(if (inicio && letra.isLetter()) letra.uppercaseChar() else letra)
        if (letra.isLetter()) inicio = false
        if (letra == '.' || letra == '!' || letra == '?' || letra == '\n') inicio = true
    }
}

@Composable
private fun ErrorSeleccion(mensaje: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(painterResource(android.R.drawable.ic_dialog_alert), contentDescription = null,
            tint = MaterialTheme.colorScheme.error)
        Text(mensaje, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun CampoDetalle(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EstadoBadge(estado: EstadoOt) {
    val colores = MaterialTheme.colorScheme
    val (fondo, texto) = when (estado) {
        EstadoOt.CREADA -> colores.surfaceVariant to colores.onSurfaceVariant
        EstadoOt.ASIGNADA, EstadoOt.ACEPTADA, EstadoOt.EN_VISITA ->
            colores.primaryContainer to colores.onPrimaryContainer
        EstadoOt.PENDIENTE_CONFORMIDAD, EstadoOt.OBSERVADA ->
            colores.tertiaryContainer to colores.onTertiaryContainer
        EstadoOt.CERRADA -> colores.secondaryContainer to colores.onSecondaryContainer
    }
    BadgeTexto(estado.etiqueta, fondo, texto)
}

@Composable
private fun PrioridadBadge(prioridad: PrioridadOt) {
    val colores = MaterialTheme.colorScheme
    val (fondo, texto) = when (prioridad) {
        PrioridadOt.ALTA -> colores.errorContainer to colores.onErrorContainer
        PrioridadOt.MEDIA -> colores.tertiaryContainer to colores.onTertiaryContainer
        PrioridadOt.BAJA -> colores.secondaryContainer to colores.onSecondaryContainer
        PrioridadOt.SIN_ESPECIFICAR -> colores.surfaceVariant to colores.onSurfaceVariant
    }
    BadgeTexto("Prioridad ${prioridad.etiqueta.lowercase()}", fondo, texto)
}

@Composable
private fun BadgeTexto(etiqueta: String, fondo: Color, texto: Color) {
    Surface(
        color = fondo,
        contentColor = texto,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            etiqueta,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
