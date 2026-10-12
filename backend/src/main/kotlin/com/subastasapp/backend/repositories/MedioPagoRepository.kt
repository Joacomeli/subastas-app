package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.MedioPago
import org.springframework.data.jpa.repository.JpaRepository

interface MedioPagoRepository : JpaRepository<MedioPago, Long> {
    fun findByPersonaId(personaId: Long): List<MedioPago>
}
