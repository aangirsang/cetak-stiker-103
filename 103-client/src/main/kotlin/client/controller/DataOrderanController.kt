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
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.cell.TextFieldTableCell
import javafx.util.converter.IntegerStringConverter
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ResourceBundle

class DataOrderanController : Initializable {

    private var selectedUmkm: DataUmkmDTO? = null
    private var selectedStiker: DataStikerDTO? = null
    private var selectedOrder: OrderanStikerDTO? = null
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

    @FXML private lateinit var lblTotalStiker: Label
    @FXML private lateinit var txtNamaUsaha: TextField
    @FXML private lateinit var txtNamaPemilik: TextField
    @FXML private lateinit var txtInstagram: TextField
    @FXML private lateinit var txtKodeStiker: TextField
    @FXML private lateinit var txtNamaStiker: TextField
    @FXML private lateinit var txtUkuranStiker: TextField
    @FXML private lateinit var txtJumlahStiker: TextField
    @FXML private lateinit var txtTanggal: TextField
    @FXML private lateinit var txtFaktur: TextField
    @FXML private lateinit var txtKontak: TextField

    @FXML private lateinit var btnCariUMKM: Button
    @FXML private lateinit var btnTambahkan: Button
    @FXML private lateinit var btnSimpan: Button
    @FXML private lateinit var btnHapus: Button
    @FXML private lateinit var btnRefresh: Button
    @FXML private lateinit var btnTutup: Button

    @FXML private lateinit var tblStiker: TableView<OrderanStikerRinciDTO>
    @FXML private lateinit var kolKodeStiker: TableColumn<OrderanStikerRinciDTO, String>
    @FXML private lateinit var kolNamaStiker: TableColumn<OrderanStikerRinciDTO, String>
    @FXML private lateinit var kolUkuran: TableColumn<OrderanStikerRinciDTO, String>
    @FXML private lateinit var kolJumlah: TableColumn<OrderanStikerRinciDTO, Int>

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        kolKodeStiker.setCellValueFactory { SimpleStringProperty(it.value.stiker?.kodeStiker ?: "") }
        kolNamaStiker.setCellValueFactory { SimpleStringProperty(it.value.stiker?.namaStiker ?: "") }
        kolUkuran.setCellValueFactory {
            val stiker = it.value.stiker
            SimpleStringProperty(if (stiker != null) "${stiker.panjang} x ${stiker.lebar}" else "")
        }
        tblStiker.isEditable = true
        kolJumlah.setCellValueFactory { SimpleIntegerProperty(it.value.jumlah).asObject() }
        kolJumlah.cellFactory = TextFieldTableCell.forTableColumn(IntegerStringConverter())

        btnCariUMKM.setOnAction {showCariUmkmPopup()}
        btnRefresh.setOnAction { bersih() }
        btnTambahkan.setOnAction { tambahStiker() }
        btnSimpan.setOnAction { onSimpanOrderan() }
        btnTutup.setOnAction { parentController?.tutupForm() }

        txtKodeStiker.setOnMouseClicked { event ->
            if (event.clickCount == 2) {
                showCariStikerPopup()
            }
        }

        txtFaktur.setOnMouseClicked { event ->
            if (event.clickCount == 2) {
                showCariOrderaPopup()
            }
        }

