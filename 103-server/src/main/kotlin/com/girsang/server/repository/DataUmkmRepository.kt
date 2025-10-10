package com.girsang.server.repository

import com.girsang.server.model.DataUmkm
import org.springframework.data.jpa.repository.JpaRepository

interface DataUmkmRepository : JpaRepository<DataUmkm, Long> {
    fun findByNamaPemilik(namaPemilik: String): List<DataUmkm>
    fun findByNamaUsaha(namaUsaha: String): List<DataUmkm>
    fun findByAlamat(alamat: String): List<DataUmkm>
}