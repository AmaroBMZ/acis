package com.proyecto.acis.domain

import java.util.Locale

/** Reglas locales del alta; la confirmación nunca se persiste. */
object ValidadorUsuario {
    fun correoNormalizado(correo: String) = correo.trim().lowercase(Locale.ROOT)

    fun validar(nombre: String, correo: String, clave: String, confirmacion: String): Map<String, String> = buildMap {
        if (nombre.trim().length !in 2..100) put("nombre", "Ingresa un nombre de 2 a 100 caracteres.")
        val email = correoNormalizado(correo)
        if (email.length > 254 || !Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email))
            put("correo", "Ingresa un correo válido.")
        if (clave.length !in 8..128 || !clave.any { it.isLetter() } || !clave.any { it.isDigit() })
            put("clave", "Usa de 8 a 128 caracteres, con letras y números.")
        if (confirmacion.isEmpty() || clave != confirmacion) put("confirmacion", "Las contraseñas deben coincidir.")
    }
}
