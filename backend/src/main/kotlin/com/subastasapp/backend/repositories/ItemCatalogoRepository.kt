package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.ItemCatalogo
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ItemCatalogoRepository : JpaRepository<ItemCatalogo, Long> {

    fun findFirstByCatalogoOrderByIdentificadorAsc(catalogo: Long): ItemCatalogo?

    /**
     * Bloqueo pesimista (SELECT ... FOR UPDATE) sobre la fila del ítem. Serializa las pujas
     * concurrentes sobre el mismo ítem: la segunda espera a que la primera termine y recién
     * ahí lee la oferta vigente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemCatalogo i where i.identificador = :id")
    fun lockById(@Param("id") id: Long): ItemCatalogo?
}
