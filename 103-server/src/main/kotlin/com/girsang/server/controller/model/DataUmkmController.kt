package com.girsang.server.controller.model

import com.girsang.server.model.DataUmkm
import com.girsang.server.service.DataUmkmService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/dataUmkm")
class DataUmkmController(private val service: DataUmkmService) {

    private val logger = LoggerFactory.getLogger(DataUmkmController::class.java)

    @GetMapping
    fun semuaDataUmkm(): ResponseEntity<List<DataUmkm>> {
        logger.info("Memuat semua data UMKM")
        return ResponseEntity.ok(service.semuaUmkm())
    }

    @GetMapping("/{id}")
    fun umkmById(@PathVariable id: Long): ResponseEntity<Any> =
        try {
            logger.info("Mencari Data UMKM dengan id=$id")
            ResponseEntity.ok(service.cariById(id))
        } catch (e: NoSuchElementException) {
            logger.warn("Data UMKM dengan id=$id tidak ditemukan")
            ResponseEntity.status(404).body(mapOf("message" to "Data UMKM dengan id $id tidak ditemukan"))
        }

    @GetMapping("/cari")
    fun cari(
        @RequestParam(required = false) namaPemilik: String?,
        @RequestParam(required = false) namaUsaha: String?,
        @RequestParam(required = false) alamat: String?
    ): ResponseEntity<List<DataUmkm>> {
        logger.info("Mencari data UMKM berdasarkan filter: namaPemilik='{}', namaUsaha='{}', alamat='{}'",
            namaPemilik, namaUsaha, alamat)

        val hasil = when {
            !namaPemilik.isNullOrBlank() -> service.cariNamaPemilikLike(namaPemilik)
            !namaUsaha.isNullOrBlank() -> service.cariNamaUsahaLike(namaUsaha)
            !alamat.isNullOrBlank() -> service.cariAlamatLike(alamat)
            else -> emptyList()
        }
        return ResponseEntity.ok(hasil)
    }

    @PostMapping
    fun simpan(@RequestBody dataUmkm: DataUmkm): ResponseEntity<DataUmkm> {
        logger.info("Menyimpan Data UMKM baru: {}", dataUmkm)
        val simpan = service.simpan(dataUmkm)
        return ResponseEntity.status(201).body(simpan)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody dataBaru: DataUmkm): ResponseEntity<Any> =
        try {
            logger.info("Mengupdate Data UMKM dengan id=$id")
            val updated = service.update(id, dataBaru)
            ResponseEntity.ok(updated)
        } catch (e: NoSuchElementException) {
            logger.warn("Gagal update: Data UMKM dengan id=$id tidak ditemukan")
            ResponseEntity.status(404).body(mapOf("message" to "Data UMKM tidak ditemukan"))
        }

    @DeleteMapping("/{id}")
    fun hapus(@PathVariable id: Long): ResponseEntity<Map<String, String>> =
        try {
            logger.info("Menghapus Data UMKM dengan id=$id")
            service.hapus(id)
            ResponseEntity.ok(mapOf("message" to "Data UMKM berhasil dihapus"))
        } catch (e: NoSuchElementException) {
            logger.warn("Gagal hapus: Data UMKM dengan id=$id tidak ditemukan")
            ResponseEntity.status(404).body(mapOf("message" to "Data UMKM tidak ditemukan"))
        }
}
