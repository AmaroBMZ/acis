package com.proyecto.acis.data

import com.proyecto.acis.domain.UsuariosRepository

/** Adaptador que reutiliza la conexión y el Mutex compartidos del almacenamiento local. */
class UsuariosLocalRepository(private val local: OrdenesRepository) : UsuariosRepository {
    // Comparte el flujo publicado por SQLite; no mantiene una segunda lista potencialmente desactualizada.
    override val usuarios = local.usuarios
    // Delega en la operación serializada y ejecutada en IO por el repositorio local.
    override suspend fun registrar(actor: String, nombre: String, correo: String, perfil: Perfil, clave: String) =
        local.registrarUsuario(actor, nombre, correo, perfil, clave)
    override suspend fun actualizarAcceso(actor: String, correo: String, perfil: Perfil, activo: Boolean) =
        local.actualizarAcceso(actor, correo, perfil, activo)
}
