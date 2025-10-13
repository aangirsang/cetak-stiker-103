package client.DTO

import kotlinx.serialization.Serializable

@Serializable
class OrderanStikerRinciDTO (
    val id: Long? = null,
    var stiker: DataStikerDTO? = null,
    val kodeStiker: String,
    val stikerNama: String,
    var jumlah: Int = 0
)