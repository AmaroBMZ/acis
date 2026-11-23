package com.proyecto.acis.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// Rutas de navegación dentro de la app ACIS
internal object RutasAcis {
    const val ORDENES = "ordenes"
    const val CREAR = "crear"
    // Las llaves declaran el argumento que NavHost recupera desde la ruta concreta.
    const val EDITAR = "editar/{ordenId}"
    const val DETALLE = "detalle/{ordenId}"

    // Pasa el ID, no una copia de la OT: el destino busca sus datos en la lista vigente.
    fun detalle(ordenId: String) = "detalle/$ordenId"
    // Sustituye el argumento de la ruta por el ID de la orden seleccionada.
    fun editar(ordenId: String) = "editar/$ordenId"
}
