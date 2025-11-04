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
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import kotlinx.serialization.encodeToString
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
import java.util.ResourceBundle

class DataStikerController : Initializable {

    @FXML private lateinit var txtNamaUsaha: TextField
    @FXML private lateinit var txtNamaPemilik: TextField
    @FXML private lateinit var txtInstagram: TextField
    @FXML private lateinit var txtKodeStiker: TextField
    @FXML private lateinit var txtNamaStiker: TextField
    @FXML private lateinit var txtPanjang: TextField
    @FXML private lateinit var txtKontak: TextField
    @FXML private lateinit var txtLebar: TextField
    @FXML private lateinit var txtCatatan: TextArea
    @FXML private lateinit var txtCariUMKM: TextField
    @FXML private lateinit var txtCariStiker: TextField
    @FXML private lateinit var txtPembuatan: TextField
    @FXML private lateinit var txtPerubahan: TextField

    @FXML private lateinit var btnCariUMKM: Button
    @FXML private lateinit var btnSimpan: Button
    @FXML private lateinit var btnHapus: Button
    @FXML private lateinit var btnRefresh: Button
    @FXML private lateinit var btnTutup: Button


    @FXML private lateinit var tblStiker: TableView<DataStikerDTO>
    @FXML private lateinit var kolKodeStiker: TableColumn<DataStikerDTO, String>
    @FXML private lateinit var kolNamaUmkm: TableColumn<DataStikerDTO, String>
    @FXML private lateinit var kolNamaStiker: TableColumn<DataStikerDTO, String>
    @FXML private lateinit var kolUkuran: TableColumn<DataStikerDTO, String>

    private var clientController: MainClientAppController? = null
    private var parentController: MainClientAppController? = null

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

    private var selectedUmkm: DataUmkmDTO? = null
    private var selectedStiker: DataStikerDTO? = null

