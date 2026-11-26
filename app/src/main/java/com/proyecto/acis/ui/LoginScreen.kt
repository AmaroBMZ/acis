package com.proyecto.acis.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

// Pantalla de acceso local: recoge credenciales y delega la autenticación al ViewModel.
@Composable
fun LoginScreen(viewModel: SesionViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsState()
    // rememberSaveable conserva el correo ante recreaciones de la actividad.
    var correo by rememberSaveable { mutableStateOf("") }
    // La contraseña no se guarda en el estado persistente de Android.
    var clave by remember { mutableStateOf("") }
    // Distribución vertical desplazable; imePadding reserva espacio para el teclado.
    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Marca y descripción breve toman la jerarquía visual del diseño entregado.
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("ACIS", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("Gestión de servicios técnicos", style = MaterialTheme.typography.titleMedium)
            Text("Climatización y refrigeración", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text("Inicia sesión", style = MaterialTheme.typography.headlineMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Text("Accede a las órdenes de trabajo de tu cuenta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(correo, { correo = it }, label = { Text("Correo") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp))
        OutlinedTextField(clave, { clave = it }, label = { Text("Contraseña") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp))
        // Muestra errores y progreso publicados por el ViewModel.
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (estado.ocupado) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        // Permite reintentar si la carga inicial no se completó.
        if (!estado.cargado) {
            OutlinedButton(onClick = viewModel::cargar, enabled = !estado.ocupado, modifier = Modifier.fillMaxWidth()) { Text("Reintentar carga") }
        }
        // Ingresar requiere datos cargados, campos completos y ninguna operación en curso.
        Button(onClick = { viewModel.iniciarSesion(correo, clave) },
            enabled = estado.cargado && !estado.ocupado && correo.isNotBlank() && clave.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(28.dp)) { Text("Ingresar") }

        // Las cuentas demo se agrupan en una tarjeta aparte del formulario.
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cuentas de prueba", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Text("Administrador, planificador, técnico o cliente.", color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("admin@acis.cl · planificador@acis.cl\ntecnico@acis.cl · cliente@acis.cl · cliente2@acis.cl",
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                HorizontalDivider()
                Text("Contraseña de demostración", color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("Acis2026!", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            }
        }
        Text("Sin internet · Datos en este dispositivo\nEl acceso requiere una cuenta activa.",
            modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
