package com.girsang.server.controller.model

import com.girsang.server.dto.OrderanStikerDTO
import com.girsang.server.model.OrderanStiker
import com.girsang.server.model.OrderanStikerRinci
import com.girsang.server.service.OrderanStikerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.annotation.RequestScope

@RestController
@RequestMapping("/api/orderan-stiker")
@CrossOrigin("*")
class OrderanStikerController(private val service: OrderanStikerService) {

    @GetMapping
    fun getAll(): List<OrderanStikerDTO> =
        service.findAll().map { OrderanStikerDTO.fromEntity(it) }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): OrderanStikerDTO? =
        service.findById(id)?.let { OrderanStikerDTO.fromEntity(it) }

    @PostMapping
    fun create(@RequestBody orderan: OrderanStiker): OrderanStikerDTO =
        OrderanStikerDTO.fromEntity(service.save(orderan))


    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody orderBaru: OrderanStiker): ResponseEntity<Any> {
        return try {
            val updated = service.update(id, orderBaru)
            ResponseEntity.ok(updated)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to "Data Orderan tidak ditemukan"))
        }
    }
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) {
        service.findById(id)?.let { service.delete(it) }
    }

    // Optional: endpoint hanya untuk lihat faktur berikutnya (preview)
    @GetMapping("/nextFaktur")
    fun getNextFaktur(): ResponseEntity<String> {
        return ResponseEntity.ok(service.generateFaktur())
    }
}