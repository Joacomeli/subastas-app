package com.subastasapp.backend.controllers

import com.subastasapp.backend.models.*
import com.subastasapp.backend.repositories.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/subastas")
class SubastaController(
    private val subastaRepository: SubastaRepository,
    private val catalogoRepository: CatalogoRepository,
    private val itemCatalogoRepository: ItemCatalogoRepository,
    private val productoRepository: ProductoRepository
) {

    @GetMapping("/activas")
    fun obtenerSubastasActivas(): ResponseEntity<*> {
        val activas = subastaRepository.findAll().filter { 
            it.estado?.lowercase() == "abierta"
        }
        return ResponseEntity.ok(activas)
    }

    @GetMapping("/{id}/items")
    fun obtenerItemsSubasta(@PathVariable id: Long): ResponseEntity<*> {
        if (!subastaRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Subasta no encontrada"))
        }

        val catalogo = catalogoRepository.findAll().find { it.subasta == id }
            ?: return ResponseEntity.ok(emptyList<Map<String, Any>>())

        val items = itemCatalogoRepository.findAll().filter { it.catalogo == catalogo.identificador }
        
        val response = items.map { item ->
            val producto = productoRepository.findById(item.producto).orElse(null)
            mapOf(
                "itemId" to item.identificador,
                "productoId" to item.producto,
                "nombre" to (producto?.descripcionCatalogo ?: "Artículo"),
                "descripcion" to (producto?.descripcionCompleta ?: ""),
                "precioBase" to item.precioBase,
                "comision" to item.comision,
                "subastado" to (item.subastado ?: "no")
            )
        }

        return ResponseEntity.ok(response)
    }
}
