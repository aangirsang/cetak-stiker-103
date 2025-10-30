package com.girsang.server.service

import com.girsang.server.dto.DataStikerDTO
import com.girsang.server.model.DataStiker
import com.girsang.server.model.DataUmkm
import com.girsang.server.repository.DataStikerRepository
import com.girsang.server.repository.DataUmkmRepository
import com.girsang.server.util.SimpanFileLogger
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestBody
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
class DataStikerService(
    private val repo: DataStikerRepository,
    private val umkmRepo: DataUmkmRepository
) {

    fun semuaStiker(): List<DataStiker> = repo.findAll()

    fun cariById(id: Long): Optional<DataStiker> = repo.findById(id)

    fun stikerPerUmkm(idUmkm: Long): List<DataStiker> =
        repo.findByDataUmkm_Id(idUmkm)

    // 🔥 fungsi pembuat kode otomatis
    private fun generateKodeStiker(umkm: DataUmkm): String {
        val tahun = LocalDate.now().year % 100  // ambil 2 digit tahun
        val lastKode = repo.findLastKodeStiker(umkm.id)
        val nextUrut = if(lastKode != null && lastKode.length >= 9) {
            val lastUrut = lastKode.takeLast(3).toIntOrNull() ?: 0
            lastUrut + 1
        } else {
            1
        }
        val urutStr = String.format("%03d", nextUrut)
        return "${umkm.namaUsaha} - $tahun$urutStr"
    }
    fun simpan(@RequestBody dto: DataStikerDTO): ResponseEntity<DataStiker> {
        val umkm = umkmRepo.findById(dto.dataUmkmId)
            .orElseThrow { RuntimeException("UMKM tidak ditemukan") }

        //val jumlahSebelumnya = repo.findAll().count { it.dataUmkm?.id == umkm.id }
        val umkmIdNonNull: Long = umkm.id ?: throw IllegalArgumentException("UMKM Id tidak boleh null")
        val jumlahSebelumnya = repo.findLastKodeStiker(umkmIdNonNull)

        val stiker = DataStiker(
            dataUmkm = umkm,
            kodeStiker = generateKodeStiker(umkm),
            namaStiker = dto.namaStiker,
            panjang = dto.panjang,
            lebar = dto.lebar,
            catatan = dto.catatan
        )

        repo.save(stiker)
        return ResponseEntity.ok(stiker)
    }

    fun update(id: Long, @RequestBody dto: DataStikerDTO): ResponseEntity<DataStiker> {
        val existing = repo.findById(id).orElseThrow { NoSuchElementException("Stiker tidak ditemukan") }
        val umkm = umkmRepo.findById(dto.dataUmkmId)
            .orElseThrow { RuntimeException("UMKM tidak ditemukan") }
        existing.apply {
            dataUmkm = umkm
            namaStiker = dto.namaStiker
            panjang = dto.panjang
            lebar = dto.lebar
            catatan = dto.catatan
            tglPerubahan = LocalDateTime.now()
        }

        val updated = repo.save(existing)
        return ResponseEntity.ok(updated)
    }

    fun hapus(id: Long) {
        if (!repo.existsById(id)) throw NoSuchElementException("Data tidak ditemukan")
        repo.deleteById(id)
    }

    fun cariNamaStikerLike(keyword: String): List<DataStiker> =
        repo.findByNamaStikerContainingIgnoreCase(keyword.trim())

    fun cariNamaUsahaLike(keyword: String): List<DataStiker> =
        repo.findByDataUmkm_NamaUsahaContainingIgnoreCase(keyword.trim())

}
