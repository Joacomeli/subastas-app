package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Pais
import org.springframework.data.jpa.repository.JpaRepository

interface PaisRepository : JpaRepository<Pais, Int>