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
import com.proyecto.acis.ui.AcisApp
import com.proyecto.acis.ui.OrdenesViewModel
import com.proyecto.acis.ui.theme.AcisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val ordenesViewModel = ViewModelProvider(this)[OrdenesViewModel::class.java]
        setContent {
            AcisTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AcisApp(viewModel = ordenesViewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
