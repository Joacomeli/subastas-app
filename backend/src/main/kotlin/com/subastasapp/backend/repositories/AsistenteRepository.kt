package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Asistente
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AsistenteRepository : JpaRepository<Asistente, Long> {
    fun findFirstByClienteAndSubasta(cliente: Long, subasta: Long): Asistente?

    @Query("select coalesce(max(a.numeroPostor), 0) from Asistente a")
    fun maxNumeroPostor(): Int
}
