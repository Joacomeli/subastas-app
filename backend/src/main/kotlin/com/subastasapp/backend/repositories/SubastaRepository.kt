package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Subasta
import org.springframework.data.jpa.repository.JpaRepository

interface SubastaRepository : JpaRepository<Subasta, Long> {
    fun findByEstadoIgnoreCase(estado: String): List<Subasta>
}
