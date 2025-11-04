package com.girsang.server.repository

import com.girsang.server.model.DataUmkm
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DataUmkmRepository : JpaRepository<DataUmkm, Long> {
//    fun findByNamaPemilikContainingIgnoreCase(namaPemilik: String): List<DataUmkm>
//    fun findByNamaUsahaContainingIgnoreCase(namaUsaha: String): List<DataUmkm>
//    fun findByAlamatContainingIgnoreCase(alamat: String): List<DataUmkm>

    @Query("""
        SELECT U
        FROM DataUmkm U
        WHERE (:namaPemilik IS NULL OR LOWER(U.namaPemilik) LIKE LOWER(CONCAT('%', :namaPemilik, '%')))
        AND (:namaUsaha IS NULL OR LOWER(U.namaUsaha) LIKE LOWER(CONCAT('%', :namaUsaha, '%')))
        AND (:alamat IS NULL OR LOWER(U.alamat) LIKE LOWER(CONCAT('%', :alamat, '%')))
    """)
    fun cariUMKM(
        @Param("namaPemilik") namaPemilik: String?,
        @Param("namaUsaha") namaUsaha: String?,
        @Param("alamat") alamat: String?
    ): List<DataUmkm>
}