package com.subastasapp.data.repository

import com.subastasapp.data.model.auction.Auction
import com.subastasapp.data.model.auction.Bid
import com.subastasapp.data.model.auction.BidConfirmation
import com.subastasapp.data.model.auction.Categoria
import com.subastasapp.data.model.auction.CatalogItem

/**
 * Acceso a subastas, catálogo y pujas.
 *
 * Punto de inserción para la futura integración con el backend (REST/STOMP).
 * En esta entrega lo implementa [MockAuctionRepository] con datos en memoria.
 */
interface AuctionRepository {

    /** Subastas abiertas (opcionalmente filtradas por [categoria]). */
    suspend fun getActiveAuctions(categoria: Categoria? = null): List<Auction>

    suspend fun getAuction(id: Long): Auction?

    suspend fun getCatalogItem(id: Long): CatalogItem?

    /** Historial de pujas actual de una subasta (orden cronológico). */
    suspend fun getBidHistory(auctionId: Long): List<Bid>

    /**
     * Registra una puja. Suspende hasta recibir la confirmación del servidor (mock),
     * modelando la regla de "una puja a la vez": el llamador debe esperar este resultado
     * antes de habilitar otra puja.
     */
    suspend fun placeBid(auctionId: Long, importe: Double, autor: String): Result<BidConfirmation>
}
