package client.DTO

import kotlinx.serialization.Serializable

@Serializable
data class DataUmkmDTO (
    val id: Long? = null,
    var namaPemilik: String = "",
    var namaUsaha: String = "",
    var kontak: String = "",
    var instagram: String = "",
    var alamat:String = ""

)