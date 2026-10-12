package com.subastasapp.data.repository

import com.subastasapp.data.model.auction.ArtMetadata
import com.subastasapp.data.model.auction.Auction
import com.subastasapp.data.model.auction.AuctionStatus
import com.subastasapp.data.model.auction.Bid
import com.subastasapp.data.model.auction.BidConfirmation
import com.subastasapp.data.model.auction.Categoria
import com.subastasapp.data.model.auction.CatalogItem
import com.subastasapp.data.model.auction.Currency
import com.subastasapp.domain.BidRules
import kotlinx.coroutines.delay
import kotlin.math.ceil

/**
 * Implementación en memoria de [AuctionRepository] para esta entrega (sin backend).
 *
 * Mantiene estado mutable por subasta (oferta actual + historial) para que las pujas del
 * usuario y las pujas simuladas de la competencia se reflejen de forma consistente.
 * Cuando el backend  esté listo, se reemplaza por una implementación REST sin
 * tocar la UI ni los ViewModels.
 *
 * Datos pensados para ejercitar todas las reglas del TPO:
 *  - Subasta #1 (ESPECIAL, ARS): límites de % normales.
 *  - Subasta #2 (ORO, USD): exenta de límites + moneda USD.
 *  - Subasta #3 (PLATINO, ARS): categoría mayor a la del usuario mock -> solo lectura.
 *  - Subasta #4 (ESPECIAL, ARS): pieza multi-elemento (juego de té).
 */
object MockAuctionRepository : AuctionRepository {

    private val items = listOf(
        CatalogItem(
            id = 1,
            numeroPieza = "#88",
            titulo = "Reloj Rolex GMT-Master II",
            descripcion = "Edición 2023, caja de acero Oystersteel, bisel Cerachrom rojo y azul. " +
                "Una de las piezas más codiciadas de la relojería deportiva contemporánea.",
            valorBase = 15000.0,
            duenioActual = "Colección privada",
            imagenes = listOf(
                "https://picsum.photos/seed/rolex1/600/400",
                "https://picsum.photos/seed/rolex2/600/400",
                "https://picsum.photos/seed/rolex3/600/400"
            )
        ),
        CatalogItem(
            id = 2,
            numeroPieza = "#42",
            titulo = "Reloj Patek Philippe Nautilus",
            descripcion = "Patek Philippe Nautilus en oro rosa. Movimiento automático de manufactura.",
            valorBase = 80000.0,
            duenioActual = "Patrimonio familiar Rossi",
            imagenes = listOf(
                "https://picsum.photos/seed/patek1/600/400",
                "https://picsum.photos/seed/patek2/600/400"
            )
        ),
        CatalogItem(
            id = 3,
            numeroPieza = "#07",
            titulo = "Naturaleza",
            descripcion = "Óleo sobre lienzo. Estudio de paisaje de la serie de jardines.",
            valorBase = 15600.0,
            duenioActual = "Galería Norte",
            imagenes = listOf("https://picsum.photos/seed/monet1/600/400"),
            arte = ArtMetadata(
                artista = "Claude Monet",
                fecha = "1899",
                historia = "Pieza atribuida al período de Giverny. Procedencia documentada desde " +
                    "1923, exhibida en dos retrospectivas europeas."
            )
        ),
        CatalogItem(
            id = 4,
            numeroPieza = "#15",
            titulo = "Juego de Té de Plata",
            descripcion = "Juego de té victoriano de plata 925, compuesto por 18 piezas.",
            valorBase = 9000.0,
            duenioActual = "Sucesión Álvarez",
            imagenes = listOf("https://picsum.photos/seed/tea1/600/400"),
            esMultiElemento = true,
            cantidadElementos = 18
        )
    )

