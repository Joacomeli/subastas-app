package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Sector
import org.springframework.data.jpa.repository.JpaRepository

interface SectorRepository : JpaRepository<Sector, Long>