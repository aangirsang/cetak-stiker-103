package com.girsang.server.service

import com.girsang.server.model.DataUmkm
import com.girsang.server.repository.DataUmkmRepository
import org.springframework.stereotype.Service

@Service
class DataUmkmService(private val repo: DataUmkmRepository) {

    fun semuaUmkm(): List<DataUmkm> = repo.findAll()

    fun cariById(id: Long): DataUmkm =
        repo.findById(id).orElseThrow { NoSuchElementException("Data UMKM dengan id $id tidak ditemukan") }

    fun cariNamaPemilikLike(keyword: String): List<DataUmkm> =
        repo.findByNamaPemilikContainingIgnoreCase(keyword.trim())

    fun cariNamaUsahaLike(keyword: String): List<DataUmkm> =
        repo.findByNamaUsahaContainingIgnoreCase(keyword.trim())

    fun cariAlamatLike(keyword: String): List<DataUmkm> =
        repo.findByAlamatContainingIgnoreCase(keyword.trim())

    fun simpan(dataUmkm: DataUmkm): DataUmkm =
        repo.save(dataUmkm)

    fun update(id: Long, dataBaru: DataUmkm): DataUmkm {
        val dataLama = cariById(id)  // pakai fungsi sendiri supaya pesan error konsisten

        dataLama.apply {
            namaPemilik = dataBaru.namaPemilik
            namaUsaha = dataBaru.namaUsaha
            kontak = dataBaru.kontak
            instagram = dataBaru.instagram
            alamat = dataBaru.alamat
        }

        return repo.save(dataLama)
    }

    fun hapus(id: Long) {
        val data = repo.findById(id)
        if (data.isPresent) {
            repo.delete(data.get())
        } else {
            throw NoSuchElementException("Data UMKM dengan id $id tidak ditemukan")
        }
    }
}
