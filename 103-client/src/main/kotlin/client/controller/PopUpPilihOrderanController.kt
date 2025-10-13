package client.controller

import client.DTO.DataStikerDTO
import client.DTO.DataUmkmDTO
import client.DTO.OrderanStikerDTO
import client.DTO.OrderanStikerRinciDTO
import client.util.LocalDateTimeSerializer
import com.girsang.client.controller.MainClientAppController
import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.DatePicker
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.stage.Stage
import javafx.util.StringConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.ResourceBundle

class PopUpPilihOrderanController : Initializable {

    @FXML private lateinit var txtCariFaktur: TextField
    @FXML private lateinit var txtCariNamaUMKM: TextField
    @FXML private lateinit var txtCariNamaStiker: TextField
    @FXML private lateinit var dpCariTangalOrder: DatePicker

    @FXML private lateinit var btnRefresh: Button
    @FXML private lateinit var btnPilih: Button
    @FXML private lateinit var btnTutup: Button

    @FXML private lateinit var tblOrderan: TableView<OrderanStikerDTO>
    @FXML private lateinit var kolFaktur: TableColumn<OrderanStikerDTO, String>
    @FXML private lateinit var kolTanggal: TableColumn<OrderanStikerDTO, String>
    @FXML private lateinit var kolNamaUMKM: TableColumn<OrderanStikerDTO, String>
    @FXML private lateinit var kolTotal: TableColumn<OrderanStikerDTO, Int>

    @FXML private lateinit var tblOrderanRinci: TableView<OrderanStikerRinciDTO>
    @FXML private lateinit var kolKodeStiker: TableColumn<OrderanStikerRinciDTO, String>
    @FXML private lateinit var kolNamaStiker: TableColumn<OrderanStikerRinciDTO, String>
    @FXML private lateinit var kolJumlah: TableColumn<OrderanStikerRinciDTO, Int>

    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))

    var selectedOrderan: OrderanStikerDTO? = null

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

    fun setClientController(controller: MainClientAppController) {
        this.clientController = controller
        if (!controller.url.isNullOrBlank()) {
            bersih() // ✅ baru panggil setelah URL diset
        } else {
            println("⚠️ URL belum di-set, data tidak bisa dimuat")
        }
    }
    fun setData(list: List<OrderanStikerDTO>) {
        tblOrderan.items = FXCollections.observableArrayList(list)
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {

        // ===== Table Orderan =====
        kolFaktur.setCellValueFactory { SimpleStringProperty(it.value.faktur) }
        kolTanggal.setCellValueFactory {
            SimpleStringProperty(it.value.tanggal?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ?: "")
        }
        kolNamaUMKM.setCellValueFactory { SimpleStringProperty(it.value.umkmNama) }
//        kolNamaUMKM.setCellValueFactory { cellData ->
//            val namaUmkm = cellData.value.umkm?.namaUsaha
//            println("DEBUG: UMKM = $namaUmkm") // debug, pastikan ada isinya
//            SimpleStringProperty(namaUmkm ?: "Tidak ada UMKM")
//        }
        kolTotal.setCellValueFactory { SimpleIntegerProperty(it.value.totalStiker).asObject() }

        // ===== Table Rinci =====

        kolKodeStiker.setCellValueFactory { SimpleStringProperty(it.value.kodeStiker) }
        kolNamaStiker.setCellValueFactory { SimpleStringProperty(it.value.stikerNama) }
        kolJumlah.setCellValueFactory { SimpleIntegerProperty(it.value.jumlah).asObject() }
        // ===== DatePicker Converter =====
        dpCariTangalOrder.converter = object : StringConverter<LocalDate>() {
            override fun toString(date: LocalDate?): String = date?.format(formatter) ?: ""
            override fun fromString(string: String?): LocalDate? = if (string.isNullOrBlank()) null else LocalDate.parse(string, formatter)
        }

        // ===== Selection Listener =====
        tblOrderan.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            selectedOrderan = tblOrderan.selectionModel.selectedItem
            loadDataOrderanRinci()
        }

        btnRefresh.setOnAction {bersih()}
        btnPilih.setOnAction {
            if(selectedOrderan == null){
                clientController?.showError("Tidak ada data yang dipilih. Silakan pilih salah satu UMKM dari tabel terlebih dahulu.")
            }
            val stage = btnTutup.scene?.window as? Stage
            stage?.close()
        }
        btnTutup.setOnAction {
            bersih()
            val stage = btnTutup.scene?.window as? Stage
            stage?.close()
        }
    }

    fun bersih() {
        selectedOrderan = null

        txtCariFaktur.clear()
        txtCariNamaUMKM.clear()
        txtCariNamaStiker.clear()
        dpCariTangalOrder.value = null

        txtCariFaktur.promptText = "Cari Faktur"
        txtCariNamaUMKM.promptText = "Cari Nama Usaha"
        txtCariNamaStiker.promptText = "Cari Nama Stiker"
        dpCariTangalOrder.promptText = "Tanggal Orderan"

        loadDataOrderan()
    }

    fun loadDataOrderan() {
        Thread {
            try {
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("${clientController?.url}/api/orderan-stiker"))
                    .GET()
                    .header("Content-Type", "application/json")
                clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }
                val request = builder.build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() in 200..299) {
                    val list = json.decodeFromString<List<OrderanStikerDTO>>(response.body())
                    Platform.runLater { tblOrderan.items = FXCollections.observableArrayList(list) }
                } else Platform.runLater { clientController?.showError("Server Error ${response.statusCode()}") }

            } catch (ex: Exception) {
                Platform.runLater { clientController?.showError(ex.message ?: "Gagal memuat data orderan stiker") }
            }
        }.start()
    }

    fun loadDataOrderanRinci() {
        val orderan = selectedOrderan ?: run {
            tblOrderanRinci.items.clear()
            return
        }
        tblOrderanRinci.items = FXCollections.observableArrayList(orderan.rincian)
    }
}
