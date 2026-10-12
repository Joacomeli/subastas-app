package com.subastasapp.backend.models

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "productos")
data class Producto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val identificador: Long? = null,

    val fecha: LocalDate? = null,

    val disponible: String? = null,

    val descripcionCatalogo: String? = "No Posee",

    @Column(nullable = false)
    val descripcionCompleta: String,

    @Column(nullable = false)
    val revisor: Long,

    @Column(nullable = false)
    val duenio: Long,

    val seguro: String? = null
)