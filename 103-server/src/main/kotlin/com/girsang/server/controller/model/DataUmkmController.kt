package com.girsang.server.controller.model

import com.girsang.server.model.DataUmkm
import com.girsang.server.service.DataUmkmService
import org.aspectj.bridge.IMessage
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dataUmkm")
class DataUmkmController (private val service: DataUmkmService){

    @GetMapping("/{id}")
    fun umkmById(@PathVariable id: Long): ResponseEntity<DataUmkm> =
        service.cariById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())

    @GetMapping
    fun semuaDataUmkm(): ResponseEntity<List<DataUmkm>> =
        ResponseEntity.ok(service.semuaUmkm())

    @GetMapping("/cari")
    fun cari(
        @RequestParam(required = false) namaPemilik: String?,
        @RequestParam(required = false) namaUsaha: String?,
        @RequestParam(required = false) alamat: String?) : List<DataUmkm>{
        return when {
            !namaPemilik.isNullOrBlank() -> service.cariNamaPemilik(namaPemilik)
            !namaUsaha.isNullOrBlank() -> service.cariNamaUsaha(namaUsaha)
            !alamat.isNullOrBlank() -> service.cariAlamat(alamat)
            else -> emptyList()
        }
    }

    @PostMapping
    fun simpan(@RequestBody dataUmkm: DataUmkm): ResponseEntity<DataUmkm> {
        val simpan = service.simpan(dataUmkm)
        return ResponseEntity.status(201).body(simpan)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody dataBaru: DataUmkm):
            ResponseEntity<Any> {
        return try {
            val updated = service.update(id, dataBaru)
            ResponseEntity.ok(updated)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to "Data UMKM tidak ditemukan"))
        }
    }

    @DeleteMapping("/{id}")
    fun hapus(@PathVariable id: Long): ResponseEntity<Map<String, String>> =
        try {
            service.hapus(id)
            ResponseEntity.ok(mapOf("message" to "Data UMKM berhasil dihapus"))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(404).body(mapOf("message" to "Data UMKM tidak Ditemukan"))
        }
//
//    @GetMapping("/cari")
//    fun cariNamaPemilik(@RequestParam("namaPemilik") namaPemilik: String): ResponseEntity<Any> {
//        println("DEBUG: mencari Data Umkm dengan namaPemilik = $namaPemilik")
//        val dataUmkm = service.cariNamaPemilik(namaPemilik)
//        return if (dataUmkm != null)
//            ResponseEntity.ok(dataUmkm)
//        else
//            ResponseEntity.status(404).body(mapOf("message" to "Data UMKM tidak ditemukan"))
//    }
//
//    @GetMapping("/cari")
//    fun cariNamaUsaha(@RequestParam("namaUsaha") namaUsaha: String): ResponseEntity<Any> {
//        println("DEBUG: mencari Data Umkm dengan namaUsaha = $namaUsaha")
//        val dataUmkm = service.cariNamaUsaha(namaUsaha)
//        return if (dataUmkm != null)
//            ResponseEntity.ok(dataUmkm)
//        else
//            ResponseEntity.status(404).body(mapOf("message" to "Data UMKM tidak ditemukan"))
//    }
//
//    @GetMapping("/cari")
//    fun cariAlamat(@RequestParam("alamat") alamat: String): ResponseEntity<Any> {
//        println("DEBUG: mencari Data Umkm dengan alamat = $alamat")
//        val dataUmkm = service.cariAlamat(alamat)
//        return if (dataUmkm != null)
//            ResponseEntity.ok(dataUmkm)
//        else
//            ResponseEntity.status(404).body(mapOf("message" to "Data UMKM tidak ditemukan"))
//    }

}