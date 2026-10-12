package com.subastasapp.data.model.response

data class UserProfileResponse(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val email: String,
    val categoria: String,
    val fotoUrl: String?
)

data class UserMetricsResponse(
    val totalAuctions: Int,
    val wins: Int,
    val totalBids: Int
)

data class PaymentMethodResponse(
    val id: Int,
    val tipo: String,       // "TARJETA", "CUENTA", "CHEQUE"
    val descripcion: String,
    val ultimosDigitos: String?,
    val verificado: Boolean
)
