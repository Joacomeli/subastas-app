package com.subastasapp.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subastasapp.data.model.auction.AuctionStatus
import com.subastasapp.data.model.auction.Categoria
import com.subastasapp.data.model.auction.Currency
import com.subastasapp.data.model.auction.formatMoney
import com.subastasapp.ui.theme.Accent
import com.subastasapp.ui.theme.Primary
import com.subastasapp.ui.theme.TextPrimary

/** Texto monetario formateado según la moneda de la subasta. */
@Composable
fun MoneyText(
    amount: Double,
    currency: Currency,
    fontSize: Int = 16,
    color: Color = TextPrimary,
    weight: FontWeight = FontWeight.Bold
) {
    Text(
        text = formatMoney(amount, currency),
        fontSize = fontSize.sp,
        fontWeight = weight,
        color = color
    )
}

/** Chip pequeño de estado de la subasta (EN VIVO / PROGRAMADA / FINALIZADA). */
@Composable
fun StatusBadge(estado: AuctionStatus, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (estado) {
        AuctionStatus.EN_VIVO -> Triple("EN VIVO", Color(0xFFFDECEA), Color(0xFFD32F2F))
        AuctionStatus.PROGRAMADA -> Triple("PROGRAMADA", Color(0xFFFFF6E0), Color(0xFF8A6D1B))
        AuctionStatus.FINALIZADA -> Triple("FINALIZADA", Color(0xFFE9E9E9), Color(0xFF555555))
    }
    Surface(color = bg, shape = RoundedCornerShape(4.dp), modifier = modifier) {
        Text(
            text = label,
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** Chip de categoría de la subasta. */
@Composable
fun CategoriaBadge(categoria: Categoria, modifier: Modifier = Modifier) {
    Surface(
        color = Primary,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = categoria.displayName.uppercase(),
            color = Accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
