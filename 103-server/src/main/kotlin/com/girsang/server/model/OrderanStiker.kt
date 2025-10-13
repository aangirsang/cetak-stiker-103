package com.girsang.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.time.LocalDateTime

@Entity
@JsonIgnoreProperties("hibernateLazyInitializer", "handler")
data class OrderanStiker(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    var faktur: String,
    var tanggal: LocalDateTime,
    var totalStiker: Int,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "umkm_id")
    var umkm: DataUmkm,

    @OneToMany(mappedBy = "orderan", cascade = [CascadeType.ALL], orphanRemoval = true)
    var rincian: MutableList<OrderanStikerRinci> = mutableListOf()
)