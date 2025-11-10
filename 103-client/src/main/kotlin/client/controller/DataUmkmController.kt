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
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
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

class DataUmkmController : Initializable{

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

    private var clientController: MainClientAppController? = null
    private var parentController: MainClientAppController? = null
    private var searchThread: Thread? = null

    override fun initialize(p0: URL?, p1: ResourceBundle?) {

        //Tabel UMKM
        kolId.setCellValueFactory { SimpleLongProperty(it.value.id ?: 0).asObject()}
        kolNamaPemilik.setCellValueFactory {SimpleStringProperty(it.value.namaPemilik)}
        kolNamaUsaha.setCellValueFactory {SimpleStringProperty(it.value.namaUsaha)}
        kolKontak.setCellValueFactory {SimpleStringProperty(it.value.kontak)}
        kolInstagram.setCellValueFactory {SimpleStringProperty(it.value.instagram)}
        kolAlamat.setCellValueFactory {SimpleStringProperty(it.value.alamat)}

        btnTutup.setOnAction { parentController?.tutupForm() }
        btnRefresh.setOnAction { bersih() }
        btnSimpan.setOnAction { simpanDataUmkm() }
        btnHapus.setOnAction { hapusData() }

        tblUmkm.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                umkmTerpilih(newValue)
            }
        }

        setupSearchListener(txtCariNamaPemilik)
        setupSearchListener(txtCariNamaUsaha)
        setupSearchListener(txtCariAlamat)


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
    private fun setupSearchListener(field: TextField) {
        field.textProperty().addListener { _, _, newValue ->
            searchThread?.interrupt() // hentikan thread sebelumnya jika user masih mengetik
            searchThread = Thread {
                try {
                    Thread.sleep(300) // debounce 300ms
                    if (Thread.interrupted()) return@Thread

                    if (newValue.isNullOrBlank()) {
                        Platform.runLater { loadDataUMKM() }
                    } else {
                        cariDataUmkm(txtCariNamaPemilik.text,
                            txtCariNamaUsaha.text,
                            txtAlamat.text)
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

        loadDataUMKM()
    }
    fun loadDataUMKM(){
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
                        PesanPeringatan.error("Load Data UMKM","Server Error ${response.statusCode()}")
                    }
                }
            } catch (ex: Exception){
                Platform.runLater {
                    PesanPeringatan.error("Load Data UMKM",ex.message ?: "Gagal memeuat data UMKM")
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
            PesanPeringatan.warning("Simpan Data UMKM","Semua field harus diisi!")
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
                            PesanPeringatan.info("Simpan Data UMKM","Data UMKM berhasil disimpan.")
                            bersih()
                        }
                    }else {
                        Platform.runLater {
                            PesanPeringatan.error("Simpan Data UMKM","Server returned ${resp?.statusCode()} : ${resp?.body()}")
                        }
                    }
                } catch (ex: Exception) {
                    Platform.runLater {
                        PesanPeringatan.error("Simpan Data UMKM",ex.message ?: "Error saat menyimpan data")
                    }
                }
            }.start()
        } else {
            val umkm = getUmkmTerpilih()
            val id = umkm?.id

            if (id==null) {
                PesanPeringatan.error("Ubah Data UMKM","ID UMKM tidak tersedia")
                return
            }

            val konfirm = PesanPeringatan.confirm("Ubah Data UMKM", "Anda yakin ingi menyimpan perubahan data?")
            if(konfirm) {
                Thread {
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

                        if (resp?.statusCode() in 200..299) {
                            Platform.runLater {
                                PesanPeringatan.info("Udah Data UMKM", "Data UMKM berhasil diperbarui.")
                                bersih()
                            }
                        } else {
                            Platform.runLater {
                                println("Server returned ${resp?.statusCode()} : ${resp?.body()}")
                                PesanPeringatan.error("Ubah Data UMKM","Server returned ${resp?.statusCode()} : ${resp?.body()}")
                            }
                        }
                    } catch (ex: Exception) {
                        Platform.runLater {
                            PesanPeringatan.error("Ubah Data UMKM",ex.message ?:"Error saat memperbarui data" )
                        }
                    }
                }.start()
            }
        }
    }
    fun hapusData(){
        val umkm = getUmkmTerpilih()
        val id = umkm?.id
        if (umkm == null) {
            PesanPeringatan.error("Hapus Data", "Tidak ada UMKM yang dipilih.")
            return
        }

        if(id == null) {
            PesanPeringatan.error("Hapus Data", "ID UMKM tidak tersedia.")
            return
        }

        val konfirm = PesanPeringatan.confirm("Hapus Data","Anda yakin ingin menghapus data ini?")
        if (konfirm) {
            Thread {
                try {
                    val builder = HttpRequest.newBuilder()
                        .uri(URI.create("${clientController?.url}/api/dataUmkm/${id}"))
                        .DELETE()

                    clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                    val request = builder.build()
                    val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                    Platform.runLater {
                        if (response.statusCode() in 200..299) {
                            PesanPeringatan.info("Hapus Data", "UMKM berhasil dihapus.")
                            bersih()
                        } else {
                            PesanPeringatan.error(
                                "Hapus Data",
                                "Server returned ${response.statusCode()} : ${response.body()}")
                        }
                    }
                } catch (ex: Exception) {
                    Platform.runLater {
                        PesanPeringatan.error("Hapus Data", ex.message ?: "Gagal menghapus UMKM")
                    }
                }
            }.start()
        }
    }
    fun cariDataUmkm(namaPemilik: String, namaUsaha: String, alamat: String) {
        if (clientController?.url.isNullOrBlank()) {
            Platform.runLater { PesanPeringatan.error("Data UMKM","URL server belum di set") }
            return
        }

        Thread {
            try {
                val uri = "${clientController?.url}/api/dataUmkm/cari?" +
                        "namaPemilik=${namaPemilik}&namaUsaha=${namaUsaha}&alamat=${alamat}"
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
                            clientController?.showInfo(
                                "Tidak ada data yang cocok untuk pencarian " +
                                        "\"$namaPemilik\" & \"$namaUsaha\"& \"$alamat\"")
                        } else {
                            // ✅ Tampilkan hasil pencarian
                            tblUmkm.items = FXCollections.observableArrayList(hasil)
                        }
                    }
                } else {
                    Platform.runLater {
                        PesanPeringatan.error("Data UMKM","Server Error ${response.statusCode()}")
                        tblUmkm.items = FXCollections.observableArrayList() // kosongkan tabel juga
                    }
                }
            } catch (ex: Exception) {
                Platform.runLater {
                    PesanPeringatan.error("Data UMKM",ex.message ?: "Gagal mencari data UMKM")
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

}