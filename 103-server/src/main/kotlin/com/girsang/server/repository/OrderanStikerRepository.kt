package com.girsang.server.repository

import com.girsang.server.model.OrderanStiker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface OrderanStikerRepository : JpaRepository<OrderanStiker, Long>{
    @Query(
        value = """
        SELECT faktur
        FROM orderan_stiker
        WHERE strftime('%Y', datetime(tanggal / 1000, 'unixepoch')) = :tahun
        ORDER BY faktur DESC
        LIMIT 1
    """,
        nativeQuery = true
    )
    fun findLastFakturByYear(@Param("tahun") tahun: String): String?

    @Query(
        value = """
        SELECT *
        FROM orderan_stiker
        WHERE (:faktur IS NULL OR LOWER(faktur) LIKE LOWER('%' || :faktur || '%'))
        ORDER BY faktur DESC
    """,
        nativeQuery = true
    )
    fun cariFaktur(@Param("faktur") faktur: String?): List<OrderanStiker>


}