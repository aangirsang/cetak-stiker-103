package com.girsang.server.repository

import com.girsang.server.model.DataStiker
import com.girsang.server.model.DataUmkm
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DataStikerRepository : JpaRepository<DataStiker, Long> {
    fun findByNamaStikerContainingIgnoreCase(namaStiker: String): List<DataStiker>
    fun findByDataUmkm_NamaUsahaContainingIgnoreCase(namaUsaha: String): List<DataStiker>
    fun findByDataUmkmId(idUmkm: Long): List<DataStiker>
}