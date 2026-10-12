package com.subastasapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subastasapp.data.model.request.*
import com.subastasapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val uiState: StateFlow<UiState<String>> = _uiState

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = repository.forgotPassword(ForgotPasswordRequest(email))
                if (response.isSuccessful) {
                    _uiState.value = UiState.Success("Código enviado")
                } else {
                    val errorMsg = response.errorBody()?.string() ?: ""
                    _uiState.value = UiState.Error("Error ${response.code()}: $errorMsg")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error de red: ${e.message}")
            }
        }
    }

    fun verifyCode(email: String, code: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = repository.verifyResetCode(VerifyCodeRequest(email, code))
                if (response.isSuccessful) {
                    _uiState.value = UiState.Success("Código verificado")
                } else {
                    val errorMsg = response.errorBody()?.string() ?: ""
                    _uiState.value = UiState.Error("Error ${response.code()}: $errorMsg")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error de red: ${e.message}")
            }
        }
    }

    fun resetPassword(email: String, code: String, newPass: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = repository.resetPassword(ResetPasswordRequest(email, code, newPass))
                if (response.isSuccessful) {
                    _uiState.value = UiState.Success("Contraseña cambiada")
                } else {
                    val errorMsg = response.errorBody()?.string() ?: ""
                    _uiState.value = UiState.Error("Error ${response.code()}: $errorMsg")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error de red: ${e.message}")
            }
        }
    }

    fun resendCode(email: String) {
        viewModelScope.launch {
            try {
                repository.resendResetCode(ForgotPasswordRequest(email))
            } catch (e: Exception) {}
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
