package com.subastasapp.backend.models

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "itemscatalogo")
data class ItemCatalogo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val identificador: Long? = null,

    @Column(nullable = false)
    val catalogo: Long,

    @Column(nullable = false)
    val producto: Long,

    @Column(nullable = false)
    val precioBase: BigDecimal,

    @Column(nullable = false)
    val comision: BigDecimal,

    val subastado: String? = null
)