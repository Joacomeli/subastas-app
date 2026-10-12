package com.subastasapp.backend.dto

/**
 * DTOs que consume el frontend de subastas (lista, detalle e historial de pujas).
 * Mapean el modelo relacional (Subasta/Catalogo/ItemCatalogo/Producto/Pujo) a una vista
 * "por subasta con su ítem actual", que es como lo muestra la app.
 */
data class SubastaResumenResponse(
    val id: Long,
    val titulo: String,
    val categoria: String,        // comun/especial/plata/oro/platino
    val estado: String,           // "abierta" / "cerrada"
    val moneda: String,           // ARS / USD
    val fechaHoraInicio: String,
    val rematador: String,
    val itemId: Long?,
    val itemTitulo: String,
    val itemNumeroPieza: String,
    val itemImagen: String?,
    val precioBase: Double,
    val ofertaActual: Double
)

data class SubastaDetalleResponse(
    val id: Long,
    val titulo: String,
    val categoria: String,
    val estado: String,
    val moneda: String,
    val fechaHoraInicio: String,
    val rematador: String,
    val itemId: Long?,
    val itemTitulo: String,
    val itemNumeroPieza: String,
    val itemDescripcion: String,
    val duenio: String,
    val imagenes: List<String>,
    val precioBase: Double,
    val ofertaActual: Double,
    val historial: List<BidResponse>
)

data class BidResponse(
    val id: Long,
    val autor: String,
    val importe: Double,
    val esGanadora: Boolean
)

data class PujarRequest(
    val importe: Double
)

data class PujarResponse(
    val ok: Boolean,
    val mensaje: String? = null,
    val nuevaOfertaActual: Double = 0.0,
    val bid: BidResponse? = null
)
