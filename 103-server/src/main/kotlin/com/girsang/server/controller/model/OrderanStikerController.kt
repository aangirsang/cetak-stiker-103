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

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) {
        service.findById(id)?.let { service.delete(it) }
    }

    @PostMapping("/{id}/add-rinci")
    fun addRinci(
        @PathVariable id: Long,
        @RequestBody rincian: OrderanStikerRinci
    ): OrderanStikerDTO? {
        val orderan = service.findById(id) ?: return null
        service.addRinci(orderan, rincian)
        return OrderanStikerDTO.fromEntity(orderan)
    }

    @PostMapping("/{id}/remove-rinci")
    fun removeRinci(
        @PathVariable id: Long,
        @RequestBody rincian: OrderanStikerRinci
    ): OrderanStikerDTO? {
        val orderan = service.findById(id) ?: return null
        service.removeRinci(orderan, rincian)
        return OrderanStikerDTO.fromEntity(orderan)
    }

    @PutMapping("/{orderId}/update-rinci/{rinciId}")
    fun updateRinci(
        @PathVariable orderId: Long,
        @PathVariable rinciId: Long,
        @RequestBody rincianBaru: OrderanStikerRinci
    ): OrderanStikerDTO? {
        val orderan = service.findById(orderId) ?: return null
        service.updateRinci(orderan, rinciId, rincianBaru)
        return OrderanStikerDTO.fromEntity(orderan)
    }

    // Optional: endpoint hanya untuk lihat faktur berikutnya (preview)
    @GetMapping("/nextFaktur")
    fun getNextFaktur(): ResponseEntity<String> {
        return ResponseEntity.ok(service.generateFaktur())
    }
}