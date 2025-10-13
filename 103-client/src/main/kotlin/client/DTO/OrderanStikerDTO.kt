package client.DTO

import client.util.LocalDateTimeSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
class OrderanStikerDTO (
    val id: Long? = null,
    val faktur: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val tanggal: LocalDateTime,
    val umkm: DataUmkmDTO? = null,
    val umkmNama: String = "",
    val totalStiker: Int,
    val rincian: List<OrderanStikerRinciDTO> = emptyList()

)