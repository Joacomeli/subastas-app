package com.subastasapp.backend.models

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "consignaciones")
data class Consignacion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "usuario_id", nullable = false)
    val usuarioId: Long,

    @Column(nullable = false)
    val nombre: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    val descripcion: String,

    val fotos: String? = null,

    val categoria: String? = null,

    @Column(nullable = false)
    val estado: String = "PENDIENTE",

    @Column(name = "precio_base")
    val precioBase: BigDecimal? = null,

    @Column(name = "fecha_subasta")
    val fechaSubasta: LocalDate? = null,

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    val motivoRechazo: String? = null,

    @Column(name = "devolucion_fisica")
    val devolucionFisica: Boolean? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
