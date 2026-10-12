package com.subastasapp.data.repository

import com.subastasapp.data.api.SubastaApiService
import com.subastasapp.data.model.BidDto
import com.subastasapp.data.model.PujarRequestDto
import com.subastasapp.data.model.SubastaDetalleDto
import com.subastasapp.data.model.SubastaResumenDto
import com.subastasapp.data.model.auction.Auction
import com.subastasapp.data.model.auction.AuctionStatus
import com.subastasapp.data.model.auction.Bid
import com.subastasapp.data.model.auction.BidConfirmation
import com.subastasapp.data.model.auction.CatalogItem
import com.subastasapp.data.model.auction.Categoria
import com.subastasapp.data.model.auction.Currency

/**
 * Implementación real de [AuctionRepository] contra el backend (PujaController) vía REST.
 * Reemplaza al MockAuctionRepository: la lista/detalle salen de las subastas publicadas por
 * el flujo de consignaciones, y las pujas se persisten en el servidor.
 */
class RealAuctionRepository(
    private val api: SubastaApiService
) : AuctionRepository {

    override suspend fun getActiveAuctions(categoria: Categoria?): List<Auction> {
        val resp = api.getSubastas()
        val list = if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
        return list.map { it.toAuction() }.filter { categoria == null || it.categoria == categoria }
    }

    override suspend fun getAuction(id: Long): Auction? {
        val resp = api.getDetalle(id)
        return if (resp.isSuccessful) resp.body()?.toAuction() else null
    }

    override suspend fun getCatalogItem(id: Long): CatalogItem? = getAuction(id)?.itemActual

    override suspend fun getBidHistory(auctionId: Long): List<Bid> {
        val resp = api.getPujas(auctionId)
        return if (resp.isSuccessful) resp.body().orEmpty().map { it.toBid() } else emptyList()
    }

    override suspend fun placeBid(auctionId: Long, importe: Double, autor: String): Result<BidConfirmation> {
        return try {
            val resp = api.pujar(auctionId, PujarRequestDto(importe))
            val body = resp.body()
            when {
                resp.isSuccessful && body != null && body.ok && body.bid != null ->
                    Result.success(BidConfirmation(body.bid.toBid().copy(esMia = true), body.nuevaOfertaActual))
                // 401/403: la sesión venció o el usuario está en modo demo sin token real.
                resp.code() == 401 || resp.code() == 403 ->
                    Result.failure(IllegalStateException("Iniciá sesión para poder pujar."))
                // 400 con mensaje del backend (regla violada) -> mostramos el mensaje literal.
                else ->
                    Result.failure(IllegalStateException(body?.mensaje ?: "No se pudo registrar la puja."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- Mapeos backend -> modelos de UI ----

    private fun SubastaResumenDto.toAuction() = Auction(
        id = id,
        titulo = titulo,
        categoria = parseCategoria(categoria),
        estado = parseEstado(estado),
        currency = parseMoneda(moneda),
        rematador = rematador,
        fechaHoraInicio = fechaHoraInicio,
        itemActual = CatalogItem(
            id = itemId ?: id,
            numeroPieza = itemNumeroPieza,
            titulo = itemTitulo,
            descripcion = "",
            valorBase = precioBase,
            duenioActual = "",
            imagenes = listOfNotNull(itemImagen)
        ),
        ofertaActual = ofertaActual
    )

    private fun SubastaDetalleDto.toAuction() = Auction(
        id = id,
        titulo = titulo,
        categoria = parseCategoria(categoria),
        estado = parseEstado(estado),
        currency = parseMoneda(moneda),
        rematador = rematador,
        fechaHoraInicio = fechaHoraInicio,
        itemActual = CatalogItem(
            id = itemId ?: id,
            numeroPieza = itemNumeroPieza,
            titulo = itemTitulo,
            descripcion = itemDescripcion,
            valorBase = precioBase,
            duenioActual = duenio,
            imagenes = imagenes
        ),
        ofertaActual = ofertaActual
    )

    private fun BidDto.toBid() = Bid(id = id, autor = autor, importe = importe, esGanadora = esGanadora)

    private fun parseCategoria(s: String): Categoria =
        try { Categoria.valueOf(s.trim().uppercase()) } catch (e: Exception) { Categoria.COMUN }

    private fun parseMoneda(s: String): Currency =
        if (s.trim().uppercase() == "USD") Currency.USD else Currency.ARS

    private fun parseEstado(s: String): AuctionStatus = when (s.trim().lowercase()) {
        "abierta" -> AuctionStatus.EN_VIVO
        "programada" -> AuctionStatus.PROGRAMADA
        else -> AuctionStatus.FINALIZADA
    }
}
