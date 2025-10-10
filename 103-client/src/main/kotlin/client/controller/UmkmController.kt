package client.controller

import com.girsang.client.controller.MainClientAppController
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.ResourceBundle

@Serializable
data class DataUmkmDTO (
    val id: Long? = null,
    var namaPemilik: String = "",
    var namaUsaha: String = "",
    var kontak: String = "",
    var instagram: String = "",
    var alamat:String = ""

)
class UmkmController : Initializable{

    private val client = HttpClient.newBuilder().build()
    private val json = Json { ignoreUnknownKeys = true }

    @FXML private lateinit var btnSimpan: Button
    @FXML private lateinit var btnRefresh: Button
    @FXML private lateinit var btnHapus: Button
    @FXML private lateinit var btnTutup: Button

    @FXML private lateinit var txtNamaPemilik: TextField
    @FXML private lateinit var txtNamaUsaha: TextField
    @FXML private lateinit var txtKontak: TextField
    @FXML private lateinit var txtInstagram: TextField
    @FXML private lateinit var txtAlamat: TextArea
    @FXML private lateinit var txtCariNamaPemilik: TextField
    @FXML private lateinit var txtCariNamaUsaha: TextField
    @FXML private lateinit var txtCariAlamat: TextField

    @FXML private lateinit var tblUmkm: TableView<DataUmkmDTO>
    @FXML private lateinit var kolId: TableColumn<DataUmkmDTO, Long>
    @FXML private lateinit var kolNamaPemilik: TableColumn<DataUmkmDTO, String>
    @FXML private lateinit var kolNamaUsaha: TableColumn<DataUmkmDTO, String>
    @FXML private lateinit var kolKontak: TableColumn<DataUmkmDTO, String>
    @FXML private lateinit var kolInstagram: TableColumn<DataUmkmDTO, String>
        @FXML private lateinit var kolAlamat: TableColumn<DataUmkmDTO, String>

    private var clientController: MainClientAppController? = null
    private var parentController: MainClientAppController? = null

    fun setClientController(controller: MainClientAppController) {
        this.clientController = controller  // ✅ simpan controller dulu

        if (!controller.url.isNullOrBlank()) {
            bersih() // ✅ baru panggil setelah URL diset
        } else {
            println("⚠️ URL belum di-set, data tidak bisa dimuat")
        }
    }

