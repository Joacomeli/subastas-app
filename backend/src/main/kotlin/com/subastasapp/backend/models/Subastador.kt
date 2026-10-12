package com.subastasapp.backend.models

import jakarta.persistence.*

@Entity
@Table(name = "subastadores")
data class Subastador(
    @Id
    val identificador: Long,

    val matricula: String? = null,

    val region: String? = null
)