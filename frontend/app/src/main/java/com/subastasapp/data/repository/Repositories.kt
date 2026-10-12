package com.subastasapp.data.repository

import com.subastasapp.data.api.AuthApiService
import com.subastasapp.data.api.UserApiService
import com.subastasapp.data.model.request.*
import okhttp3.MultipartBody
import okhttp3.RequestBody

import com.subastasapp.data.model.CrearConsignacionRequest
import com.subastasapp.data.model.ConfirmarConsignacionRequest
import com.subastasapp.data.model.ConsignacionResponse
import com.subastasapp.data.model.NotificacionResponse

class AuthRepository(private val api: AuthApiService) {
    suspend fun login(request: LoginRequest) = api.login(request)
    
    suspend fun register(request: RegisterRequest) = api.register(request)

    suspend fun registerStep1(request: RegisterStep1Request) = api.registerStep1(request)

    suspend fun registerStep3(request: Any) = api.registerStep3(request)

    suspend fun completeRegistration(request: CompleteRegistrationRequest) = api.completeRegistration(request)
    suspend fun forgotPassword(request: ForgotPasswordRequest) = api.forgotPassword(request)
    suspend fun verifyResetCode(request: VerifyCodeRequest) = api.verifyResetCode(request)
    suspend fun resetPassword(request: ResetPasswordRequest) = api.resetPassword(request)
    suspend fun resendResetCode(request: ForgotPasswordRequest) = api.resendResetCode(request)
    
    suspend fun logout() = api.logout()
}

class UserRepository(private val api: UserApiService) {
    suspend fun getProfile() = api.getProfile()
    suspend fun getMetrics() = api.getMetrics()
    suspend fun getPaymentMethods() = api.getPaymentMethods()
    suspend fun addPaymentMethod(request: PaymentMethodRequest) = api.addPaymentMethod(request)
    suspend fun deletePaymentMethod(id: Int) = api.deletePaymentMethod(id)

    suspend fun crearConsignacion(request: CrearConsignacionRequest) = api.crearConsignacion(request)
    suspend fun getConsignaciones() = api.getConsignaciones()
    suspend fun confirmarConsignacion(id: Long, request: ConfirmarConsignacionRequest) = api.confirmarConsignacion(id, request)
    suspend fun getNotificaciones() = api.getNotificaciones()
    suspend fun marcarNotificacionLeida(id: Long) = api.marcarNotificacionLeida(id)
    suspend fun marcarTodasLeidas() = api.marcarTodasLeidas()
}
