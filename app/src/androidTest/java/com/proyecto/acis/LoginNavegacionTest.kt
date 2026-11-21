package com.proyecto.acis

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.proyecto.acis.data.CuentasPrueba
import org.junit.Rule
import org.junit.Test

// Pruebas Compose en dispositivo: abren MainActivity e interactúan con su interfaz real.
class LoginNavegacionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    // Rellena credenciales y espera a que la carga habilite el ingreso.
    private fun entrar(correo: String, clave: String = CuentasPrueba.CLAVE) {
        compose.onNodeWithText("Correo").performTextReplacement(correo)
        compose.onNodeWithText("Contraseña", substring = false).performTextReplacement(clave)
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText("Ingresar") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
    }

    // Comprueba el rechazo inicial y un segundo intento correcto sin reiniciar la pantalla.
    @Test fun credencialesInvalidasNoDanAccesoYSePuedeReintentar() {
        entrar("planificador@acis.cl", "incorrecta")
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("Correo o contraseña incorrectos.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Correo o contraseña incorrectos.").assertExists()
        compose.onNodeWithText("Salir").assertDoesNotExist()
        entrar("planificador@acis.cl")
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Salir").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Perfil activo: Planificador").assertExists()
    }

    // Cambia de cuenta y verifica que solo el planificador dispone del botón de creación.
    @Test fun logoutVuelveAlLoginYElCambioDeCuentaCambiaPermisos() {
        entrar("planificador@acis.cl")
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Salir").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("+").assertExists()
        compose.onNodeWithText("Buscar OT, local o equipo").performScrollTo().performTextInput("sin coincidencias xyz")
        compose.onNodeWithText("0 órdenes encontradas").assertExists()
        compose.onNodeWithText("Salir").performClick()
        compose.onNodeWithText("Ingresa a tu cuenta").assertExists()
        compose.onNodeWithText("+").assertDoesNotExist()
        entrar("cliente2@acis.cl")
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Salir").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Perfil activo: Cliente").assertExists()
        compose.onNodeWithText("+").assertDoesNotExist()
        compose.onNodeWithText("Salir").performClick()
        entrar("tecnico@acis.cl")
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Salir").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Perfil activo: Técnico").assertExists()
        compose.onNodeWithText("+").assertDoesNotExist()
    }
}
