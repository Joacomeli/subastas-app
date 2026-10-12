package com.subastasapp.ui.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subastasapp.data.datastore.TokenManager
import com.subastasapp.data.model.request.CompleteRegistrationRequest
import com.subastasapp.data.model.request.LoginRequest
import com.subastasapp.data.model.request.PaymentMethodRequest
import com.subastasapp.data.model.request.RegisterStep1Request
import com.subastasapp.data.model.response.PaymentMethodResponse
import com.subastasapp.data.repository.AuthRepository
import com.subastasapp.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Any>>(UiState.Idle)
    val uiState: StateFlow<UiState<Any>> = _uiState

    // Estado temporal para el flujo de 3 pasos
    var tempNombre = ""
    var tempApellido = ""
    var tempDomicilio = ""
    var tempPais = ""

    private val _paymentMethods = MutableStateFlow<List<PaymentMethodResponse>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethodResponse>> = _paymentMethods

    // Email ingresado en el Paso 1; lo reusan el Paso 2 (para crear la clave) y el Paso 3
    // (para asociar los medios de pago a la persona ya creada en el backend).
    var tempEmail = ""

    // Agregamos una lista temporal para enviar al backend en el paso 3
    private val _paymentMethodsRequests = mutableListOf<PaymentMethodRequest>()

    fun registerStep1(
        nombre: String,
        apellido: String,
        domicilio: String,
        pais: String,
        frenteFile: File,
        dorsoFile: File
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Llamamos al backend para validar el paso 1
                val request = RegisterStep1Request(
                    nombre = nombre,
                    apellido = apellido,
                    domicilioLegal = domicilio,
                    paisOrigen = pais
                )
                val response = authRepository.registerStep1(request)
                
                if (response.isSuccessful) {
                    tempNombre = nombre
                    tempApellido = apellido
                    tempDomicilio = domicilio
                    tempPais = pais
                    _uiState.value = UiState.Success("Paso 1 completado")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val msg = if (errorBody != null) {
                        try {
                            com.google.gson.Gson().fromJson(errorBody, com.subastasapp.data.model.response.GenericResponse::class.java).message
                        } catch (e: Exception) { "Error en Paso 1" }
                    } else "Error en Paso 1"
                    _uiState.value = UiState.Error(msg)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error de red: ${e.message}")
            }
        }
    }

    fun completeRegistration(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val request = com.subastasapp.data.model.request.RegisterRequest(
                    nombre = tempNombre,
                    apellido = tempApellido,
                    email = email,
                    password = password,
                    domicilioLegal = tempDomicilio,
                    paisOrigen = tempPais
                )
                
                val response = authRepository.register(request)
                if (response.isSuccessful) {
                    tempEmail = email // Guardamos el email exitoso

                    // Login automático justo después de registrarse: así el Paso 3 y todo
                    // lo que venga después (incluido el Perfil al entrar a Main) ya tienen
                    // token guardado en el TokenManager y los GET autenticados funcionan.
                    val loginResp = authRepository.login(LoginRequest(email, password))
                    val token = if (loginResp.isSuccessful) loginResp.body()?.token else null
                    if (token != null) {
                        tokenManager.saveToken(token)
                    }
                    _uiState.value = UiState.Success("Paso 2 completado")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val msg = if (errorBody != null) {
                        try {
                            com.google.gson.Gson().fromJson(errorBody, com.subastasapp.data.model.response.GenericResponse::class.java).message
                        } catch (e: Exception) { "Error en registro" }
                    } else "Error en registro"
                    _uiState.value = UiState.Error(msg)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error de red: ${e.message}")
            }
        }
    }

    fun finishRegistrationStep3() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Enviamos los métodos de pago vinculados al email del usuario
                val body = mapOf(
                    "email" to tempEmail,
                    "paymentMethods" to _paymentMethodsRequests
                )
                val response = authRepository.registerStep3(body as Any)
                if (response.isSuccessful) {
                    _uiState.value = UiState.Success("Registro finalizado")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val msg = if (errorBody != null) {
                        try {
                            com.google.gson.Gson().fromJson(errorBody, com.subastasapp.data.model.response.GenericResponse::class.java).message
                        } catch (e: Exception) { "Error en Paso 3" }
                    } else "Error en Paso 3"
                    _uiState.value = UiState.Error(msg)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error de red: ${e.message}")
            }
        }
    }

    fun loadPaymentMethods() {
        // ... (sin cambios)
    }

    fun addPaymentMethod(request: PaymentMethodRequest) {
        _uiState.value = UiState.Loading
        _paymentMethodsRequests.add(request) // Guardamos para el envío final

        // Simulado: Agregamos localmente para la UI
        val newList = _paymentMethods.value.toMutableList()
        val description = when(request.type) {
            "BANK_ACCOUNT" -> "${request.banco} - ${request.cbu}"
            "CREDIT_CARD" -> "Visa **** ${request.numeroUltimos4}"
            else -> "Cheque ${request.banco} $${request.monto}"
        }
        newList.add(
            PaymentMethodResponse(
                id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                tipo = request.type,
                descripcion = description,
                ultimosDigitos = request.numeroUltimos4,
                verificado = true
            )
        )
        _paymentMethods.value = newList
        _uiState.value = UiState.Success("Método agregado")
    }

    fun deletePaymentMethod(id: Int) {
        // En una app real también lo borraríamos de _paymentMethodsRequests
        val newList = _paymentMethods.value.filter { it.id != id }
        _paymentMethods.value = newList
    }

    /**
     * Edita en memoria el medio de pago seleccionado. Por ahora marca la descripción
     * con el prefijo "EDITADA - " para que la UI refleje el cambio; la edición real
     * (formulario, persistencia al backend) queda como TODO.
     */
    fun editPaymentMethod(id: Int) {
        val newList = _paymentMethods.value.toMutableList()
        val index = newList.indexOfFirst { it.id == id }
        if (index == -1) return
        val old = newList[index]
        newList[index] = PaymentMethodResponse(
            id = old.id,
            tipo = old.tipo,
            descripcion = "EDITADA - ${old.descripcion}",
            ultimosDigitos = old.ultimosDigitos,
            verificado = old.verificado
        )
        _paymentMethods.value = newList
        _uiState.value = UiState.Success("Tarjeta editada")
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
