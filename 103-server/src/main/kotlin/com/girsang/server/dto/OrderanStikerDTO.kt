package com.girsang.server.dto

import com.girsang.server.model.DataStiker
import com.girsang.server.model.DataUmkm
import com.girsang.server.model.OrderanStiker
import com.girsang.server.model.OrderanStikerRinci
import java.time.LocalDateTime

data class OrderanStikerRinciDTO(
    val id: Long? = null,
    val jumlah: Int = 0,
    val stiker: DataStiker?,
    val stikerId: Long? = null,
    val kodeStiker: String? = null,
    val stikerNama: String? = null,
    val ukuran: String? = null
)
{
    companion object {
        fun fromEntity(entity: OrderanStikerRinci): OrderanStikerRinciDTO =
            OrderanStikerRinciDTO(
                id = entity.id,
                jumlah = entity.jumlah,
                stiker = entity.stiker,
                stikerId = entity.stiker?.id,
                kodeStiker = entity.stiker?.kodeStiker,
                stikerNama = entity.stiker?.namaStiker,
                ukuran = "${entity.stiker?.panjang} x ${entity.stiker?.lebar}"
            )
    }
}

data class OrderanStikerDTO(
    val id: Long? = null,
    var faktur: String,
    val tanggal: LocalDateTime,
    val totalStiker: Int,
    val umkm: DataUmkm,
    val umkmNama: String? = null,
    val rincian: List<OrderanStikerRinciDTO> = emptyList()
)
{
    companion object {
        fun fromEntity(entity: OrderanStiker): OrderanStikerDTO =
            OrderanStikerDTO(
                id = entity.id,
                faktur = entity.faktur,
                tanggal = entity.tanggal,
                totalStiker = entity.totalStiker,
                umkm = entity.umkm,
                umkmNama = entity.umkm?.namaUsaha,
                rincian = entity.rincian.map { OrderanStikerRinciDTO.fromEntity(it) }
            )
    }
}

data class DataUmkmDTO(
    val id: Long? = null,
    val namaPemilik: String,
    val namaUsaha: String,
    val kontak: String,
    val instagram: String,
    val alamat: String
) {
    fun toEntity(): DataUmkm {
        return DataUmkm(
            id = id,
            namaPemilik = namaPemilik,
            namaUsaha = namaUsaha,
            kontak = kontak,
            instagram = instagram,
            alamat = alamat
        )
    }
}