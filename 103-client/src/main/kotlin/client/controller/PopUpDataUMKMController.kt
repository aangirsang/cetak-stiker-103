package client.controller

import client.DTO.DataUmkmDTO
import client.util.LocalDateTimeSerializer
import client.util.PesanPeringatan
import com.girsang.client.controller.MainClientAppController
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.stage.Stage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime
import java.util.ResourceBundle

class PopUpDataUMKMController : Initializable {

    var selectedUmkm: DataUmkmDTO? = null
    fun setData(list: List<DataUmkmDTO>) {
        tblUmkm.items = FXCollections.observableArrayList(list)
    }
    val title: String = "Data UMKM"

    @FXML private lateinit var txtNamaPemilik: TextField
    @FXML private lateinit var txtNamaUsaha: TextField
    @FXML private lateinit var txtKontak: TextField
    @FXML private lateinit var txtInstagram: TextField
    @FXML private lateinit var txtCariNamaPemilik: TextField
    @FXML private lateinit var txtCariNamaUsaha: TextField
    @FXML private lateinit var txtAlamat: TextArea

    @FXML private lateinit var btnPilih: Button
    @FXML private lateinit var btnSimpan: Button
    @FXML private lateinit var btnHapus: Button
    @FXML private lateinit var btnRefresh: Button
    @FXML private lateinit var btnTutup: Button

    @FXML private lateinit var tblUmkm: TableView<DataUmkmDTO>
    @FXML private lateinit var kolNamaPemilik: TableColumn<DataUmkmDTO, String>
    @FXML private lateinit var kolNamaUsaha: TableColumn<DataUmkmDTO, String>
    @FXML private lateinit var kolKontak: TableColumn<DataUmkmDTO, String>

