package com.subastasapp.backend.models

import jakarta.persistence.*

/**
 * Medio de pago asociado a una persona (tarjeta de crédito, cuenta bancaria o cheque).
 *
 * Compatibilidad: el script viejo `init.sql` creó la tabla `medios_pago` con la columna
 * `usuario_id`. La aplicación real referencia a Persona vía `persona_id`. Mantenemos ambas
 * columnas mapeadas para que Hibernate no rompa contra esquemas viejos; el repositorio
 * trabaja con `personaId`.
 */
@Entity
@Table(name = "medios_pago")
data class MedioPago(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "usuario_id")
    val usuarioId: Long? = null,

    @Column(name = "persona_id")
    val personaId: Long? = null,

    val tipo: String? = null,

    val detalle: String? = null,

    @Column(name = "ultimos_digitos")
    val ultimosDigitos: String? = null,

    val verificado: Boolean? = false
)
