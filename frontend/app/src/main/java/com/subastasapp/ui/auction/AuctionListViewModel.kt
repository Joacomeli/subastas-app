package com.subastasapp.ui.auction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subastasapp.data.model.auction.Auction
import com.subastasapp.data.model.auction.Categoria
import com.subastasapp.data.repository.AuctionRepository
import com.subastasapp.data.repository.MockAuctionRepository
import com.subastasapp.ui.auth.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuctionListViewModel(
    private val repository: AuctionRepository = MockAuctionRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Auction>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Auction>>> = _state.asStateFlow()

    private val _selectedCategoria = MutableStateFlow<Categoria?>(null)
    val selectedCategoria: StateFlow<Categoria?> = _selectedCategoria.asStateFlow()

    init {
        load()
    }

    fun selectCategoria(categoria: Categoria?) {
        _selectedCategoria.value = categoria
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val auctions = repository.getActiveAuctions(_selectedCategoria.value)
                _state.value = UiState.Success(auctions)
            } catch (e: Exception) {
                _state.value = UiState.Error("No se pudieron cargar las subastas")
            }
        }
    }
}
