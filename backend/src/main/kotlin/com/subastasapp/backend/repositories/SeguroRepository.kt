package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Seguro
import org.springframework.data.jpa.repository.JpaRepository

interface SeguroRepository : JpaRepository<Seguro, String>