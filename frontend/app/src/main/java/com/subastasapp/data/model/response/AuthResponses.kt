package com.subastasapp.data.model.response

data class AuthResponse(
    val token: String,
    val type: String = "Bearer"
)

data class GenericResponse(
    val success: Boolean,
    val message: String
)
