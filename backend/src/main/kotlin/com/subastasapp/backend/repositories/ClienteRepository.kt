package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Cliente
import org.springframework.data.jpa.repository.JpaRepository

interface ClienteRepository : JpaRepository<Cliente, Long>