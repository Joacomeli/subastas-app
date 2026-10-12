package com.subastasapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subastasapp.data.datastore.TokenManager
import com.subastasapp.data.model.response.*
import com.subastasapp.data.repository.AuthRepository
import com.subastasapp.data.repository.UserRepository
import com.subastasapp.ui.auth.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _profileState = MutableStateFlow<UiState<UserProfileResponse>>(UiState.Idle)
    val profileState: StateFlow<UiState<UserProfileResponse>> = _profileState

    private val _metricsState = MutableStateFlow<UiState<UserMetricsResponse>>(UiState.Idle)
    val metricsState: StateFlow<UiState<UserMetricsResponse>> = _metricsState

    private val _paymentMethodsState = MutableStateFlow<UiState<List<PaymentMethodResponse>>>(UiState.Idle)
    val paymentMethodsState: StateFlow<UiState<List<PaymentMethodResponse>>> = _paymentMethodsState

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadProfile()
        loadMetrics()
        loadPaymentMethods()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = UiState.Loading
            try {
                val response = userRepository.getProfile()
                if (response.isSuccessful) {
                    _profileState.value = UiState.Success(response.body()!!)
                } else if (response.code() == 401) {
                    _profileState.value = UiState.Error("Session expired")
                } else {
                    _profileState.value = UiState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _profileState.value = UiState.Error("Network error")
            }
        }
    }

    private fun loadMetrics() {
        viewModelScope.launch {
            _metricsState.value = UiState.Loading
            try {
                val response = userRepository.getMetrics()
                if (response.isSuccessful) {
                    _metricsState.value = UiState.Success(response.body()!!)
                } else {
                    _metricsState.value = UiState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _metricsState.value = UiState.Error("Network error")
            }
        }
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            _paymentMethodsState.value = UiState.Loading
            try {
                val response = userRepository.getPaymentMethods()
                if (response.isSuccessful) {
                    _paymentMethodsState.value = UiState.Success(response.body()!!)
                } else {
                    _paymentMethodsState.value = UiState.Error("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                _paymentMethodsState.value = UiState.Error("Network error")
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (e: Exception) {
                // Ignore logout error
            } finally {
                tokenManager.deleteToken()
                onSuccess()
            }
        }
    }
}
