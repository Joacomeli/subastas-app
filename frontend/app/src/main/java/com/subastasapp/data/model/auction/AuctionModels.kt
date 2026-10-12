package com.subastasapp.data.model.auction

import java.util.Locale

/**
 * Categoría de un usuario o de una subasta.
 *
 * El orden de declaración define la jerarquía (COMUN es la más baja, PLATINO la más alta).
 * Un usuario puede acceder a una subasta cuando la categoría de la subasta es menor o igual
 * a la propia (regla del TPO).
 */
enum class Categoria {
    COMUN, ESPECIAL, PLATA, ORO, PLATINO;

    /** El usuario (this) puede acceder a una subasta de categoría [subasta]. */
    fun puedeAcceder(subasta: Categoria): Boolean = this.ordinal >= subasta.ordinal

    /** Las subastas ORO y PLATINO están exentas de los límites de % en la puja. */
    val exentaDeLimites: Boolean get() = this == ORO || this == PLATINO

    val displayName: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

/** Moneda de la subasta. Una subasta es en ARS o en USD, nunca bimonetaria. */
enum class Currency(val symbol: String) {
    ARS("$"),
    USD("US$")
}

/** Formatea un importe con separador de miles estilo es-AR: 15500.0 -> "$15.500". */
fun formatMoney(amount: Double, currency: Currency): String {
    val rounded = Math.round(amount)
    val withSeparators = String.format(Locale.GERMAN, "%,d", rounded) // GERMAN usa '.' como miles
    return "${currency.symbol}$withSeparators"
}

enum class AuctionStatus { PROGRAMADA, EN_VIVO, FINALIZADA }

/** Estado de la participación del usuario en la subasta en vivo. */
enum class BiddingStatus {
    /** El usuario tiene la oferta más alta. */
    GANANDO,
    /** Otro postor superó la oferta del usuario. */
    SUPERADO,
    /** La subasta cerró y el usuario ganó la pieza. */
    GANADA,
    /** El usuario puede ver pero no pujar (categoría, medio de pago o invitado). */
    VIEW_ONLY
}

/** Metadatos extra para obras de arte u objetos de diseñador. */
data class ArtMetadata(
    val artista: String,
    val fecha: String,
    val historia: String
)

/**
 * Un ítem (pieza) del catálogo de una subasta.
 * Una pieza puede estar compuesta por varios elementos (ej. "Juego de Té – 18 piezas").
 */
data class CatalogItem(
    val id: Long,
    val numeroPieza: String,
    val titulo: String,
    val descripcion: String,
    val valorBase: Double,
    val duenioActual: String,
    val imagenes: List<String> = emptyList(),
    val esMultiElemento: Boolean = false,
    val cantidadElementos: Int = 1,
    val arte: ArtMetadata? = null
)

/** Una subasta abierta o programada. */
data class Auction(
    val id: Long,
    val titulo: String,
    val categoria: Categoria,
    val estado: AuctionStatus,
    val currency: Currency,
    val rematador: String,
    val fechaHoraInicio: String,
    val itemActual: CatalogItem,
    val ofertaActual: Double = itemActual.valorBase
)

/** Una puja registrada en la subasta. */
data class Bid(
    val id: Long,
    val autor: String,
    val importe: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val esGanadora: Boolean = false,
    val esMia: Boolean = false
)

/** Usuario actual (mock). Modela lo necesario para el gating de la puja. */
data class CurrentUser(
    val id: Long,
    val nombre: String,
    val categoria: Categoria,
    val tieneMedioPagoVerificado: Boolean,
    val registrado: Boolean
) {
    companion object {
        /** Invitado: no registrado, sin categoría efectiva ni medio de pago. */
        val GUEST = CurrentUser(
            id = -1,
            nombre = "Invitado",
            categoria = Categoria.COMUN,
            tieneMedioPagoVerificado = false,
            registrado = false
        )
    }
}

/** Confirmación devuelta por el servidor (mock) tras registrar una puja. */
data class BidConfirmation(
    val bid: Bid,
    val nuevaOfertaActual: Double
)
