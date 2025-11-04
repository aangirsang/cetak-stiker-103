package com.girsang.server.controller.model

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.girsang.server.dto.DataStikerDTO
import com.girsang.server.model.DataStiker
import com.girsang.server.service.DataStikerService
import com.girsang.server.util.SimpanFileLogger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.text.SimpleDateFormat
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/api/dataStiker")
@CrossOrigin(origins = ["*"])
class DataStikerController(private val service: DataStikerService) {



    @GetMapping
    fun semua(): ResponseEntity<List<DataStiker>> {
        SimpanFileLogger.info("Memuat semua data Pengguna")
        return ResponseEntity.ok(service.semuaStiker())
    }

    @GetMapping("/{id}")
    fun byId(@PathVariable id: Long): ResponseEntity<DataStiker> =
        service.cariById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())

    @GetMapping("/umkm/{idUmkm}")
    fun stikerPerUmkm(@PathVariable idUmkm: Long): ResponseEntity<List<DataStiker>> =
        ResponseEntity.ok(service.stikerPerUmkm(idUmkm))

    @PostMapping
    fun simpan(@RequestBody stiker: DataStikerDTO): ResponseEntity<Any> =
        try {
            val mapper = jacksonObjectMapper()
            mapper.registerModule(JavaTimeModule())
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            mapper.dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            SimpanFileLogger.info("Simpan data Stiker ${mapper.writeValueAsString(stiker)}")
            val saved = service.simpan(stiker)
            ResponseEntity.status(201).body(saved)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to e.message))
        }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody stiker: DataStikerDTO): ResponseEntity<Any> =
        try {
            val mapper = jacksonObjectMapper()
            mapper.registerModule(JavaTimeModule())
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            mapper.dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            SimpanFileLogger.info("Simpan perubahab data Stiker ${mapper.writeValueAsString(stiker)}")
            val updated = service.update(id, stiker)
            ResponseEntity.ok(updated)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to e.message))
        }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        return try {
            SimpanFileLogger.info("Hapus data Stiker ID: $id")
            service.hapus(id)
            ResponseEntity.ok(mapOf("message" to "Data stiker berhasil dihapus"))

        } catch (ex: Exception) {
            // Cek error Foreign Key
            val isFK = ex.cause?.cause is org.hibernate.exception.ConstraintViolationException
                    || ex.message?.contains("Referential integrity") == true

            val msg = if (isFK) {
                "Data tidak bisa dihapus karena masih digunakan pada Data Lain"
            } else {
                ex.message ?: "Gagal menghapus data"
            }
            SimpanFileLogger.error(msg)
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("Error " to msg))
        }
    }

    @GetMapping("/cari")
    fun search(
        @RequestParam(required = false) namaStiker: String?,
        @RequestParam(required = false) namaUsaha: String?
    ): List<DataStiker> {
        return service.cariStiker(namaStiker, namaUsaha)
    }


}