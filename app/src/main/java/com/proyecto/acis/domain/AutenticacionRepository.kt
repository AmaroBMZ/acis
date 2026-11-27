package com.proyecto.acis.domain

import com.proyecto.acis.data.Usuario

data class SesionAutenticada(val usuario: Usuario, val token: String? = null)

/** Contrato del acceso local. La sesión no necesita conocer SQLite ni consultas SQL. */
interface AutenticacionRepository {
    suspend fun preparar()
    suspend fun autenticar(correo: String, clave: String): SesionAutenticada?
}
