package com.subastasapp.backend.models

import jakarta.persistence.*

@Entity
@Table(name = "personas")
data class Persona(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val identificador: Long? = null,

    @Column(nullable = false)
    val documento: String,

    @Column(nullable = false)
    val nombre: String,

    val direccion: String? = null,

    val estado: String? = null,

    val foto: ByteArray? = null,

    @Column(unique = true)
    val email: String? = null,

    val password: String? = null
)