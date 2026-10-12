package com.subastasapp.domain

import com.subastasapp.data.model.auction.Categoria
import com.subastasapp.data.model.auction.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BidRulesTest {

    private val ars = Currency.ARS

    // Ejemplo del enunciado: base 10.000, última 15.000 -> mínimo 15.100, máximo 17.000.
    @Test
    fun `minPuja aplica 1 por ciento del valor base`() {
        assertEquals(15100.0, BidRules.minPuja(10000.0, 15000.0), 0.001)
    }

    @Test
    fun `maxPuja aplica 20 por ciento del valor base`() {
        assertEquals(17000.0, BidRules.maxPuja(10000.0, 15000.0), 0.001)
    }

    @Test
    fun `puja por debajo del minimo es rechazada en categoria comun`() {
        val r = BidRules.validatePuja(15050.0, 10000.0, 15000.0, Categoria.COMUN, ars)
        assertTrue(r is BidRules.BidValidation.TooLow)
    }

    @Test
    fun `puja en el limite inferior es valida`() {
        val r = BidRules.validatePuja(15100.0, 10000.0, 15000.0, Categoria.COMUN, ars)
        assertTrue(r is BidRules.BidValidation.Ok)
    }

    @Test
    fun `puja en el limite superior es valida`() {
        val r = BidRules.validatePuja(17000.0, 10000.0, 15000.0, Categoria.COMUN, ars)
        assertTrue(r is BidRules.BidValidation.Ok)
    }

    @Test
    fun `puja por encima del maximo es rechazada en categoria comun`() {
        val r = BidRules.validatePuja(17001.0, 10000.0, 15000.0, Categoria.COMUN, ars)
        assertTrue(r is BidRules.BidValidation.TooHigh)
    }

    @Test
    fun `monto nulo o no positivo es invalido`() {
        assertTrue(BidRules.validatePuja(null, 10000.0, 15000.0, Categoria.COMUN, ars)
                is BidRules.BidValidation.Invalid)
        assertTrue(BidRules.validatePuja(0.0, 10000.0, 15000.0, Categoria.COMUN, ars)
                is BidRules.BidValidation.Invalid)
    }

    // ORO y PLATINO están exentas de los límites de %: solo debe superar la oferta actual.
    @Test
    fun `categoria oro permite superar el maximo del 20 por ciento`() {
        // 50.000 supera ampliamente el +20% (18.000) pero es válido por ser exenta.
        val r = BidRules.validatePuja(50000.0, 10000.0, 15000.0, Categoria.ORO, ars)
        assertTrue(r is BidRules.BidValidation.Ok)
    }

    @Test
    fun `categoria platino exige solo superar la oferta actual`() {
        val igual = BidRules.validatePuja(15000.0, 10000.0, 15000.0, Categoria.PLATINO, ars)
        assertTrue(igual is BidRules.BidValidation.TooLow)
        val mayor = BidRules.validatePuja(15001.0, 10000.0, 15000.0, Categoria.PLATINO, ars)
        assertTrue(mayor is BidRules.BidValidation.Ok)
    }

    @Test
    fun `puedeAcceder respeta la jerarquia de categorias`() {
        assertTrue(Categoria.ORO.puedeAcceder(Categoria.ESPECIAL))
        assertTrue(Categoria.ORO.puedeAcceder(Categoria.ORO))
        assertTrue(!Categoria.ORO.puedeAcceder(Categoria.PLATINO))
    }
}
