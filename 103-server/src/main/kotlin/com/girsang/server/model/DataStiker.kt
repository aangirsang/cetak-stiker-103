package com.girsang.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@JsonIgnoreProperties("hibernateLazyInitializer", "handler")
data class DataStiker(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "data_umkm_id", nullable = false)
    @JsonIgnoreProperties("daftarStiker") // 🟢 biar gak loop, tapi tetap tampil dataUmkm
    var dataUmkm: DataUmkm? = null,

    var kodeStiker: String = "",
    var namaStiker: String = "",
    var panjang: Int = 0,
    var lebar: Int = 0,
    var catatan: String = "",
    var tglPembuatan: LocalDateTime = LocalDateTime.now(),
    var tglPerubahan: LocalDateTime = LocalDateTime.now()
){
    // 🩵 Wajib untuk deserialisasi dan Hibernate
    constructor() : this(
        null,
        null,
        "",
        "",
        0,
        0,
        "",
        LocalDateTime.now(),
        LocalDateTime.now()
    )
}
