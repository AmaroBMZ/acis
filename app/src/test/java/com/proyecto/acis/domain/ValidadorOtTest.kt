package com.proyecto.acis.domain

import com.proyecto.acis.data.PrioridadOt
import com.proyecto.acis.data.TipoOt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para la lógica de validación de dominio (ValidadorOt).
 */
class ValidadorOtTest {

    @Test
    fun crearOtConDatosVaciosRetornaErrores() {
        val datosVacios = DatosCrearOt()
        val errores = ValidadorOt.crear(datosVacios)

        assertTrue("Debe indicar que existen errores en los datos vacíos", errores.hayErrores)
        assertNotNull("Debe exigir el campo local", errores.local)
        assertNotNull("Debe exigir el campo equipo", errores.equipo)
        assertNotNull("Debe exigir el campo descripción", errores.descripcion)
        assertNotNull("Debe exigir el tipo de trabajo", errores.tipo)
        assertNotNull("Debe exigir la prioridad", errores.prioridad)
        assertNotNull("Debe exigir la fecha", errores.fecha)
        assertNotNull("Debe exigir la hora", errores.hora)
    }

    @Test
    fun crearOtConDatosValidosPasaSinErrores() {
        val datosValidos = DatosCrearOt(
            local = "Mall Plaza Vespucio",
            equipo = "Rooftop Carrier 15 TR",
            descripcion = "Mantenimiento preventivo anual",
            tipo = TipoOt.PREVENTIVA,
            prioridad = PrioridadOt.ALTA,
            fecha = "28/10/2026",
            hora = "10:00",
        )
        val errores = ValidadorOt.crear(datosValidos)

        assertFalse("No debe contener errores cuando todos los datos son válidos", errores.hayErrores)
        assertNull(errores.local)
        assertNull(errores.equipo)
        assertNull(errores.descripcion)
        assertNull(errores.tipo)
        assertNull(errores.prioridad)
        assertNull(errores.fecha)
        assertNull(errores.hora)
    }

    @Test
    fun informeTecnicoConDiagnosticoYActividadValidos() {
        val diagnostico = "Fuga en válvula Schrader de alta presión."
        val actividad = "Reemplazo de válvula y recarga de refrigerante R404A."

        val errores = ValidadorOt.informe(diagnostico, actividad)

        assertFalse("El informe no debe tener errores si diagnóstico y actividad están completos", errores.hayErrores)
        assertNull(errores.diagnostico)
        assertNull(errores.actividad)
    }

    @Test
    fun informeTecnicoSinDiagnosticoRetornaError() {
        val errores = ValidadorOt.informe("", "Limpieza de filtros")

        assertTrue("Debe retornar error si el diagnóstico está vacío", errores.hayErrores)
        assertEquals("Ingresa el diagnóstico", errores.diagnostico)
        assertNull(errores.actividad)
    }

    @Test
    fun observacionClienteValidaYVacia() {
        assertNotNull("Observación vacía debe retornar mensaje de error", ValidadorOt.observacion("   "))
        assertNull("Observación con texto válido debe retornar null", ValidadorOt.observacion("Trabajo conforme"))
    }

    @Test
    fun respuestaTecnicoValidaYVacia() {
        assertNotNull("Respuesta vacía debe retornar mensaje de error", ValidadorOt.respuesta(""))
        assertNull("Respuesta con contenido debe retornar null", ValidadorOt.respuesta("Se adjunta medición de amperaje"))
    }
}
