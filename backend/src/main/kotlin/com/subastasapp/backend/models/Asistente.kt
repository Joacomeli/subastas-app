package com.subastasapp.backend.models

import jakarta.persistence.*

@Entity
@Table(name = "asistentes")
data class Asistente(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val identificador: Long? = null,

    @Column(nullable = false)
    val numeroPostor: Int,

    @Column(nullable = false)
    val cliente: Long,

    @Column(nullable = false)
    val subasta: Long
)