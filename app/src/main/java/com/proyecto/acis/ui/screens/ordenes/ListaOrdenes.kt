package com.proyecto.acis.ui.screens.ordenes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.composable
import com.proyecto.acis.data.EstadoOt
import com.proyecto.acis.data.OrdenTrabajo
import com.proyecto.acis.data.Perfil
import com.proyecto.acis.data.PrioridadOt
import com.proyecto.acis.ui.OrdenesViewModel
import com.proyecto.acis.ui.components.*

/**
 * Vista principal inspirada en la distribución de la captura de pantalla:
 * 1. Encabezado con Avatar e Identidad de la aplicación ("ACIS · Servicios Técnicos").
 * 2. Tarjeta con el perfil autenticado y filtros de búsqueda.
 * 3. Tarjeta destacada de Orden de Trabajo Reciente.
 * 4. Listado completo de "Todas las Órdenes de Trabajo" estilizado en bloques verticales.
 */
@Composable
internal fun ListaOrdenesAdaptada(
    todasOrdenes: List<OrdenTrabajo>,
    perfil: Perfil,
    onSeleccionar: (String) -> Unit,
) {
    // Los filtros se conservan al recrear la pantalla; se aplican sobre las órdenes ya autorizadas.
    var busqueda by rememberSaveable { mutableStateOf("") }
    var estadoFiltro by rememberSaveable { mutableStateOf<String?>(null) }
    val ordenes = todasOrdenes.filter {
        (estadoFiltro == null || it.estado.name == estadoFiltro) &&
            "${it.id} ${it.local} ${it.equipo}".contains(busqueda.trim(), ignoreCase = true)
    }
    // El ViewModel entrega la lista por última modificación; esta tarjeta usa la primera coincidencia.
    val ordenReciente = ordenes.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. ENCABEZADO CON NOMBRE DE LA APLICACIÓN Y AVATAR
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar con iniciales ACIS
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "AC",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "ACIS · Servicios Técnicos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Climatización & Refrigeración",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 2. PERFIL DE LA CUENTA AUTENTICADA
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Perfil activo: ${perfil.etiqueta}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        Text("Acceso según tu cuenta")
                    }
                }
            }
        }

        // 3. TARJETA DESTACADA: ÓRDENES RECIENTES
        item {
            OutlinedTextField(busqueda, { busqueda = it }, label = { Text("Buscar OT, local o equipo") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = estadoFiltro == null, onClick = { estadoFiltro = null }, label = { Text("Todas") })
                EstadoOt.entries.forEach { estado ->
                    FilterChip(selected = estadoFiltro == estado.name, onClick = { estadoFiltro = estado.name }, label = { Text(estado.etiqueta) })
                }
            }
            Text("${ordenes.size} órdenes encontradas")
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (ordenReciente != null) Modifier.clickable { onSeleccionar(ordenReciente.id) }
                        else Modifier
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                text = "🛠️",
                                modifier = Modifier.padding(8.dp),
                                fontSize = 16.sp,
                            )
                        }
                        if (ordenReciente != null) {
                            EstadoBadge(ordenReciente.estado)
                        }
                    }

                    if (ordenReciente != null) {
                        Text(
                            text = "¡Última orden creada!",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${ordenReciente.id} · ${ordenReciente.local}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Equipo: ${ordenReciente.equipo}\n${ordenReciente.descripcion}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PrioridadBadge(ordenReciente.prioridad)
                            Text(
                                text = "Programado: ${ordenReciente.fechaProgramada}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    } else {
                        Text(
                            text = "¡No hay órdenes recientes!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Prueba otra búsqueda o filtro. Si eres planificador, usa + para registrar una orden.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        // 4. SECCIÓN: TODAS LAS ÓRDENES DE TRABAJO
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Todas las órdenes (${ordenes.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (ordenes.isEmpty()) {
            item {
                Text(
                    text = "No hay órdenes disponibles para este perfil.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Listado vertical de cada una de las órdenes (estilo bloques "Mis asignaturas")
        items(ordenes, key = { it.id }) { orden ->
            val colorIndicador = when (orden.prioridad) {
                PrioridadOt.ALTA -> MaterialTheme.colorScheme.primary
                PrioridadOt.MEDIA -> MaterialTheme.colorScheme.tertiary
                PrioridadOt.BAJA -> MaterialTheme.colorScheme.secondary
                PrioridadOt.SIN_ESPECIFICAR -> MaterialTheme.colorScheme.surfaceVariant
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeleccionar(orden.id) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                ) {
                    // Franja vertical de color a la izquierda estilo bloque de asignatura (|)
                    Surface(
                        modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight(),
                        color = colorIndicador,
                    ) {}

                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "│ ",
                                    color = colorIndicador,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = orden.id,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            EstadoBadge(orden.estado)
                        }
                        Text(
                            text = orden.local,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = orden.equipo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = orden.tipo.etiqueta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            PrioridadBadge(orden.prioridad)
                        }
                        Text(
                            text = "Programado: ${orden.fechaProgramada}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
