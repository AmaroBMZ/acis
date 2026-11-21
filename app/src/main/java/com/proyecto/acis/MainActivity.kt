package com.proyecto.acis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.proyecto.acis.ui.navigation.AcisApp
import com.proyecto.acis.ui.OrdenesViewModel
import com.proyecto.acis.ui.theme.AcisTheme

// Android crea esta actividad al abrir la app; aquí se conecta el ciclo de vida con Compose.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Inicializa la actividad antes de preparar su contenido.
        super.onCreate(savedInstanceState)
        // Permite dibujar hasta los bordes; el padding del Scaffold respeta las barras del sistema.
        enableEdgeToEdge()
        // ViewModelProvider conserva la misma instancia durante cambios de configuración, como rotar.
        val ordenesViewModel = ViewModelProvider(this)[OrdenesViewModel::class.java]
        // Define la interfaz declarativa: tema compartido, contenedor y pantalla principal.
        setContent {
            AcisTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AcisApp(viewModel = ordenesViewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
