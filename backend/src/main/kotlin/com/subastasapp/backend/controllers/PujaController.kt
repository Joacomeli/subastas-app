package com.subastasapp.backend.controllers

import com.subastasapp.backend.dto.*
import com.subastasapp.backend.models.*
import com.subastasapp.backend.repositories.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import com.subastasapp.backend.services.PujaService
import com.subastasapp.backend.services.ResultadoPuja

/**
 * Endpoints de subastas que consume la app: listado, detalle, historial y puja.
 * La lógica de puja (reglas del TPO y concurrencia) vive en [PujaService]; la respuesta HTTP
 * ES la confirmación (la app bloquea otra puja hasta recibirla).
 */
@RestController
@RequestMapping("/api/subastas")
class PujaController(
    private val subastaRepository: SubastaRepository,
    private val productoRepository: ProductoRepository,
    private val pujoRepository: PujoRepository,
    private val asistenteRepository: AsistenteRepository,
    private val personaRepository: PersonaRepository,
    private val pujaService: PujaService
) {

    // ---------- Endpoints ----------

    /** Listado de subastas abiertas con su ítem actual y oferta vigente. */
    @GetMapping
    fun listado(): ResponseEntity<*> {
        val abiertas = subastaRepository.findByEstadoIgnoreCase("abierta")
        val resp = abiertas.mapNotNull { subasta ->
            val item = itemActual(subasta.identificador!!) ?: return@mapNotNull null
            val producto = productoRepository.findById(item.producto).orElse(null)
            SubastaResumenResponse(
                id = subasta.identificador,
                titulo = tituloSubasta(subasta),
                categoria = subasta.categoria ?: "comun",
                estado = subasta.estado ?: "abierta",
                moneda = subasta.moneda ?: "ARS",
                fechaHoraInicio = "${subasta.fecha ?: ""} ${subasta.hora}",
                rematador = rematadorNombre(subasta),
                itemId = item.identificador,
                itemTitulo = producto?.descripcionCatalogo ?: "Artículo",
                itemNumeroPieza = "#${item.identificador}",
                itemImagen = null,
                precioBase = item.precioBase.toDouble(),
                ofertaActual = ofertaActual(item)
            )
        }
        return ResponseEntity.ok(resp)
    }

    /** Detalle de una subasta + historial de pujas. */
    @GetMapping("/{id}/detalle")
    fun detalle(@PathVariable id: Long): ResponseEntity<*> {
        val subasta = subastaRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Subasta no encontrada"))
        val item = itemActual(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "La subasta no tiene ítems"))
        val producto = productoRepository.findById(item.producto).orElse(null)

        val detalle = SubastaDetalleResponse(
            id = subasta.identificador!!,
            titulo = tituloSubasta(subasta),
            categoria = subasta.categoria ?: "comun",
            estado = subasta.estado ?: "abierta",
            moneda = subasta.moneda ?: "ARS",
            fechaHoraInicio = "${subasta.fecha ?: ""} ${subasta.hora}",
            rematador = rematadorNombre(subasta),
            itemId = item.identificador,
            itemTitulo = producto?.descripcionCatalogo ?: "Artículo",
            itemNumeroPieza = "#${item.identificador}",
            itemDescripcion = producto?.descripcionCompleta ?: "",
            duenio = duenioNombre(producto),
            imagenes = emptyList(),
            precioBase = item.precioBase.toDouble(),
            ofertaActual = ofertaActual(item),
            historial = historial(item)
        )
        return ResponseEntity.ok(detalle)
    }

    /** Historial de pujas de la subasta (para el polling de la app). */
    @GetMapping("/{id}/pujas")
    fun pujas(@PathVariable id: Long): ResponseEntity<*> {
        val item = itemActual(id)
            ?: return ResponseEntity.ok(emptyList<BidResponse>())
        return ResponseEntity.ok(historial(item))
    }

    /** Registrar una puja (requiere usuario autenticado). */
    @PostMapping("/{id}/pujar")
    fun pujar(@PathVariable id: Long, @RequestBody request: PujarRequest): ResponseEntity<*> {
        val persona = getAuthenticatedPersona()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(PujarResponse(false, "No autorizado."))

        return when (val r = pujaService.registrarPuja(persona.identificador!!, id, request.importe)) {
            is ResultadoPuja.Rechazada -> ResponseEntity.status(r.status).body(PujarResponse(false, r.mensaje))
            is ResultadoPuja.Aceptada -> ResponseEntity.ok(
                PujarResponse(
                    ok = true,
                    nuevaOfertaActual = r.importe.toDouble(),
                    bid = BidResponse(r.pujo.identificador!!, r.autor, r.importe.toDouble(), true)
                )
            )
        }
    }

    // ---------- Helpers ----------

    private fun getAuthenticatedPersona(): Persona? {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return null
        return personaRepository.findByEmail(email).orElse(null)
    }

    private fun itemActual(subastaId: Long): ItemCatalogo? = pujaService.itemActual(subastaId)

    private fun ofertaActual(item: ItemCatalogo): Double =
        (pujoRepository.maxImporte(item.identificador!!) ?: item.precioBase).toDouble()

    private fun historial(item: ItemCatalogo): List<BidResponse> =
        pujoRepository.findByItemOrderByIdentificadorAsc(item.identificador!!).map { pujo ->
            BidResponse(id = pujo.identificador!!, autor = autorDePuja(pujo), importe = pujo.importe.toDouble(), esGanadora = pujo.ganador == "si")
        }

    private fun autorDePuja(pujo: Pujo): String {
        val asistente = asistenteRepository.findById(pujo.asistente).orElse(null) ?: return "Postor"
        // Cliente.identificador == Persona.identificador
        val persona = personaRepository.findById(asistente.cliente).orElse(null)
        return persona?.nombre ?: "Postor #${asistente.numeroPostor}"
    }

    private fun rematadorNombre(subasta: Subasta): String {
        val id = subasta.subastador ?: return "Subastador"
        return personaRepository.findById(id).orElse(null)?.nombre ?: "Subastador"
    }

    private fun duenioNombre(producto: Producto?): String {
        val id = producto?.duenio ?: return "—"
        return personaRepository.findById(id).orElse(null)?.nombre ?: "—"
    }

    private fun tituloSubasta(subasta: Subasta): String =
        "Subasta ${(subasta.categoria ?: "").replaceFirstChar { it.uppercase() }} · ${subasta.fecha ?: ""}".trim()
}
