package com.subastasapp.backend.models

import jakarta.persistence.*

@Entity
@Table(name = "empleados")
data class Empleado(
    @Id
    val identificador: Long,

    val cargo: String? = null,

    val sector: Int? = null
)