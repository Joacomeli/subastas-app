package com.subastasapp.backend.models

import jakarta.persistence.*

@Entity
@Table(name = "sectores")
data class Sector(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val identificador: Long? = null,

    @Column(nullable = false)
    val nombreSector: String,

    val codigoSector: String? = null,

    val responsableSector: Int? = null
)