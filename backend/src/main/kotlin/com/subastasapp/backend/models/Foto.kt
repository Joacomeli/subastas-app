package com.subastasapp.backend.models

import jakarta.persistence.*

@Entity
@Table(name = "fotos")
data class Foto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val identificador: Long? = null,

    @Column(nullable = false)
    val producto: Long,

    @Column(nullable = false)
    val foto: ByteArray
)