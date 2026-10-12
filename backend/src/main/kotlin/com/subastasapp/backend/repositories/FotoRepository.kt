package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Foto
import org.springframework.data.jpa.repository.JpaRepository

interface FotoRepository : JpaRepository<Foto, Long>