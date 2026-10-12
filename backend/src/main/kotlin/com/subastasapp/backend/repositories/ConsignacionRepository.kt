package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Consignacion
import org.springframework.data.jpa.repository.JpaRepository

interface ConsignacionRepository : JpaRepository<Consignacion, Long> {
    fun findByUsuarioIdOrderByCreatedAtDesc(usuarioId: Long): List<Consignacion>
}
