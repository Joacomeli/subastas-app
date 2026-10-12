package com.subastasapp.data.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val nombre: String,
    val apellido: String,
    val email: String,
    val password: String
)

data class JwtResponse(
    val token: String,
    val email: String,
    val nombre: String,
    val categoria: String
)

data class Auction(
    val id: Long,
    val titulo: String,
    val estado: String,
    val precioBaseTotal: Double
)
