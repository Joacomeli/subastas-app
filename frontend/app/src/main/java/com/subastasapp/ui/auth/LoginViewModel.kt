package com.subastasapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subastasapp.data.datastore.TokenManager
import com.subastasapp.data.model.request.LoginRequest
import com.subastasapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val uiState: StateFlow<UiState<String>> = _uiState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = repository.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (token != null) {
                        tokenManager.saveToken(token)
                        _uiState.value = UiState.Success(token)
                    } else {
                        _uiState.value = UiState.Error("Token nulo")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null) {
                        try {
                            // Intentamos parsear el JSON de GenericResponse
                            val json = com.google.gson.Gson().fromJson(errorBody, com.subastasapp.data.model.response.GenericResponse::class.java)
                            json.message
                        } catch (e: Exception) {
                            "Error del servidor: ${response.code()}"
                        }
                    } else {
                        "Error desconocido"
                    }
                    _uiState.value = UiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Sin conexión a internet")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
