package com.subastasapp.backend.controllers

import com.subastasapp.backend.dto.*
import com.subastasapp.backend.models.MedioPago
import com.subastasapp.backend.models.PasswordResetCode
import com.subastasapp.backend.models.Persona
import com.subastasapp.backend.repositories.MedioPagoRepository
import com.subastasapp.backend.repositories.PasswordResetCodeRepository
import com.subastasapp.backend.repositories.PersonaRepository
import com.subastasapp.backend.security.JwtUtils
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.beans.factory.annotation.Value
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime

@RestController
@RequestMapping("/auth")
class AuthController(
    private val personaRepository: PersonaRepository,
    private val passwordResetCodeRepository: PasswordResetCodeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtils: JwtUtils,
    private val medioPagoRepository: MedioPagoRepository,
    @Value("\${app.log-reset-codes:false}") private val logResetCodes: Boolean
) {

    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<*> {
        if (loginRequest.email.isBlank()) {
            return ResponseEntity.badRequest().body(GenericResponse(false, "Usuario incompleto."))
        }
        if (loginRequest.password.isBlank()) {
            return ResponseEntity.badRequest().body(GenericResponse(false, "Contraseña incompleta."))
        }

        val persona = personaRepository.findByEmail(loginRequest.email).orElse(null)
            ?: return ResponseEntity.status(401).body(GenericResponse(false, "Usuario o contraseña incorrectos."))

        if (!passwordEncoder.matches(loginRequest.password, persona.password)) {
            return ResponseEntity.status(401).body(GenericResponse(false, "Usuario o contraseña incorrectos."))
        }

        val token = jwtUtils.generateJwtToken(persona.email!!)
        return ResponseEntity.ok(JwtResponse(
            token = token,
            email = persona.email,
            nombre = persona.nombre,
            categoria = "COMUN"
        ))
    }

    @PostMapping("/register/step1")
    fun registerStep1(@RequestBody request: RegisterStep1Request): ResponseEntity<*> {
        if (request.nombre.isBlank()) {
            return ResponseEntity.badRequest().body(GenericResponse(false, "El nombre es obligatorio."))
        }
        if (request.apellido.isBlank()) {
            return ResponseEntity.badRequest().body(GenericResponse(false, "El apellido es obligatorio."))
        }
        if (request.domicilioLegal.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(GenericResponse(false, "El domicilio legal es obligatorio."))
        }
        if (request.paisOrigen.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(GenericResponse(false, "El país de origen es obligatorio."))
        }

        return ResponseEntity.ok(GenericResponse(true, "Paso 1 completado exitosamente."))
    }

    @PostMapping("/register")
    fun register(@RequestBody registerRequest: RegisterRequest): ResponseEntity<*> {
        if (registerRequest.email.isBlank()) {
            return ResponseEntity.badRequest().body(GenericResponse(false, "El email es obligatorio."))
        }
        if (registerRequest.password.isBlank()) {
            return ResponseEntity.badRequest().body(GenericResponse(false, "La contraseña es obligatoria."))
        }
        if (personaRepository.existsByEmail(registerRequest.email)) {
            return ResponseEntity.badRequest().body(GenericResponse(false, "El email ya está registrado."))
        }

        val persona = Persona(
            documento = registerRequest.documento ?: "sin-documento",
            nombre = "${registerRequest.nombre} ${registerRequest.apellido}",
            direccion = registerRequest.domicilioLegal,
            estado = "activo",
            email = registerRequest.email,
            password = passwordEncoder.encode(registerRequest.password)
        )

        personaRepository.save(persona)
        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "Usuario registrado exitosamente.",
            "email" to persona.email
        ))
    }

    /**
     * Cierre del registro en 3 pasos: persiste los medios de pago asociados al usuario.
     *
     * Body esperado:
     *   { "email": "...", "paymentMethods": [ { type, banco, cbu, numeroUltimos4, monto, ... }, ... ] }
     *
     * Tipos soportados (alineados con [PaymentMethodRequest] del frontend):
     *   - BANK_ACCOUNT (banco + cbu)
     *   - CREDIT_CARD  (banco + numeroUltimos4)
     *   - cheque       (banco + monto)
     *
     * El TPO exige al menos un medio de pago para terminar el registro.
     */
    @PostMapping("/register/step3")
    fun registerStep3(@RequestBody request: Map<String, Any>): ResponseEntity<*> {
        val email = request["email"] as? String
            ?: return ResponseEntity.badRequest().body(GenericResponse(false, "Email no proporcionado."))

        val persona = personaRepository.findByEmail(email).orElse(null)
            ?: return ResponseEntity.status(404).body(GenericResponse(false, "Usuario no encontrado."))

        @Suppress("UNCHECKED_CAST")
        val paymentMethodsData = request["paymentMethods"] as? List<Map<String, Any>> ?: emptyList()

        if (paymentMethodsData.isEmpty()) {
            return ResponseEntity.badRequest().body(
                GenericResponse(false, "Debés agregar al menos un método de pago.")
            )
        }

        paymentMethodsData.forEach { data ->
            val type = data["type"] as? String ?: return@forEach
            val detalle = when (type) {
                "BANK_ACCOUNT" -> "${data["banco"]} - ${data["cbu"]}"
                "CREDIT_CARD"  -> "Visa **** ${data["numeroUltimos4"]}"
                else           -> "Cheque ${data["banco"]} $${data["monto"]}"
            }
            medioPagoRepository.save(
                MedioPago(
                    personaId = persona.identificador,
                    tipo = type,
                    detalle = detalle,
                    ultimosDigitos = data["numeroUltimos4"] as? String,
                    verificado = true
                )
            )
        }

        return ResponseEntity.ok(GenericResponse(true, "Registro finalizado exitosamente."))
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@RequestBody request: ForgotPasswordRequest): ResponseEntity<*> {
        val respuesta = ResponseEntity.ok(mapOf("message" to "Si el email existe, se enviará un código."))
        personaRepository.findByEmail(request.email).orElse(null) ?: return respuesta

        // Anti-abuso: no se emite un código nuevo si ya se emitió uno hace menos de 1 minuto.
        val vigente = passwordResetCodeRepository.findTopByEmailAndUsedFalseOrderByExpiryDateDesc(request.email).orElse(null)
        if (vigente != null && vigente.expiryDate.isAfter(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES - 1))) return respuesta

        // Un solo código activo por email.
        passwordResetCodeRepository.findAllByEmailAndUsedFalse(request.email)
            .forEach { passwordResetCodeRepository.save(it.copy(used = true)) }

        val code = (100000 + secureRandom.nextInt(900000)).toString()
        passwordResetCodeRepository.save(
            PasswordResetCode(email = request.email, code = code, expiryDate = LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES))
        )
        // Sin servicio de mail en este proyecto: el código solo se loguea si se activa a propósito (desarrollo local).
        if (logResetCodes) println("[DEV] Código de recuperación para ${request.email}: $code")

        return respuesta
    }

    @PostMapping("/verify-reset-code")
    fun verifyCode(@RequestBody request: VerifyCodeRequest): ResponseEntity<*> {
        val error = validarCodigo(request.email, request.code)
        return if (error != null) ResponseEntity.badRequest().body(error)
        else ResponseEntity.ok(mapOf("message" to "Código verificado correctamente"))
    }

    @PostMapping("/reset-password")
    fun resetPassword(@RequestBody request: ResetPasswordRequest): ResponseEntity<*> {
        if (request.newPassword.length < MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest().body("La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres")
        }
        val error = validarCodigo(request.email, request.code)
        if (error != null) return ResponseEntity.badRequest().body(error)

        val persona = personaRepository.findByEmail(request.email).orElse(null)
            ?: return ResponseEntity.badRequest().body("Código inválido o expirado")

        personaRepository.save(persona.copy(password = passwordEncoder.encode(request.newPassword)))
        passwordResetCodeRepository.findAllByEmailAndUsedFalse(request.email)
            .forEach { passwordResetCodeRepository.save(it.copy(used = true)) }

        return ResponseEntity.ok(mapOf("message" to "Contraseña actualizada exitosamente"))
    }

    /**
     * Valida el código contra el último emitido para el email. Cada fallo suma un intento; al llegar
     * a [MAX_CODE_ATTEMPTS] el código se invalida, así que no se puede recorrer el espacio de 10^6 valores.
     * Devuelve null si es válido, o el mensaje de error (genérico, sin revelar si el email existe).
     */
    private fun validarCodigo(email: String, code: String): String? {
        val generico = "Código inválido o expirado"
        val guardado = passwordResetCodeRepository.findTopByEmailAndUsedFalseOrderByExpiryDateDesc(email).orElse(null) ?: return generico
        if (guardado.expiryDate.isBefore(LocalDateTime.now())) return generico
        if (guardado.intentos >= MAX_CODE_ATTEMPTS) {
            passwordResetCodeRepository.save(guardado.copy(used = true))
            return "Demasiados intentos. Pedí un código nuevo."
        }
        val coincide = MessageDigest.isEqual(guardado.code.toByteArray(), code.toByteArray())
        if (!coincide) {
            passwordResetCodeRepository.save(guardado.copy(intentos = guardado.intentos + 1))
            return generico
        }
        return null
    }

    companion object {
        private const val CODE_TTL_MINUTES = 15L
        private const val MAX_CODE_ATTEMPTS = 5
        private const val MIN_PASSWORD_LENGTH = 8
        private val secureRandom = SecureRandom()
    }
}
