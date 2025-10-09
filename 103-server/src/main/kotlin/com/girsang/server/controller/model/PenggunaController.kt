package com.girsang.server.controller.model

import com.girsang.server.model.Pengguna
import com.girsang.server.service.PenggunaService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/pengguna")
class PenggunaController(private val service: PenggunaService) {

    @GetMapping
    fun semuaPengguna(): ResponseEntity<List<Pengguna>> =
        ResponseEntity.ok(service.semuaPengguna())

    @GetMapping("/{id}")
    fun penggunaById(@PathVariable id: Long): ResponseEntity<Pengguna> =
        service.cariById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())

    @PostMapping
    fun simpan(@RequestBody pengguna: Pengguna): ResponseEntity<Pengguna> {
        val saved = service.simpan(pengguna)
        return ResponseEntity.status(201).body(saved)
    }

    @PutMapping("/{id}")
    fun updatePengguna(
        @PathVariable id: Long,
        @RequestBody penggunaBaru: Pengguna
    ): ResponseEntity<Any> {
        return try {
            val updated = service.update(id, penggunaBaru)
            ResponseEntity.ok(updated)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to "Pengguna tidak ditemukan"))
        }
    }

    @DeleteMapping("/{id}")
    fun hapus(@PathVariable id: Long): ResponseEntity<Map<String, String>> =
        try {
            service.hapus(id)
            ResponseEntity.ok(mapOf("message" to "Pengguna berhasil dihapus"))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to "Pengguna tidak ditemukan"))
        }

    @GetMapping("/ping")
    fun ping(): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(mapOf("status" to "ok"))

    @GetMapping("/cari")
    fun cariByNamaAkun(@RequestParam namaAkun: String): ResponseEntity<Any> {
        val pengguna = service.findByNamaAkun(namaAkun)
        return if (pengguna != null)
            ResponseEntity.ok(pengguna)
        else
            ResponseEntity.status(404).body(mapOf("message" to "Pengguna tidak ditemukan"))
    }
}
