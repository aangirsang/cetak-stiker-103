package com.girsang.server.repository

import com.girsang.server.model.OrderanStikerRinci
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OrderanStikerRinciRepository : JpaRepository<OrderanStikerRinci, Long>{
    @Query("SELECT r FROM OrderanStikerRinci r WHERE r.orderan.id = :orderanId")
    fun findByOrderanId(orderanId: Long): List<OrderanStikerRinci>
}