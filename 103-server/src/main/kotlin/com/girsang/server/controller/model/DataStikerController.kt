package com.girsang.server.controller.model

import com.girsang.server.dto.DataStikerDTO
import com.girsang.server.model.DataStiker
import com.girsang.server.model.DataUmkm
import com.girsang.server.service.DataStikerService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/dataStiker")
@CrossOrigin(origins = ["*"])
class DataStikerController(private val service: DataStikerService) {

    private val logger = LoggerFactory.getLogger(DataUmkmController::class.java)

    @GetMapping
    fun semua(): ResponseEntity<List<DataStiker>> =
        ResponseEntity.ok(service.semuaStiker())

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
            val saved = service.simpan(stiker)
            ResponseEntity.status(201).body(saved)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to e.message))
        }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody stiker: DataStiker): ResponseEntity<Any> =
        try {
            val updated = service.update(id, stiker)
            ResponseEntity.ok(updated)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to e.message))
        }

    @DeleteMapping("/{id}")
    fun hapus(@PathVariable id: Long): ResponseEntity<Map<String, String>> =
        try {
            service.hapus(id)
            ResponseEntity.ok(mapOf("message" to "Data stiker berhasil dihapus"))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to e.message!!))
        }

    @GetMapping("/cari")
    fun cari(
        @RequestParam(required = false) namaUsaha: String?,
        @RequestParam(required = false) namaStiker: String?,
    ): ResponseEntity<List<DataStiker>> {
        logger.info("Mencari data stiker berdasarkan filter: namaUsaha='{}',  namaStiker='{}'",
            namaUsaha, namaStiker)

        val hasil = when {
            !namaUsaha.isNullOrBlank() -> service.cariNamaUsahaLike(namaUsaha)
            !namaStiker.isNullOrBlank() -> service.cariNamaStikerLike(namaStiker)
            else -> emptyList()
        }
        return ResponseEntity.ok(hasil)
    }
}