package com.girsang.server.service

import com.girsang.server.model.DataStiker
import com.girsang.server.model.DataUmkm
import com.girsang.server.repository.DataStikerRepository
import com.girsang.server.repository.DataUmkmRepository
import org.springframework.stereotype.Service
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
        repo.findAll().filter { it.dataUmkm?.id == idUmkm }

    // 🔥 fungsi pembuat kode otomatis
    private fun generateKodeStiker(namaUsaha: String, jumlahSebelumnya: Int): String {
        val tahun = LocalDate.now().year % 100  // ambil 2 digit tahun
        val urutan = String.format("%03d", jumlahSebelumnya + 1) // 001, 002, dst
        return "$namaUsaha - $tahun$urutan"
    }

    fun simpan(stiker: DataStiker): DataStiker {
        val umkm = stiker.dataUmkm?.id?.let { umkmRepo.findById(it).orElseThrow() }
            ?: throw IllegalArgumentException("Data UMKM tidak ditemukan")

        // 🔥 Hitung jumlah stiker UMKM tersebut
        val jumlahSebelumnya = repo.findAll().count { it.dataUmkm?.id == umkm.id }

        // 🔥 Buat kode otomatis
        stiker.kodeStiker = generateKodeStiker(umkm.namaUsaha, jumlahSebelumnya)
        stiker.dataUmkm = umkm
        stiker.tglPembuatan = LocalDateTime.now()
        stiker.tglPerubahan = LocalDateTime.now()

        return repo.save(stiker)
    }

    fun update(id: Long, stiker: DataStiker): DataStiker {
        val existing = repo.findById(id).orElseThrow { NoSuchElementException("Stiker tidak ditemukan") }
        existing.namaStiker = stiker.namaStiker
        existing.panjang = stiker.panjang
        existing.lebar = stiker.lebar
        existing.catatan = stiker.catatan
        existing.dataUmkm = stiker.dataUmkm
        existing.tglPerubahan = LocalDateTime.now()
        return repo.save(existing)
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