        kolJumlah.setOnEditCommit { event ->
            var stiker = event.rowValue
            val nilaiBaru = event.newValue ?: 0

            if (nilaiBaru <= 0) {
                // ⚠️ Hapus item dari tabel
                event.tableView.items.remove(stiker)
                println("❌ Baris dihapus karena jumlah <= 0")
            } else {
                // ✅ Update jumlah dan refresh tampilan
                stiker.jumlah = nilaiBaru
                event.tableView.refresh()
                println("Jumlah diubah jadi: $nilaiBaru")
            }

            // 🔹 Hitung ulang total setelah setiap edit
            hitungTotalStiker()
        }

    }
    fun bersih(){
        selectedUmkm = null
        selectedOrder = null
        selectedStiker = null
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
        txtTanggal.text = LocalDate.now().format(formatter)

        lblTotalStiker.text = "Total Stiker = 0"
        txtNamaUsaha.clear()
        txtNamaPemilik.clear()
        txtInstagram.clear()
        txtKodeStiker.clear()
        txtNamaStiker.clear()
        txtUkuranStiker.clear()
        txtJumlahStiker.clear()
        txtFaktur.clear()
        txtKontak.clear()

        txtNamaUsaha.promptText = "Nama Usaha"
        txtNamaPemilik.promptText = "Nama Pemilik Usaha"
        txtInstagram.promptText = "Akun Instagram Usaha"
        txtKodeStiker.promptText = "Kode Stiker"
        txtNamaStiker.promptText = "Nama Stiker"
        txtUkuranStiker.promptText = "Ukuran Stiker"
        txtJumlahStiker.promptText = "Jumlah Stiker"
        txtKontak.promptText = "Kontak Pemilik Usaha"
        txtFaktur.clear()

        btnSimpan.text = "Simpan"

        tblStiker.selectionModel.clearSelection()
        fakturOtomatis()
        tblStiker.items.clear()
    }
    fun umkmTerpilih(dto: DataUmkmDTO){
        selectedUmkm = dto
        txtNamaUsaha.text = dto.namaUsaha
        txtNamaPemilik.text = dto.namaPemilik
        txtInstagram.text = dto.instagram
        txtKontak.text = dto.kontak
    }
    fun stikerTerpilih(dto: DataStikerDTO){
        selectedStiker = dto
        txtKodeStiker.text = selectedStiker?.kodeStiker
        txtNamaStiker.text = selectedStiker?.namaStiker
        txtUkuranStiker.text = "${selectedStiker?.panjang} x ${selectedStiker?.lebar}"
    }
    fun showCariUmkmPopup() {
        selectedUmkm = null
        try {
            // 🔹 Ambil data dari server
            val builder = HttpRequest.newBuilder()
                .uri(URI.create("${clientController?.url}/api/dataUmkm"))
                .GET()
                .header("Content-Type", "application/json")

            clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }
            val request = builder.build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                clientController?.showError("Gagal memuat data UMKM (${response.statusCode()})")
                return
            }

            val list = json.decodeFromString<List<DataUmkmDTO>>(response.body())

            // 🔹 Muat FXML popup
            val loader = javafx.fxml.FXMLLoader(javaClass.getResource("/fxml/popup-pilih-umkm.fxml"))
            val root = loader.load<javafx.scene.Parent>()
            val controller = loader.getController<PopUpUMKMController>()
            controller.setClientController(clientController!!)
            controller.setData(list)

            val stage = javafx.stage.Stage()
            stage.title = "Pilih Data UMKM"
            stage.scene = javafx.scene.Scene(root)
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL)
            stage.showAndWait()

            // 🔹 Setelah popup ditutup
            val selected = controller.selectedUmkm
            if (selected != null) {
                umkmTerpilih(selected)
                println("🟢 UMKM ID = ${selectedUmkm?.id}")
                println("UMKM yang dipilih ${selectedUmkm?.namaUsaha}")
            }

        } catch (e: Exception) {
            clientController?.showError("Error: ${e.message}")
        }
    }
    fun showCariOrderaPopup() {
        try {
            // 🔹 Ambil data orderan dari server
            val builder = HttpRequest.newBuilder()
                .uri(URI.create("${clientController?.url}/api/orderan-stiker"))
                .GET()
                .header("Content-Type", "application/json")

            clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

            val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                clientController?.showError("Gagal memuat data orderan (${response.statusCode()})")
                return
            }

            // 🔹 Cetak response untuk debug
            println("Response orderan: ${response.body()}")

            val list = json.decodeFromString<List<OrderanStikerDTO>>(response.body())

            // 🔹 Muat FXML popup dengan pengecekan null
            val fxmlUrl = javaClass.getResource("/fxml/popup-pilih-orderan.fxml")
            println("FXML URL = $fxmlUrl")

            if (fxmlUrl == null) {
                clientController?.showError("FXML popup-pilih-orderan.fxml tidak ditemukan!")
                return
            }

            val loader = javafx.fxml.FXMLLoader(fxmlUrl)
            val root = loader.load<javafx.scene.Parent>()

            val controller = loader.getController<PopUpPilihOrderanController>()
            if (controller == null) {
                clientController?.showError("Controller PopUpPilihOrderanController tidak ditemukan di FXML!")
                return
            }

            controller.setClientController(clientController!!)
            controller.setData(list) // pastikan method setData ada di PopUpPilihOrderanController

            val stage = javafx.stage.Stage()
            stage.title = "Pilih Data Orderan"
            stage.scene = javafx.scene.Scene(root)
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL)
            stage.showAndWait()

            // 🔹 Setelah popup ditutup
            val selected = controller.selectedOrderan
            if (selected != null) {
                selectedOrder = selected
                selectedUmkm = selected.umkm
                umkmTerpilih(selected.umkm!!)
                loadDataOrderRinci(selected)

                txtFaktur.text = selected.faktur
                val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
                txtTanggal.text = selected.tanggal.format(formatter)
                lblTotalStiker.text = "Total Stiker = ${selected.totalStiker} Lembar"
                btnSimpan.text = "Update"
            }

        } catch (e: Exception) {
            // Cetak stack trace lengkap
            e.printStackTrace()
            clientController?.showError("Error PopUp Orderan: ${e.message ?: "Unknown"}")
        }
    }
    fun showCariStikerPopup() {
        try {
            // 🔹 Ambil data dari server
            if(selectedUmkm==null){
                clientController?.showError("Pilih UMKM terlebih dahulu!")
                return
            }
            val idUMKM = selectedUmkm?.id
            println("${clientController?.url}/api/dataStiker/umkm/$idUMKM")
            val builder = HttpRequest.newBuilder()
                .uri(URI.create("${clientController?.url}/api/dataStiker/umkm/$idUMKM"))
                .GET()
                .header("Content-Type", "application/json")

            clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }
            val request = builder.build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                println("Gagal memuat data stiker (${response.statusCode()})")
                clientController?.showError("Gagal memuat data stiker (${response.statusCode()})")
                return
            }

            val list = json.decodeFromString<List<DataStikerDTO>>(response.body())

            // 🔹 Muat FXML popup
            val loader = javafx.fxml.FXMLLoader(javaClass.getResource("/fxml/popup-pilih-stiker.fxml"))
            val root = loader.load<javafx.scene.Parent>()
            val controller = loader.getController<PopUpPilihStikerController>()
            controller.setClientController(clientController!!)
            controller.setData(list, selectedUmkm!!)

            val stage = javafx.stage.Stage()
            stage.title = "Pilih Data Stiker"
            stage.scene = javafx.scene.Scene(root)
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL)
            stage.showAndWait()

            // 🔹 Setelah popup ditutup
            val selected = controller.selectedStiker
            if (selected != null) {
                stikerTerpilih(selected)
                println("Stiker yang dipilih ${selectedUmkm?.namaUsaha}")
            }

        } catch (e: Exception) {
            println("Error: ${e.message}")
            clientController?.showError("Error: ${e.message}")
        }
    }
    fun loadDataOrderRinci(order: OrderanStikerDTO){
        if(order == null){
            tblStiker.items.clear()
        } else {
            Thread {
                try {
                    val id = order.id
                    val builder = HttpRequest.newBuilder()
                        .uri(URI.create("${clientController?.url}/api/orderan-stiker/$id"))
                        .GET()
                        .header("Content-Type", "application/json")

                    clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                    val request = builder.build()
                    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                    if (response.statusCode() in 200..299) {
                        val list = json.decodeFromString<List<OrderanStikerRinciDTO>>(response.body())
                        Platform.runLater {
                            tblStiker.items = FXCollections.observableArrayList(list)
                        }
                    } else {
                        Platform.runLater {
                            println("Server Error ${response.statusCode()}")
                            clientController?.showError("Server Error ${response.statusCode()}")
                        }
                    }
                } catch (ex: Exception) {
                    Platform.runLater {
                        println(ex.message ?: "Gagal memeuat data UMKM")
                        clientController?.showError(ex.message ?: "Gagal memeuat data UMKM")
                    }
                }
            }.start()
        }
    }
    fun fakturOtomatis(){
        Thread {
            try {
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("${clientController?.url}/api/orderan-stiker/nextFaktur"))
                    .GET()
                    .header("Content-Type", "application/json")

                // Tambahkan Authorization header jika diperlukan
                clientController?.buildAuthHeader()?.let {
                    builder.header("Authorization", it)
                }

                val request = builder.build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() in 200..299) {
                    val faktur = response.body().trim()
                    Platform.runLater {
                        txtFaktur.text = faktur
                    }
                } else if (response.statusCode() == 401) {
                    Platform.runLater {
                        clientController?.showError("Akses ditolak (401). Silakan login terlebih dahulu.")
                    }
                } else {
                    Platform.runLater {
                        clientController?.showError("Server Error ${response.statusCode()}")
                    }
                }

            } catch (ex: Exception) {
                Platform.runLater {
                    clientController?.showError(ex.message ?: "Gagal mengambil nomor faktur otomatis")
                }
            }
        }.start()
    }
    fun tambahStiker(){
        if (selectedStiker == null) {
            clientController?.showError("Pilih stiker terlebih dahulu!")
            return
        }
        if (selectedUmkm == null) {
            clientController?.showError("Pilih UMKM terlebih dahulu!")
            return
        }
        val jumlah = txtJumlahStiker.text.toIntOrNull()
        if (jumlah == null || jumlah <= 0) {
            clientController?.showError("Masukkan jumlah stiker yang valid!")
            return
        }

        val currentItems = tblStiker.items ?: FXCollections.observableArrayList()

        // Cek apakah stiker sudah ada
        val existing = currentItems.find { it.stiker?.id == selectedStiker?.id }
        if (existing != null) {
            // Jika sudah ada, tambahkan jumlahnya
            existing.jumlah += jumlah
        } else {
            // Jika belum ada, buat baris baru
            val rinci = OrderanStikerRinciDTO(
                id = null,
                stiker = selectedStiker!!,
                kodeStiker = selectedStiker!!.kodeStiker,
                stikerNama = selectedStiker!!.namaStiker,
                jumlah = jumlah
            )
            currentItems.add(rinci)
        }

        tblStiker.items = currentItems
        tblStiker.refresh()
        hitungTotalStiker()

        // Bersihkan input
        txtKodeStiker.clear()
        txtNamaStiker.clear()
        txtUkuranStiker.clear()
        txtJumlahStiker.clear()
        selectedStiker = null
    }
    fun hapusStiker(){
        val selected = tblStiker.selectionModel.selectedItem
        if (selected == null) {
            clientController?.showError("Pilih stiker yang ingin dihapus!")
            return
        }

        tblStiker.items.remove(selected)
        hitungTotalStiker()
    }
    fun hitungTotalStiker() {
        val total = tblStiker.items.sumOf { it.jumlah }
        lblTotalStiker.text = "Total Stiker = $total Lembar"
    }
    fun onSimpanOrderan() {
        if (selectedUmkm == null) {
            clientController?.showError("Pilih UMKM terlebih dahulu!")
            return
        }
        if (tblStiker.items.isEmpty()) {
            clientController?.showError("Belum ada stiker yang ditambahkan!")
            return
        }

        val orderan = OrderanStikerDTO(
            id = null,
            faktur = txtFaktur.text,
            tanggal = LocalDateTime.now(),
            umkm = DataUmkmDTO(id = selectedUmkm!!.id),
            totalStiker = tblStiker.items.sumOf { it.jumlah },
            rincian = tblStiker.items.map {
                OrderanStikerRinciDTO(
                    id = null,
                    stiker = DataStikerDTO(id = it.stiker?.id),
                    kodeStiker = it.stiker?.kodeStiker ?: "",
                    stikerNama = it.stiker?.namaStiker ?: "",
                    jumlah = it.jumlah
                )
            }
        )

        Thread {
            try {
                val body = json.encodeToString(orderan)
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("${clientController?.url}/api/orderan-stiker"))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/json")

                clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                val req = builder.build()
                val resp = clientController?.makeRequest(req)
                if (resp?.statusCode() in 200..299) {
                    Platform.runLater {
                        clientController?.showInfo("Orderan berhasil disimpan!")
                        bersih()
                    }
                } else {
                    Platform.runLater {
                        println("Gagal menyimpan orderan: ${resp?.statusCode()}")
                        clientController?.showError("Gagal menyimpan orderan: ${resp?.statusCode()}")
                    }
                }
            } catch (e: Exception) {
                Platform.runLater {
                    clientController?.showError("Error: ${e.message}")
                }
            }
        }.start()
    }
}