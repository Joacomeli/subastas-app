package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Empleado
import org.springframework.data.jpa.repository.JpaRepository

interface EmpleadoRepository : JpaRepository<Empleado, Long>