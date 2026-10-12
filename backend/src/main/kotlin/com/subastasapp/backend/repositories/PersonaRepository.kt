package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Persona
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PersonaRepository : JpaRepository<Persona, Long> {
    fun findByEmail(email: String): Optional<Persona>
    fun existsByEmail(email: String): Boolean
}