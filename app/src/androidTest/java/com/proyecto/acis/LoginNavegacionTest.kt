package com.proyecto.acis

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.proyecto.acis.data.CuentasPrueba
import org.junit.Rule
import org.junit.Test

// Pruebas Compose en dispositivo: abren MainActivity e interactúan con su interfaz real.
class LoginNavegacionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @org.junit.Before fun comenzarSinSesion() {
        // Application vive entre casos: cada prueba comienza con identidad y borradores limpios.
        lateinit var acceso: com.proyecto.acis.ui.SesionViewModel
        lateinit var ordenes: com.proyecto.acis.ui.OrdenesViewModel
        compose.runOnIdle {
            val proveedor = androidx.lifecycle.ViewModelProvider(compose.activity)
            acceso = proveedor[com.proyecto.acis.ui.SesionViewModel::class.java]
            ordenes = proveedor[com.proyecto.acis.ui.OrdenesViewModel::class.java]
        }
        compose.waitUntil(20_000) { !acceso.estado.value.ocupado && !ordenes.ocupado }
        compose.runOnIdle {
            ordenes.limpiarBorradores()
            acceso.cerrarSesion()
        }
    }

    // Usa el botón real de salida y comprueba que no quedan borradores para la siguiente cuenta.
    @Test fun salirLimpiaBorradoresYConservaOrdenesGuardadas() {
        entrar("planificador@acis.cl")
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Salir").fetchSemanticsNodes().isNotEmpty() }
        lateinit var vm: com.proyecto.acis.ui.OrdenesViewModel
        lateinit var orden: com.proyecto.acis.data.OrdenTrabajo
        var cantidad = 0
        compose.runOnIdle {
            vm = androidx.lifecycle.ViewModelProvider(compose.activity)[com.proyecto.acis.ui.OrdenesViewModel::class.java]
            cantidad = vm.ordenes.value.size
            orden = com.proyecto.acis.data.OrdenTrabajo("OT-BORRADOR-TEST", "Local", "Equipo", "Trabajo", "26/11/2026 10:00")
            vm.cambiarLocal("Borrador que debe desaparecer")
            vm.formularioDetalle(orden)
            vm.cambiarDiagnostico(orden.id, "Texto sin guardar")
        }
        compose.onNodeWithText("Salir").performClick()
        compose.onNodeWithText("Ingresa a tu cuenta").assertExists()
        compose.runOnIdle {
            org.junit.Assert.assertNull(vm.usuario)
            org.junit.Assert.assertEquals("", vm.datosCrear.local)
            org.junit.Assert.assertEquals("", vm.formularioDetalle(orden).diagnostico)
            org.junit.Assert.assertEquals(cantidad, vm.ordenes.value.size)
        }
        entrar("cliente2@acis.cl")
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Perfil activo: Cliente").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("+").assertDoesNotExist()
    }

    // Rellena credenciales y espera a que la carga habilite el ingreso.
    private fun entrar(correo: String, clave: String = CuentasPrueba.CLAVE) {
        compose.onNodeWithText("Correo").performTextReplacement(correo)
        compose.onNodeWithText("Contraseña", substring = false).performTextReplacement(clave)
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText("Ingresar") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
    }

    // Desplazarse al final no debe ocultar las acciones de navegación.
    // Intentar guardar sin campos completos tampoco debe cerrar el formulario.
    @Test fun volverPermaneceVisibleYUnFormularioInvalidoNoSeCierra() {
        entrar("planificador@acis.cl")
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Salir").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("+").performClick()
        compose.onNodeWithText("Nueva orden de trabajo").assertExists()
        // LazyColumn no compone todos los elementos: desplazar el contenedor hasta encontrar el botón.
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Guardar OT"))
        compose.onNodeWithText("Guardar OT").performClick()
        compose.waitForIdle()
        // Los errores agregan filas; volver al final comprueba que sigue en el formulario.
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Guardar OT"))
        compose.onNodeWithText("Guardar OT").assertExists()
        compose.onNodeWithText("Volver", substring = false).assertIsDisplayed()
        compose.onNodeWithText("Salir").assertIsDisplayed()
        compose.onNodeWithText("Volver", substring = false).performClick()
        compose.onNodeWithText("+").assertIsDisplayed()
        compose.onNodeWithText("Guardar OT").assertDoesNotExist()
    }

    @Test fun administradorAbreCuentasSinAccionesDeOrdenes() {
        // Comprueba el destino de entrada administrativo y el retorno al login al cerrar sesión.
        entrar("admin@acis.cl")
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Administrar cuentas").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Perfil activo: Administrador").assertExists()
        compose.onNodeWithText("+").assertDoesNotExist()
        compose.onNodeWithText("Salir").assertIsDisplayed().performClick()
        compose.onNodeWithText("Ingresa a tu cuenta").assertExists()
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
