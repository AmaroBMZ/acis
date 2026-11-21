package com.proyecto.acis.ui.screens.ordenes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import com.proyecto.acis.data.PrioridadOt
import com.proyecto.acis.data.TipoOt
import java.util.Calendar
import java.util.Locale
import com.proyecto.acis.ui.OrdenesViewModel
import com.proyecto.acis.ui.components.*

/**
 * Formulario para ingresar una nueva Orden de Trabajo.
 * Contiene campos de texto con validación y selectores de fecha/hora.
 */
@Composable
internal fun CrearOrden(
    viewModel: OrdenesViewModel,
    onCrear: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    // Fecha inicial de los selectores; remember evita recrearla en cada recomposición.
    val ahora = remember { Calendar.getInstance() }
    val datos = viewModel.datosCrear
    val errores = viewModel.erroresCrear
    // Estado propio de los diálogos de fecha y hora; el formulario definitivo vive en el ViewModel.
    var dia by rememberSaveable { mutableStateOf(ahora.get(Calendar.DAY_OF_MONTH)) }
    var mes by rememberSaveable { mutableStateOf(ahora.get(Calendar.MONTH)) }
    var anio by rememberSaveable { mutableStateOf(ahora.get(Calendar.YEAR)) }
    var hora by rememberSaveable { mutableStateOf(ahora.get(Calendar.HOUR_OF_DAY)) }
    var minuto by rememberSaveable { mutableStateOf(ahora.get(Calendar.MINUTE)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Nueva orden de trabajo", style = MaterialTheme.typography.headlineSmall) }
        item { SelectorCliente(viewModel) }
            item { CampoFormulario(datos.local, viewModel::cambiarLocal, "Local", errores.local) }
        item { CampoFormulario(datos.equipo, viewModel::cambiarEquipo, "Equipo", errores.equipo) }
        item { CampoFormulario(datos.descripcion, viewModel::cambiarDescripcion,
            "Descripción del trabajo", errores.descripcion, minLines = 3) }
        item { Text("Tipo de trabajo", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                TipoOt.entries.filter { it != TipoOt.SIN_ESPECIFICAR }.forEach { tipo ->
                    FilterChip(selected = datos.tipo == tipo,
                        onClick = { viewModel.cambiarTipo(tipo) }, label = { Text(tipo.etiqueta) })
                }
            }
        }
        if (errores.tipo != null) item { ErrorSeleccion(errores.tipo) }
        item { Text("Prioridad", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
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
            Button(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onCrear()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.ocupado,
            ) { Text("Guardar OT") }
        }
    }
}
