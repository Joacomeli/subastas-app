package com.subastasapp.ui.auction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subastasapp.data.model.auction.Auction
import com.subastasapp.data.model.auction.Bid
import com.subastasapp.data.model.auction.BiddingStatus
import com.subastasapp.data.model.auction.CurrentUser
import com.subastasapp.data.realtime.BidStream
import com.subastasapp.data.realtime.MockBidStream
import com.subastasapp.data.repository.AuctionRepository
import com.subastasapp.data.repository.CurrentUserProvider
import com.subastasapp.data.repository.MockAuctionRepository
import com.subastasapp.domain.BidRules
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la pantalla de subasta en vivo. */
data class LiveAuctionUiState(
    val loading: Boolean = true,
    val auction: Auction? = null,
    val historial: List<Bid> = emptyList(),
    val ofertaActual: Double = 0.0,
    val rangoMin: Double = 0.0,
    val rangoMax: Double? = null,   // null = subasta exenta de límite máximo (ORO/PLATINO)
    val status: BiddingStatus? = null,
    val canBid: Boolean = false,
    val accessReason: String? = null,
    val isBidPending: Boolean = false,
    val inputError: String? = null,
    val finalizada: Boolean = false
)

class LiveAuctionViewModel(
    private val auctionId: Long,
    private val isGuest: Boolean,
    private val repository: AuctionRepository = MockAuctionRepository,
    private val bidStream: BidStream = MockBidStream
) : ViewModel() {

    private val _state = MutableStateFlow(LiveAuctionUiState())
    val state: StateFlow<LiveAuctionUiState> = _state.asStateFlow()

    private val user: CurrentUser = CurrentUserProvider.current(isGuest)
    private var haParticipado = false
    private var winWatch: Job? = null
    private var streamJob: Job? = null

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val auction = repository.getAuction(auctionId)
            if (auction == null) {
                _state.value = _state.value.copy(loading = false)
                return@launch
            }
            val historial = repository.getBidHistory(auctionId)
            val oferta = auction.ofertaActual

            val (canBid, reason) = evaluarAcceso(auction)

            _state.value = _state.value.copy(
                loading = false,
                auction = auction,
                historial = historial,
                ofertaActual = oferta,
                rangoMin = rangoMinDe(auction, oferta),
                rangoMax = rangoMaxDe(auction, oferta),
                canBid = canBid,
                accessReason = reason,
                status = if (!canBid) BiddingStatus.VIEW_ONLY else null
            )

            observarPujas()
        }
    }

    /** Regla TPO: registrado + categoría suficiente + medio de pago verificado. */
    private fun evaluarAcceso(auction: Auction): Pair<Boolean, String?> {
        if (!user.registrado) {
            return false to "Iniciá sesión para poder pujar."
        }
        if (!user.categoria.puedeAcceder(auction.categoria)) {
            return false to "Tu categoría (${user.categoria.displayName}) no habilita una subasta " +
                "${auction.categoria.displayName}. Podés ver la subasta pero no pujar."
        }
        if (!user.tieneMedioPagoVerificado) {
            return false to "Necesitás al menos un medio de pago verificado para pujar."
        }
        return true to null
    }

    // Para subastas exentas (ORO/PLATINO) el mínimo es solo superar la oferta actual; el resto
    // aplica el +1% del valor base. Las exentas no tienen máximo.
    private fun rangoMinDe(auction: Auction, oferta: Double): Double =
        if (auction.categoria.exentaDeLimites) oferta
        else BidRules.minPuja(auction.itemActual.valorBase, oferta)

    private fun rangoMaxDe(auction: Auction, oferta: Double): Double? =
        if (auction.categoria.exentaDeLimites) null
        else BidRules.maxPuja(auction.itemActual.valorBase, oferta)

    private fun observarPujas() {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            bidStream.observeBids(auctionId).collect { bid ->
                // Si la subasta ya finalizó, ignoramos pujas tardías.
                if (_state.value.finalizada) {
                    streamJob?.cancel()
                    return@collect
                }
                // Llega una puja en tiempo real. Ignoramos las ya presentes (p. ej. la propia,
                // que ya agregó placeBid, y que el polling también devuelve).
                if (_state.value.historial.any { it.id == bid.id }) return@collect
                winWatch?.cancel()
                val s = _state.value
                val auction = s.auction ?: return@collect
                val nuevoStatus = when {
                    !s.canBid -> BiddingStatus.VIEW_ONLY
                    haParticipado -> BiddingStatus.SUPERADO
                    else -> null
                }
                _state.value = s.copy(
                    historial = s.historial.map { it.copy(esGanadora = false) } + bid,
                    ofertaActual = bid.importe,
                    rangoMin = rangoMinDe(auction, bid.importe),
                    rangoMax = rangoMaxDe(auction, bid.importe),
                    status = nuevoStatus
                )
            }
        }
    }

    fun onAmountChange() {
        if (_state.value.inputError != null) {
            _state.value = _state.value.copy(inputError = null)
        }
    }

    fun placeBid(montoTexto: String) {
        val s = _state.value
        val auction = s.auction ?: return
        if (!s.canBid || s.isBidPending || s.finalizada) return

        val monto = montoTexto.replace(".", "").replace(",", ".").toDoubleOrNull()
        val validation = BidRules.validatePuja(
            importe = monto,
            valorBase = auction.itemActual.valorBase,
            ofertaActual = s.ofertaActual,
            categoria = auction.categoria,
            currency = auction.currency
        )

        val errorMsg = when (validation) {
            is BidRules.BidValidation.Ok -> null
            is BidRules.BidValidation.TooLow -> validation.mensaje
            is BidRules.BidValidation.TooHigh -> validation.mensaje
            is BidRules.BidValidation.Invalid -> validation.mensaje
        }
        if (errorMsg != null) {
            _state.value = s.copy(inputError = errorMsg)
            return
        }

        // Regla TPO: una puja a la vez. Bloqueamos hasta recibir la confirmación.
        _state.value = s.copy(isBidPending = true, inputError = null)
        viewModelScope.launch {
            val result = repository.placeBid(auctionId, monto!!, user.nombre)
            result.onSuccess { confirmation ->
                haParticipado = true
                val cur = _state.value
                val auc = cur.auction!!
                _state.value = cur.copy(
                    historial = cur.historial.map { it.copy(esGanadora = false) } + confirmation.bid,
                    ofertaActual = confirmation.nuevaOfertaActual,
                    rangoMin = rangoMinDe(auc, confirmation.nuevaOfertaActual),
                    rangoMax = rangoMaxDe(auc, confirmation.nuevaOfertaActual),
                    status = BiddingStatus.GANANDO,
                    isBidPending = false
                )
                vigilarCierre()
            }.onFailure { err ->
                _state.value = _state.value.copy(
                    isBidPending = false,
                    inputError = err.message ?: "No se pudo registrar la puja. Reintentá."
                )
            }
        }
    }

    /**
     * Si el usuario mantiene la oferta más alta y nadie puja durante un tiempo, gana la pieza
     * (regla: cuando ya nadie puja con un valor más alto, gana el de la última puja).
     */
    private fun vigilarCierre() {
        winWatch?.cancel()
        winWatch = viewModelScope.launch {
            delay(7000)
            val s = _state.value
            if (s.status == BiddingStatus.GANANDO && !s.finalizada) {
                streamJob?.cancel()
                _state.value = s.copy(finalizada = true, status = BiddingStatus.GANADA)
            }
        }
    }
}
