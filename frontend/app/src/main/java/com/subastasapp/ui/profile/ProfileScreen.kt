package com.subastasapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.subastasapp.data.model.response.*
import com.subastasapp.ui.auth.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    isGuest: Boolean = false,
    onNavigateToHistory: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToConsignments: () -> Unit,
    onLogout: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsState()
    val metricsState by viewModel.metricsState.collectAsState()
    val paymentMethodsState by viewModel.paymentMethodsState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Cargar (o recargar) los datos cada vez que se entra a la pantalla de Perfil.
    // Antes el ViewModel solo llamaba a los endpoints en su init -y eso pasaba al iniciar
    // la activity, antes de que el usuario tuviera token-, así que el primer 401 dejaba
    // los estados en Error y nunca se reintentaba.
    LaunchedEffect(Unit) {
        viewModel.loadAllData()
    }

    LaunchedEffect(profileState, metricsState, paymentMethodsState) {
        // Solo deslogueamos automáticamente si el perfil falla con 401 después de que ya hay
        // un token guardado. Si las métricas o medios de pago fallan, mostramos snackbar
        // sin tocar la sesión (no son endpoints críticos para la identidad del usuario).
        if (profileState is UiState.Error && (profileState as UiState.Error).message == "Session expired") {
            viewModel.logout(onLogout)
        }
        if (profileState is UiState.Error || metricsState is UiState.Error || paymentMethodsState is UiState.Error) {
            snackbarHostState.showSnackbar("Sin conexión a internet")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Subasta APP",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )
                },
                actions = {
                    IconButton(onClick = { /* Notificaciones */ }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF1E1E1E)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White

    ) { padding ->
        if (isGuest) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFFE9E9E9)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Estás navegando como invitado",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Creá una cuenta o iniciá sesión para acceder a tu perfil y poder participar en subastas",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9A84C)),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Iniciá sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 32.dp),
                    shape = RoundedCornerShape(50.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC9A84C))
                ) {
                    Text("Registrate", color = Color(0xFFC9A84C), fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                HeaderSection(profileState)

                MetricsSection(metricsState)

                PaymentMethodsSection(paymentMethodsState)

                ManagementSection(
                    onHistoryClick = onNavigateToHistory,
                    onFavoritesClick = onNavigateToFavorites,
                    onConsignmentsClick = onNavigateToConsignments
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.logout(onLogout) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9A84C)),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Cerrar sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HeaderSection(state: UiState<UserProfileResponse>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFE9E9E9)),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is UiState.Success -> {
                    if (state.data.fotoUrl != null) {
                        AsyncImage(
                            model = state.data.fotoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                    }
                }
                else -> Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        when (state) {
            is UiState.Success -> {
                Text(
                    text = "${state.data.nombre} ${state.data.apellido}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E)
                )
            }
            is UiState.Loading -> CircularProgressIndicator(color = Color(0xFFC9A84C), modifier = Modifier.size(24.dp))
            else -> Text("--", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MetricsSection(state: UiState<UserMetricsResponse>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val metrics = if (state is UiState.Success) state.data else null
        
        // "--" sólo cuando todavía no se cargaron las métricas; "0" es un valor legítimo
        // (un usuario nuevo recién registrado tiene 0 subastas, 0 ganadas, 0 pujas).
        MetricCard(modifier = Modifier.weight(1f), value = metrics?.totalAuctions?.toString() ?: "--", label = "Subastas")
        MetricCard(modifier = Modifier.weight(1f), value = metrics?.wins?.toString() ?: "--", label = "Ganadas")
        MetricCard(modifier = Modifier.weight(1f), value = metrics?.totalBids?.toString() ?: "--", label = "Pujas")
    }
}

@Composable
fun MetricCard(modifier: Modifier, value: String, label: String) {
    Card(
        modifier = modifier.height(70.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FD)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9E9E9)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A1931))
            Text(text = label, fontSize = 11.sp, color = Color(0xFF555555))
        }
    }
}

@Composable
fun PaymentMethodsSection(state: UiState<List<PaymentMethodResponse>>) {
    Column(modifier = Modifier.padding(top = 32.dp)) {
        Text(
            text = "Mis medios de pago",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1E1E),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (state) {
            is UiState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data) { method ->
                        PaymentMethodCard(method)
                    }
                }
            }
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFC9A84C))
                }
            }
            else -> {
                Text(
                    "No hay medios de pago",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PaymentMethodCard(method: PaymentMethodResponse) {
    Card(
        modifier = Modifier.width(220.dp).height(80.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9E9E9)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (method.tipo == "CREDIT_CARD" || method.tipo == "TARJETA") Icons.Default.CreditCard else Icons.Default.AccountBalance,
                contentDescription = null,
                tint = Color(0xFF0A1931),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (method.tipo == "CREDIT_CARD" || method.tipo == "TARJETA") "Visa terminada en ${method.ultimosDigitos ?: "****"}" else method.descripcion,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E1E1E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "VERIFICADO",
                        color = Color(0xFF2E7D32),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ManagementSection(
    onHistoryClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onConsignmentsClick: () -> Unit
) {
    Column(modifier = Modifier.padding(start = 24.dp, top = 32.dp, end = 24.dp)) {
        Text(
            text = "Gestión",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1E1E)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE9E9E9), RoundedCornerShape(8.dp))
        ) {
            ManagementItem(icon = Icons.Default.History, text = "Mi historial", onClick = onHistoryClick)
            Divider(color = Color(0xFFE9E9E9), modifier = Modifier.padding(horizontal = 16.dp))
            ManagementItem(icon = Icons.Default.FavoriteBorder, text = "Favoritos", onClick = onFavoritesClick)
            Divider(color = Color(0xFFE9E9E9), modifier = Modifier.padding(horizontal = 16.dp))
            ManagementItem(icon = Icons.Default.ListAlt, text = "Mis consignaciones", onClick = onConsignmentsClick)
        }
    }
}

@Composable
fun ManagementItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF0A1931), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, fontSize = 14.sp, color = Color(0xFF1E1E1E), modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}
