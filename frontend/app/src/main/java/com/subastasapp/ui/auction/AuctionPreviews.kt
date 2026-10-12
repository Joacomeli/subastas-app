package com.subastasapp.ui.auction

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.subastasapp.data.model.auction.ArtMetadata
import com.subastasapp.data.model.auction.Auction
import com.subastasapp.data.model.auction.AuctionStatus
import com.subastasapp.data.model.auction.Categoria
import com.subastasapp.data.model.auction.CatalogItem
import com.subastasapp.data.model.auction.Currency
import com.subastasapp.ui.theme.SubastasAppTheme

private val sampleItem = CatalogItem(
    id = 1,
    numeroPieza = "#88",
    titulo = "Reloj GMT de acero",
    descripcion = "Edición 2023, caja de acero Oystersteel.",
    valorBase = 15000.0,
    duenioActual = "Colección privada",
    arte = ArtMetadata("Demo", "2023", "Pieza deportiva contemporánea.")
)

private val sampleAuction = Auction(
    id = 1,
    titulo = "Subasta de Relojes de Lujo",
    categoria = Categoria.ESPECIAL,
    estado = AuctionStatus.EN_VIVO,
    currency = Currency.ARS,
    rematador = "Mtro. Gómez",
    fechaHoraInicio = "Hoy 20:00",
    itemActual = sampleItem,
    ofertaActual = 15500.0
)

@Preview(name = "AuctionCard - registrado", showBackground = true)
@Composable
private fun AuctionCardRegisteredPreview() {
    SubastasAppTheme {
        AuctionCard(auction = sampleAuction, isGuest = false, onClick = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "AuctionCard - invitado", showBackground = true)
@Composable
private fun AuctionCardGuestPreview() {
    SubastasAppTheme {
        AuctionCard(auction = sampleAuction, isGuest = true, onClick = {}, modifier = Modifier.padding(16.dp))
    }
}
