package com.proyecto.acis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import com.proyecto.acis.data.EstadoOt
import com.proyecto.acis.data.Perfil
import com.proyecto.acis.data.PrioridadOt
import kotlinx.coroutines.launch
import com.proyecto.acis.ui.OrdenesViewModel

@Composable
// Respaldo cuando el ID no existe o la cuenta no puede consultar esa orden.
internal fun OrdenNoDisponible(onVolver: () -> Unit) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("La orden no existe o no está disponible para tu cuenta.")
        Button(onClick = onVolver) { Text("Volver") }
    }
}

@Composable
// Presenta un menú desplegable solo con clientes activos.
internal fun SelectorCliente(viewModel: OrdenesViewModel) {
    val usuarios by viewModel.usuarios.collectAsState()
    val clientes = usuarios.filter { it.activo && it.perfil == Perfil.CLIENTE }
    SelectorUsuario(
        titulo = "Cliente de la orden",
        seleccionado = clientes.firstOrNull { it.correo == viewModel.datosCrear.clienteCorreo }?.let { "${it.nombre} · ${it.correo}" },
        opciones = clientes.map { it.correo to "${it.nombre} · ${it.correo}" },
        error = viewModel.erroresCrear.clienteCorreo,
        alSeleccionar = viewModel::cambiarCliente,
    )
}

@Composable
// Mantiene el menú de técnicos separado del selector de clientes en la creación de una OT.
internal fun SelectorTecnico(viewModel: OrdenesViewModel) {
    val usuarios by viewModel.usuarios.collectAsState()
    val tecnicos = usuarios.filter { it.activo && it.perfil == Perfil.TECNICO }
    SelectorUsuario(
        titulo = "Técnico asignado",
        seleccionado = tecnicos.firstOrNull { it.correo == viewModel.datosCrear.tecnicoCorreo }?.let { "${it.nombre} · ${it.correo}" },
        opciones = tecnicos.map { it.correo to "${it.nombre} · ${it.correo}" },
        error = viewModel.erroresCrear.tecnicoCorreo,
        alSeleccionar = viewModel::cambiarTecnico,
    )
}

@Composable
private fun SelectorUsuario(
    titulo: String,
    seleccionado: String?,
    opciones: List<Pair<String, String>>,
    error: String?,
    alSeleccionar: (String) -> Unit,
) {
    var expandido by remember { mutableStateOf(false) }
    Column {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(onClick = { expandido = true }, modifier = Modifier.fillMaxWidth()) {
                Text(seleccionado ?: "Seleccionar ${titulo.lowercase()}")
            }
            DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                opciones.forEach { (valor, etiqueta) ->
                    DropdownMenuItem(
                        text = { Text(etiqueta) },
                        onClick = {
                            alSeleccionar(valor)
                            expandido = false
                        },
                    )
                }
            }
        }
        if (opciones.isEmpty()) Text("No hay usuarios activos disponibles.", color = MaterialTheme.colorScheme.error)
        error?.let { ErrorSeleccion(it) }
    }
}

/**
 * Campo de texto estandarizado (OutlinedTextField) con soporte para validación,
 * mensaje de error e icono de advertencia (M3).
 */
@Composable
internal fun CampoFormulario(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String? = null,
    minLines: Int = 1,
) {
    // Al enfocar el campo, solicita desplazarlo a una zona visible sobre el teclado.
    val traerAlFrente = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    OutlinedTextField(
        value = valor,
        // Entrega el texto transformado al dueño del estado; este componente no guarda datos en SQLite.
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

/**
 * Utilidad para capitalizar automáticamente la primera letra de cada oración.
 */
internal fun capitalizarOraciones(texto: String): String = buildString {
    // Recorre letras conservando el texto; tras puntuación o salto de línea vuelve a capitalizar.
    var inicio = true
    texto.forEach { letra ->
        append(if (inicio && letra.isLetter()) letra.uppercaseChar() else letra)
        if (letra.isLetter()) inicio = false
        if (letra == '.' || letra == '!' || letra == '?' || letra == '\n') inicio = true
    }
}

/**
 * Mensaje de error para selección de chips (Tipo / Prioridad).
 */
@Composable
internal fun ErrorSeleccion(mensaje: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(painterResource(android.R.drawable.ic_dialog_alert), contentDescription = null,
            tint = MaterialTheme.colorScheme.error)
        Text(mensaje, color = MaterialTheme.colorScheme.error)
    }
}

/**
 * Componente para mostrar un par etiqueta-valor en la vista de detalle.
 */
@Composable
internal fun CampoDetalle(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Badge redondeado con color según el estado actual de la Orden de Trabajo.
 */
@Composable
internal fun EstadoBadge(estado: EstadoOt) {
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

/**
 * Badge redondeado con color según la prioridad de la Orden de Trabajo.
 */
@Composable
internal fun PrioridadBadge(prioridad: PrioridadOt) {
    val colores = MaterialTheme.colorScheme
    val (fondo, texto) = when (prioridad) {
        PrioridadOt.ALTA -> colores.errorContainer to colores.onErrorContainer
        PrioridadOt.MEDIA -> colores.tertiaryContainer to colores.onTertiaryContainer
        PrioridadOt.BAJA -> colores.secondaryContainer to colores.onSecondaryContainer
        PrioridadOt.SIN_ESPECIFICAR -> colores.surfaceVariant to colores.onSurfaceVariant
    }
    BadgeTexto("Prioridad ${prioridad.etiqueta.lowercase()}", fondo, texto)
}

/**
 * Componente base para renderizar un Badge con fondo y texto personalizados.
 */
@Composable
internal fun BadgeTexto(etiqueta: String, fondo: Color, texto: Color) {
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
