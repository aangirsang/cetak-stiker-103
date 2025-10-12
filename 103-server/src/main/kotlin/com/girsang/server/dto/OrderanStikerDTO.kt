package com.girsang.server.dto

import com.girsang.server.model.OrderanStiker
import com.girsang.server.model.OrderanStikerRinci
import java.time.LocalDateTime

data class OrderanStikerRinciDTO(
    val id: Long?,
    val jumlah: Int,
    val stikerId: Long?,
    val stikerNama: String?
) {
    companion object {
        fun fromEntity(entity: OrderanStikerRinci): OrderanStikerRinciDTO =
            OrderanStikerRinciDTO(
                id = entity.id,
                jumlah = entity.jumlah,
                stikerId = entity.stiker?.id,
                stikerNama = entity.stiker?.namaStiker ?: entity.stiker?.toString()
            )
    }
}

data class OrderanStikerDTO(
    val id: Long?,
    val faktur: String,
    val tanggal: LocalDateTime,
    val totalStiker: Int,
    val rincian: List<OrderanStikerRinciDTO>
) {
    companion object {
        fun fromEntity(entity: OrderanStiker): OrderanStikerDTO =
            OrderanStikerDTO(
                id = entity.id,
                faktur = entity.faktur,
                tanggal = entity.tanggal,
                totalStiker = entity.totalStiker,
                rincian = entity.rincian.map { OrderanStikerRinciDTO.fromEntity(it) }
            )
    }
}