package com.subastasapp.data.api

import com.google.gson.Gson
import com.subastasapp.data.model.ConfirmarConsignacionRequest
import com.subastasapp.data.model.ConsignacionResponse
import com.subastasapp.data.model.CrearConsignacionRequest
import com.subastasapp.data.model.NotificacionResponse
import com.subastasapp.data.model.request.CompleteRegistrationRequest
import com.subastasapp.data.model.request.LoginRequest
import com.subastasapp.data.model.request.PaymentMethodRequest
import com.subastasapp.data.model.response.AuthResponse
import com.subastasapp.data.model.response.GenericResponse
import com.subastasapp.data.model.response.PaymentMethodResponse
import com.subastasapp.data.model.response.UserMetricsResponse
import com.subastasapp.data.model.response.UserProfileResponse
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Backend de demostración: responde en memoria a los endpoints de auth, perfil, medios de pago,
 * consignaciones y notificaciones, sin salir a la red. Se activa con BuildConfig.DEMO_MODE
 * (por defecto activo; para usar el backend real: -PdemoMode=false).
 *
 * Los datos son ficticios y se pierden al cerrar la app. El login acepta cualquier
 * email/contraseña no vacíos.
 */
object DemoBackend : Interceptor {

    private val gson = Gson()
    private val jsonType = "application/json".toMediaType()

    private var email = "demo@subastas.app"
    private var nextId = 100L

    private val payments = mutableListOf(
        PaymentMethodResponse(1, "TARJETA", "Visa demo", "4242", true)
    )
    private var nextPaymentId = 2

    private val consignaciones = mutableListOf(
        ConsignacionResponse(
            id = 1, usuarioId = 1, nombre = "Mate de plata", descripcion = "Mate de plata 800 con bombilla, años 40.",
            fotos = null, categoria = "ESPECIAL", estado = "CONFIRMADO", precioBase = 8000.0,
            fechaSubasta = daysFromNow(12), motivoRechazo = null, devolucionFisica = null, createdAt = daysAgo(6)
        ),
        ConsignacionResponse(
            id = 2, usuarioId = 1, nombre = "Reproductor de CD", descripcion = "Reproductor de CD genérico, sin caja.",
            fotos = null, categoria = "COMUN", estado = "RECHAZADO", precioBase = null,
            fechaSubasta = null, motivoRechazo = "El artículo no cumple el valor mínimo para subasta.",
            devolucionFisica = null, createdAt = daysAgo(9)
        )
    )
    /** Momento de creación de las consignaciones nuevas, para simular la revisión del tasador. */
    private val creadas = mutableMapOf<Long, Long>()

    private val notificaciones = mutableListOf(
        NotificacionResponse(1, 1, "Tu artículo \"Mate de plata\" fue confirmado para subasta.", 1, false, daysAgo(5)),
        NotificacionResponse(2, 1, "Tu artículo \"Reproductor de CD\" fue rechazado.", 2, true, daysAgo(8))
    )

    private const val REVISION_DEMO_MS = 8_000L

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath.trim('/')
        val method = req.method
        val body = bodyOf(req)

