package com.girsang.server.controller.model

import com.girsang.server.dto.OrderanStikerDTO
import com.girsang.server.model.OrderanStiker
import com.girsang.server.service.OrderanStikerService
import com.girsang.server.util.SimpanFileLogger
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
    fun getAll(): List<OrderanStikerDTO> {
        SimpanFileLogger.info("Memuat semua data Orderan")
        return service.findAll().map { OrderanStikerDTO.fromEntity(it) }
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): OrderanStikerDTO? =
        service.findById(id)?.let { OrderanStikerDTO.fromEntity(it) }

    @GetMapping("/cari-faktur")
    fun cariFaktur(faktur: String): List<OrderanStikerDTO> {
        SimpanFileLogger.info("Mencari Faktur Orderan")
        return service.cariFaktur(faktur).map { OrderanStikerDTO.fromEntity(it) }
    }

    @PostMapping
    fun create(@RequestBody orderan: OrderanStiker): ResponseEntity<OrderanStiker> {
        val simpan = service.save(orderan)
        SimpanFileLogger.info("Simpan data Orderan " +
                "${orderan.faktur}, ${orderan.tanggal}, ${orderan.umkm.namaUsaha},  ${orderan.totalStiker}")
        return ResponseEntity.ok(simpan)

    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody dto: OrderanStikerDTO): ResponseEntity<OrderanStiker> {
        SimpanFileLogger.info("Simpan perubahan data Orderan $dto")
        val updated = service.update(id, dto)
        return ResponseEntity.ok(updated)
    }
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) {
        SimpanFileLogger.info("Hapus data Orderan $id")
        service.findById(id)?.let { service.delete(it) }
    }

    // Optional: endpoint hanya untuk lihat faktur berikutnya (preview)
    @GetMapping("/nextFaktur")
    fun getNextFaktur(): ResponseEntity<String> {
        return ResponseEntity.ok(service.generateFaktur())
    }
}