    private val client = HttpClient.newBuilder().build()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        serializersModule = SerializersModule {
            contextual(LocalDateTime::class, LocalDateTimeSerializer)
        }
    }
    private var clientController: MainClientAppController? = null
    private var parentController: MainClientAppController? = null

    private var searchThread: Thread? = null

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
        kolNamaPemilik.setCellValueFactory {SimpleStringProperty(it.value.namaPemilik)}
        kolNamaUsaha.setCellValueFactory {SimpleStringProperty(it.value.namaUsaha)}
        kolKontak.setCellValueFactory {SimpleStringProperty(it.value.kontak)}

        setupSearchListener(txtCariNamaPemilik, "namaPemilik")
        setupSearchListener(txtCariNamaUsaha, "namaUsaha")

        btnRefresh.setOnAction{ bersih()}
        btnPilih.setOnAction{ pilihUMKM()}
        btnTutup.setOnAction {
            val stage = btnTutup.scene?.window as? Stage
            stage?.close()
        }
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

        tblUmkm.selectionModel.clearSelection()

        txtNamaPemilik.promptText = "Data Nama Pemilik Usaha"
        txtNamaUsaha.promptText = "Data Nama Usaha"
        txtKontak.promptText = "Data Kontak No. Handphone atau WhatsApp"
        txtInstagram.promptText = "Data Akun Instagram"
        txtAlamat.promptText = "Data Alamat"
        txtCariNamaPemilik.promptText = "Cari Nama Pemilik Usaha"
        txtCariNamaUsaha.promptText = "Cari Nama Usaha"

        loadDataUMKM()
    }
    private fun setupSearchListener(field: TextField, paramName: String) {
        field.textProperty().addListener { _, _, newValue ->
            searchThread?.interrupt() // hentikan thread sebelumnya jika user masih mengetik
            searchThread = Thread {
                try {
                    Thread.sleep(300) // debounce 300ms
                    if (Thread.interrupted()) return@Thread

                    if (newValue.isNullOrBlank()) {
                        Platform.runLater { loadDataUMKM() }
                    } else {
                        cariDataUmkm(paramName, newValue)
                    }
                } catch (_: InterruptedException) {
                }
            }
            searchThread?.start()
        }
    }
    fun loadDataUMKM(){
        println("DEBUG: clientController = $clientController, url = ${clientController?.url}")
        if(clientController?.url.isNullOrBlank()){
            Platform.runLater { PesanPeringatan.error(title,"URL server belum di set") }
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
                        PesanPeringatan.error(title,"Server Error ${response.statusCode()}")
                    }
                }
            } catch (ex: Exception){
                Platform.runLater {
                    PesanPeringatan.error(title,ex.message ?: "Gagal memeuat data UMKM")
                }
            }
        }.start()
    }
    fun cariDataUmkm(paramName: String, keyword: String) {
        if (clientController?.url.isNullOrBlank()) {
            Platform.runLater { PesanPeringatan.error(title,"URL server belum di set") }
            return
        }

        Thread {
            try {
                val uri = "${clientController?.url}/api/dataUmkm/cari?$paramName=${keyword}"
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .GET()
                    .header("Content-Type", "application/json")

                clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                val request = builder.build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() in 200..299) {
                    val hasil = json.decodeFromString<List<DataUmkmDTO>>(response.body())

                    Platform.runLater {
                        if (hasil.isEmpty()) {
                            // ⚠️ Kosongkan tabel jika tidak ada hasil
                            tblUmkm.items = FXCollections.observableArrayList()
                            clientController?.showInfo("Tidak ada data yang cocok untuk pencarian \"$keyword\"")
                        } else {
                            // ✅ Tampilkan hasil pencarian
                            tblUmkm.items = FXCollections.observableArrayList(hasil)
                        }
                    }
                } else {
                    Platform.runLater {
                        PesanPeringatan.error(title,"Server Error ${response.statusCode()}")
                        tblUmkm.items = FXCollections.observableArrayList() // kosongkan tabel juga
                    }
                }
            } catch (ex: Exception) {
                Platform.runLater {
                    PesanPeringatan.error(title,ex.message ?: "Gagal mencari data UMKM")
                    tblUmkm.items = FXCollections.observableArrayList() // kosongkan tabel jika error
                }
            }
        }.start()
    }
    fun pilihUMKM(){
        val selected = tblUmkm.selectionModel.selectedItem
        if (selected != null) {
            selectedUmkm = selected
            val stage = btnPilih.scene?.window as? Stage
            stage?.close()
        } else {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Peringatan"
            alert.headerText = "Tidak ada data yang dipilih"
            alert.contentText = "Silakan pilih salah satu UMKM dari tabel terlebih dahulu."
            alert.showAndWait()
        }
    }
    fun getUmkmTerpilih(): DataUmkmDTO? {
        return tblUmkm.selectionModel.selectedItem
    }
    fun umkmTerpilih(dto: DataUmkmDTO){
        txtNamaPemilik.text = dto.namaPemilik
        txtNamaUsaha.text = dto.namaUsaha
        txtKontak.text = dto.kontak
        txtInstagram.text = dto.instagram
        txtAlamat.text = dto.alamat
        btnSimpan.text = "Ubah"
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
            PesanPeringatan.error(title,"Semua field harus diisi!")
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
                            PesanPeringatan.error(title,"Server returned ${resp?.statusCode()} : ${resp?.body()}")
                        }
                    }
                } catch (ex: Exception) {
                    Platform.runLater {
                        PesanPeringatan.error(title,ex.message ?: "Error saat menyimpan data")
                    }
                }
            }.start()
        } else {
            val umkm = getUmkmTerpilih()
            val id = umkm?.id

            if (id==null) {
                PesanPeringatan.error(title,"ID UMKM tidak tersedia")
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
                            PesanPeringatan.error(title,"Server returned ${resp?.statusCode()} : ${resp?.body()}")
                        }
                    }
                } catch (ex: Exception) {
                    Platform.runLater {
                        PesanPeringatan.error(title,ex.message ?: "Error saat memperbarui data")
                    }
                }
            }.start()
        }
    }
}