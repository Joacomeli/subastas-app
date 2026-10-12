package com.subastasapp.ui.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import coil.compose.AsyncImage
import com.subastasapp.data.model.auction.Auction
import com.subastasapp.data.repository.MockAuctionRepository
import com.subastasapp.ui.components.CategoriaBadge
import com.subastasapp.ui.components.MoneyText
import com.subastasapp.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogItemDetailScreen(
    auctionId: Long,
    isGuest: Boolean,
    repository: com.subastasapp.data.repository.AuctionRepository,
    onVerEnVivo: (Long) -> Unit,
    onBack: () -> Unit
) {
    val auction by produceState<Auction?>(initialValue = null, auctionId) {
        value = repository.getAuction(auctionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de la pieza", fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color(0xFF1E1E1E))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        val a = auction
        if (a == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFC9A84C))
            }
            return@Scaffold
        }

        val item = a.itemActual
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Carrusel de imágenes (hasta ~6). Si no hay imágenes (ej. ítem recién consignado),
            // mostramos un placeholder amigable para no dejar el espacio vacío.
            if (item.imagenes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFEFEF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sin imágenes disponibles",
                        color = Color(0xFF888888),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    items(item.imagenes) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = item.titulo,
                            modifier = Modifier
                                .width(280.dp)
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE9E9E9)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(a.estado)
                    CategoriaBadge(a.categoria)
                }
                Spacer(Modifier.height(8.dp))
                Text("Pieza ${item.numeroPieza}", fontSize = 12.sp, color = Color(0xFF555555))
                Text(item.titulo, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))

                Spacer(Modifier.height(4.dp))
                Text("Subasta: ${a.titulo} · ${a.rematador}", fontSize = 12.sp, color = Color(0xFF555555))
                Text("Inicio: ${a.fechaHoraInicio}", fontSize = 12.sp, color = Color(0xFF555555))

                if (item.esMultiElemento) {
                    Spacer(Modifier.height(8.dp))
                    Surface(color = Color(0xFFF8F9FD), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "Pieza compuesta por ${item.cantidadElementos} elementos",
                            fontSize = 12.sp,
                            color = Color(0xFF0A1931),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Descripción", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.descripcion.ifBlank { "Sin descripción adicional." },
                    fontSize = 14.sp,
                    color = Color(0xFF555555)
                )

                Spacer(Modifier.height(16.dp))
                Text("Dueño actual", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                Text(
                    text = item.duenioActual.ifBlank { "Información no disponible" },
                    fontSize = 14.sp,
                    color = Color(0xFF555555)
                )

                // Regla TPO: el precio base solo es visible para usuarios registrados.
                Spacer(Modifier.height(16.dp))
                Text("Precio base", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                if (isGuest) {
                    Text(
                        "Iniciá sesión para ver el precio base",
                        fontSize = 14.sp,
                        color = Color(0xFF555555),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    MoneyText(item.valorBase, a.currency, fontSize = 20, color = Color(0xFFC9A84C))
                }

                // Metadatos de arte / diseñador (historial de la pieza)
                item.arte?.let { arte ->
                    Spacer(Modifier.height(16.dp))
                    ArtHistorySection(arte.artista, arte.fecha, arte.historia)
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onVerEnVivo(a.id) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9A84C)),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Ver subasta en vivo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ArtHistorySection(artista: String, fecha: String, historia: String) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Historial de la pieza", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFF555555)
            )
        }
        Text("$artista · $fecha", fontSize = 13.sp, color = Color(0xFFC9A84C), fontWeight = FontWeight.Medium)
        if (expanded) {
            Spacer(Modifier.height(4.dp))
            Text(historia, fontSize = 14.sp, color = Color(0xFF555555))
        }
    }
}
