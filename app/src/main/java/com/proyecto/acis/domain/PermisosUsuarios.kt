package com.proyecto.acis.domain

import com.proyecto.acis.data.Perfil
import com.proyecto.acis.data.Usuario

/** El administrador gestiona accesos; el planificador mantiene las operaciones de las OT. */
object PermisosUsuarios {
    // Se requieren las dos condiciones: cuenta activa y rol administrativo.
    fun puedeGestionar(actor: Usuario?): Boolean = actor?.activo == true && actor.perfil == Perfil.ADMINISTRADOR

    // actor es quien solicita el cambio; destino es la cuenta editada.
    // perfil y activo son los valores propuestos, no los que ya están guardados.
    fun puedeModificar(actor: Usuario?, destino: Usuario?, perfil: Perfil, activo: Boolean): Boolean {
        // Rechaza solicitudes sin autorización o dirigidas a una cuenta inexistente.
        if (!puedeGestionar(actor) || destino == null) return false
        // Impide perder el propio acceso administrativo y dejar la instalación sin administrador.
        if (actor?.correo == destino.correo && (!activo || perfil != Perfil.ADMINISTRADOR)) return false
        return true
    }
}
