package com.proyecto.acis

import com.proyecto.acis.data.Claves
import org.junit.Assert.*
import org.junit.Test

// Pruebas JVM de derivación: preparan sal/hash y comparan claves aceptadas y rechazadas.
class ClavesTest {
    // La comparación respeta el contenido exacto de la contraseña, incluidos los espacios.
    @Test fun claveCorrectaSeVerificaYLaIncorrectaSeRechaza() {
        val sal = Claves.nuevaSal()
        val hash = Claves.derivar("Acis2026!", sal)
        assertTrue(Claves.verificar("Acis2026!", sal, hash))
        assertFalse(Claves.verificar("incorrecta", sal, hash))
        assertFalse(Claves.verificar("", sal, hash))
        assertFalse(Claves.verificar("Acis2026! ", sal, hash))
    }
    // Dos sales distintas deben producir hashes distintos incluso con la misma contraseña.
    @Test fun mismaClaveConDistintaSalNoProduceElMismoHash() {
        val sal1 = Claves.nuevaSal()
        val sal2 = Claves.nuevaSal()
        assertNotEquals(sal1, sal2)
        assertNotEquals(Claves.derivar("clave", sal1), Claves.derivar("clave", sal2))
    }
}
