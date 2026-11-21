package com.proyecto.acis.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

// Pantalla de acceso local: recoge credenciales y delega la autenticación al ViewModel.
@Composable
fun LoginScreen(viewModel: OrdenesViewModel, modifier: Modifier = Modifier) {
    // rememberSaveable conserva el correo ante recreaciones de la actividad.
    var correo by rememberSaveable { mutableStateOf("") }
    // La contraseña no se guarda en el estado persistente de Android.
    var clave by remember { mutableStateOf("") }
    // Distribución vertical desplazable; imePadding reserva espacio para el teclado.
    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("ACIS", style = MaterialTheme.typography.headlineLarge)
        Text("Ingresa a tu cuenta", style = MaterialTheme.typography.titleLarge)
        Text("Gestión de órdenes de trabajo en este dispositivo")
        OutlinedTextField(correo, { correo = it }, label = { Text("Correo") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(clave, { clave = it }, label = { Text("Contraseña") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        // Muestra errores y progreso publicados por el ViewModel.
        viewModel.errorGeneral?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (viewModel.ocupado) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        // Permite reintentar si la carga inicial no se completó.
        if (!viewModel.cargado) {
            Button(onClick = viewModel::cargar, enabled = !viewModel.ocupado) { Text("Reintentar carga") }
        }
        // Ingresar requiere datos cargados, campos completos y ninguna operación en curso.
        Button(onClick = { viewModel.iniciarSesion(correo, clave) },
            enabled = viewModel.cargado && !viewModel.ocupado && correo.isNotBlank() && clave.isNotBlank(),
            modifier = Modifier.fillMaxWidth()) { Text("Ingresar") }
        Text("Cuentas de prueba", style = MaterialTheme.typography.titleMedium)
        Text("admin@acis.cl")
        Text("planificador@acis.cl\ntecnico@acis.cl\ncliente@acis.cl\ncliente2@acis.cl")
        Text("Contraseña de prueba: Acis2026!")
    }
}
