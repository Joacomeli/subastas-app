package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Catalogo
import org.springframework.data.jpa.repository.JpaRepository

interface CatalogoRepository : JpaRepository<Catalogo, Long> {
    fun findFirstBySubastaOrderByIdentificadorAsc(subasta: Long): Catalogo?
}
