package com.subastasapp.backend.models

import jakarta.persistence.*

@Entity
@Table(name = "duenios")
data class Duenio(
    @Id
    val identificador: Long,

    val numeroPais: Int? = null,

    val verificacionFinanciera: String? = null,

    val verificacionJudicial: String? = null,

    val calificacionRiesgo: Int? = null,

    @Column(nullable = false)
    val verificador: Long
)