package com.proyecto.acis

import com.proyecto.acis.data.*
import com.proyecto.acis.domain.PermisosUsuarios
import com.proyecto.acis.domain.PermisosOt
import org.junit.Assert.*
import org.junit.Test

class PermisosUsuariosTest {
    // Identidades en memoria: estas pruebas validan reglas sin abrir SQLite ni iniciar Android.
    private val admin = Usuario("admin@acis.cl", "Admin", Perfil.ADMINISTRADOR)
    private val cliente = Usuario("cliente@acis.cl", "Cliente", Perfil.CLIENTE)

    @Test fun soloUnAdministradorActivoPuedeGestionar() {
        // Compara el caso autorizado con ausencia de sesión, cuenta desactivada y otros roles.
        assertTrue(PermisosUsuarios.puedeGestionar(admin))
        assertFalse(PermisosUsuarios.puedeGestionar(null))
        assertFalse(PermisosUsuarios.puedeGestionar(admin.copy(activo = false)))
        Perfil.entries.filter { it != Perfil.ADMINISTRADOR }.forEach {
            assertFalse(PermisosUsuarios.puedeGestionar(admin.copy(perfil = it)))
        }
    }

    @Test fun administradorPuedeModificarOtraCuentaExistente() {
        // Cambiar otra cuenta existente está permitido; inventar un destino o usar otro rol no lo está.
        assertTrue(PermisosUsuarios.puedeModificar(admin, cliente, Perfil.TECNICO, true))
        assertTrue(PermisosUsuarios.puedeModificar(admin, cliente, Perfil.CLIENTE, false))
        assertFalse(PermisosUsuarios.puedeModificar(admin, null, Perfil.CLIENTE, true))
        assertFalse(PermisosUsuarios.puedeModificar(cliente, admin, Perfil.CLIENTE, false))
    }

    @Test fun administradorNoPuedePerderSuPropioAcceso() {
        // Evita bloquear la administración por una desactivación o degradación de la propia cuenta.
        assertFalse(PermisosUsuarios.puedeModificar(admin, admin, Perfil.CLIENTE, true))
        assertFalse(PermisosUsuarios.puedeModificar(admin, admin, Perfil.ADMINISTRADOR, false))
        assertTrue(PermisosUsuarios.puedeModificar(admin, admin, Perfil.ADMINISTRADOR, true))
    }

    @Test fun administradorNoHeredaLosPermisosDelPlanificador() {
        // Administrar identidades no concede operaciones de negocio sobre las órdenes.
        val orden = OrdenTrabajo("OT-1", "Local", "Equipo", "Trabajo", "21/11/2026 10:00")
        assertFalse(PermisosOt.puedeVer(admin, orden))
        assertFalse(PermisosOt.puedeActuar(admin, orden, Perfil.PLANIFICADOR))
        assertFalse(PermisosOt.puedeVer(admin.copy(perfil = Perfil.PLANIFICADOR, activo = false), orden))
    }
}
