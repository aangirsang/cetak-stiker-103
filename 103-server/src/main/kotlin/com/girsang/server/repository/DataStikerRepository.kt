package com.girsang.server.repository

import com.girsang.server.model.DataStiker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DataStikerRepository : JpaRepository<DataStiker, Long> {
    fun findByNamaStikerContainingIgnoreCase(namaStiker: String): List<DataStiker>
    fun findByDataUmkm_NamaUsahaContainingIgnoreCase(namaUsaha: String): List<DataStiker>
    fun findByDataUmkm_Id(idUmkm: Long): List<DataStiker>

    @Query(
        value = """
            SELECT KODE_STIKER   
            FROM DATA_STIKER  
            WHERE DATA_UMKM_ID  = :ID
            ORDER BY KODE_STIKER DESC 
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun findLastKodeStiker(ID: Long?): String?
}