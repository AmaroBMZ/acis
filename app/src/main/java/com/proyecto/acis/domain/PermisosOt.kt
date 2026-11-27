package com.proyecto.acis.domain

import com.proyecto.acis.data.*

/** La identidad autenticada determina el acceso, no un selector visual de perfiles. */
object PermisosOt {
    // Las OTs remotas guardan correo del técnico; acepta también el nombre usado por semillas locales.
    fun puedeVer(usuario: Usuario?, orden: OrdenTrabajo): Boolean {
        if (usuario == null || !usuario.activo) return false
        return when (usuario.perfil) {
        Perfil.PLANIFICADOR -> true
        Perfil.TECNICO -> orden.tecnico.equals(usuario.correo, ignoreCase = true) ||
            orden.tecnico.equals(usuario.nombre, ignoreCase = true)
        Perfil.CLIENTE -> orden.clienteCorreo == usuario.correo
        Perfil.ADMINISTRADOR -> false
        }
    }

    // Exige simultáneamente el rol solicitado y visibilidad. El estado de la OT se controla aparte.
    fun puedeActuar(usuario: Usuario?, orden: OrdenTrabajo, rol: Perfil): Boolean =
        usuario?.perfil == rol && puedeVer(usuario, orden)
}
