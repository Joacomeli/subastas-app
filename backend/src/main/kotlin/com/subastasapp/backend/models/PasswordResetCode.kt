package com.subastasapp.backend.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "password_reset_codes")
data class PasswordResetCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val email: String,

    @Column(nullable = false)
    val code: String,

    @Column(nullable = false)
    val expiryDate: LocalDateTime,

    val used: Boolean = false,

    // Intentos fallidos de verificación; al llegar al máximo el código se invalida.
    val intentos: Int = 0
)
