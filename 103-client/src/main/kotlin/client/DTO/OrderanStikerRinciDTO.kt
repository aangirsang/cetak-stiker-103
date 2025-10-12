package client.DTO

import kotlinx.serialization.Serializable

@Serializable
class OrderanStikerRinciDTO (
    val id: Long? = null,
    val stiker: DataStikerDTO,
    var jumlah: Int
)