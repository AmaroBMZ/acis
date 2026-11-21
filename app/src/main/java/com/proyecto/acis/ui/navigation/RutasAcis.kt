package com.proyecto.acis.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// Rutas de navegación dentro de la app ACIS
internal object RutasAcis {
    const val ORDENES = "ordenes"
    const val CREAR = "crear"
    const val EDITAR = "editar/{ordenId}"
    const val DETALLE = "detalle/{ordenId}"

    fun detalle(ordenId: String) = "detalle/$ordenId"
    fun editar(ordenId: String) = "editar/$ordenId"
}
