package com.girsang.server.dto

import java.time.LocalDateTime

class DataStikerDTO (
    val id: Long? = null,
    var dataUmkmId: Long,
    var kodeStiker: String = "",
    var namaStiker: String = "",
    var panjang: Int = 0,
    var lebar: Int = 0,
    var catatan: String = "",
    var tglPembuatan: LocalDateTime = LocalDateTime.now(),
    var tglPerubahan: LocalDateTime = LocalDateTime.now()
)