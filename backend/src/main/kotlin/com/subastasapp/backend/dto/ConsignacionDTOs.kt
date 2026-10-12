package com.subastasapp.backend.dto

import java.math.BigDecimal
import java.time.LocalDate

data class CrearConsignacionRequest(
    val nombre: String,
    val descripcion: String,
    val fotos: String? = null,
    val categoria: String? = null
)

data class RevisarConsignacionRequest(
    val decision: String,
    val precio_base: BigDecimal? = null,
    val fecha_subasta: LocalDate? = null,
    val motivo_rechazo: String? = null
)

data class ConfirmarConsignacionRequest(
    val aceptar: Boolean,
    val devolucionFisica: Boolean? = null
)
