package com.subastasapp.backend.models

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "seguros")
data class Seguro(
    @Id
    val nroPoliza: String,

    @Column(nullable = false)
    val compania: String,

    val polizaCombinada: String? = null,

    @Column(nullable = false)
    val importe: BigDecimal
)