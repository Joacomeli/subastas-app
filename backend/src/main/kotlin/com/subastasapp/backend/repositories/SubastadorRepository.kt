package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Subastador
import org.springframework.data.jpa.repository.JpaRepository

interface SubastadorRepository : JpaRepository<Subastador, Long>