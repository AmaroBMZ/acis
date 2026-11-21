package com.proyecto.acis

import com.proyecto.acis.data.*
import com.proyecto.acis.domain.*
import org.junit.Assert.*
import org.junit.Test

// Pruebas JVM de reglas de negocio; copy cambia un campo del caso válido para aislar cada error.
class ValidadorOtTest {
    private val valido = DatosCrearOt("Local 1", "Equipo 1", "Mantención", TipoOt.PREVENTIVA,
        PrioridadOt.MEDIA, "18/11/2026", "09:30")

    // Comprueba el caso base que cumple todos los requisitos.
    @Test fun formularioCompletoEsValido() {
        assertFalse(ValidadorOt.crear(valido).hayErrores)
    }
    // Exige mensajes en los campos incompletos de un formulario nuevo.
    @Test fun camposVaciosImpidenCrear() {
        val errores = ValidadorOt.crear(DatosCrearOt())
        assertTrue(errores.hayErrores)
        assertNotNull(errores.local); assertNotNull(errores.equipo); assertNotNull(errores.descripcion)
        assertNotNull(errores.tipo); assertNotNull(errores.prioridad); assertNotNull(errores.fecha); assertNotNull(errores.hora)
    }
    // Rechaza espacios, tabulaciones y saltos de línea usados como contenido.
    @Test fun espaciosNoSonContenido() {
        assertNotNull(ValidadorOt.crear(valido.copy(local = "  ")).local)
        assertTrue(ValidadorOt.informe(" ", "\n").hayErrores)
        assertNotNull(ValidadorOt.observacion(" "))
        assertNotNull(ValidadorOt.respuesta("\t"))
    }
    // Recorre fechas inexistentes, formato incompleto y sufijos inválidos.
    @Test fun rechazaFechaImposibleYTextoExtra() {
        listOf("31/02/2026", "29/02/2025", "18/13/2026", "1/1/2026", "18/11/2026x").forEach {
            assertNotNull(it, ValidadorOt.crear(valido.copy(fecha = it)).fecha)
        }
    }
    // Comprueba que una fecha real de año bisiesto sí se acepta.
    @Test fun aceptaFebreroBisiesto() {
        assertNull(ValidadorOt.crear(valido.copy(fecha = "29/02/2028")).fecha)
    }
    // Rechaza horas/minutos fuera de rango y formatos incompletos.
    @Test fun rechazaHorasFueraDeRango() {
        listOf("24:00", "12:60", "-1:00", "9:30", "10:30x").forEach {
            assertNotNull(it, ValidadorOt.crear(valido.copy(hora = it)).hora)
        }
    }
    // Acepta los extremos válidos del formato de 24 horas.
    @Test fun aceptaLimitesDeHora() {
        assertNull(ValidadorOt.crear(valido.copy(hora = "00:00")).hora)
        assertNull(ValidadorOt.crear(valido.copy(hora = "23:59")).hora)
    }
    // SIN_ESPECIFICAR representa ausencia de selección y debe generar error.
    @Test fun exigeTipoYPrioridadSeleccionados() {
        assertNotNull(ValidadorOt.crear(valido.copy(tipo = TipoOt.SIN_ESPECIFICAR)).tipo)
        assertNotNull(ValidadorOt.crear(valido.copy(prioridad = PrioridadOt.SIN_ESPECIFICAR)).prioridad)
    }
    // El propietario debe ser una cuenta demo con perfil CLIENTE.
    @Test fun exigeCuentaDeClienteValida() {
        assertNotNull(ValidadorOt.crear(valido.copy(clienteCorreo = "tecnico@acis.cl")).clienteCorreo)
        assertNotNull(ValidadorOt.crear(valido.copy(clienteCorreo = "")).clienteCorreo)
    }
    // Comprueba los dos campos exigidos por el validador del informe.
    @Test fun informeExigeDiagnosticoYActividadPeroNoMateriales() {
        assertFalse(ValidadorOt.informe("Fuga", "Reemplazo de válvula").hayErrores)
        assertTrue(ValidadorOt.informe("Fuga", "").hayErrores)
        assertTrue(ValidadorOt.informe("", "Reparación").hayErrores)
    }
    // Un texto no vacío permite enviar la observación o la respuesta.
    @Test fun observacionYRespuestaConContenidoSonValidas() {
        assertNull(ValidadorOt.observacion("Falta medición"))
        assertNull(ValidadorOt.respuesta("Medición adjunta"))
    }
}
