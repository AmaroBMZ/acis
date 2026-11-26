package com.proyecto.acis.domain

import com.proyecto.acis.data.Usuario

/** Contrato del acceso local. La sesión no necesita conocer SQLite ni consultas SQL. */
interface AutenticacionRepository {
    suspend fun preparar()
    suspend fun autenticar(correo: String, clave: String): Usuario?
}
