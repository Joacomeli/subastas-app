package com.subastasapp.data.realtime

import com.subastasapp.data.api.SubastaApiService
import com.subastasapp.data.model.auction.Bid
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Implementación real de [BidStream] por polling REST: cada [intervalMs] consulta el historial
 * de pujas del backend y emite únicamente las pujas nuevas (no vistas). Es el "tiempo real"
 * aproximado de esta entrega; el día de mañana se reemplaza por WebSocket sin tocar la UI.
 */
class PollingBidStream(
    private val api: SubastaApiService,
    private val intervalMs: Long = 4000L
) : BidStream {

    override fun observeBids(auctionId: Long): Flow<Bid> = flow {
        val seen = mutableSetOf<Long>()
        // Sembramos con lo ya existente para no re-emitir el historial inicial.
        runCatching { api.getPujas(auctionId).body()?.forEach { seen.add(it.id) } }

        while (true) {
            delay(intervalMs)
            runCatching {
                val pujas = api.getPujas(auctionId).body() ?: return@runCatching
                for (p in pujas) {
                    if (seen.add(p.id)) {
                        emit(Bid(id = p.id, autor = p.autor, importe = p.importe, esGanadora = p.esGanadora))
                    }
                }
            }
        }
    }
}
