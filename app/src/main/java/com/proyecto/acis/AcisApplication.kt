package com.proyecto.acis

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.proyecto.acis.data.AutenticacionRemotaRepository
import com.proyecto.acis.data.OrdenesRepository
import com.proyecto.acis.data.OrdenesRemotoRepository
import com.proyecto.acis.data.UsuariosRemotoRepository
import com.proyecto.acis.session.SesionActual
import com.proyecto.acis.ui.OrdenesViewModel
import com.proyecto.acis.ui.SesionViewModel

/** Punto de composición: construye dependencias una vez para el proceso, sin bibliotecas de DI. */
class AcisApplication : Application() {
    private val repository by lazy { OrdenesRepository(this) }
    private val usuariosRepository by lazy { UsuariosRemotoRepository() }
    private val ordenesRemotoRepository by lazy { OrdenesRemotoRepository() }
    private val sesion = SesionActual()

    // La conexión compartida vive durante el proceso; ningún ViewModel la cierra por separado.
    val viewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val instancia = when (modelClass) {
                SesionViewModel::class.java -> SesionViewModel(AutenticacionRemotaRepository(), sesion)
                OrdenesViewModel::class.java -> OrdenesViewModel(repository, sesion, usuariosRepository, ordenesRemotoRepository)
                com.proyecto.acis.ui.UsuariosViewModel::class.java -> com.proyecto.acis.ui.UsuariosViewModel(usuariosRepository, sesion)
                else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
            }
            return modelClass.cast(instancia)!!
        }
    }
}
