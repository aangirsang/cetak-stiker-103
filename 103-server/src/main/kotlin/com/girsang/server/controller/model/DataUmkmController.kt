package com.girsang.server.controller.model

import com.girsang.server.model.DataUmkm
import com.girsang.server.service.DataUmkmService
import com.girsang.server.util.SimpanFileLogger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/dataUmkm")
class DataUmkmController(private val service: DataUmkmService) {


    @GetMapping
    fun semuaDataUmkm(): ResponseEntity<List<DataUmkm>> {
        SimpanFileLogger.info("Memuat semua data UMKM")
        return ResponseEntity.ok(service.semuaUmkm())
    }

    @GetMapping("/{id}")
    fun umkmById(@PathVariable id: Long): ResponseEntity<Any> =
        try {
            SimpanFileLogger.info("Mencari Data UMKM dengan id=$id")
            ResponseEntity.ok(service.cariById(id))
        } catch (e: NoSuchElementException) {
            SimpanFileLogger.error("Data UMKM dengan id=$id tidak ditemukan")
            ResponseEntity.status(404).body(mapOf("message" to "Data UMKM dengan id $id tidak ditemukan"))
        }

//    @GetMapping("/cari")
//    fun cari(
//        @RequestParam(required = false) namaPemilik: String?,
//        @RequestParam(required = false) namaUsaha: String?,
//        @RequestParam(required = false) alamat: String?
//    ): ResponseEntity<List<DataUmkm>> {
//        SimpanFileLogger.info("Mencari data UMKM berdasarkan filter: namaPemilik='{$namaPemilik}', " +
//                "namaUsaha='{$namaUsaha}', alamat='{$alamat}'")
//
//        val hasil = when {
//            !namaPemilik.isNullOrBlank() -> service.cariNamaPemilikLike(namaPemilik)
//            !namaUsaha.isNullOrBlank() -> service.cariNamaUsahaLike(namaUsaha)
//            !alamat.isNullOrBlank() -> service.cariAlamatLike(alamat)
//            else -> emptyList()
//        }
//        return ResponseEntity.ok(hasil)
//    }

    @PostMapping
    fun simpan(@RequestBody dataUmkm: DataUmkm): ResponseEntity<DataUmkm> {
        SimpanFileLogger.info("Menyimpan Data UMKM baru: {$dataUmkm}")
        val simpan = service.simpan(dataUmkm)
        return ResponseEntity.status(201).body(simpan)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody dataBaru: DataUmkm): ResponseEntity<Any> =
        try {
            SimpanFileLogger.info("Mengupdate Data UMKM dengan id=$dataBaru")
            val updated = service.update(id, dataBaru)
            ResponseEntity.ok(updated)
        } catch (e: NoSuchElementException) {
            SimpanFileLogger.error("Gagal update: Data UMKM dengan id=$id tidak ditemukan")
            ResponseEntity.status(404).body(mapOf("message" to "Data UMKM tidak ditemukan"))
        }

    @DeleteMapping("/{id}")
    fun hapus(@PathVariable id: Long): ResponseEntity<Map<String, String>> =
        try {
            SimpanFileLogger.info("Menghapus Data UMKM dengan id=$id")
            service.hapus(id)
            ResponseEntity.ok(mapOf("message" to "Data UMKM berhasil dihapus"))
        } catch (e: NoSuchElementException) {
            SimpanFileLogger.error("Gagal hapus: Data UMKM dengan id=$id tidak ditemukan")
            ResponseEntity.status(404).body(mapOf("message" to "Data UMKM tidak ditemukan"))
        } catch (e: org.springframework.dao.DataIntegrityViolationException) {
            SimpanFileLogger.error("Gagal hapus: Data UMKM id=$id masih digunakan di tabel lain")
            ResponseEntity.status(409).body(mapOf("message" to "Data UMKM masih digunakan di data lain"))
        } catch (e: Exception) {
            SimpanFileLogger.error("Error tidak terduga saat menghapus UMKM id=$id: ${e.message}")
            ResponseEntity.status(500).body(mapOf("message" to "Terjadi kesalahan pada server"))
        }

    @GetMapping("/cari")
    fun cariUMKM(
        @RequestParam(required = false) namaPemilik: String?,
        @RequestParam(required = false) namaUsaha: String?,
        @RequestParam(required = false) alamat: String?
    ): List<DataUmkm> {
        return service.cariUMKM(namaPemilik, namaUsaha, alamat)
    }
}
