package com.girsang.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
data class OrderanStikerRinci(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderan_id")
    var orderan: OrderanStiker? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stiker_id")
    var stiker: DataStiker? = null,

    var jumlah: Int = 0,
    )