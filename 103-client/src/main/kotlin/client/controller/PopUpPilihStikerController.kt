package client.controller

import client.DTO.DataStikerDTO
import client.DTO.DataUmkmDTO
import client.util.LocalDateTimeSerializer
import client.util.PesanPeringatan
import com.girsang.client.controller.MainClientAppController
import javafx.application.Platform
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

class PopUpPilihStikerController : Initializable{

    @FXML private lateinit var txtCariUMKM: TextField
    @FXML private lateinit var txtCariStiker: TextField

    @FXML private lateinit var btnRefresh: Button
    @FXML private lateinit var btnPilih: Button
    @FXML private lateinit var btnTutup: Button

    @FXML private lateinit var tblStiker: TableView<DataStikerDTO>
    @FXML private lateinit var kolKodeStiker: TableColumn<DataStikerDTO, String>
    @FXML private lateinit var kolNamaUmkm: TableColumn<DataStikerDTO, String>
    @FXML private lateinit var kolNamaStiker: TableColumn<DataStikerDTO, String>
    @FXML private lateinit var kolUkuran: TableColumn<DataStikerDTO, String>

    var selectedStiker: DataStikerDTO? = null
    var selectedUmkm: DataUmkmDTO? = null
    val title = "Data Stiker"
    private var searchThread: Thread? = null
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
    fun setData(list: List<DataStikerDTO>, umkm: DataUmkmDTO) {
        selectedUmkm = umkm
        tblStiker.items = FXCollections.observableArrayList(list)
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        kolKodeStiker.setCellValueFactory {SimpleStringProperty(it.value.kodeStiker)}
        kolNamaUmkm.setCellValueFactory { SimpleStringProperty(it.value.dataUmkm?.namaUsaha ?: "") }
        kolNamaStiker.setCellValueFactory {SimpleStringProperty(it.value.namaStiker)}
        kolUkuran.setCellValueFactory {SimpleStringProperty("${it.value.panjang} x ${it.value.lebar}")}

        setupSearchListener(txtCariUMKM, "namaUsaha")
        setupSearchListener(txtCariStiker, "namaStiker")

        btnRefresh.setOnAction {bersih()}
        btnTutup.setOnAction {
            bersih()
            val stage = btnTutup.scene?.window as? Stage
            stage?.close()
        }
        btnPilih.setOnAction {
            if(selectedStiker == null){
                PesanPeringatan.error("Pilih Stiker","Tidak ada data yang dipilih. Silakan pilih salah satu UMKM dari tabel terlebih dahulu.")
            }
            val stage = btnTutup.scene?.window as? Stage
            stage?.close()
        }

        tblStiker.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                selectedStiker = tblStiker.selectionModel.selectedItem
            }
        }
        tblStiker.setRowFactory {
            val row = javafx.scene.control.TableRow<DataStikerDTO>()
            row.setOnMouseClicked { event ->
                if (event.clickCount == 2 && !row.isEmpty) {
                    val stiker = row.item
                    selectedStiker = stiker

                    // Tutup popup
                    val stage = tblStiker.scene.window as? javafx.stage.Stage
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
        }
    }
    fun bersih(){

        selectedStiker = null

        txtCariUMKM.clear()
        txtCariStiker.clear()

        txtCariUMKM.promptText = "Cari Nama UMKM"
        txtCariStiker.promptText = "Cari Nama Stiker"


        tblStiker.selectionModel.clearSelection()

        loadDataStiker()
    }
    fun loadDataStiker(){
        if(clientController?.url.isNullOrBlank()){
            Platform.runLater { PesanPeringatan.error(title,"URL server belum di set") }
            return
        }
        Thread {
            try {
                val idUMKM = selectedUmkm?.id
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("${clientController?.url}/api/dataStiker/umkm/$idUMKM"))
                    .GET()
                    .header("Content-Type", "application/json")

                clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                val request = builder.build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if(response.statusCode() in 200..299){
                    val list = json.decodeFromString<List<DataStikerDTO>>(response.body())
                    Platform.runLater {
                        tblStiker.items = FXCollections.observableArrayList(list)
                    }
                } else {
                    Platform.runLater {
                        PesanPeringatan.error("Pilih Stiker", "Server Error ${response.statusCode()}")
                    }
                }
            } catch (ex: Exception){
                Platform.runLater {
                    PesanPeringatan.error("Pilih Stiker","Gagal memeuat data UMKM")
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
                        Platform.runLater { loadDataStiker() }
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
                val uri = "${clientController?.url}/api/dataStiker/cari?$paramName=${keyword}"
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .GET()
                    .header("Content-Type", "application/json")

                clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                val request = builder.build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() in 200..299) {
                    val hasil = json.decodeFromString<List<DataStikerDTO>>(response.body())

                    Platform.runLater {
                        if (hasil.isEmpty()) {
                            // ⚠️ Kosongkan tabel jika tidak ada hasil
                            tblStiker.items = FXCollections.observableArrayList()
                            PesanPeringatan.warning("Pilih Stiker", "Tidak ada data yang cocok untuk pencarian \"$keyword\"")
                        } else {
                            // ✅ Tampilkan hasil pencarian
                            tblStiker.items = FXCollections.observableArrayList(hasil)
                        }
                    }
                } else {
                    Platform.runLater {
                        PesanPeringatan.error("Pilih Stiker", "Server Error ${response.statusCode()}")
                        tblStiker.items = FXCollections.observableArrayList() // kosongkan tabel juga
                    }
                }
            } catch (ex: Exception) {
                Platform.runLater {
                    PesanPeringatan.error("Pilih Stiker", "Gagal mencari data UMKM")
                    tblStiker.items = FXCollections.observableArrayList() // kosongkan tabel jika error
                }
            }
        }.start()
    }

}