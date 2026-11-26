package com.proyecto.acis.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Pruebas unitarias para el modelo de datos OrdenTrabajo y los Enums de estado/prioridad.
 */
class OrdenTrabajoTest {

    @Test
    fun ordenTrabajoValoresPorDefecto() {
        val orden = OrdenTrabajo(
            id = "OT-1001",
            local = "Supermercado Unimarc",
            equipo = "Cámara Bitzer",
            descripcion = "Mantención preventiva",
            fechaProgramada = "28/10/2026 11:30",
        )

        assertEquals("OT-1001", orden.id)
        assertEquals(EstadoOt.CREADA, orden.estado)
        assertEquals(TipoOt.SIN_ESPECIFICAR, orden.tipo)
        assertEquals(PrioridadOt.SIN_ESPECIFICAR, orden.prioridad)
        assertEquals("", orden.tecnico)
        assertEquals("", orden.checkIn)
        assertEquals("", orden.fotoEvidencia)
    }

    @Test
    fun etiquetasDeEstados() {
        assertEquals("Creada", EstadoOt.CREADA.etiqueta)
        assertEquals("Asignada", EstadoOt.ASIGNADA.etiqueta)
        assertEquals("Aceptada", EstadoOt.ACEPTADA.etiqueta)
        assertEquals("En visita", EstadoOt.EN_VISITA.etiqueta)
        assertEquals("Pendiente de conformidad", EstadoOt.PENDIENTE_CONFORMIDAD.etiqueta)
        assertEquals("Observada", EstadoOt.OBSERVADA.etiqueta)
        assertEquals("Cerrada", EstadoOt.CERRADA.etiqueta)
    }

    @Test
    fun etiquetasDePerfiles() {
        assertEquals("Planificador", Perfil.PLANIFICADOR.etiqueta)
        assertEquals("Técnico", Perfil.TECNICO.etiqueta)
        assertEquals("Cliente", Perfil.CLIENTE.etiqueta)
    }
}
