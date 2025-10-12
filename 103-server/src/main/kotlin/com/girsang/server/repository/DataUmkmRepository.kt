package com.girsang.server.repository

import com.girsang.server.model.DataUmkm
import org.springframework.data.jpa.repository.JpaRepository

interface DataUmkmRepository : JpaRepository<DataUmkm, Long> {
    fun findByNamaPemilikContainingIgnoreCase(namaPemilik: String): List<DataUmkm>
    fun findByNamaUsahaContainingIgnoreCase(namaUsaha: String): List<DataUmkm>
    fun findByAlamatContainingIgnoreCase(alamat: String): List<DataUmkm>
}