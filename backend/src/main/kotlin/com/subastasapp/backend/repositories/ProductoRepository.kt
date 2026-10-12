package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Producto
import org.springframework.data.jpa.repository.JpaRepository

interface ProductoRepository : JpaRepository<Producto, Long>