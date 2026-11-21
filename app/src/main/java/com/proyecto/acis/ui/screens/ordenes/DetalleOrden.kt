package com.proyecto.acis.ui.screens.ordenes

import android.content.ActivityNotFoundException
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.compose.composable
import com.proyecto.acis.data.EstadoOt
import com.proyecto.acis.data.OrdenTrabajo
import com.proyecto.acis.data.Perfil
import com.proyecto.acis.data.obtenerUbicacionCheckIn
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch
import com.proyecto.acis.ui.OrdenesViewModel
import com.proyecto.acis.ui.components.*

/**
 * Pantalla de detalle e intervención técnica de una Orden de Trabajo.
 * Permite realizar check-in con ubicación, capturar foto de evidencia,
 * completar el informe, revisar observaciones, dar conformidad y eliminar la orden.
 */
@Composable
internal fun DetalleOrden(
    orden: OrdenTrabajo,
    perfil: Perfil,
    viewModel: OrdenesViewModel,
    onVolver: () -> Unit,
    onEditar: () -> Unit = {},
) {
    val context = LocalContext.current
    val scopeUbicacion = rememberCoroutineScope()
    var buscandoUbicacion by rememberSaveable(orden.id) { mutableStateOf(false) }
    // Lanza la búsqueda sin bloquear la pantalla y pasa el resultado al inicio de visita.
    val iniciarConUbicacion: () -> Unit = {
        buscandoUbicacion = true
        scopeUbicacion.launch {
            val ubicacion = obtenerUbicacionCheckIn(context)
            viewModel.iniciarVisita(orden.id, ubicacion)
            buscandoUbicacion = false
        }
    }
    // El resultado del permiso decide si buscar coordenadas o registrar el check-in sin ubicación.
    val permisoUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) iniciarConUbicacion()
        else viewModel.iniciarVisita(orden.id, null)
    }
    // Guarda el nombre del archivo pendiente mientras la app de cámara está abierta.
    var fotoPendiente by rememberSaveable(orden.id) { mutableStateOf("") }
    var errorCamara by rememberSaveable(orden.id) { mutableStateOf("") }
    // TakePicture informa si se capturó la imagen; después se intenta asociarla a la OT.
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
        val nombre = fotoPendiente
        fotoPendiente = ""
        if (nombre.isNotBlank()) {
            val archivo = File(File(context.filesDir, "evidencias"), nombre)
            if (guardada) {
                viewModel.guardarFoto(orden.id, nombre) { guardado ->
                    if (guardado) errorCamara = ""
                    else {
                        archivo.delete()
                        errorCamara = "No se pudo guardar la foto."
                    }
                }
            } else {
                archivo.delete()
                if (guardada) errorCamara = "No se pudo guardar la foto."
            }
        }
    }
    // Recupera el borrador de esta OT y calcula los errores que corresponde mostrar.
    val formulario = viewModel.formularioDetalle(orden)
    val erroresInforme = viewModel.erroresInforme(orden.id, formulario.diagnostico, formulario.actividad)
    val errorObservacion = viewModel.errorObservacion(orden.id, formulario.observacion)
    val errorRespuesta = viewModel.errorRespuesta(orden.id, formulario.respuesta)
    val mensaje = viewModel.mensaje(orden.id)

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
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
                    CampoDetalle("Cliente", orden.clienteCorreo.ifBlank { "Pendiente de asignación" })
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
                        // Evita decodificar de nuevo la misma evidencia en cada recomposición.
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
        // Asignación y edición por el Planificador (CRUD - Update)
        if (perfil == Perfil.PLANIFICADOR) {
            if (orden.estado == EstadoOt.CREADA) {
                item { Button(onClick = { viewModel.asignar(orden.id) }, modifier = Modifier.fillMaxWidth()) { Text("Asignar a técnico demo") } }
            }
            item {
                OutlinedButton(onClick = onEditar, modifier = Modifier.fillMaxWidth()) {
                    Text("Editar datos de esta OT")
                }
            }
        }
        // Aceptación por el Técnico
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.ASIGNADA) {
            item { Button(onClick = { viewModel.aceptar(orden.id) }, modifier = Modifier.fillMaxWidth()) { Text("Aceptar OT") } }
        }
        // Check-in con ubicación por el Técnico
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.ACEPTADA) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Se pedirá ubicación aproximada para el check-in. Puedes continuar si no la permites.",
                        style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) iniciarConUbicacion()
                        else permisoUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }, enabled = !buscandoUbicacion, modifier = Modifier.fillMaxWidth()) {
                        Text(if (buscandoUbicacion) "Buscando ubicación..." else "Iniciar visita y registrar check-in")
                    }
                }
            }
        }
        // Informe técnico y toma de fotografía durante la visita
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
                        // Comparte el archivo con la cámara mediante una URI content del FileProvider.
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
                }, modifier = Modifier.fillMaxWidth()) { Text(if (orden.fotoEvidencia.isBlank()) "Tomar foto de evidencia" else "Cambiar foto de evidencia") }
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
                    }, modifier = Modifier.fillMaxWidth()) { Text("Guardar borrador") }
                    Button(onClick = {
                        viewModel.finalizarVisita(orden.id, formulario.diagnostico, formulario.actividad, formulario.materiales) { guardado -> if (guardado) onVolver() }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Finalizar visita") }
                }
            }
        }
        // Conformidad u observaciones por parte del Cliente
        if (perfil == Perfil.CLIENTE && orden.estado == EstadoOt.PENDIENTE_CONFORMIDAD) {
            item { Text("Revisar conformidad", style = MaterialTheme.typography.titleLarge) }
            item { CampoFormulario(formulario.observacion, { viewModel.cambiarObservacion(orden.id, it) },
                "Observación si no estás conforme", errorObservacion) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.aprobar(orden.id) { guardado -> if (guardado) onVolver() } }, modifier = Modifier.fillMaxWidth()) { Text("Aprobar") }
                    OutlinedButton(onClick = {
                        viewModel.observar(orden.id, formulario.observacion) { guardado -> if (guardado) onVolver() }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Observar") }
                }
            }
        }
        // Respuesta del técnico a una observación del cliente
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.OBSERVADA) {
            item { Text("Responder observación", style = MaterialTheme.typography.titleLarge) }
            item { CampoFormulario(formulario.respuesta, { viewModel.cambiarRespuesta(orden.id, it) }, "Respuesta al cliente",
                errorRespuesta, minLines = 2) }
            item { Button(onClick = {
                viewModel.responderObservacion(orden.id, formulario.respuesta) { guardado -> if (guardado) onVolver() }
            }, modifier = Modifier.fillMaxWidth()) { Text("Enviar respuesta") } }
        }
        // Opción para eliminar la OT (disponible para el Planificador - CRUD Delete)
        if (perfil == Perfil.PLANIFICADOR) {
            item {
                var mostrarConfirmacion by rememberSaveable(orden.id) { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { mostrarConfirmacion = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Eliminar esta orden de trabajo")
                    }
                    if (mostrarConfirmacion) {
                        AlertDialog(
                            onDismissRequest = { if (!viewModel.ocupado) mostrarConfirmacion = false },
                            title = { Text("Eliminar Orden de Trabajo") },
                            text = { Text("¿Estás seguro de que deseas eliminar la orden ${orden.id}? Esta acción borrará permanentemente sus registros de terreno y foto de evidencia.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.borrar(orden.id) { borrada ->
                                            if (borrada) {
                                                mostrarConfirmacion = false
                                                onVolver()
                                            } else mostrarConfirmacion = false
                                        }
                                    },
                                    enabled = !viewModel.ocupado,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Text("Eliminar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { mostrarConfirmacion = false }, enabled = !viewModel.ocupado) {
                                    Text("Cancelar")
                                }
                            },
                        )
                    }
                }
            }
        }
        if (mensaje.isNotBlank()) item { Text(mensaje, color = MaterialTheme.colorScheme.primary) }
    }
}