    private val auctions: List<Auction> = listOf(
        Auction(
            id = 1,
            titulo = "Subasta de Relojes de Lujo",
            categoria = Categoria.ESPECIAL,
            estado = AuctionStatus.EN_VIVO,
            currency = Currency.ARS,
            rematador = "Mtro. Gómez",
            fechaHoraInicio = "Hoy 20:00",
            itemActual = items[0],
            ofertaActual = 15500.0
        ),
        Auction(
            id = 2,
            titulo = "Alta Relojería Internacional",
            categoria = Categoria.ORO,
            estado = AuctionStatus.EN_VIVO,
            currency = Currency.USD,
            rematador = "Mtra. Lombardi",
            fechaHoraInicio = "Hoy 21:30",
            itemActual = items[1],
            ofertaActual = 85000.0
        ),
        Auction(
            id = 3,
            titulo = "Arte Moderno y Contemporáneo",
            categoria = Categoria.PLATINO,
            estado = AuctionStatus.EN_VIVO,
            currency = Currency.ARS,
            rematador = "Mtro. Funes",
            fechaHoraInicio = "Mañana 19:00",
            itemActual = items[2],
            ofertaActual = 16000.0
        ),
        Auction(
            id = 4,
            titulo = "Antigüedades y Platería",
            categoria = Categoria.ESPECIAL,
            estado = AuctionStatus.EN_VIVO,
            currency = Currency.ARS,
            rematador = "Mtra. Paz",
            fechaHoraInicio = "Hoy 18:00",
            itemActual = items[3],
            ofertaActual = 9000.0
        )
    )

    private class State(
        var ofertaActual: Double,
        val history: MutableList<Bid>,
        var nextBidId: Long
    )

    private val nombresCompetencia = listOf(
        "Mariana", "Lucas", "Sofía", "Diego", "Valentina", "Martín"
    )

    private val state: MutableMap<Long, State> = auctions.associate { a ->
        val seed = mutableListOf(
            Bid(
                id = 1,
                autor = "Subastador",
                importe = a.itemActual.valorBase,
                esGanadora = false
            )
        )
        if (a.ofertaActual > a.itemActual.valorBase) {
            seed.add(
                Bid(
                    id = 2,
                    autor = nombresCompetencia.random(),
                    importe = a.ofertaActual,
                    esGanadora = true
                )
            )
        } else {
            seed[0] = seed[0].copy(esGanadora = true)
        }
        a.id to State(a.ofertaActual, seed, nextBidId = seed.size.toLong() + 1)
    }.toMutableMap()

    /** Oferta más alta vigente de la subasta (para validar pujas en el ViewModel). */
    fun ofertaActual(auctionId: Long): Double = state[auctionId]?.ofertaActual ?: 0.0

    override suspend fun getActiveAuctions(categoria: Categoria?): List<Auction> =
        auctions
            .filter { it.estado != AuctionStatus.FINALIZADA }
            .filter { categoria == null || it.categoria == categoria }
            .map { it.copy(ofertaActual = ofertaActual(it.id)) }

    override suspend fun getAuction(id: Long): Auction? =
        auctions.find { it.id == id }?.copy(ofertaActual = ofertaActual(id))

    override suspend fun getCatalogItem(id: Long): CatalogItem? =
        items.find { it.id == id }

    override suspend fun getBidHistory(auctionId: Long): List<Bid> =
        state[auctionId]?.history?.toList() ?: emptyList()

    override suspend fun placeBid(
        auctionId: Long,
        importe: Double,
        autor: String
    ): Result<BidConfirmation> {
        val st = state[auctionId]
            ?: return Result.failure(IllegalStateException("Subasta inexistente"))

        // Simula la confirmación del servidor: hasta no recibirla, la UI bloquea otra puja.
        delay(900)

        st.history.replaceAll { it.copy(esGanadora = false) }
        val bid = Bid(
            id = st.nextBidId++,
            autor = autor,
            importe = importe,
            esGanadora = true,
            esMia = true
        )
        st.history.add(bid)
        st.ofertaActual = importe
        return Result.success(BidConfirmation(bid, importe))
    }

    /**
     * Genera una puja simulada de la competencia, respetando los límites de la subasta.
     * Usado por [com.subastasapp.data.realtime.MockBidStream] para mover la subasta en vivo.
     */
    fun simulateCompetingBid(auctionId: Long): Bid? {
        val auction = auctions.find { it.id == auctionId } ?: return null
        val st = state[auctionId] ?: return null
        val base = auction.itemActual.valorBase
        // La competencia "se rinde" cuando el precio sube demasiado, permitiendo que el
        // usuario pueda quedarse con la pieza (regla: cuando ya nadie puja, gana el último).
        if (st.ofertaActual >= base * 1.35) return null
        val importe = if (auction.categoria.exentaDeLimites) {
            ceil(st.ofertaActual * 1.02)
        } else {
            ceil(BidRules.minPuja(base, st.ofertaActual))
        }
        st.history.replaceAll { it.copy(esGanadora = false) }
        val bid = Bid(
            id = st.nextBidId++,
            autor = nombresCompetencia.random(),
            importe = importe,
            esGanadora = true,
            esMia = false
        )
        st.history.add(bid)
        st.ofertaActual = importe
        return bid
    }
}
