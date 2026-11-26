package com.proyecto.acis.data

import com.proyecto.acis.domain.AutenticacionRepository

/** Adaptador de transición: reutiliza la conexión, inicialización y Mutex existentes. */
class AutenticacionLocalRepository(private val ordenes: OrdenesRepository) : AutenticacionRepository {
    override suspend fun preparar() = ordenes.cargar()
    override suspend fun autenticar(correo: String, clave: String) = ordenes.autenticar(correo, clave)
}
