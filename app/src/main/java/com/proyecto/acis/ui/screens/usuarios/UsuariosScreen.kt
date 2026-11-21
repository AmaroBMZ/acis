package com.proyecto.acis.ui.screens.usuarios

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.proyecto.acis.data.Perfil
import com.proyecto.acis.ui.OrdenesViewModel

/** Administración local de accesos. La lista nunca recibe hashes ni contraseñas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuariosScreen(viewModel: OrdenesViewModel, modifier: Modifier = Modifier) {
    val usuarios by viewModel.usuarios.collectAsState()
    var seleccionado by rememberSaveable { mutableStateOf<String?>(null) }
    var rol by rememberSaveable { mutableStateOf(Perfil.CLIENTE.name) }
    var activo by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf(false) }
    val destino = usuarios.firstOrNull { it.correo == seleccionado }
    BackHandler(enabled = viewModel.ocupado) { }

    Scaffold(modifier = modifier.fillMaxSize(), topBar = {
        TopAppBar(title = { Text("Administrar cuentas") }, actions = {
            TextButton(onClick = viewModel::cerrarSesion, enabled = !viewModel.ocupado) { Text("Salir") }
        })
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Perfil activo: Administrador", style = MaterialTheme.typography.titleMedium) }
            item { Text("Gestiona el acceso de las cuentas locales. Las órdenes las administra el planificador.") }
            if (viewModel.ocupado) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            items(usuarios, key = { it.correo }) { cuenta ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(cuenta.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(cuenta.correo)
                        Text("${cuenta.perfil.etiqueta} · ${if (cuenta.activo) "Activa" else "Desactivada"}")
                        OutlinedButton(onClick = {
                            seleccionado = cuenta.correo
                            rol = cuenta.perfil.name
                            activo = cuenta.activo
                            error = false
                        }, enabled = !viewModel.ocupado) { Text("Modificar acceso") }
                    }
                }
            }
        }
    }
    if (destino != null) {
        AlertDialog(onDismissRequest = { if (!viewModel.ocupado) seleccionado = null },
            title = { Text("Modificar acceso") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(destino.correo)
                    Text("Rol de la cuenta")
                    Perfil.entries.forEach { perfil ->
                        FilterChip(selected = rol == perfil.name, onClick = { rol = perfil.name },
                            enabled = !viewModel.ocupado, label = { Text(perfil.etiqueta) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Cuenta activa")
                        Switch(checked = activo, onCheckedChange = { activo = it }, enabled = !viewModel.ocupado)
                    }
                    Text("Los cambios de rol o activación se aplican al siguiente inicio de sesión de esa cuenta.")
                    if (error) Text(viewModel.errorGeneral.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = !viewModel.ocupado, onClick = {
                    error = false
                    viewModel.actualizarAcceso(destino.correo, Perfil.valueOf(rol), activo) { guardado ->
                        // Cierra después de guardar; si falla, conserva la selección para corregirla.
                        if (guardado) seleccionado = null else error = true
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { seleccionado = null }, enabled = !viewModel.ocupado) { Text("Cancelar") }
            })
    }
}
