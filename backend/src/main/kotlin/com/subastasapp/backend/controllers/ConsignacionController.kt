package com.subastasapp.backend.controllers

import com.subastasapp.backend.dto.*
import com.subastasapp.backend.models.*
import com.subastasapp.backend.repositories.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RestController
@RequestMapping("/api")
class ConsignacionController(
    private val personaRepository: PersonaRepository,
    private val consignacionRepository: ConsignacionRepository,
    private val notificacionRepository: NotificacionRepository,
    private val productoRepository: ProductoRepository,
    private val subastaRepository: SubastaRepository,
    private val catalogoRepository: CatalogoRepository,
    private val itemCatalogoRepository: ItemCatalogoRepository,
    private val duenioRepository: DuenioRepository,
    private val empleadoRepository: EmpleadoRepository,
    private val subastadorRepository: SubastadorRepository,
    // Emails con permiso de revisar consignaciones (variable ADMIN_EMAILS, separados por coma).
    @Value("\${app.admin-emails:}") private val adminEmails: List<String>
) {

    private fun getAuthenticatedPersona(): Persona? {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return null
        return personaRepository.findByEmail(email).orElse(null)
    }

    @PostMapping("/consignaciones")
    fun crearConsignacion(@RequestBody request: CrearConsignacionRequest): ResponseEntity<*> {
        val persona = getAuthenticatedPersona()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(GenericResponse(false, "No autorizado."))

        if (request.nombre.isBlank()) {
            return ResponseEntity.badRequest().body(GenericResponse(false, "El nombre del artículo es obligatorio."))
        }
        if (request.descripcion.isBlank()) {
            return ResponseEntity.badRequest().body(GenericResponse(false, "La descripción del artículo es obligatoria."))
        }

        val consignacion = Consignacion(
            usuarioId = persona.identificador!!,
            nombre = request.nombre,
            descripcion = request.descripcion,
            fotos = request.fotos,
            categoria = request.categoria ?: "COMUN",
            estado = "PENDIENTE"
        )

        val saved = consignacionRepository.save(consignacion)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @GetMapping("/consignaciones")
    fun obtenerConsignaciones(): ResponseEntity<*> {
        val persona = getAuthenticatedPersona()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(GenericResponse(false, "No autorizado."))

        val list = consignacionRepository.findByUsuarioIdOrderByCreatedAtDesc(persona.identificador!!)
        return ResponseEntity.ok(list)
    }

    // Acción del administrador/tasador. Como no hay pantalla de admin en la app, se invoca
    // por POST (Postman/Insomnia): decide aceptado/rechazado, actualiza el estado y notifica
    // al usuario. PENDIENTE -> APROBADO_PENDIENTE_CONFIRMACION | RECHAZADO.
    @PostMapping("/consignaciones/{id}/revisar")
    fun revisarConsignacion(
        @PathVariable id: Long,
        @RequestBody request: RevisarConsignacionRequest
    ): ResponseEntity<*> {
        val revisor = getAuthenticatedPersona()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(GenericResponse(false, "No autorizado."))
        // Solo un administrador puede revisar: antes cualquier usuario autenticado podía aprobar
        // su propia consignación.
        if (revisor.email == null || revisor.email !in adminEmails) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(GenericResponse(false, "Solo un administrador puede revisar consignaciones."))
        }

        val consignacion = consignacionRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse(false, "Consignación no encontrada."))

        if (consignacion.estado != "PENDIENTE") {
            return ResponseEntity.badRequest().body(GenericResponse(false, "Solo se pueden revisar consignaciones en estado PENDIENTE."))
        }

        val decision = request.decision.lowercase()
        if (decision != "aceptado" && decision != "rechazado") {
            return ResponseEntity.badRequest().body(GenericResponse(false, "La decisión debe ser 'aceptado' o 'rechazado'."))
        }

        val updated = if (decision == "aceptado") {
            if (request.precio_base == null || request.precio_base <= BigDecimal.ZERO) {
                return ResponseEntity.badRequest().body(GenericResponse(false, "El precio base es obligatorio para aceptar y debe ser mayor a cero."))
            }
            if (request.fecha_subasta == null) {
                return ResponseEntity.badRequest().body(GenericResponse(false, "La fecha de subasta es obligatoria para aceptar."))
            }
            val minDate = LocalDate.now().plusDays(10)
            if (!request.fecha_subasta.isAfter(minDate)) {
                return ResponseEntity.badRequest().body(
                    GenericResponse(false, "La fecha de subasta debe ser al menos 10 días posterior a la fecha actual (posterior al $minDate).")
                )
            }

            consignacion.copy(
                estado = "APROBADO_PENDIENTE_CONFIRMACION",
                precioBase = request.precio_base,
                fechaSubasta = request.fecha_subasta,
                motivoRechazo = null
            )
        } else {
            if (request.motivo_rechazo.isNullOrBlank()) {
                return ResponseEntity.badRequest().body(GenericResponse(false, "El motivo de rechazo es obligatorio para rechazar."))
            }

            consignacion.copy(
                estado = "RECHAZADO",
                precioBase = null,
                fechaSubasta = null,
                motivoRechazo = request.motivo_rechazo
            )
        }

        val saved = consignacionRepository.save(updated)

        // Registrar notificación in-app para el dueño de la consignación
        val mensajeNotificacion = if (decision == "aceptado") {
            "Tu artículo '${saved.nombre}' fue aceptado por el tasador con un precio base de $${saved.precioBase} y fecha propuesta de subasta para el ${saved.fechaSubasta}. Por favor, confirma si aceptas la propuesta."
        } else {
            "Tu artículo '${saved.nombre}' fue rechazado. Motivo: ${saved.motivoRechazo}"
        }

        notificacionRepository.save(
            Notificacion(
                usuarioId = saved.usuarioId,
                mensaje = mensajeNotificacion,
                consignacionId = saved.id
            )
        )

        return ResponseEntity.ok(saved)
    }

    @PostMapping("/consignaciones/{id}/confirmar")
    fun confirmarConsignacion(
        @PathVariable id: Long,
        @RequestBody request: ConfirmarConsignacionRequest
    ): ResponseEntity<*> {
        val persona = getAuthenticatedPersona()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(GenericResponse(false, "No autorizado."))

        val consignacion = consignacionRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse(false, "Consignación no encontrada."))

        if (consignacion.usuarioId != persona.identificador) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(GenericResponse(false, "No tienes permiso sobre esta consignación."))
        }

        if (consignacion.estado != "APROBADO_PENDIENTE_CONFIRMACION") {
            return ResponseEntity.badRequest().body(GenericResponse(false, "Solo se pueden confirmar consignaciones en estado APROBADO_PENDIENTE_CONFIRMACION."))
        }

        val updated = if (request.aceptar) {
            val nextState = consignacion.copy(
                estado = "CONFIRMADO",
                devolucionFisica = null
            )
            val saved = consignacionRepository.save(nextState)

            // Publicar en el catálogo relacional
            publicarEnSubastas(saved, persona)

            notificacionRepository.save(
                Notificacion(
                    usuarioId = saved.usuarioId,
                    mensaje = "Aceptaste la propuesta para tu artículo '${saved.nombre}'. El artículo ya se encuentra visible en la sección de subastas.",
                    consignacionId = saved.id
                )
            )

            saved
        } else {
            val nextState = consignacion.copy(
                estado = "CANCELADO",
                devolucionFisica = request.devolucionFisica ?: false
            )
            val saved = consignacionRepository.save(nextState)

            val devMsg = if (saved.devolucionFisica == true) "con solicitud de devolución física" else "sin solicitud de devolución física"
            notificacionRepository.save(
                Notificacion(
                    usuarioId = saved.usuarioId,
                    mensaje = "Rechazaste la propuesta para tu artículo '${saved.nombre}' ($devMsg).",
                    consignacionId = saved.id
                )
            )

            saved
        }

        return ResponseEntity.ok(updated)
    }

    @GetMapping("/notificaciones")
    fun obtenerNotificaciones(): ResponseEntity<*> {
        val persona = getAuthenticatedPersona()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(GenericResponse(false, "No autorizado."))

        val list = notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(persona.identificador!!)
        return ResponseEntity.ok(list)
    }

    /** Marca una notificación como leída (solo el dueño puede). */
    @PostMapping("/notificaciones/{id}/leida")
    fun marcarLeida(@PathVariable id: Long): ResponseEntity<*> {
        val persona = getAuthenticatedPersona()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(GenericResponse(false, "No autorizado."))

        val notif = notificacionRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(GenericResponse(false, "Notificación no encontrada."))
        if (notif.usuarioId != persona.identificador) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(GenericResponse(false, "No autorizado."))
        }
        val saved = notificacionRepository.save(notif.copy(leido = true))
        return ResponseEntity.ok(saved)
    }

    /** Marca todas las notificaciones del usuario como leídas (acción del botón en lista). */
    @PostMapping("/notificaciones/leer-todas")
    fun marcarTodasLeidas(): ResponseEntity<*> {
        val persona = getAuthenticatedPersona()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(GenericResponse(false, "No autorizado."))

        val pendientes = notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(persona.identificador!!)
            .filter { !it.leido }
        pendientes.forEach { notificacionRepository.save(it.copy(leido = true)) }
        return ResponseEntity.ok(mapOf("actualizadas" to pendientes.size))
    }

    private fun publicarEnSubastas(consignacion: Consignacion, persona: Persona) {
        val employeeId = ensureDuenioAndEmployee(persona)

        // 1. Guardar el Producto
        val producto = Producto(
            descripcionCompleta = "${consignacion.nombre} - ${consignacion.descripcion}",
            descripcionCatalogo = consignacion.nombre,
            duenio = persona.identificador!!,
            revisor = employeeId,
            disponible = "si",
            fecha = LocalDate.now(),
            seguro = "No Posee"
        )
        val savedProducto = productoRepository.save(producto)

        // 2. Buscar o crear la Subasta para la fecha
        val fechaSubasta = consignacion.fechaSubasta!!
        val subasta = subastaRepository.findAll().find { it.fecha == fechaSubasta }
            ?: subastaRepository.save(
                Subasta(
                    fecha = fechaSubasta,
                    hora = LocalTime.of(18, 0),
                    estado = "abierta",
                    subastador = getOrCreateDummySubastador(),
                    ubicacion = "Online",
                    capacidadAsistentes = 100,
                    tieneDeposito = "no",
                    seguridadPropia = "no",
                    categoria = consignacion.categoria ?: "comun"
                )
            )

        // 3. Buscar o crear el Catálogo para la subasta
        val catalogo = catalogoRepository.findAll().find { it.subasta == subasta.identificador }
            ?: catalogoRepository.save(
                Catalogo(
                    descripcion = "Catálogo de subasta del $fechaSubasta",
                    subasta = subasta.identificador,
                    responsable = employeeId
                )
            )

        // 4. Crear el ItemCatalogo
        val itemCatalogo = ItemCatalogo(
            catalogo = catalogo.identificador!!,
            producto = savedProducto.identificador!!,
            precioBase = consignacion.precioBase!!,
            comision = consignacion.precioBase.multiply(BigDecimal("0.10")), // 10% comision
            subastado = "no"
        )
        itemCatalogoRepository.save(itemCatalogo)
    }

    private fun ensureDuenioAndEmployee(consignor: Persona): Long {
        val employeePersona = personaRepository.findByEmail("tasador@subastas.com").orElseGet {
            personaRepository.save(
                Persona(
                    documento = "11111111",
                    nombre = "Tasador Oficial",
                    direccion = "Sede Central",
                    estado = "activo",
                    email = "tasador@subastas.com",
                    password = ""
                )
            )
        }

        val employeeId = employeePersona.identificador!!
        if (!empleadoRepository.existsById(employeeId)) {
            empleadoRepository.save(
                Empleado(
                    identificador = employeeId,
                    cargo = "Tasador",
                    sector = 1
                )
            )
        }

        val consignorId = consignor.identificador!!
        if (!duenioRepository.existsById(consignorId)) {
            duenioRepository.save(
                Duenio(
                    identificador = consignorId,
                    numeroPais = 1,
                    verificacionFinanciera = "si",
                    verificacionJudicial = "si",
                    calificacionRiesgo = 1,
                    verificador = employeeId
                )
            )
        }

        return employeeId
    }

    private fun getOrCreateDummySubastador(): Long {
        val subastadorPersona = personaRepository.findByEmail("subastador@subastas.com").orElseGet {
            personaRepository.save(
                Persona(
                    documento = "22222222",
                    nombre = "Subastador Oficial",
                    direccion = "Sede Central",
                    estado = "activo",
                    email = "subastador@subastas.com",
                    password = ""
                )
            )
        }
        val subastadorId = subastadorPersona.identificador!!
        if (!subastadorRepository.existsById(subastadorId)) {
            subastadorRepository.save(
                Subastador(
                    identificador = subastadorId,
                    matricula = "MAT-999",
                    region = "Central"
                )
            )
        }
        return subastadorId
    }
}
