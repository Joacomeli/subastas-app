package com.subastasapp.backend

import com.subastasapp.backend.models.*
import com.subastasapp.backend.repositories.*
import com.subastasapp.backend.services.PujaService
import com.subastasapp.backend.services.ResultadoPuja
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
class PujaServiceTest(
    @Autowired private val service: PujaService,
    @Autowired private val personas: PersonaRepository,
    @Autowired private val clientes: ClienteRepository,
    @Autowired private val mediosPago: MedioPagoRepository,
    @Autowired private val subastas: SubastaRepository,
    @Autowired private val catalogos: CatalogoRepository,
    @Autowired private val items: ItemCatalogoRepository,
    @Autowired private val pujos: PujoRepository
) {
    private var subastaId = 0L
    private var itemId = 0L
    private val postores = mutableListOf<Long>()

    @BeforeEach
    fun preparar() {
        pujos.deleteAll(); items.deleteAll(); catalogos.deleteAll(); subastas.deleteAll()
        postores.clear()
        val subasta = subastas.save(Subasta(fecha = LocalDate.now().plusDays(20), hora = LocalTime.NOON, estado = "abierta", categoria = "comun"))
        subastaId = subasta.identificador!!
        val catalogo = catalogos.save(Catalogo(descripcion = "Catálogo de prueba", subasta = subastaId, responsable = 1))
        itemId = items.save(ItemCatalogo(catalogo = catalogo.identificador!!, producto = 1, precioBase = BigDecimal("1000.00"), comision = BigDecimal("10.00"))).identificador!!
        repeat(8) { i ->
            val p = personas.save(Persona(documento = "doc$i-${System.nanoTime()}", nombre = "Postor $i", email = "postor$i-${System.nanoTime()}@test.local", password = "x"))
            val pid = p.identificador!!
            clientes.save(Cliente(identificador = pid, numeroPais = 1, admitido = "si", categoria = "comun", verificador = 1))
            mediosPago.save(MedioPago(personaId = pid, tipo = "TARJETA", verificado = true))
            postores += pid
        }
    }

    @Test
    fun `pujas simultaneas por el mismo monto - solo una es aceptada y hay un unico ganador`() {
        val hilos = Executors.newFixedThreadPool(postores.size)
        val salida = CountDownLatch(1)
        val tareas = postores.map { id -> hilos.submit<ResultadoPuja> { salida.await(); service.registrarPuja(id, subastaId, 1010.0) } }
        salida.countDown()
        val resultados = tareas.map { it.get() }
        hilos.shutdown()

        assertEquals(1, resultados.count { it is ResultadoPuja.Aceptada }, "solo una puja puede ganar por el mismo importe")
        val guardadas = pujos.findByItemOrderByIdentificadorAsc(itemId)
        assertEquals(1, guardadas.size)
        assertEquals(1, guardadas.count { it.ganador == "si" })
    }

    @Test
    fun `una puja valida deja a la anterior como no ganadora`() {
        assertTrue(service.registrarPuja(postores[0], subastaId, 1010.0) is ResultadoPuja.Aceptada)
        assertTrue(service.registrarPuja(postores[1], subastaId, 1100.0) is ResultadoPuja.Aceptada)
        val guardadas = pujos.findByItemOrderByIdentificadorAsc(itemId)
        assertEquals(listOf("no", "si"), guardadas.map { it.ganador })
    }

    @Test
    fun `se rechazan pujas fuera de los limites del 1 y el 20 por ciento`() {
        assertTrue(service.registrarPuja(postores[0], subastaId, 1005.0) is ResultadoPuja.Rechazada) // < +1%
        assertTrue(service.registrarPuja(postores[0], subastaId, 1300.0) is ResultadoPuja.Rechazada) // > +20%
        assertEquals(0, pujos.count())
    }

    @Test
    fun `un cliente comun no puede pujar en una subasta de categoria superior`() {
        subastas.save(subastas.findById(subastaId).get().copy(categoria = "oro"))
        val r = service.registrarPuja(postores[0], subastaId, 1010.0)
        assertTrue(r is ResultadoPuja.Rechazada)
    }

    @Test
    fun `sin medio de pago verificado no se puede pujar`() {
        mediosPago.deleteAll()
        assertTrue(service.registrarPuja(postores[0], subastaId, 1010.0) is ResultadoPuja.Rechazada)
    }
}
