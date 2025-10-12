package com.girsang.server.repository

import com.girsang.server.model.OrderanStiker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OrderanStikerRepository : JpaRepository<OrderanStiker, Long>{
    @Query(
        value = """
            SELECT faktur 
            FROM orderan_stiker 
            WHERE YEAR(tanggal) = :year 
            ORDER BY faktur DESC 
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun findLastFakturByYear(year: Int): String?
}