    override fun initialize(p0: URL?, p1: ResourceBundle?) {

        kolKodeStiker.setCellValueFactory {SimpleStringProperty(it.value.kodeStiker)}
        kolNamaUmkm.setCellValueFactory { SimpleStringProperty(it.value.dataUmkm?.namaUsaha ?: "") }
        kolNamaStiker.setCellValueFactory {SimpleStringProperty(it.value.namaStiker)}
        kolUkuran.setCellValueFactory {SimpleStringProperty("${it.value.panjang} x ${it.value.lebar}")}

        btnTutup.setOnAction { parentController?.tutupForm() }
        btnRefresh.setOnAction { bersih() }
        btnSimpan.setOnAction { simpanStiker() }
        btnHapus.setOnAction { hapusStiker() }
        btnCariUMKM.setOnAction {showCariUmkmPopup()}

        txtNamaPemilik.isEditable = false
        txtNamaUsaha.isEditable = false
        txtInstagram.isEditable = false
        txtKontak.isEditable = false
        txtKodeStiker.isEditable = false
        txtPembuatan.isEditable = false
        txtPerubahan.isEditable = false

        setupSearchListener(txtCariUMKM)
        setupSearchListener(txtCariStiker)

        txtPanjang.textFormatter = TextFormatter<String> { change ->
            if (change.controlNewText.matches(Regex("\\d*"))) change else null
        }
        txtLebar.textFormatter = TextFormatter<String> { change ->
            if (change.controlNewText.matches(Regex("\\d*"))) change else null
        }
        tblStiker.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                stikerTerpilih(newValue)
            }
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
    fun setParentController(controller: MainClientAppController) {
        this.parentController = controller
    }
    fun cariDataUmkm(namaStiker: String, namaUsaha: String) {
        if (clientController?.url.isNullOrBlank()) {
            Platform.runLater {
                PesanPeringatan.error("Data Stiker", "URL server belum di set")
            }
            return
        }

        Thread {
            try {
                val uri = "${clientController?.url}/api/dataStiker/cari?namaStiker=${namaStiker}&namaUsaha=${namaUsaha}"
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
                            PesanPeringatan.warning("Data Stiker", "Tidak ada data yang cocok untuk pencarian \"$namaStiker\" dan \"$namaUsaha\"")
                        } else {
                            // ✅ Tampilkan hasil pencarian
                            tblStiker.items = FXCollections.observableArrayList(hasil)
                        }
                    }
                } else {
                    Platform.runLater {
                        PesanPeringatan.error("Data Stiker", "Server Error ${response.statusCode()}")
                        tblStiker.items = FXCollections.observableArrayList() // kosongkan tabel juga
                    }
                }
            } catch (ex: Exception) {
                Platform.runLater {
                    PesanPeringatan.error("Data Stiker", ex.message ?: "Gagal mencari data UMKM")
                    tblStiker.items = FXCollections.observableArrayList() // kosongkan tabel jika error
                }
            }
        }.start()
    }
    private fun setupSearchListener(textFiled: TextField) {
        textFiled.textProperty().addListener { _, _, newValue ->
            searchThread?.interrupt() // hentikan thread sebelumnya jika user masih mengetik
            searchThread = Thread {
                try {
                    Thread.sleep(300) // debounce 300ms
                    if (Thread.interrupted()) return@Thread

                    if (newValue.isNullOrBlank()) {
                        Platform.runLater { loadDataStiker() }
                    } else {
                        cariDataUmkm(txtCariStiker.text, txtCariUMKM.text)
                    }
                } catch (_: InterruptedException) {
                }
            }
            searchThread?.start()
        }
    }
    fun bersih(){
        selectedUmkm = null
        selectedStiker = null

        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
        txtPembuatan.text = LocalDate.now().format(formatter)
        txtPerubahan.text = LocalDate.now().format(formatter)

        txtNamaUsaha.clear()
        txtNamaPemilik.clear()
        txtInstagram.clear()
        txtKodeStiker.clear()
        txtNamaStiker.clear()
        txtPanjang.clear()
        txtKontak.clear()
        txtLebar.clear()
        txtCatatan.clear()
        txtCariUMKM.clear()
        txtCariStiker.clear()

        txtNamaUsaha.promptText = "Nama Usaha"
        txtNamaPemilik.promptText = "Nama Pemilik Usaha"
        txtInstagram.promptText = "Akun Instagram Usaha"
        txtKodeStiker.promptText = "Kode Stiker"
        txtNamaStiker.promptText = "Nama Stiker"
        txtPanjang.promptText = "Panjang Stiker"
        txtKontak.promptText = "Kontak UMKMr"
        txtLebar.promptText = "Lebar Stiker"
        txtCatatan.promptText = "Catatan Stiker"
        txtCariUMKM.promptText = "Cari Nama UMKM"
        txtCariStiker.promptText = "Cari Nama Stiker"

        btnSimpan.text = "Simpan"
        btnCariUMKM.isDisable = false

        tblStiker.selectionModel.clearSelection()

        loadDataStiker()
    }
    fun loadDataStiker(){
        if(clientController?.url.isNullOrBlank()){
            Platform.runLater { PesanPeringatan.error("Data Stiker", "URL server belum di set")
            }
            return
        }
        Thread {
            try {
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("${clientController?.url}/api/dataStiker"))
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
                        PesanPeringatan.warning("Data Stiker", "Server Error ${response.statusCode()}")
                    }
                }
            } catch (ex: Exception){
                Platform.runLater {
                    PesanPeringatan.warning("Data Stiker", ex.message ?: "Gagal memeuat data UMKM")
                }
            }
        }.start()
    }
    fun showCariUmkmPopup() {
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
            }

        } catch (e: Exception) {
            PesanPeringatan.error("Data Stiker", "Error: ${e.message}")
        }
    }
    fun umkmTerpilih(dto: DataUmkmDTO){
        selectedUmkm = dto
        txtNamaUsaha.text = dto.namaUsaha
        txtNamaPemilik.text = dto.namaPemilik
        txtInstagram.text = dto.instagram
        txtKontak.text = dto.kontak
    }
    fun stikerTerpilih(dto: DataStikerDTO){
        // Cek apakah dataUmkm tersedia
        dto.dataUmkm?.let {
            umkmTerpilih(it) // isi field UMKM
        } ?: run {
            // Jika null, tampilkan error tapi tetap isi field lain
            PesanPeringatan.error("Data Stiker", "Data UMKM untuk stiker ini tidak tersedia!")
        }

        // Isi field stiker
        txtKodeStiker.text = dto.kodeStiker
        txtNamaStiker.text = dto.namaStiker
        txtPanjang.text = dto.panjang.toString()
        txtLebar.text = dto.lebar.toString()
        txtCatatan.text = dto.catatan

        // Format tanggal pembuatan dan perubahan, aman jika null
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
        txtPembuatan.text = dto.tglPembuatan.format(formatter) ?: ""
        txtPerubahan.text = dto.tglPerubahan.format(formatter) ?: ""

        // Ubah tombol menjadi "Ubah" agar tahu user sedang edit
        btnSimpan.text = "Ubah"

        // Simpan DTO yang sedang dipilih supaya bisa update nanti
        selectedStiker = dto

        btnCariUMKM.isDisable = true

    }
    fun getStikerTerpilih(): DataStikerDTO?{
        return tblStiker.selectionModel.selectedItem
    }
    fun simpanStiker(){
        val umkm = selectedUmkm
        val namaStiker = txtNamaStiker.text.trim()
        val panjang = txtPanjang.text.toIntOrNull() ?: 0
        val lebar = txtLebar.text.toIntOrNull() ?: 0
        val catatan = txtCatatan.text.trim()
        if(selectedUmkm == null){
            PesanPeringatan.error("Data Stiker", "Data UMKM belum dipilih!")
            return
        } else if(namaStiker.isEmpty()||
            txtPanjang.text == ""||
            txtLebar.text == ""){
            PesanPeringatan.error("Data Stiker", "Data stiker belum lengkap!")
            return
        }
        if(btnSimpan.text == "Simpan"){
            Thread{
                try {
                    val stiker = DataStikerDTO (
                        dataUmkmId = umkm?.id,
                        namaStiker = namaStiker,
                        panjang = panjang,
                        lebar = lebar,
                        catatan = catatan,
                    )
                    val body = json.encodeToString(stiker)
                    val builder = HttpRequest.newBuilder()
                        .uri(URI.create("${clientController?.url}/api/dataStiker"))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .header("Content-Type", "application/json")

                    clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                    val req = builder.build()
                    val resp = clientController?.makeRequest(req)

                    if(resp?.statusCode() in 200..299){
                        Platform.runLater {
                            PesanPeringatan.info("Data Stiker", "Data stiker berhasil disimpan.")
                            bersih()
                        }
                    }else {
                        Platform.runLater {
                            PesanPeringatan.error("Data Stiker", "Server returned ${resp?.statusCode()} : ${resp?.body()}")
                        }
                    }
                } catch (ex: Exception) {
                    Platform.runLater {
                        PesanPeringatan.error("Data Stiker", ex.message ?: "Error saat menyimpan data")
                    }
                }
            }.start()
        } else if(btnSimpan.text == "Ubah"){
            val stiker = getStikerTerpilih()
            val id = stiker?.id

            if (id==null) {
                PesanPeringatan.error("Data Stiker", "ID Stiker tidak tersedia")
                return
            }
            val konfirm = PesanPeringatan.confirm("Data Stiker","Apakah anda yakin ingin merubah data ini?")
            if (konfirm) {
                Thread {
                    try {
                        val stikerDTO = DataStikerDTO(
                            id = id,
                            dataUmkmId = umkm?.id,
                            namaStiker = namaStiker,
                            panjang = panjang,
                            lebar = lebar,
                            catatan = catatan,
                        )
                        val body = json.encodeToString(stikerDTO)
                        val builder = HttpRequest.newBuilder()
                            .uri(URI.create("${clientController?.url}/api/dataStiker/${id}"))
                            .PUT(HttpRequest.BodyPublishers.ofString(body))
                            .header("Content-Type", "application/json")

                        clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                        val req = builder.build()
                        val resp = clientController?.makeRequest(req)

                        if (resp?.statusCode() in 200..299) {
                            Platform.runLater {
                                PesanPeringatan.info("Data Stiker", "Data stiker berhasil diubah.")
                                bersih()
                            }
                        } else {
                            Platform.runLater {
                                println("Server returned ${resp?.statusCode()} : ${resp?.body()}")
                                PesanPeringatan.error("Simpan Data","Server returned ${resp?.statusCode()} : ${resp?.body()}")
                            }
                        }
                    } catch (ex: Exception) {
                        Platform.runLater {
                            PesanPeringatan.error("Simpan Data",ex.message ?:"Error saat memperbarui data" )
                        }
                    }
                }.start()
            }
        }
    }
    fun hapusStiker(){
        val stiker = getStikerTerpilih()
        val id = stiker?.id
        if (stiker == null) {
            PesanPeringatan.error("Hapus Data", "Tidak ada data stiker yang dipilih.")
            return
        }
        val konfirm = PesanPeringatan.confirm("Hapus Data","Anda yakin ingin menghapus data ini?")
        if (konfirm) {
            Thread {
                try {
                    val builder = HttpRequest.newBuilder()
                        .uri(URI.create("${clientController?.url}/api/dataStiker/${id}"))
                        .DELETE()

                    clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                    val request = builder.build()
                    val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                    Platform.runLater {
                        if (response.statusCode() in 200..299) {
                            PesanPeringatan.info("Hapus Data", "Data stiker berhasil dihapus.")
                            bersih()
                        } else {
                            val body = response.body()
                            PesanPeringatan.error("Hapus Data", body)
                        }

                    }
                } catch (ex: Exception) {
                    Platform.runLater {
                        PesanPeringatan.error("Hapus Data", ex.message ?: "Gagal menghapus data Stiker")
                    }
                }
            }.start()
        }
    }


}