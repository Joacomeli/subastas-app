package com.subastasapp.data.model.request

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val nombre: String,
    val apellido: String,
    val email: String,
    val password: String,
    val domicilioLegal: String? = null,
    val paisOrigen: String? = null
)

data class CompleteRegistrationRequest(
    val email: String,
    val password: String
)

data class RegisterStep1Request(
    val nombre: String,
    val apellido: String,
    val domicilioLegal: String? = null,
    val paisOrigen: String? = null
)

data class ForgotPasswordRequest(
    val email: String
)

data class VerifyCodeRequest(
    val email: String,
    val code: String
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

data class PaymentMethodRequest(
    val type: String, // "BANK_ACCOUNT", "CREDIT_CARD", "CHEQUE"
    val banco: String? = null,
    val cbu: String? = null,
    val alias: String? = null,
    val titular: String? = null,
    val documentoTitular: String? = null,
    val cuitCuil: String? = null,
    val tipoCuenta: String? = null, // "CAJA_AHORRO", "CUENTA_CORRIENTE"
    val numeroCuenta: String? = null,
    val moneda: String? = null, // "ARS", "USD"
    val numero: String? = null,
    val numeroUltimos4: String? = null,
    val vencimiento: String? = null,
    val cvv: String? = null,
    val monto: Double? = null,
    val numeroCheque: String? = null,
    val fechaEmision: String? = null,
    val sucursal: String? = null,
    val fotoFrentePath: String? = null,
    val fotoDorsoPath: String? = null
)
