package com.subastasapp.data.realtime

import com.subastasapp.data.model.auction.Bid
import kotlinx.coroutines.flow.Flow

/**
 * Fuente de pujas en tiempo real para una subasta.
 *
 * Hoy la implementa [com.subastasapp.data.repository.MockBidStream] emitiendo pujas simuladas.
 * Cuando el backend  esté disponible, una implementación `StompBidStream`
 * (OkHttp WebSocket / Krossbow) reemplazará al mock sin tocar la UI ni los ViewModels.
 */
interface BidStream {
    /** Emite cada nueva puja que llega para la subasta [auctionId]. */
    fun observeBids(auctionId: Long): Flow<Bid>
}
