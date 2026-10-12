package com.subastasapp.backend.controllers

import com.subastasapp.backend.models.Persona
import com.subastasapp.backend.repositories.AsistenteRepository
import com.subastasapp.backend.repositories.ClienteRepository
import com.subastasapp.backend.repositories.MedioPagoRepository
import com.subastasapp.backend.repositories.PersonaRepository
import com.subastasapp.backend.repositories.PujoRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * Endpoints del usuario autenticado: perfil, métricas y medios de pago.
 *
 * Cubre los endpoints que el frontend (UserApiService) ya consumía pero que antes no
 * estaban implementados en el backend (ProfileScreen + pantalla de métricas).
 */
@RestController
@RequestMapping("/user")
class UserController(
    private val personaRepository: PersonaRepository,
    private val asistenteRepository: AsistenteRepository,
    private val pujoRepository: PujoRepository,
    private val clienteRepository: ClienteRepository,
    private val medioPagoRepository: MedioPagoRepository
) {

    private fun getAuthenticatedPersona(): Persona? {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return null
        return personaRepository.findByEmail(email).orElse(null)
    }

    @GetMapping("/profile")
    fun getProfile(): ResponseEntity<*> {
        val persona = getAuthenticatedPersona()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "No autorizado"))

        // El nombre se guarda como "Nombre Apellido" en una sola columna; lo dividimos para la UI.
        val nombres = persona.nombre.split(" ")
        val nombre = nombres.firstOrNull() ?: persona.nombre
        val apellido = if (nombres.size > 1) nombres.drop(1).joinToString(" ") else ""

        return ResponseEntity.ok(mapOf(
            "id" to persona.identificador,
            "nombre" to nombre,
            "apellido" to apellido,
            "email" to persona.email,
            "fotoUrl" to null,
            "categoria" to "COMUN"
        ))
    }

    /**
     * Métricas que pide el TPO §43: cantidad de subastas en las que participó, cuántas ganó,
     * y total de pujas realizadas.
     */
    @GetMapping("/metrics")
    fun getMetrics(): ResponseEntity<*> {
        val persona = getAuthenticatedPersona()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "No autorizado"))

        val personaId = persona.identificador!!

        val asistentes = asistenteRepository.findAll().filter { it.cliente == personaId }
        val totalSubastas = asistentes.size
        val totalPujas = pujoRepository.findAll().count { pujo ->
            asistentes.any { it.identificador == pujo.asistente }
        }
        val ganancias = pujoRepository.findAll().count { pujo ->
            pujo.ganador == "si" && asistentes.any { it.identificador == pujo.asistente }
        }

        return ResponseEntity.ok(mapOf(
            "totalAuctions" to totalSubastas,
            "wins" to ganancias,
            "totalBids" to totalPujas
        ))
    }

    @GetMapping("/payment-methods")
    fun getPaymentMethods(): ResponseEntity<*> {
        val persona = getAuthenticatedPersona()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "No autorizado"))

        val methods = medioPagoRepository.findByPersonaId(persona.identificador!!)
        return ResponseEntity.ok(methods.map { mp ->
            mapOf(
                "id" to mp.id,
                "tipo" to (mp.tipo ?: ""),
                "descripcion" to (mp.detalle ?: ""),
                "ultimosDigitos" to mp.ultimosDigitos,
                "verificado" to (mp.verificado ?: false)
            )
        })
    }
}
