package com.girsang.server.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
data class DataUmkm (
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var namaPemilik: String = "",
    var namaUsaha: String = "",
    var kontak: String = "",
    var instagram: String = "",
    var alamat:String = ""

)