    fun setParentController(controller: MainClientAppController) {
        this.parentController = controller
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        kolId.setCellValueFactory {javafx.beans.property.SimpleLongProperty(it.value.id?: 0).asObject()}
        kolNamaPemilik.setCellValueFactory {javafx.beans.property.SimpleStringProperty(it.value.namaPemilik)}
        kolNamaUsaha.setCellValueFactory {javafx.beans.property.SimpleStringProperty(it.value.namaUsaha)}
        kolKontak.setCellValueFactory {javafx.beans.property.SimpleStringProperty(it.value.kontak)}
        kolInstagram.setCellValueFactory {javafx.beans.property.SimpleStringProperty(it.value.instagram)}
        kolAlamat.setCellValueFactory {javafx.beans.property.SimpleStringProperty(it.value.alamat)}

        btnTutup.setOnAction { parentController?.tutupForm() }
        btnRefresh.setOnAction { bersih() }
        btnSimpan.setOnAction { simpanDataUmkm() }
        btnHapus.setOnAction { hapusData() }

        tblUmkm.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                umkmTerpilih(newValue)
            }
        }
    }
    fun bersih(){
        txtNamaPemilik.clear()
        txtNamaUsaha.clear()
        txtKontak.clear()
        txtInstagram.clear()
        txtAlamat.clear()
        txtCariNamaPemilik.clear()
        txtCariNamaUsaha.clear()
        txtCariAlamat.clear()

        tblUmkm.selectionModel.clearSelection()

        btnSimpan.text = "Simpan"

        loadData()
    }
    fun loadData(){
        println("DEBUG: clientController = $clientController, url = ${clientController?.url}")
        if(clientController?.url.isNullOrBlank()){
            Platform.runLater { clientController?.showError("URL server belum di set") }
            return
        }
        Thread {
            try {
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("${clientController?.url}/api/dataUmkm"))
                    .GET()
                    .header("Content-Type", "application/json")

                clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                val request = builder.build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if(response.statusCode() in 200..299){
                    val list = json.decodeFromString<List<DataUmkmDTO>>(response.body())
                    Platform.runLater {
                        tblUmkm.items = FXCollections.observableArrayList(list)
                    }
                } else {
                    Platform.runLater {
                        clientController?.showError("Server Error ${response.statusCode()}")
                    }
                }
            } catch (ex: Exception){
                Platform.runLater {
                    clientController?.showError(ex.message ?: "Gagal memeuat data UMKM")
                }
            }
        }.start()
    }

    fun simpanDataUmkm() {
        val namaPemilik = txtNamaPemilik.text.trim()
        val namaUsaha = txtNamaUsaha.text.trim()
        val kontak = txtKontak.text.trim()
        val instagram = txtInstagram.text.trim()
        val alamat = txtAlamat.text.trim()
        if (namaPemilik.isEmpty() ||
            namaUsaha.isEmpty() ||
            kontak.isEmpty() ||
            instagram.isEmpty()||
            alamat.isEmpty()){
            clientController?.showError("Semua field harus diisi!")
            return
        }

        if (btnSimpan.text == "Simpan") {
            Thread{
                try{
                    val dto = DataUmkmDTO(
                        namaPemilik = namaPemilik,
                        namaUsaha = namaUsaha,
                        kontak = kontak,
                        instagram = instagram,
                        alamat = alamat
                        )
                    val body = json.encodeToString(dto)
                    val builder = HttpRequest.newBuilder()
                        .uri(URI.create("${clientController?.url}/api/dataUmkm"))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .header("Content-Type", "application/json")

                    clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                    val req = builder.build()
                    val resp = clientController?.makeRequest(req)

                    if(resp?.statusCode() in 200..299){
                        Platform.runLater {
                            clientController?.showInfo("Data UMKM berhasil disimpan.")
                            bersih()
                        }
                    }else {
                        Platform.runLater {
                            println("Server returned ${resp?.statusCode()} : ${resp?.body()}")
                            clientController?.showError("Server returned ${resp?.statusCode()} : ${resp?.body()}")
                        }
                    }
                } catch (ex: Exception) {
                    Platform.runLater {
                        clientController?.showError(ex.message ?: "Error saat menyimpan data")
                    }
                }
            }.start()
        } else {
            val umkm = getUmkmTerpilih()
            val id = umkm?.id

            if (id==null) {
                clientController?.showError("ID UMKM tidak tersedia")
                return
            }

            Thread{
                try {
                    val dto = DataUmkmDTO(
                        id = id,
                        namaPemilik = namaPemilik,
                        namaUsaha = namaUsaha,
                        kontak = kontak,
                        instagram = instagram,
                        alamat = alamat
                    )
                    val body = json.encodeToString(dto)
                    val builder = HttpRequest.newBuilder()
                        .uri(URI.create("${clientController?.url}/api/dataUmkm/${id}"))
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .header("Content-Type", "application/json")

                    clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                    val req = builder.build()
                    val resp = clientController?.makeRequest(req)

                    if(resp?.statusCode() in 200..299){
                        Platform.runLater {
                            clientController?.showInfo("Data UMKM berhasil diperbarui.")
                            bersih()
                        }
                    }else {
                        Platform.runLater {
                            println("Server returned ${resp?.statusCode()} : ${resp?.body()}")
                            clientController?.showError("Server returned ${resp?.statusCode()} : ${resp?.body()}")
                        }
                    }
                } catch (ex: Exception) {
                    Platform.runLater {
                        clientController?.showError(ex.message ?: "Error saat memperbarui data")
                    }
                }
            }.start()
        }
    }
    fun hapusData(){
        val umkm = getUmkmTerpilih()
        val id = umkm?.id
        if (umkm == null) {
            clientController?.showError("Tidak ada data UMKM yang dipilih!")
            return
        }

        if(id == null) {
            clientController?.showError("ID data UMKM tidak tersedia!")
            return
        }

        Thread{
            try {
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("${clientController?.url}/api/dataUmkm/${id}"))
                    .DELETE()

                clientController?.buildAuthHeader()?.let {  builder.header("Authorization", it) }

                val request = builder.build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                Platform.runLater {
                    if (response.statusCode() in 200..299) {
                        bersih()
                        clientController?.showInfo("Data UMKM berhasil dihapus.")
                    } else {
                        clientController?.showError("Server returned ${response.statusCode()} : ${response.body()}")
                    }
                }
            } catch (ex: Exception) {
                Platform.runLater {
                    clientController?.showError(ex.message ?: "Gagal menghapus data UMKM")
                }
            }
        }.start()
    }
    fun umkmTerpilih(dto: DataUmkmDTO){
        txtNamaPemilik.text = dto.namaPemilik
        txtNamaUsaha.text = dto.namaUsaha
        txtKontak.text = dto.kontak
        txtInstagram.text = dto.instagram
        txtAlamat.text = dto.alamat
        btnSimpan.text = "Ubah"
    }
    fun getUmkmTerpilih(): DataUmkmDTO? {
        return tblUmkm.selectionModel.selectedItem
    }
}