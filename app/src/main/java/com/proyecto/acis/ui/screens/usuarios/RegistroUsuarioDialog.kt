package com.proyecto.acis.ui.screens.usuarios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.proyecto.acis.data.Perfil
import com.proyecto.acis.ui.UsuariosViewModel

/** Borrador local del alta: cancelar no escribe; las claves no se guardan en el estado restaurable. */
@Composable
fun RegistroUsuarioDialog(viewModel: UsuariosViewModel, onCerrar: () -> Unit) {
    // Datos no secretos sobreviven a una recreación; las contraseñas se mantienen solo en memoria.
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var rol by rememberSaveable { mutableStateOf(Perfil.CLIENTE.name) }
    var clave by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = { if (!viewModel.ocupado) onCerrar() },
        title = { Text("Registrar usuario") },
        text = {
            // El contenido se desplaza para que todos los campos sigan accesibles con el teclado abierto.
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("La cuenta quedará activa. Entrega la contraseña inicial al usuario para que pueda ingresar.")
                CampoRegistro("Nombre completo", nombre, { nombre = it }, viewModel.errores["nombre"], !viewModel.ocupado)
                CampoRegistro("Correo del usuario", correo, { correo = it }, viewModel.errores["correo"], !viewModel.ocupado, KeyboardType.Email)
                Text("Rol de la cuenta")
                Perfil.entries.forEach { perfil ->
                    FilterChip(selected = rol == perfil.name, onClick = { rol = perfil.name },
                        enabled = !viewModel.ocupado, label = { Text(perfil.etiqueta) })
                }
                CampoRegistro("Contraseña inicial", clave, { clave = it }, viewModel.errores["clave"], !viewModel.ocupado, KeyboardType.Password)
                Text("De 8 a 128 caracteres, con letras y números.")
                CampoRegistro("Confirmar contraseña", confirmacion, { confirmacion = it }, viewModel.errores["confirmacion"], !viewModel.ocupado, KeyboardType.Password)
                viewModel.errorGeneral?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (viewModel.ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = !viewModel.ocupado, onClick = {
                // El diálogo se cierra solo tras confirmar el guardado; un fallo conserva lo ingresado.
                viewModel.registrar(nombre, correo, Perfil.valueOf(rol), clave, confirmacion) { creado ->
                    if (creado) { clave = ""; confirmacion = ""; onCerrar() }
                }
            }) { Text("Crear cuenta") }
        },
        dismissButton = { TextButton(enabled = !viewModel.ocupado, onClick = onCerrar) { Text("Cancelar") } },
    )
}

// Los errores pertenecen al campo correspondiente; los secretos se muestran enmascarados.
@Composable
private fun CampoRegistro(etiqueta: String, valor: String, cambiar: (String) -> Unit, error: String?, habilitado: Boolean, tipo: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value = valor, onValueChange = cambiar, label = { Text(etiqueta) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = tipo),
        visualTransformation = if (tipo == KeyboardType.Password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        isError = error != null, supportingText = error?.let { { Text(it) } })
}
