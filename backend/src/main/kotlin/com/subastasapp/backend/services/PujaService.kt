package com.subastasapp.backend.services

import com.subastasapp.backend.models.*
import com.subastasapp.backend.repositories.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

sealed interface ResultadoPuja {
    data class Aceptada(val pujo: Pujo, val importe: BigDecimal, val autor: String) : ResultadoPuja
    data class Rechazada(val status: HttpStatus, val mensaje: String) : ResultadoPuja
}

/**
 * Registro de pujas. Toda la operación corre en UNA transacción y con un bloqueo pesimista
 * (SELECT ... FOR UPDATE) sobre el ítem subastado. Sin eso, dos pujas simultáneas leen la
 * misma oferta vigente, ambas pasan la validación y las dos quedan como ganadoras.
 */
@Service
class PujaService(
    private val subastaRepository: SubastaRepository,
    private val catalogoRepository: CatalogoRepository,
    private val itemCatalogoRepository: ItemCatalogoRepository,
    private val pujoRepository: PujoRepository,
    private val asistenteRepository: AsistenteRepository,
    private val clienteRepository: ClienteRepository,
    private val empleadoRepository: EmpleadoRepository,
    private val personaRepository: PersonaRepository,
    private val medioPagoRepository: MedioPagoRepository
) {
    companion object {
        /** Orden de categorías: un cliente puede pujar en subastas de su categoría o inferiores. */
        val CATEGORIAS = listOf("comun", "especial", "plata", "oro", "platino")
        private val EXENTAS_DE_MAXIMO = setOf("oro", "platino")
        private val UNO_POR_CIENTO = BigDecimal("0.01")
        private val VEINTE_POR_CIENTO = BigDecimal("0.20")
    }

    @Transactional
    fun registrarPuja(personaId: Long, subastaId: Long, importeSolicitado: Double): ResultadoPuja {
        val subasta = subastaRepository.findById(subastaId).orElse(null)
            ?: return rechazo(HttpStatus.NOT_FOUND, "Subasta no encontrada.")
        if (subasta.estado?.lowercase() != "abierta") return rechazo(HttpStatus.BAD_REQUEST, "La subasta no está abierta.")

        val itemSinBloqueo = itemActual(subastaId) ?: return rechazo(HttpStatus.BAD_REQUEST, "La subasta no tiene ítems.")
        // A partir de acá las pujas sobre este ítem quedan serializadas.
        val item = itemCatalogoRepository.lockById(itemSinBloqueo.identificador!!)
            ?: return rechazo(HttpStatus.BAD_REQUEST, "La subasta no tiene ítems.")

        // --- Quién puede pujar ---
        val persona = personaRepository.findById(personaId).orElse(null) ?: return rechazo(HttpStatus.UNAUTHORIZED, "No autorizado.")
        val cliente = clienteRepository.findById(personaId).orElseGet { crearCliente(personaId) }
        if (cliente.admitido?.lowercase() != "si") return rechazo(HttpStatus.FORBIDDEN, "Tu cuenta todavía no fue admitida para pujar.")
        if (rango(cliente.categoria) < rango(subasta.categoria)) {
            return rechazo(HttpStatus.FORBIDDEN, "Tu categoría no alcanza para esta subasta (${subasta.categoria}).")
        }
        if (medioPagoRepository.findByPersonaId(personaId).none { it.verificado == true }) {
            return rechazo(HttpStatus.FORBIDDEN, "Necesitás un medio de pago verificado para pujar.")
        }

        // --- Reglas de puja (con BigDecimal: no se compara dinero con Double) ---
        val importe = BigDecimal.valueOf(importeSolicitado).setScale(2, RoundingMode.HALF_UP)
        val base = item.precioBase
        val itemId = item.identificador!!
        val oferta = pujoRepository.maxImporte(itemId) ?: base
        if (importe <= oferta) return rechazo(HttpStatus.BAD_REQUEST, "La puja debe superar la oferta actual.")
        val minimo = oferta + UNO_POR_CIENTO * base
        if (importe < minimo) {
            return rechazo(HttpStatus.BAD_REQUEST, "La puja mínima es %.2f (oferta actual + 1%% del valor base).".format(minimo))
        }
        if ((subasta.categoria ?: "").lowercase() !in EXENTAS_DE_MAXIMO) {
            val maximo = oferta + VEINTE_POR_CIENTO * base
            if (importe > maximo) {
                return rechazo(HttpStatus.BAD_REQUEST, "La puja máxima es %.2f (oferta actual + 20%% del valor base).".format(maximo))
            }
        }

        // --- Persistencia ---
        val asistente = asistenteRepository.findFirstByClienteAndSubasta(personaId, subastaId)
            ?: asistenteRepository.save(
                Asistente(numeroPostor = asistenteRepository.maxNumeroPostor() + 1, cliente = personaId, subasta = subastaId)
            )
        pujoRepository.quitarGanador(itemId)
        val pujo = pujoRepository.save(Pujo(asistente = asistente.identificador!!, item = itemId, importe = importe, ganador = "si"))
        return ResultadoPuja.Aceptada(pujo, importe, persona.nombre)
    }

    fun itemActual(subastaId: Long): ItemCatalogo? {
        val catalogo = catalogoRepository.findFirstBySubastaOrderByIdentificadorAsc(subastaId) ?: return null
        return itemCatalogoRepository.findFirstByCatalogoOrderByIdentificadorAsc(catalogo.identificador!!)
    }

    private fun rango(categoria: String?) = CATEGORIAS.indexOf((categoria ?: "comun").lowercase()).coerceAtLeast(0)

    private fun rechazo(status: HttpStatus, mensaje: String) = ResultadoPuja.Rechazada(status, mensaje)

    /** Un cliente nuevo arranca en la categoría más baja; las categorías superiores las otorga un administrador. */
    private fun crearCliente(personaId: Long): Cliente =
        clienteRepository.save(Cliente(identificador = personaId, numeroPais = 1, admitido = "si", categoria = "comun", verificador = ensureTasador()))

    private fun ensureTasador(): Long {
        val tasador = personaRepository.findByEmail("tasador@subastas.com").orElseGet {
            personaRepository.save(
                Persona(documento = "11111111", nombre = "Tasador Oficial", direccion = "Sede Central", estado = "activo", email = "tasador@subastas.com", password = "")
            )
        }
        val id = tasador.identificador!!
        if (!empleadoRepository.existsById(id)) empleadoRepository.save(Empleado(identificador = id, cargo = "Tasador", sector = 1))
        return id
    }
}
