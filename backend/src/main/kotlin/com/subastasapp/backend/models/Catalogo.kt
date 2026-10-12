package com.subastasapp.backend.models

import jakarta.persistence.*

@Entity
@Table(name = "catalogos")
data class Catalogo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val identificador: Long? = null,

    @Column(nullable = false)
    val descripcion: String,

    val subasta: Long? = null,

    @Column(nullable = false)
    val responsable: Long
)