package com.girsang.server.repository

import com.girsang.server.model.OrderanStikerRinci
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderanStikerRinciRepository : JpaRepository<OrderanStikerRinci, Long>