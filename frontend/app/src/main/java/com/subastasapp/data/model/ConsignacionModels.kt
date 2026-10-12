package com.subastasapp.data.model

data class CrearConsignacionRequest(
    val nombre: String,
    val descripcion: String,
    val fotos: String? = null,
    val categoria: String? = null
)

data class ConfirmarConsignacionRequest(
    val aceptar: Boolean,
    val devolucionFisica: Boolean? = null
)

data class ConsignacionResponse(
    val id: Long,
    val usuarioId: Long,
    val nombre: String,
    val descripcion: String,
    val fotos: String?,
    val categoria: String?,
    val estado: String,
    val precioBase: Double?,
    val fechaSubasta: String?,
    val motivoRechazo: String?,
    val devolucionFisica: Boolean?,
    val createdAt: String
)

data class NotificacionResponse(
    val id: Long,
    val usuarioId: Long,
    val mensaje: String,
    val consignacionId: Long?,
    val leido: Boolean,
    val createdAt: String
)
