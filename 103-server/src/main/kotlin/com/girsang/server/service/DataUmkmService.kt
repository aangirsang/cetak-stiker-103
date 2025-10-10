package com.girsang.server.service

import com.girsang.server.model.DataUmkm
import com.girsang.server.repository.DataUmkmRepository
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class DataUmkmService (private val repo: DataUmkmRepository) {

    fun semuaUmkm(): List<DataUmkm> = repo.findAll()
    fun cariById(id: Long): Optional<DataUmkm> = repo.findById(id)
    fun cariNamaPemilik(namaPemilik: String): List<DataUmkm> = repo.findByNamaPemilik(namaPemilik)
    fun cariNamaUsaha(namaUsaha: String): List<DataUmkm> = repo.findByNamaUsaha(namaUsaha)
    fun cariAlamat(alamat: String): List<DataUmkm> = repo.findByAlamat(alamat)

    fun simpan(dataUmkm: DataUmkm): DataUmkm = repo.save(dataUmkm)
    fun update(id: Long, dataBaru: DataUmkm): DataUmkm {
        val dataLama = repo.findById(id).orElseThrow { NoSuchElementException() }
        dataLama.namaPemilik = dataBaru.namaPemilik
        dataLama.namaUsaha = dataBaru.namaUsaha
        dataLama.kontak = dataBaru.kontak
        dataLama.instagram = dataBaru.instagram
        dataLama.alamat = dataBaru.alamat
        return repo.save(dataLama)
    }
    fun hapus(id: Long){
        if (repo.existsById(id)) {
            repo.deleteById(id)
        } else {
            throw NoSuchElementException("Data dengan id $id tidak ditemukan")
        }
    }

}