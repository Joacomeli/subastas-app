package com.subastasapp.ui.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.subastasapp.GenericViewModelFactory
import com.subastasapp.data.model.auction.Bid
import com.subastasapp.data.model.auction.BiddingStatus
import com.subastasapp.data.model.auction.Currency
import com.subastasapp.data.model.auction.formatMoney
import com.subastasapp.ui.components.MoneyText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveAuctionScreen(
    auctionId: Long,
    isGuest: Boolean,
    repository: com.subastasapp.data.repository.AuctionRepository,
    bidStream: com.subastasapp.data.realtime.BidStream,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit
) {
    val viewModel: LiveAuctionViewModel = viewModel(
        key = "live_$auctionId",
        factory = GenericViewModelFactory { LiveAuctionViewModel(auctionId, isGuest, repository, bidStream) }
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subasta en vivo", fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color(0xFF1E1E1E))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FD)
    ) { padding ->
        val auction = state.auction
        if (state.loading || auction == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                if (state.loading) CircularProgressIndicator(color = Color(0xFFC9A84C))
                else Text("Subasta no encontrada", color = Color.Gray)
            }
            return@Scaffold
        }

        val currency = auction.currency
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            StatusBanner(state.status, state.accessReason)

            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {

                // Artículo actual
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = auction.itemActual.imagenes.firstOrNull(),
                            contentDescription = auction.itemActual.titulo,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE9E9E9)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Artículo actual", fontSize = 11.sp, color = Color(0xFF555555))
                            Text(
                                auction.itemActual.titulo,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1E1E)
                            )
                            Text("Pieza ${auction.itemActual.numeroPieza}", fontSize = 12.sp, color = Color(0xFF555555))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Oferta actual + rango
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoBox(
                        modifier = Modifier.weight(1f),
                        label = "Oferta actual",
                        value = formatMoney(state.ofertaActual, currency),
                        valueColor = Color(0xFF0A1931)
                    )
                    InfoBox(
                        modifier = Modifier.weight(1f),
                        label = "Rango de puja válido",
                        value = rangoText(state.rangoMin, state.rangoMax, currency),
                        valueColor = Color(0xFFC9A84C),
                        small = true
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("Historial de pujas", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E1E1E))
                Spacer(Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(state.historial.reversed()) { bid ->
                        BidRow(bid, currency)
                        Divider(color = Color(0xFFE9E9E9))
                    }
                }
            }

            // Control de puja
            BidControl(
                canBid = state.canBid,
                isGuest = isGuest,
                isBidPending = state.isBidPending,
                finalizada = state.finalizada,
                inputError = state.inputError,
                accessReason = state.accessReason,
                currency = currency,
                onAmountChange = viewModel::onAmountChange,
                onPujar = viewModel::placeBid,
                onRequireLogin = onRequireLogin
            )
        }
    }
}

@Composable
private fun StatusBanner(status: BiddingStatus?, reason: String?) {
    if (status == null) return
    val (text, bg, fg) = when (status) {
        BiddingStatus.GANANDO -> Triple("Vas ganando la subasta", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        BiddingStatus.SUPERADO -> Triple("Te superaron la oferta", Color(0xFFFDECEA), Color(0xFFD32F2F))
        BiddingStatus.GANADA -> Triple("¡OFERTA GANADORA! Ganaste la pieza", Color(0xFFC9A84C), Color.White)
        BiddingStatus.VIEW_ONLY -> Triple(reason ?: "Modo solo lectura", Color(0xFFEDEDED), Color(0xFF555555))
    }
    Surface(color = bg, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun InfoBox(modifier: Modifier, label: String, value: String, valueColor: Color, small: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = Color(0xFF555555))
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = if (small) 14.sp else 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun BidRow(bid: Bid, currency: Currency) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (bid.esMia) "${bid.autor} (vos)" else bid.autor,
                fontSize = 14.sp,
                fontWeight = if (bid.esMia) FontWeight.Bold else FontWeight.Normal,
                color = Color(0xFF1E1E1E)
            )
            if (bid.esGanadora) {
                Text("Oferta más alta", fontSize = 11.sp, color = Color(0xFF2E7D32))
            }
        }
        MoneyText(bid.importe, currency, fontSize = 15, color = Color(0xFF0A1931))
    }
}

@Composable
private fun BidControl(
    canBid: Boolean,
    isGuest: Boolean,
    isBidPending: Boolean,
    finalizada: Boolean,
    inputError: String?,
    accessReason: String?,
    currency: Currency,
    onAmountChange: () -> Unit,
    onPujar: (String) -> Unit,
    onRequireLogin: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            when {
                finalizada -> {
                    Text(
                        "La subasta finalizó.",
                        color = Color(0xFF555555),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                !canBid -> {
                    Text(
                        accessReason ?: "No podés pujar en esta subasta.",
                        color = Color(0xFF555555),
                        fontSize = 13.sp
                    )
                    if (isGuest) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onRequireLogin,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9A84C)),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text("Iniciá sesión para pujar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {
                    var amount by remember { mutableStateOf("") }
                    if (inputError != null) {
                        Text(inputError, color = Color(0xFFD32F2F), fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it; onAmountChange() },
                            label = { Text("Monto en ${currency.name}") },
                            singleLine = true,
                            enabled = !isBidPending,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = inputError != null,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onPujar(amount); },
                            enabled = !isBidPending,
                            modifier = Modifier.height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A1931)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isBidPending) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Text("PUJAR", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (isBidPending) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Esperando confirmación del sistema…",
                            fontSize = 11.sp,
                            color = Color(0xFF555555)
                        )
                    }
                }
            }
        }
    }
}

private fun rangoText(min: Double, max: Double?, currency: Currency): String {
    return if (max == null) {
        // Subasta exenta (ORO/PLATINO): solo debe superar la oferta actual, sin máximo.
        "Mayor a ${formatMoney(min, currency)} (sin máx.)"
    } else {
        "${formatMoney(min, currency)} - ${formatMoney(max, currency)}"
    }
}
