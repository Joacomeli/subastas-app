package com.subastasapp.backend.repositories

import com.subastasapp.backend.models.Pujo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal

interface PujoRepository : JpaRepository<Pujo, Long> {

    fun findByItemOrderByIdentificadorAsc(item: Long): List<Pujo>

    @Query("select max(p.importe) from Pujo p where p.item = :item")
    fun maxImporte(@Param("item") item: Long): BigDecimal?

    /** Marca como no ganadoras todas las pujas previas del ítem (se usa dentro de la transacción de puja). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Pujo p set p.ganador = 'no' where p.item = :item and p.ganador = 'si'")
    fun quitarGanador(@Param("item") item: Long): Int
}
