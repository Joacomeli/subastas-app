package com.subastasapp.backend.models

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "subastas")
data class Subasta(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val identificador: Long? = null,

    val fecha: LocalDate? = null,

    @Column(nullable = false)
    val hora: LocalTime,

    val estado: String? = null,

    val subastador: Long? = null,

    val ubicacion: String? = null,

    val capacidadAsistentes: Int? = null,

    val tieneDeposito: String? = null,

    val seguridadPropia: String? = null,

    val categoria: String? = null,

    // Moneda de la subasta (ARS/USD). El TPO: cada subasta es en pesos o dólares, no bimonetaria.
    val moneda: String? = "ARS"
)