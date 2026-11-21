package com.proyecto.acis

import com.proyecto.acis.data.*
import com.proyecto.acis.domain.PermisosOt
import org.junit.Assert.*
import org.junit.Test

// Pruebas JVM de permisos con una OT y cuentas de ejemplo; no requieren base de datos.
class PermisosOtTest {
    private val orden = OrdenTrabajo("OT-1", "Local", "Equipo", "Trabajo", "18/11/2026 09:30",
        tecnico = "Técnico demo", clienteCorreo = CuentasPrueba.CLIENTE)
    private val planificador = CuentasPrueba.usuarios[0]
    private val tecnico = CuentasPrueba.usuarios[1]
    private val cliente = CuentasPrueba.usuarios[2]

    // Una identidad nula no puede consultar la OT.
    @Test fun sinSesionNoHayAcceso() { assertFalse(PermisosOt.puedeVer(null, orden)) }
    // El planificador puede consultar la orden sin ser su técnico ni cliente.
    @Test fun planificadorVeTodasLasOrdenes() { assertTrue(PermisosOt.puedeVer(planificador, orden)) }
    // Compara asignación propia, otro técnico y ausencia de asignación.
    @Test fun tecnicoSoloVeLasAsignadasASuIdentidad() {
        assertTrue(PermisosOt.puedeVer(tecnico, orden))
        assertFalse(PermisosOt.puedeVer(tecnico, orden.copy(tecnico = "Otro técnico")))
        assertFalse(PermisosOt.puedeVer(tecnico, orden.copy(tecnico = "")))
    }
    // El correo propietario separa las órdenes de los dos clientes.
    @Test fun clienteNoVeOrdenesAjenasNiSinPropietario() {
        assertTrue(PermisosOt.puedeVer(cliente, orden))
        assertFalse(PermisosOt.puedeVer(CuentasPrueba.usuarios[3], orden))
        assertFalse(PermisosOt.puedeVer(cliente, orden.copy(clienteCorreo = "")))
    }
    // La visibilidad del cliente depende de propiedad, no del estado.
    @Test fun clienteConsultaSuOrdenEnTodosLosEstados() {
        EstadoOt.entries.forEach { assertTrue(PermisosOt.puedeVer(cliente, orden.copy(estado = it))) }
    }
    // Consultar una OT no permite ejecutar acciones reservadas a otro perfil.
    @Test fun verUnaOrdenNoAutorizaAccionesDeOtroRol() {
        assertFalse(PermisosOt.puedeActuar(cliente, orden, Perfil.PLANIFICADOR))
        assertFalse(PermisosOt.puedeActuar(planificador, orden, Perfil.CLIENTE))
        assertFalse(PermisosOt.puedeActuar(tecnico, orden, Perfil.CLIENTE))
        assertTrue(PermisosOt.puedeActuar(cliente, orden, Perfil.CLIENTE))
    }
}
