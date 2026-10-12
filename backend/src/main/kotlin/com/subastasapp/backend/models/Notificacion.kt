package com.subastasapp.backend.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notificaciones")
data class Notificacion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "usuario_id", nullable = false)
    val usuarioId: Long,

    @Column(nullable = false, columnDefinition = "TEXT")
    val mensaje: String,

    @Column(name = "consignacion_id")
    val consignacionId: Long? = null,

    val leido: Boolean = false,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
