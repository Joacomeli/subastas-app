package com.subastasapp.data.api

import com.subastasapp.data.model.request.*
import com.subastasapp.data.model.response.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

import com.subastasapp.data.model.CrearConsignacionRequest
import com.subastasapp.data.model.ConfirmarConsignacionRequest
import com.subastasapp.data.model.ConsignacionResponse
import com.subastasapp.data.model.NotificacionResponse

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<GenericResponse>

    @POST("auth/register/step1")
    suspend fun registerStep1(@Body request: RegisterStep1Request): Response<GenericResponse>

    @POST("auth/register/step3")
    suspend fun registerStep3(@Body request: Any): Response<GenericResponse>

    @POST("auth/complete-registration")
    suspend fun completeRegistration(@Body request: CompleteRegistrationRequest): Response<AuthResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<GenericResponse>

    @POST("auth/verify-reset-code")
    suspend fun verifyResetCode(@Body request: VerifyCodeRequest): Response<GenericResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<GenericResponse>

    @POST("auth/resend-reset-code")
    suspend fun resendResetCode(@Body request: ForgotPasswordRequest): Response<GenericResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}

interface UserApiService {
    @GET("user/profile")
    suspend fun getProfile(): Response<UserProfileResponse>

    @GET("user/metrics")
    suspend fun getMetrics(): Response<UserMetricsResponse>

    @GET("user/payment-methods")
    suspend fun getPaymentMethods(): Response<List<PaymentMethodResponse>>

    @POST("user/payment-methods")
    suspend fun addPaymentMethod(@Body request: PaymentMethodRequest): Response<PaymentMethodResponse>

    @DELETE("user/payment-methods/{id}")
    suspend fun deletePaymentMethod(@Path("id") id: Int): Response<Unit>

    @POST("api/consignaciones")
    suspend fun crearConsignacion(@Body request: CrearConsignacionRequest): Response<ConsignacionResponse>

    @GET("api/consignaciones")
    suspend fun getConsignaciones(): Response<List<ConsignacionResponse>>

    @POST("api/consignaciones/{id}/confirmar")
    suspend fun confirmarConsignacion(
        @Path("id") id: Long,
        @Body request: ConfirmarConsignacionRequest
    ): Response<ConsignacionResponse>

    @GET("api/notificaciones")
    suspend fun getNotificaciones(): Response<List<NotificacionResponse>>

    @POST("api/notificaciones/{id}/leida")
    suspend fun marcarNotificacionLeida(@Path("id") id: Long): Response<NotificacionResponse>

    @POST("api/notificaciones/leer-todas")
    suspend fun marcarTodasLeidas(): Response<Map<String, Int>>
}
