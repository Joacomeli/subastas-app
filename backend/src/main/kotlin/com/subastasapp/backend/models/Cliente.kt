package com.subastasapp.backend.models

import jakarta.persistence.*

@Entity
@Table(name = "clientes")
data class Cliente(
    @Id
    val identificador: Long,

    val numeroPais: Int? = null,

    val admitido: String? = null,

    val categoria: String? = null,

    @Column(nullable = false)
    val verificador: Long
)