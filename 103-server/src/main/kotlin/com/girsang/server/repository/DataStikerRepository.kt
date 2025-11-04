package com.girsang.server.repository

import com.girsang.server.model.DataStiker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DataStikerRepository : JpaRepository<DataStiker, Long> {
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


    @Query("""
        SELECT s
        FROM DataStiker s
        WHERE (:namaStiker IS NULL OR LOWER(s.namaStiker) LIKE LOWER(CONCAT('%', :namaStiker, '%')))
        AND (:namaUsaha IS NULL OR LOWER(s.dataUmkm.namaUsaha) LIKE LOWER(CONCAT('%', :namaUsaha, '%')))
        """)
    fun cariStiker(
        @Param("namaStiker") namaStiker: String?,
        @Param("namaUsaha") namaUsaha: String?
    ): List<DataStiker>
}