package com.subastasapp.backend.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val nombre: String,
    val apellido: String,
    val email: String,
    val password: String,
    val documento: String? = null,
    val domicilioLegal: String? = null,
    val paisOrigen: String? = null
)

data class RegisterStep1Request(
    val nombre: String,
    val apellido: String,
    val domicilioLegal: String? = null,
    val paisOrigen: String? = null
)

data class GenericResponse(
    val success: Boolean,
    val message: String
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

data class JwtResponse(
    val token: String,
    val email: String,
    val nombre: String,
    val categoria: String
)
