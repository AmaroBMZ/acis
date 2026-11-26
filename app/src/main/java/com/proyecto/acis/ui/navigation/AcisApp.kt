package com.proyecto.acis.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.proyecto.acis.data.Perfil
import com.proyecto.acis.ui.LoginScreen
import com.proyecto.acis.ui.OrdenesViewModel
import com.proyecto.acis.ui.SesionViewModel
import com.proyecto.acis.ui.screens.ordenes.*
import com.proyecto.acis.ui.components.OrdenNoDisponible

/**
 * Contenedor principal de la interfaz ACIS adaptado al diseño de referencia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcisApp(viewModel: OrdenesViewModel, sesionViewModel: SesionViewModel, modifier: Modifier = Modifier) {
    val usuario by sesionViewModel.usuario.collectAsState()
    // Coordina la salida sin acoplar los ViewModels entre sí. No abandona una escritura en curso.
    val salir: () -> Unit = {
        if (!viewModel.ocupado && !sesionViewModel.estado.value.ocupado) {
            viewModel.limpiarBorradores()
            sesionViewModel.cerrarSesion()
        }
    }
    if (usuario == null) {
        LoginScreen(sesionViewModel, modifier)
    } else if (usuario?.perfil == Perfil.ADMINISTRADOR) {
        // El administrador tiene su propio inicio y no entra al navegador operativo de órdenes.
        key(usuario?.correo) { com.proyecto.acis.ui.screens.usuarios.UsuariosScreen(viewModel, salir, modifier) }
    } else {
        // Cambiar de cuenta descarta el navegador y los estados de la sesión anterior.
        key(usuario?.correo) { AplicacionAutenticada(viewModel, salir, modifier) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Organiza barra superior, navegación y acciones de la sesión activa.
private fun AplicacionAutenticada(viewModel: OrdenesViewModel, onSalir: () -> Unit, modifier: Modifier) {
    val navController = rememberNavController()
    // collectAsState convierte el StateFlow en estado Compose: una nueva lista actualiza la UI.
    val ordenes by viewModel.ordenes.collectAsState()
    val perfil = viewModel.perfil
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route
    // Consume Atrás mientras el ViewModel procesa una operación.
    BackHandler(enabled = viewModel.ocupado) { }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(viewModel.usuario?.nombre.orEmpty()) },
                    // La navegación permanece visible aunque el formulario se desplace.
                    navigationIcon = {
                        // En la lista principal no hay un destino anterior dentro de este navegador.
                        if (rutaActual != null && rutaActual != RutasAcis.ORDENES) {
                            TextButton(onClick = { navController.popBackStack() }, enabled = !viewModel.ocupado) {
                                Text("Volver")
                            }
                        }
                    },
                    actions = { TextButton(onClick = onSalir, enabled = !viewModel.ocupado) { Text("Salir") } },
                )
                viewModel.errorGeneral?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
                if (viewModel.ocupado) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        floatingActionButton = {
            // Botón flotante (+) para crear nueva OT (visible para Planificador en la lista)
            if (rutaActual == RutasAcis.ORDENES && perfil == Perfil.PLANIFICADOR) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    onClick = { if (!viewModel.ocupado) {
                        viewModel.nuevoFormulario()
                        navController.navigate(RutasAcis.CREAR) { launchSingleTop = true }
                    } },
                ) {
                    Text(
                        text = "+",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            // Navegador entre la lista de OTs, creación, edición y detalle
            NavHost(
                navController = navController,
                startDestination = RutasAcis.ORDENES,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    fadeIn(tween(380)) + slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(380),
                    )
                },
                exitTransition = {
                    fadeOut(tween(320)) + slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(320),
                    )
                },
                popEnterTransition = {
                    fadeIn(tween(380)) + slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(380),
                    )
                },
                popExitTransition = {
                    fadeOut(tween(320)) + slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(320),
                    )
                },
            ) {
                // Pantalla principal adaptada con la distribución del prototipo
                composable(RutasAcis.ORDENES) {
                    Crossfade(targetState = perfil, animationSpec = tween(380), label = "cambio de perfil") { perfilVisible ->
                        ListaOrdenesAdaptada(
                            todasOrdenes = viewModel.ordenesParaPerfil(ordenes, perfilVisible),
                            perfil = perfilVisible,
                            onSeleccionar = { id -> navController.navigate(RutasAcis.detalle(id)) { launchSingleTop = true } },
                        )
                    }
                }
                // Pantalla de formulario para crear OT (CRUD - Create)
                composable(RutasAcis.CREAR) {
                    CrearOrden(
                        viewModel = viewModel,
                        onCrear = {
                            viewModel.crear { id ->
                                if (id != null) {
                                    // El formulario se cierra solo después de persistir la OT.
                                    navController.popBackStack()
                                }
                            }
                        },
                    )
                }
                // Pantalla de edición de OT (CRUD - Update)
                composable(
                    route = RutasAcis.EDITAR,
                    arguments = listOf(navArgument("ordenId") { type = NavType.StringType }),
                ) { entrada ->
                    val ordenId = entrada.arguments?.getString("ordenId")
                    val orden = ordenes.firstOrNull { it.id == ordenId }
                    if (orden != null && perfil == Perfil.PLANIFICADOR) {
                        EditarOrden(
                            orden = orden,
                            viewModel = viewModel,
                            onVolver = { navController.popBackStack() },
                        )
                    } else OrdenNoDisponible { navController.popBackStack() }
                }
                // Pantalla de detalle e intervención de la OT (CRUD - Read / Actions)
                composable(
                    route = RutasAcis.DETALLE,
                    arguments = listOf(navArgument("ordenId") { type = NavType.StringType }),
                ) { entrada ->
                    val ordenId = entrada.arguments?.getString("ordenId")
                    val orden = ordenes.firstOrNull { it.id == ordenId }
                    if (orden != null && viewModel.puedeVer(orden)) {
                        DetalleOrden(
                            orden = orden,
                            perfil = perfil,
                            viewModel = viewModel,
                            onVolver = { navController.popBackStack() },
                            onEditar = { navController.navigate(RutasAcis.editar(orden.id)) },
                        )
                    } else OrdenNoDisponible { navController.popBackStack() }
                }
            }
        }
    }
}
