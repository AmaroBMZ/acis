package com.proyecto.acis.data

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// Identidad de la sesión; no incluye la contraseña ni el hash almacenado en SQLite.
data class Usuario(val correo: String, val nombre: String, val perfil: Perfil, val activo: Boolean = true)

/** Cuentas locales para la evaluación. No constituyen autenticación contra un servidor. */
object CuentasPrueba {
    const val CLIENTE = "cliente@acis.cl"
    val usuarios = listOf(
        Usuario("planificador@acis.cl", "Planificador demo", Perfil.PLANIFICADOR),
        Usuario("tecnico@acis.cl", "Técnico demo", Perfil.TECNICO),
        Usuario(CLIENTE, "Cliente demo", Perfil.CLIENTE),
        Usuario("cliente2@acis.cl", "Segundo cliente demo", Perfil.CLIENTE),
        Usuario("admin@acis.cl", "Administrador demo", Perfil.ADMINISTRADOR),
    )
    const val CLAVE = "Acis2026!"
}

// Agrupa la derivación y comparación de contraseñas para las cuentas locales.
object Claves {
    // Genera 16 bytes aleatorios por cuenta y los convierte a texto hexadecimal.
    fun nuevaSal(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()

    // PBKDF2 combina clave y sal: 120 000 iteraciones y resultado de 256 bits.
    fun derivar(clave: String, sal: String): String {
        // Reconstruye los bytes de la sal hexadecimal y prepara los parámetros del algoritmo.
        val especificacion = PBEKeySpec(clave.toCharArray(), sal.chunked(2).map { it.toInt(16).toByte() }.toByteArray(), 120_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(especificacion).encoded.toHex()
        // Limpia la copia de la contraseña contenida en PBEKeySpec, incluso si ocurre un error.
        } finally { especificacion.clearPassword() }
    }

    // Deriva la clave ingresada con la sal guardada y compara el resultado con el hash esperado.
    fun verificar(clave: String, sal: String, esperado: String): Boolean =
        MessageDigest.isEqual(derivar(clave, sal).toByteArray(Charsets.UTF_8), esperado.toByteArray(Charsets.UTF_8))

    // Extensión de ByteArray: representa cada byte con dos dígitos hexadecimales.
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
