package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.RegistroDeSubasta
import org.springframework.data.jpa.repository.JpaRepository

interface RegistroDeSubastaRepository : JpaRepository<RegistroDeSubasta, Long>