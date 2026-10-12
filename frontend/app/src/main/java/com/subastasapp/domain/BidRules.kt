package com.subastasapp.domain

import com.subastasapp.data.model.auction.Categoria
import com.subastasapp.data.model.auction.Currency
import com.subastasapp.data.model.auction.formatMoney

/**
 * Reglas de validación de pujas según el TPO. Funciones puras (testeables sin Android).
 *
 * - Puja mínima = mejor oferta actual + 1% del valor base.
 * - Puja máxima = última oferta + 20% del valor base.
 *   (en una subasta dinámica ascendente "mejor oferta" y "última oferta" coinciden:
 *    es el mayor valor ofertado hasta el momento)
 * - Las subastas ORO y PLATINO están exentas de estos límites: solo debe ser
 *   estrictamente mayor a la oferta actual.
 *
 * Ejemplo del enunciado: base 10.000, última 15.000 -> rango válido 15.100 … 17.000.
 */
object BidRules {

    private const val MIN_PCT = 0.01
    private const val MAX_PCT = 0.20

    fun minPuja(valorBase: Double, ofertaActual: Double): Double =
        ofertaActual + MIN_PCT * valorBase

    fun maxPuja(valorBase: Double, ofertaActual: Double): Double =
        ofertaActual + MAX_PCT * valorBase

    sealed class BidValidation {
        object Ok : BidValidation()
        data class TooLow(val minimo: Double, val mensaje: String) : BidValidation()
        data class TooHigh(val maximo: Double, val mensaje: String) : BidValidation()
        data class Invalid(val mensaje: String) : BidValidation()
    }

    /**
     * Valida un [importe] de puja para un ítem con [valorBase], cuya mayor oferta hasta el
     * momento es [ofertaActual], en una subasta de [categoria] y [currency].
     */
    fun validatePuja(
        importe: Double?,
        valorBase: Double,
        ofertaActual: Double,
        categoria: Categoria,
        currency: Currency
    ): BidValidation {
        if (importe == null || importe <= 0.0) {
            return BidValidation.Invalid("Ingresá un monto válido.")
        }

        if (categoria.exentaDeLimites) {
            return if (importe > ofertaActual) {
                BidValidation.Ok
            } else {
                BidValidation.TooLow(
                    minimo = ofertaActual,
                    mensaje = "La puja debe ser mayor a ${formatMoney(ofertaActual, currency)}."
                )
            }
        }

        val min = minPuja(valorBase, ofertaActual)
        val max = maxPuja(valorBase, ofertaActual)

        return when {
            importe < min -> BidValidation.TooLow(
                minimo = min,
                mensaje = "La puja mínima es ${formatMoney(min, currency)}."
            )
            importe > max -> BidValidation.TooHigh(
                maximo = max,
                mensaje = "La puja máxima es ${formatMoney(max, currency)}."
            )
            else -> BidValidation.Ok
        }
    }
}
