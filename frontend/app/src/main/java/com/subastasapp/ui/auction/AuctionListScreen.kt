package com.subastasapp.ui.auction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.subastasapp.GenericViewModelFactory
import com.subastasapp.data.model.auction.Auction
import com.subastasapp.data.model.auction.AuctionStatus
import com.subastasapp.data.model.auction.Categoria
import com.subastasapp.ui.auth.UiState
import com.subastasapp.ui.components.CategoriaBadge
import com.subastasapp.ui.components.MoneyText
import com.subastasapp.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionListScreen(
    isGuest: Boolean,
    repository: com.subastasapp.data.repository.AuctionRepository,
    onAuctionClick: (Long) -> Unit,
    onNotificationsClick: (() -> Unit)? = null,
    viewModel: AuctionListViewModel = viewModel(factory = GenericViewModelFactory { AuctionListViewModel(repository) })
) {
    val state by viewModel.state.collectAsState()
    val selected by viewModel.selectedCategoria.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subasta APP", fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E)) },
                actions = {
                    IconButton(onClick = { onNotificationsClick?.invoke() }) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF1E1E1E))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "Subastas activas",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E1E),
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )

            CategoryFilterRow(selected = selected, onSelect = viewModel::selectCategoria)

            when (val s = state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFC9A84C))
                }
                is UiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = viewModel::load) { Text("Reintentar", color = Color(0xFFC9A84C)) }
                    }
                }
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("No hay subastas en esta categoría", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(s.data, key = { it.id }) { auction ->
                                AuctionCard(
                                    auction = auction,
                                    isGuest = isGuest,
                                    onClick = { onAuctionClick(auction.id) }
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
private fun CategoryFilterRow(selected: Categoria?, onSelect: (Categoria?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterPill(label = "Todas", active = selected == null) { onSelect(null) }
        Categoria.values().forEach { cat ->
            FilterPill(label = cat.displayName, active = selected == cat) { onSelect(cat) }
        }
    }
}

@Composable
private fun FilterPill(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) Color(0xFF0A1931) else Color.White,
        border = BorderStroke(1.dp, if (active) Color(0xFF0A1931) else Color(0xFFE9E9E9)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (active) Color.White else Color(0xFF555555),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun AuctionCard(auction: Auction, isGuest: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE9E9E9)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = auction.itemActual.imagenes.firstOrNull(),
                    contentDescription = auction.itemActual.titulo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(Color(0xFFE9E9E9)),
                    contentScale = ContentScale.Crop
                )
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadge(auction.estado)
                    CategoriaBadge(auction.categoria)
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = auction.itemActual.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E)
                )
                Text(
                    text = "${auction.rematador} · ${auction.fechaHoraInicio}",
                    fontSize = 12.sp,
                    color = Color(0xFF555555)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (auction.estado == AuctionStatus.EN_VIVO) "Oferta actual" else "Precio base",
                            fontSize = 11.sp,
                            color = Color(0xFF555555)
                        )
                        // Regla TPO: solo los usuarios registrados ven los precios.
                        if (isGuest) {
                            Text("Iniciá sesión para ver", fontSize = 14.sp, color = Color(0xFF555555), fontWeight = FontWeight.Medium)
                        } else {
                            MoneyText(
                                amount = auction.ofertaActual,
                                currency = auction.currency,
                                fontSize = 18,
                                color = Color(0xFFC9A84C)
                            )
                        }
                    }
                    Surface(
                        color = Color(0xFFC9A84C),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (auction.estado == AuctionStatus.EN_VIVO) "Ver en vivo" else "Ver detalle",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
