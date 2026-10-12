package com.subastasapp.backend.models

import jakarta.persistence.*

@Entity
@Table(name = "paises")
data class Pais(
    @Id
    val numero: Int,

    @Column(nullable = false)
    val nombre: String,

    val nombreCorto: String? = null,

    @Column(nullable = false)
    val capital: String,

    @Column(nullable = false)
    val nacionalidad: String,

    @Column(nullable = false)
    val idiomas: String
)