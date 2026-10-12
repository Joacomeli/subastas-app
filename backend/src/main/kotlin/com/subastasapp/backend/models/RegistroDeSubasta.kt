package com.subastasapp.backend.models

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "registrodesubasta")
data class RegistroDeSubasta(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val identificador: Long? = null,

    @Column(nullable = false)
    val subasta: Long,

    @Column(nullable = false)
    val duenio: Long,

    @Column(nullable = false)
    val producto: Long,

    @Column(nullable = false)
    val cliente: Long,

    @Column(nullable = false)
    val importe: BigDecimal,

    @Column(nullable = false)
    val comision: BigDecimal
)