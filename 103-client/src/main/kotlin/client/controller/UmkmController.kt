package client.controller

import client.DTO.DataStikerDTO
import client.DTO.DataUmkmDTO
import client.util.LocalDateTimeSerializer
import com.girsang.client.controller.MainClientAppController
import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.DatePicker
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ResourceBundle

class UmkmController : Initializable{

    private val client = HttpClient.newBuilder().build()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        serializersModule = SerializersModule {
            contextual(LocalDateTime::class, LocalDateTimeSerializer)
        }
    }


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

    @FXML private lateinit var txtStikerKode: TextField
    @FXML private lateinit var txtStikerNama: TextField
    @FXML private lateinit var txtStikerPanjang: TextField
    @FXML private lateinit var txtStikerLebar: TextField
    @FXML private lateinit var txtStikerCatatan: TextArea
    @FXML private lateinit var txtStikerDicetak: TextField
    @FXML private lateinit var dpStikerPembuatan: DatePicker
    @FXML private lateinit var dpStikerPerubahan: DatePicker

    @FXML private lateinit var btnStikerTambah: Button
    @FXML private lateinit var btnStikerHapus: Button

    @FXML private lateinit var tblStiker: TableView<DataStikerDTO>
    @FXML private lateinit var kolStikerNamaUMKM: TableColumn<DataStikerDTO, String>
    @FXML private lateinit var kolStikerKodeStiker: TableColumn<DataStikerDTO, String>
    @FXML private lateinit var kolStikerPanjang: TableColumn<DataStikerDTO, Int>
    @FXML private lateinit var kolStikerLebar: TableColumn<DataStikerDTO, Int>

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

        //Tabel UMKM
        kolId.setCellValueFactory { SimpleLongProperty(it.value.id ?: 0).asObject()}
        kolNamaPemilik.setCellValueFactory {SimpleStringProperty(it.value.namaPemilik)}
        kolNamaUsaha.setCellValueFactory {SimpleStringProperty(it.value.namaUsaha)}
        kolKontak.setCellValueFactory {SimpleStringProperty(it.value.kontak)}
        kolInstagram.setCellValueFactory {SimpleStringProperty(it.value.instagram)}
        kolAlamat.setCellValueFactory {SimpleStringProperty(it.value.alamat)}

        //Tabel Stiker
        kolStikerNamaUMKM.setCellValueFactory {
            SimpleStringProperty(it.value.dataUmkm?.namaUsaha ?: "")
        }
        kolStikerKodeStiker.setCellValueFactory {SimpleStringProperty(it.value.kodeStiker)}
        kolStikerPanjang.setCellValueFactory { SimpleIntegerProperty(it.value.panjang).asObject() }
        kolStikerLebar.setCellValueFactory { SimpleIntegerProperty(it.value.lebar).asObject() }

        btnTutup.setOnAction { parentController?.tutupForm() }
        btnRefresh.setOnAction { bersih() }
        btnSimpan.setOnAction { simpanDataUmkm() }
        btnHapus.setOnAction { hapusData() }
        btnStikerTambah.setOnAction { tambahStikerUntukUmkm() }

        tblUmkm.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                umkmTerpilih(newValue)
            }
        }

        setupSearchListener(txtCariNamaPemilik, "namaPemilik")
        setupSearchListener(txtCariNamaUsaha, "namaUsaha")
        setupSearchListener(txtCariAlamat, "alamat")

        tblUmkm.columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY

        tblUmkm.widthProperty().addListener { _, _, newWidth ->
            val w = newWidth.toDouble() - 20 // padding kecil biar gak scroll
            kolId.prefWidth = w * 0.04
            kolNamaPemilik.prefWidth = w * 0.172
            kolNamaUsaha.prefWidth = w * 0.2
            kolKontak.prefWidth = w * 0.10
            kolInstagram.prefWidth = w * 0.15
            kolAlamat.prefWidth = w * 0.35
        }
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

    fun bersih(){
        txtNamaPemilik.clear()
        txtNamaUsaha.clear()
        txtKontak.clear()
        txtInstagram.clear()
        txtAlamat.clear()
        txtCariNamaPemilik.clear()
        txtCariNamaUsaha.clear()
        txtCariAlamat.clear()

        txtNamaPemilik.promptText = "Data Nama Pemilik Usaha"
        txtNamaUsaha.promptText = "Data Nama Usaha"
        txtKontak.promptText = "Data Kontak No. Handphone atau WhatsApp"
        txtInstagram.promptText = "Data Akun Instagram"
        txtAlamat.promptText = "Data Alamat"
        txtCariNamaPemilik.promptText = "Cari Nama Pemilik Usaha"
        txtCariNamaUsaha.promptText = "Cari Nama Usaha"
        txtCariAlamat.promptText = "Cari Alamat"

        tblUmkm.selectionModel.clearSelection()

        btnSimpan.text = "Simpan"

        txtStikerKode.clear()
        txtStikerNama.clear()
        txtStikerPanjang.clear()
        txtStikerLebar.clear()
        txtStikerCatatan.clear()
        txtStikerDicetak.clear()
        dpStikerPembuatan.value = java.time.LocalDate.now()
        dpStikerPerubahan.value = java.time.LocalDate.now()

        loadDataUMKM()
        loadDataStiker()
    }

    fun loadDataUMKM(){
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

    fun loadDataStiker(){
        println("DEBUG: clientController = $clientController, url = ${clientController?.url}")
        if(clientController?.url.isNullOrBlank()){
            Platform.runLater { clientController?.showError("URL server belum di set") }
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

    fun cariDataUmkm(paramName: String, keyword: String) {
        if (clientController?.url.isNullOrBlank()) {
            Platform.runLater { clientController?.showError("URL server belum di set") }
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
                        clientController?.showError("Server Error ${response.statusCode()}")
                        tblUmkm.items = FXCollections.observableArrayList() // kosongkan tabel juga
                    }
                }
            } catch (ex: Exception) {
                Platform.runLater {
                    clientController?.showError(ex.message ?: "Gagal mencari data UMKM")
                    tblUmkm.items = FXCollections.observableArrayList() // kosongkan tabel jika error
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

    fun tambahStikerUntukUmkm() {
        val umkm = getUmkmTerpilih()
        if (umkm == null) {
            clientController?.showError("Pilih data UMKM terlebih dahulu.")
            return
        }

        val stiker = DataStikerDTO(
            dataUmkm = umkm,
            kodeStiker = txtStikerKode.text.trim(),
            namaStiker = txtStikerNama.text.trim(),
            panjang = txtStikerPanjang.text.toIntOrNull() ?: 0,
            lebar = txtStikerLebar.text.toIntOrNull() ?: 0,
            catatan = txtStikerCatatan.text.trim(),
            tglPembuatan = dpStikerPembuatan.value?.atStartOfDay() ?: LocalDateTime.now(),
            tglPerubahan = dpStikerPerubahan.value?.atStartOfDay() ?: LocalDateTime.now()
        )

        Thread {
            try {
                val body = json.encodeToString(stiker)
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("${clientController?.url}/api/dataStiker"))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/json")

                clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                val req = builder.build()
                val resp = client.send(req, HttpResponse.BodyHandlers.ofString())

                if (resp.statusCode() in 200..299) {
                    Platform.runLater {
                        clientController?.showInfo("Stiker berhasil ditambahkan.")
                        // bisa reload tabel stiker di sini kalau kamu punya tabelnya
                    }
                } else {
                    Platform.runLater {
                        clientController?.showError("Server Error ${resp.statusCode()}: ${resp.body()}")
                    }
                }
            } catch (ex: Exception) {
                Platform.runLater {
                    println("Gagal menambahkan stiker: ${ex.message}")
                    clientController?.showError("Gagal menambahkan stiker: ${ex.message}")
                }
            }
        }.start()
    }

}