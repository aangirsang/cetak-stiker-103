package com.girsang.server.controller.model

import com.girsang.server.model.Pengguna
import com.girsang.server.service.PenggunaService
import com.girsang.server.util.SimpanFileLogger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/pengguna")
class PenggunaController(private val service: PenggunaService) {

    @GetMapping
    fun semuaPengguna(): ResponseEntity<List<Pengguna>> {
        SimpanFileLogger.info("Memuat semua data Pengguna")
        return ResponseEntity.ok(service.semuaPengguna())
    }

    @GetMapping("/{id}")
    fun penggunaById(@PathVariable id: Long): ResponseEntity<Pengguna> {
        SimpanFileLogger.info("Cari data Pengguna dengan ID: $id")
        return service.cariById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }

    @PostMapping
    fun simpan(@RequestBody pengguna: Pengguna): ResponseEntity<Pengguna> {
        SimpanFileLogger.info("Simpan data Pengguna $pengguna")
        val saved = service.simpan(pengguna)
        return ResponseEntity.status(201).body(saved)
    }

    @PutMapping("/{id}")
    fun updatePengguna(
        @PathVariable id: Long,
        @RequestBody penggunaBaru: Pengguna
    ): ResponseEntity<Any> {
        return try {
            SimpanFileLogger.info("Simpan perubahan data Pengguna $penggunaBaru")
            val updated = service.update(id, penggunaBaru)
            ResponseEntity.ok(updated)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to "Pengguna tidak ditemukan"))
        }
    }

    @DeleteMapping("/{id}")
    fun hapus(@PathVariable id: Long): ResponseEntity<Map<String, String>> =
        try {
            SimpanFileLogger.info("Hapus data Pengguna ID $id")
            service.hapus(id)
            ResponseEntity.ok(mapOf("message" to "Pengguna berhasil dihapus"))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to "Pengguna tidak ditemukan"))
        }

    @GetMapping("/ping")
    fun ping(): ResponseEntity<Map<String, String>> {
        SimpanFileLogger.info("Client terhubung")
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }

    @GetMapping("/cari")
    fun cariByNamaAkun(@RequestParam("namaAkun") namaAkun: String): ResponseEntity<Any> {
        val pengguna = service.findByNamaAkun(namaAkun)
        return if (pengguna != null)
            ResponseEntity.ok(pengguna)
        else
            ResponseEntity.status(404).body(mapOf("message" to "Pengguna tidak ditemukan"))
    }
}
