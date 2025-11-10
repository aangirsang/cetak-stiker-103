package client.controller

import client.DTO.DataUmkmDTO
import client.util.LocalDateTimeSerializer
import client.util.PesanPeringatan
import com.girsang.client.controller.MainClientAppController
import javafx.application.Platform
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.stage.Stage
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime
import java.util.ResourceBundle

class PopUpUMKMController : Initializable{

    @FXML private lateinit var txtCariNamaPemilik: TextField
    @FXML private lateinit var txtCariNamaUsaha: TextField
    @FXML private lateinit var txtCariAlamat: TextField

    @FXML private lateinit var btnRefresh: Button
    @FXML private lateinit var btnPilih: Button
    @FXML private lateinit var btnTutup: Button

    @FXML private lateinit var tblUmkm: TableView<DataUmkmDTO>
    @FXML private lateinit var kolId: TableColumn<DataUmkmDTO, Long>
    @FXML private lateinit var kolNamaPemilik: TableColumn<DataUmkmDTO, String>
    @FXML private lateinit var kolNamaUsaha: TableColumn<DataUmkmDTO, String>
    @FXML private lateinit var kolKontak: TableColumn<DataUmkmDTO, String>
    @FXML private lateinit var kolInstagram: TableColumn<DataUmkmDTO, String>
    @FXML private lateinit var kolAlamat: TableColumn<DataUmkmDTO, String>

    val title = "Data UMKM"
    var selectedUmkm: DataUmkmDTO? = null
    fun setData(list: List<DataUmkmDTO>) {
        tblUmkm.items = FXCollections.observableArrayList(list)
    }

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

    private var searchThread: Thread? = null

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        kolId.setCellValueFactory { SimpleLongProperty(it.value.id ?: 0).asObject()}
        kolNamaPemilik.setCellValueFactory {SimpleStringProperty(it.value.namaPemilik)}
        kolNamaUsaha.setCellValueFactory {SimpleStringProperty(it.value.namaUsaha)}
        kolKontak.setCellValueFactory {SimpleStringProperty(it.value.kontak)}
        kolInstagram.setCellValueFactory {SimpleStringProperty(it.value.instagram)}
        kolAlamat.setCellValueFactory {SimpleStringProperty(it.value.alamat)}

        setupSearchListener(txtCariNamaPemilik, "namaPemilik")
        setupSearchListener(txtCariNamaUsaha, "namaUsaha")
        setupSearchListener(txtCariAlamat, "alamat")

        tblUmkm.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                selectedUmkm = tblUmkm.selectionModel.selectedItem
            }
        }
        btnRefresh.setOnAction {bersih()}
        btnTutup.setOnAction {
            bersih()
            val stage = btnTutup.scene?.window as? Stage
            stage?.close()
        }
        btnPilih.setOnAction {
            if(selectedUmkm == null){
                PesanPeringatan.error(title,"Tidak ada data yang dipilih. Silakan pilih salah satu UMKM dari tabel terlebih dahulu.")
            }
            val stage = btnTutup.scene?.window as? Stage
            stage?.close()
        }
        tblUmkm.setRowFactory {
            val row = javafx.scene.control.TableRow< DataUmkmDTO>()
            row.setOnMouseClicked { event ->
                if (event.clickCount == 2 && !row.isEmpty) {
                    val umkm = row.item
                    selectedUmkm = umkm
                    println("🟢 Stiker dipilih lewat double-click: ${umkm.namaUsaha}")

                    // Tutup popup
                    val stage = tblUmkm.scene.window as? javafx.stage.Stage
                    stage?.close()
                }
            }
            row
        }
    }
    fun setClientController(controller: MainClientAppController) {
        this.clientController = controller  // ✅ simpan controller dulu

        if (!controller.url.isNullOrBlank()) {
            bersih() // ✅ baru panggil setelah URL diset
        } else {
            println("⚠️ URL belum di-set, data tidak bisa dimuat")
        }
    }
    fun bersih(){

        selectedUmkm = null

        tblUmkm.selectionModel.clearSelection()
        txtCariNamaPemilik.clear()
        txtCariNamaUsaha.clear()
        txtCariAlamat.clear()

        txtCariNamaPemilik.promptText = "Cari Nama Pemilik Usaha"
        txtCariNamaUsaha.promptText = "Cari Nama Usaha"
        txtCariAlamat.promptText = "Cari Alamat"

        loadDataUMKM()
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
        selectedUmkm = null
    }


}