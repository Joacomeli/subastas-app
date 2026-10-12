package com.subastasapp.data.model

/** DTOs que devuelve el backend de subastas (PujaController). */
data class SubastaResumenDto(
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
    val itemImagen: String?,
    val precioBase: Double,
    val ofertaActual: Double
)

data class SubastaDetalleDto(
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
    val historial: List<BidDto>
)

data class BidDto(
    val id: Long,
    val autor: String,
    val importe: Double,
    val esGanadora: Boolean
)

data class PujarRequestDto(
    val importe: Double
)

data class PujarResponseDto(
    val ok: Boolean,
    val mensaje: String? = null,
    val nuevaOfertaActual: Double = 0.0,
    val bid: BidDto? = null
)
