package com.girsang.server.service

import com.girsang.server.model.DataUmkm
import com.girsang.server.repository.DataUmkmRepository
import org.springframework.stereotype.Service

@Service
class DataUmkmService(private val repo: DataUmkmRepository) {

    fun semuaUmkm(): List<DataUmkm> = repo.findAll()

    fun cariById(id: Long): DataUmkm =
        repo.findById(id).orElseThrow { NoSuchElementException("Data UMKM dengan id $id tidak ditemukan") }

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

    fun cariUMKM(namaPemilik: String?, namaUsaha: String?, alamat: String?): List<DataUmkm>{
        val keyPemilik = namaPemilik?.trim()?.takeIf { it.isNotEmpty() }
        val keyUsaha = namaUsaha?.trim()?.takeIf { it.isNotEmpty() }
        val keyAlamat = alamat?.trim()?.takeIf { it.isNotEmpty() }

        return repo.cariUMKM(keyPemilik, keyUsaha, keyAlamat)
    }
}
