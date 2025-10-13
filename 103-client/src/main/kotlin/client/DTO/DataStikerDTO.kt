package client.DTO

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class DataStikerDTO(
    val id: Long? = null,

    var dataUmkm: DataUmkmDTO? = null,

    var dataUmkmId: Long? = null,

    var kodeStiker: String = "",
    var namaStiker: String = "",
    var panjang: Int = 0,
    var lebar: Int = 0,
    var catatan: String = "",
    @Contextual var tglPembuatan: LocalDateTime = LocalDateTime.now(),
    @Contextual var tglPerubahan: LocalDateTime = LocalDateTime.now()
)