package com.girsang.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*

@Entity
@JsonIgnoreProperties("hibernateLazyInitializer", "handler", "daftarStiker")
data class DataUmkm(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    var namaPemilik: String = "",
    var namaUsaha: String = "",
    var kontak: String = "",
    var instagram: String = "",
    var alamat: String = "",

    @OneToMany(mappedBy = "dataUmkm", cascade = [CascadeType.ALL], orphanRemoval = true)
    var daftarStiker: MutableList<DataStiker> = mutableListOf()
){
    // 🩵 Tambahkan ini agar Jackson bisa membuat instance kosong
    constructor() : this(null, "", "", "", "", "", mutableListOf())
}
