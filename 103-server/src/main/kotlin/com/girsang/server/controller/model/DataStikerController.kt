package com.girsang.server.controller.model

import com.girsang.server.dto.DataStikerDTO
import com.girsang.server.model.DataStiker
import com.girsang.server.service.DataStikerService
import com.girsang.server.util.SimpanFileLogger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
            SimpanFileLogger.info("Simpan data Stiker $stiker")
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
            SimpanFileLogger.info("Simpan perubahan data Stiker $stiker")
            val updated = service.update(id, stiker)
            ResponseEntity.ok(updated)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to e.message))
        }

    @DeleteMapping("/{id}")
    fun hapus(@PathVariable id: Long): ResponseEntity<Map<String, String>> =
        try {
            SimpanFileLogger.info("Hapus data Stiker ID $id")
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
        SimpanFileLogger.info("Mencari data stiker berdasarkan filter: namaUsaha='{$namaUsaha}',  namaStiker='{$namaStiker}'")

        val hasil = when {
            !namaUsaha.isNullOrBlank() -> service.cariNamaUsahaLike(namaUsaha)
            !namaStiker.isNullOrBlank() -> service.cariNamaStikerLike(namaStiker)
            else -> emptyList()
        }
        return ResponseEntity.ok(hasil)
    }
}