        return when {
            // ---------- Auth ----------
            method == "POST" && path == "auth/login" -> {
                val r = parse(body, LoginRequest::class.java)
                if (r == null || r.email.isBlank() || r.password.isBlank()) {
                    json(req, 401, GenericResponse(false, "Ingresá email y contraseña."))
                } else {
                    email = r.email.trim()
                    json(req, 200, AuthResponse("demo-token"))
                }
            }
            method == "POST" && path == "auth/complete-registration" -> {
                parse(body, CompleteRegistrationRequest::class.java)?.let { email = it.email.trim() }
                json(req, 200, AuthResponse("demo-token"))
            }
            method == "POST" && path in listOf("auth/register", "auth/register/step1", "auth/register/step3") ->
                json(req, 200, GenericResponse(true, "Modo demo: registro simulado."))
            method == "POST" && path in listOf(
                "auth/forgot-password", "auth/verify-reset-code", "auth/reset-password", "auth/resend-reset-code"
            ) -> json(req, 200, GenericResponse(true, "Modo demo: usá cualquier código (por ejemplo 123456)."))
            method == "POST" && path == "auth/logout" -> json(req, 200, null)

            // ---------- Perfil ----------
            method == "GET" && path == "user/profile" ->
                json(req, 200, UserProfileResponse(1, "Usuario", "Demo", email, "ORO", null))
            method == "GET" && path == "user/metrics" ->
                json(req, 200, UserMetricsResponse(totalAuctions = 4, wins = 1, totalBids = 12))

            // ---------- Medios de pago ----------
            method == "GET" && path == "user/payment-methods" -> json(req, 200, payments.toList())
            method == "POST" && path == "user/payment-methods" -> {
                val r = parse(body, PaymentMethodRequest::class.java)
                    ?: return json(req, 400, GenericResponse(false, "Datos inválidos."))
                val created = r.toResponse(nextPaymentId++)
                payments.add(created)
                json(req, 200, created)
            }
            method == "DELETE" && Regex("user/payment-methods/(\\d+)").matches(path) -> {
                val id = path.substringAfterLast('/').toInt()
                payments.removeAll { it.id == id }
                json(req, 200, null)
            }

            // ---------- Consignaciones ----------
            method == "GET" && path == "api/consignaciones" -> {
                revisarPendientes()
                json(req, 200, consignaciones.sortedByDescending { it.id })
            }
            method == "POST" && path == "api/consignaciones" -> {
                val r = parse(body, CrearConsignacionRequest::class.java)
                if (r == null || r.nombre.isBlank()) {
                    json(req, 400, GenericResponse(false, "Completá el nombre del artículo."))
                } else {
                    val c = ConsignacionResponse(
                        id = nextId++, usuarioId = 1, nombre = r.nombre, descripcion = r.descripcion,
                        fotos = r.fotos, categoria = r.categoria ?: "COMUN", estado = "PENDIENTE",
                        precioBase = null, fechaSubasta = null, motivoRechazo = null,
                        devolucionFisica = null, createdAt = now()
                    )
                    consignaciones.add(c)
                    creadas[c.id] = System.currentTimeMillis()
                    json(req, 200, c)
                }
            }
            method == "POST" && Regex("api/consignaciones/(\\d+)/confirmar").matches(path) -> {
                val id = path.split('/')[2].toLong()
                val r = parse(body, ConfirmarConsignacionRequest::class.java)
                val idx = consignaciones.indexOfFirst { it.id == id }
                if (idx < 0 || r == null) {
                    json(req, 404, GenericResponse(false, "Consignación no encontrada."))
                } else {
                    val actual = consignaciones[idx]
                    val nueva = actual.copy(
                        estado = if (r.aceptar) "CONFIRMADO" else "CANCELADO",
                        devolucionFisica = r.devolucionFisica
                    )
                    consignaciones[idx] = nueva
                    notificar(
                        if (r.aceptar) "Confirmaste la subasta de \"${actual.nombre}\"."
                        else "Cancelaste la consignación de \"${actual.nombre}\".",
                        actual.id
                    )
                    json(req, 200, nueva)
                }
            }

            // ---------- Notificaciones ----------
            method == "GET" && path == "api/notificaciones" -> {
                revisarPendientes()
                json(req, 200, notificaciones.sortedByDescending { it.id })
            }
            method == "POST" && path == "api/notificaciones/leer-todas" -> {
                val n = notificaciones.count { !it.leido }
                notificaciones.replaceAll { it.copy(leido = true) }
                json(req, 200, mapOf("actualizadas" to n))
            }
            method == "POST" && Regex("api/notificaciones/(\\d+)/leida").matches(path) -> {
                val id = path.split('/')[2].toLong()
                val idx = notificaciones.indexOfFirst { it.id == id }
                if (idx < 0) json(req, 404, GenericResponse(false, "No encontrada."))
                else {
                    notificaciones[idx] = notificaciones[idx].copy(leido = true)
                    json(req, 200, notificaciones[idx])
                }
            }

            else -> json(req, 404, GenericResponse(false, "No disponible en modo demo."))
        }
    }

    /** Simula al tasador: pasados unos segundos, aprueba las consignaciones pendientes. */
    private fun revisarPendientes() {
        val ahora = System.currentTimeMillis()
        for (i in consignaciones.indices) {
            val c = consignaciones[i]
            val t = creadas[c.id] ?: continue
            if (c.estado == "PENDIENTE" && ahora - t >= REVISION_DEMO_MS) {
                consignaciones[i] = c.copy(
                    estado = "APROBADO_PENDIENTE_CONFIRMACION",
                    precioBase = 12000.0,
                    fechaSubasta = daysFromNow(15)
                )
                notificar("Tu artículo \"${c.nombre}\" fue aprobado. Confirmá la subasta.", c.id)
            }
        }
    }

    private fun notificar(mensaje: String, consignacionId: Long) {
        notificaciones.add(NotificacionResponse(nextId++, 1, mensaje, consignacionId, false, now()))
    }

    private fun PaymentMethodRequest.toResponse(id: Int): PaymentMethodResponse {
        val tipoUi = when (type) {
            "CREDIT_CARD" -> "TARJETA"
            "CHEQUE" -> "CHEQUE"
            else -> "CUENTA"
        }
        val ultimos = numeroUltimos4 ?: numero?.takeLast(4) ?: numeroCuenta?.takeLast(4) ?: numeroCheque?.takeLast(4)
        val desc = banco ?: titular ?: when (tipoUi) {
            "TARJETA" -> "Tarjeta"
            "CHEQUE" -> "Cheque"
            else -> "Cuenta bancaria"
        }
        // En la demo los medios agregados quedan "pendientes de verificación".
        return PaymentMethodResponse(id, tipoUi, desc, ultimos, false)
    }

    private fun bodyOf(req: Request): String = try {
        val buf = okio.Buffer()
        req.body?.writeTo(buf)
        buf.readUtf8()
    } catch (e: Exception) {
        ""
    }

    private fun <T> parse(body: String, type: Class<T>): T? = try {
        gson.fromJson(body, type)
    } catch (e: Exception) {
        null
    }

    private fun json(req: Request, code: Int, payload: Any?): Response =
        Response.Builder()
            .request(req)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body((if (payload == null) "" else gson.toJson(payload)).toResponseBody(jsonType))
            .build()

    private fun fmt(d: Date, pattern: String) = SimpleDateFormat(pattern, Locale.US).format(d)
    private fun now() = fmt(Date(), "yyyy-MM-dd'T'HH:mm:ss")
    private fun shifted(days: Int): Date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }.time
    private fun daysAgo(n: Int) = fmt(shifted(-n), "yyyy-MM-dd'T'HH:mm:ss")
    private fun daysFromNow(n: Int) = fmt(shifted(n), "yyyy-MM-dd")
}
