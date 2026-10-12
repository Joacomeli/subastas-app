package com.subastasapp.ui.consignar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.subastasapp.data.model.ConsignacionResponse
import com.subastasapp.data.model.NotificacionResponse
import com.subastasapp.ui.auth.UiState
import com.subastasapp.ui.theme.Accent
import com.subastasapp.ui.theme.Background
import com.subastasapp.ui.theme.Primary
import com.subastasapp.ui.theme.TextPrimary
import com.subastasapp.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyConsignmentsScreen(
    viewModel: ConsignarViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val state by viewModel.consignaciones.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargarConsignaciones()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Consignaciones", fontWeight = FontWeight.Bold, color = TextPrimary) },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = Primary,
                contentColor = Accent
            ) {
                Icon(Icons.Default.Add, contentDescription = "Consignar artículo")
            }
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
                is UiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.cargarConsignaciones() }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                            Text("Reintentar", color = Accent)
                        }
                    }
                }
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("No has consignado ningún artículo aún.", color = TextSecondary, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(s.data, key = { it.id }) { item ->
                                ConsignacionCard(item = item, onClick = { onNavigateToDetail(item.id) })
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
fun ConsignacionCard(item: ConsignacionResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.fotos.isNullOrBlank()) {
                AsyncImage(
                    model = item.fotos,
                    contentDescription = item.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFE9E9E9), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.descripcion,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConsignacionStatusBadge(estado = item.estado)
                    if (item.precioBase != null) {
                        Text(
                            text = "$${String.format("%,.0f", item.precioBase)} Base",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConsignacionStatusBadge(estado: String) {
    val (label, bg, fg) = when (estado) {
        "PENDIENTE" -> Triple("PENDIENTE", Color(0xFFFFF6E0), Color(0xFF8A6D1B))
        "APROBADO_PENDIENTE_CONFIRMACION" -> Triple("PENDIENTE CONFIRMACIÓN", Color(0xFFE8F0FE), Color(0xFF1967D2))
        "CONFIRMADO" -> Triple("CONFIRMADO", Color(0xFFE6F4EA), Color(0xFF137333))
        "RECHAZADO" -> Triple("RECHAZADO", Color(0xFFFCE8E6), Color(0xFFC5221F))
        "CANCELADO" -> Triple("CANCELADO", Color(0xFFF1F3F4), Color(0xFF5F6368))
        else -> Triple(estado, Color.LightGray, Color.DarkGray)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateConsignmentScreen(
    viewModel: ConsignarViewModel,
    onBack: () -> Unit
) {
    val crearState by viewModel.crearState.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fotos by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("COMUN") }
    var declaracion by remember { mutableStateOf(false) }

    val categorias = listOf("COMUN", "ESPECIAL", "PLATA", "ORO", "PLATINO")
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(crearState) {
        if (crearState is UiState.Success) {
            showSuccessDialog = true
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                viewModel.resetCrearState()
                onBack()
            },
            title = { Text("Consignación Exitosa", fontWeight = FontWeight.Bold) },
            text = { Text("Tu artículo ha sido enviado. Queda en revisión PENDIENTE por parte del tasador.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.resetCrearState()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Aceptar", color = Accent)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consignar Artículo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del Artículo") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción Completa") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
            )

            OutlinedTextField(
                value = fotos,
                onValueChange = { fotos = it },
                label = { Text("URL de la Foto") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
            )

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = categoria,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría Requerida") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    categorias.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                categoria = cat
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Declaración de pertenencia (obligatoria por el TPO).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { declaracion = !declaracion }
            ) {
                Checkbox(
                    checked = declaracion,
                    onCheckedChange = { declaracion = it },
                    colors = CheckboxDefaults.colors(checkedColor = Primary)
                )
                Text(
                    "Declaro que el bien me pertenece legalmente y no posee ningún impedimento para subastarlo.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.weight(1f))

            if (crearState is UiState.Error) {
                Text(
                    text = (crearState as UiState.Error).message,
                    color = Color.Red,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    if (nombre.isNotBlank() && descripcion.isNotBlank() && declaracion) {
                        viewModel.crearConsignacion(nombre, descripcion, fotos.ifBlank { null }, categoria)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(8.dp),
                enabled = crearState !is UiState.Loading && declaracion
            ) {
                if (crearState is UiState.Loading) {
                    CircularProgressIndicator(color = Accent, modifier = Modifier.size(24.dp))
                } else {
                    Text("ENVIAR A TASACIÓN", color = Accent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsignmentDetailScreen(
    consignmentId: Long,
    viewModel: ConsignarViewModel,
    onBack: () -> Unit
) {
    val listState by viewModel.consignaciones.collectAsState()
    val confirmarState by viewModel.confirmarState.collectAsState()

    var showRejectDialog by remember { mutableStateOf(false) }
    var devolucionFisica by remember { mutableStateOf(false) }

    val item = remember(listState, consignmentId) {
        if (listState is UiState.Success) {
            (listState as UiState.Success<List<ConsignacionResponse>>).data.find { it.id == consignmentId }
        } else null
    }

    LaunchedEffect(confirmarState) {
        if (confirmarState is UiState.Success) {
            viewModel.resetConfirmarState()
            onBack()
        }
    }

    // Dialogo para rechazar y decidir devolución física
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Rechazar Tasación", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("¿Estás seguro de que deseas rechazar la propuesta del tasador?")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Solicitar devolución física")
                        Switch(
                            checked = devolucionFisica,
                            onCheckedChange = { devolucionFisica = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Accent, checkedTrackColor = Primary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectDialog = false
                        viewModel.confirmarConsignacion(consignmentId, aceptar = false, devolucionFisica = devolucionFisica)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5221F))
                ) {
                    Text("Confirmar Rechazo", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Volver", color = Primary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Consignación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Background
    ) { padding ->
        if (item == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!item.fotos.isNullOrBlank()) {
                    AsyncImage(
                        model = item.fotos,
                        contentDescription = item.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                Text(
                    text = item.nombre,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConsignacionStatusBadge(estado = item.estado)
                    Text(
                        text = "Categoría: ${item.categoria ?: "COMUN"}",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Divider(color = Color(0xFFE9E9E9))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Descripción", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(item.descripcion, fontSize = 15.sp, color = TextPrimary)
                }

                Spacer(Modifier.weight(1f))

                // Flujo según el estado de la consignación
                when (item.estado) {
                    "APROBADO_PENDIENTE_CONFIRMACION" -> {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Propuesta de Subasta",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Precio Base", fontSize = 11.sp, color = TextSecondary)
                                        Text("$${String.format("%,.2f", item.precioBase ?: 0.0)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Fecha Propuesta", fontSize = 11.sp, color = TextSecondary)
                                        Text(item.fechaSubasta ?: "", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                }
                            }
                        }

                        if (confirmarState is UiState.Error) {
                            Text(
                                text = (confirmarState as UiState.Error).message,
                                color = Color.Red,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFC5221F)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC5221F)),
                                enabled = confirmarState !is UiState.Loading
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Rechazar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.confirmarConsignacion(item.id, aceptar = true) },
                                modifier = Modifier
                                    .weight(2.5f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                enabled = confirmarState !is UiState.Loading
                            ) {
                                if (confirmarState is UiState.Loading) {
                                    CircularProgressIndicator(color = Accent, modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("ACEPTAR PROPUESTA", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    "RECHAZADO" -> {
                        Surface(
                            color = Color(0xFFFDECEA),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFF5C2C2)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Rechazado por el Tasador",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Motivo: ${item.motivoRechazo ?: "No especificado"}",
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    "CONFIRMADO" -> {
                        Surface(
                            color = Color(0xFFE6F4EA),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF137333))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Confirmado. El artículo ya está publicado y visible en el módulo de subastas.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF137333),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    "CANCELADO" -> {
                        Surface(
                            color = Color(0xFFF1F3F4),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Propuesta Cancelada por el Usuario",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5F6368)
                                )
                                Spacer(Modifier.height(4.dp))
                                val dev = if (item.devolucionFisica == true) "Sí" else "No"
                                Text(
                                    text = "Solicitud de devolución física: $dev",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    viewModel: ConsignarViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.notificaciones.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargarNotificaciones()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón "Marcar todas como leídas" — habilitado solo si hay no-leídas.
                    val s = state
                    val hayNoLeidas = s is UiState.Success && s.data.any { !it.leido }
                    TextButton(
                        onClick = { viewModel.marcarTodasLeidas() },
                        enabled = hayNoLeidas
                    ) {
                        Text(
                            "Marcar todas",
                            color = if (hayNoLeidas) Primary else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
                is UiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.cargarNotificaciones() }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                            Text("Reintentar", color = Accent)
                        }
                    }
                }
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("No tienes notificaciones in-app.", color = TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(s.data, key = { it.id }) { notif ->
                                NotificacionCard(
                                    notif = notif,
                                    onClick = { if (!notif.leido) viewModel.marcarLeida(notif.id) }
                                )
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
fun NotificacionCard(notif: NotificacionResponse, onClick: () -> Unit = {}) {
    // No-leídas con fondo levemente coloreado para destacarlas.
    val bg = if (notif.leido) Color.White else Color(0xFFFFFBEA)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFF6E0), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Aviso de Consignación",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        if (!notif.leido) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(Accent, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = notif.mensaje,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = notif.createdAt.substringBefore("T"),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
