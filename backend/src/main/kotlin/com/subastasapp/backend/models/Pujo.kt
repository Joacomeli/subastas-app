package com.subastasapp.backend.models

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "pujos")
data class Pujo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val identificador: Long? = null,

    @Column(nullable = false)
    val asistente: Long,

    @Column(nullable = false)
    val item: Long,

    @Column(nullable = false)
    val importe: BigDecimal,

    val ganador: String? = "no"
)