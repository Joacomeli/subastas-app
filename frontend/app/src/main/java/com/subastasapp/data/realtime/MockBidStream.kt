package com.subastasapp.data.realtime

import com.subastasapp.data.model.auction.Bid
import com.subastasapp.data.repository.MockAuctionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Implementación mock de [BidStream]: cada cierto tiempo genera una puja de la competencia
 * para la subasta observada, simulando el comportamiento en tiempo real que más adelante
 * proveerá un WebSocket/STOMP contra el backend.
 */
object MockBidStream : BidStream {

    private const val INTERVALO_MS = 9000L

    override fun observeBids(auctionId: Long): Flow<Bid> = flow {
        while (true) {
            delay(INTERVALO_MS)
            val bid = MockAuctionRepository.simulateCompetingBid(auctionId)
            if (bid != null) emit(bid)
        }
    }
}
