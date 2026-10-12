package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Duenio
import org.springframework.data.jpa.repository.JpaRepository

interface DuenioRepository : JpaRepository<Duenio, Long>