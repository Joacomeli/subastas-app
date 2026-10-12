package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Notificacion
import org.springframework.data.jpa.repository.JpaRepository

interface NotificacionRepository : JpaRepository<Notificacion, Long> {
    fun findByUsuarioIdOrderByCreatedAtDesc(usuarioId: Long): List<Notificacion>
}
