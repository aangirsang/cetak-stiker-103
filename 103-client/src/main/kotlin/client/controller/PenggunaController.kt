package com.girsang.client.controller

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.ResourceBundle
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Serializable
data class PenggunaDTO(
    val id: Long? = null,
    val namaLengkap: String,
    val namaAkun: String,
    val kataSandi: String
)

class PenggunaController : Initializable {

    private val client = HttpClient.newBuilder().build()
    private val json = Json { ignoreUnknownKeys = true }

    @FXML private lateinit var txtNamaPengguna: TextField
    @FXML private lateinit var txtNamaAkun: TextField
    @FXML private lateinit var txtPassword: TextField
    @FXML private lateinit var txtUlangPassword: TextField
    @FXML private lateinit var btnTutup: Button
    @FXML private lateinit var btnSimpan: Button
    @FXML private lateinit var btnRefresh: Button
    @FXML private lateinit var btnCari: Button
    @FXML private lateinit var btnHapus: Button

    @FXML private lateinit var tblPengguna: TableView<PenggunaDTO>
    @FXML private lateinit var colId: TableColumn<PenggunaDTO, Long>
    @FXML private lateinit var colNama: TableColumn<PenggunaDTO, String>
    @FXML private lateinit var colAkun: TableColumn<PenggunaDTO, String>

    private var clientController: MainClientAppController? = null
    private var parentController: MainClientAppController? = null

    fun setClientController(controller: MainClientAppController) {
        this.clientController = controller  // ✅ simpan controller dulu

        if (!controller.url.isNullOrBlank()) {
            loadData() // ✅ baru panggil setelah URL diset
        } else {
            println("⚠️ URL belum di-set, data tidak bisa dimuat")
        }
    }

    fun setParentController(controller: MainClientAppController) {
        this.parentController = controller
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        colId.setCellValueFactory { javafx.beans.property.SimpleLongProperty(it.value.id ?: 0).asObject() }
        colNama.setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.namaLengkap) }
        colAkun.setCellValueFactory { javafx.beans.property.SimpleStringProperty(it.value.namaAkun) }

        btnTutup.setOnAction { tutup() }
        btnSimpan.setOnAction { simpanPengguna() }
        btnRefresh.setOnAction { bersih() }
        btnHapus.setOnAction {hapusData()}
        btnCari.setOnAction { cariPenggunaByNamaAkun(txtNamaAkun.text) }

        // 🔹 Tambahkan listener untuk selection
        tblPengguna.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                penggunaTerpilih(newValue)
            }
        }

    }

    fun bersih() {
        txtNamaPengguna.clear()
        txtNamaAkun.clear()
        txtPassword.clear()
        txtUlangPassword.clear()
        tblPengguna.selectionModel.clearSelection()

        btnSimpan.text  = "Simpan"

        loadData()
    }

    fun tutup() {
        parentController?.tutupForm()
    }

    fun simpanPengguna() {
        if (btnSimpan.text == "Simpan"){
            val namaPengguna = txtNamaPengguna.text.trim()
            val namaAkun = txtNamaAkun.text.trim()
            val password = txtPassword.text.trim()
            val ulangPassword = txtUlangPassword.text.trim()

            if (namaPengguna.isEmpty() || namaAkun.isEmpty() || password.isEmpty() || ulangPassword.isEmpty()) {
                clientController?.showError("Semua field harus diisi!")
                return
            }
            if (password != ulangPassword) {
                clientController?.showError("Kata sandi tidak cocok!")
                return
            }

            Thread {
                try {
                    val dto = PenggunaDTO(namaLengkap = namaPengguna, namaAkun = namaAkun, kataSandi = password)
                    val body = json.encodeToString(dto)
                    val builder = HttpRequest.newBuilder()
                        .uri(URI.create("${clientController?.url}/api/pengguna"))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .header("Content-Type", "application/json")

                    clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                    val req = builder.build()
                    val resp = clientController?.makeRequest(req)

                    if (resp?.statusCode() in 200..299) {
                        Platform.runLater {
                            bersih()
                            clientController?.showInfo("Data pengguna berhasil disimpan.")
                        }
                    } else {
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
        }else if (btnSimpan.text == "Ubah") {
            val pengguna = getPenggunaTerpilih()
            val id = pengguna?.id
            val namaPengguna = txtNamaPengguna.text.trim()
            val namaAkun = txtNamaAkun.text.trim()
            val password = txtPassword.text.trim()
            val ulangPassword = txtUlangPassword.text.trim()

            if (id == null) {
                clientController?.showError("ID pengguna tidak tersedia.")
                return
            }
            if (namaPengguna.isEmpty() || namaAkun.isEmpty() || password.isEmpty() || ulangPassword.isEmpty()) {
                clientController?.showError("Semua field harus diisi!")
                return
            }
            if (password != ulangPassword) {
                clientController?.showError("Kata sandi tidak cocok!")
                return
            }

            Thread {
                try {
                    val dto = PenggunaDTO(id = id, namaLengkap = namaPengguna, namaAkun = namaAkun, kataSandi = password)
                    val body = json.encodeToString(dto)
                    val builder = HttpRequest.newBuilder()
                        .uri(URI.create("${clientController?.url}/api/pengguna/${id}"))
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .header("Content-Type", "application/json")

                    clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                    val req = builder.build()
                    val resp = clientController?.makeRequest(req)

                    if (resp?.statusCode() in 200..299) {
                        Platform.runLater {
                            bersih()
                            clientController?.showInfo("Data pengguna berhasil diperbarui.")
                        }
                    } else {
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
        val pengguna = getPenggunaTerpilih()
        if (pengguna == null) {
            clientController?.showError("Tidak ada pengguna yang dipilih.")
            return
        }
        val id = pengguna.id
        if (id == null) {
            clientController?.showError("ID pengguna tidak tersedia.")
            return
        }

        Thread {
            try {
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("${clientController?.url}/api/pengguna/$id"))
                    .DELETE()

                clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                val request = builder.build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                Platform.runLater {
                    if (response.statusCode() in 200..299) {
                        bersih()
                        clientController?.showInfo("Pengguna berhasil dihapus.")
                    } else {
                        clientController?.showError("Server returned ${response.statusCode()} : ${response.body()}")
                    }
                }
            } catch (ex: Exception) {
                Platform.runLater {
                    clientController?.showError(ex.message ?: "Gagal menghapus pengguna")
                }
            }
        }.start()
    }
    fun loadData() {
        println("DEBUG: clientController = $clientController, url = ${clientController?.url}")
        if (clientController?.url.isNullOrBlank()) {
            Platform.runLater {
                clientController?.showError("URL server belum diset.")
            }
            return
        }

        Thread {
            try {
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("${clientController?.url}/api/pengguna"))
                    .GET()
                    .header("Content-Type", "application/json")

                // 🔐 Tambahkan header Authorization
                clientController?.buildAuthHeader()?.let { builder.header("Authorization", it) }

                val request = builder.build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() in 200..299) {
                    val list = json.decodeFromString<List<PenggunaDTO>>(response.body())
                    Platform.runLater {
                        tblPengguna.items = FXCollections.observableArrayList(list)
                    }
                } else {
                    Platform.runLater {
                        clientController?.showError("Server error ${response.statusCode()}")
                    }
                }
            } catch (ex: Exception) {
                Platform.runLater {
                    clientController?.showError(ex.message ?: "Gagal memuat data pengguna")
                }
            }
        }.start()
    }

    fun cariPenggunaByNamaAkun(namaAkun: String) {
        if (clientController?.url.isNullOrBlank()) {
            clientController?.showError("URL server belum diset.")
            return
        }

        Thread {
            try {
                // 🔹 Encode namaAkun agar URL valid, ganti + menjadi %20 supaya lebih rapi
                val encodedNamaAkun = URLEncoder.encode(namaAkun, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20")
                val fullUrl = "${clientController?.url}/api/pengguna/cari/$encodedNamaAkun"
                println("DEBUG: Mencari pengguna dengan namaAkun = $namaAkun")
                println("DEBUG: URL request = $fullUrl")

                val builder = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .GET()
                    .header("Content-Type", "application/json")

                // Tambahkan header Authorization jika ada
                clientController?.buildAuthHeader()?.let {
                    builder.header("Authorization", it)
                }

                val request = builder.build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                println("DEBUG: StatusCode = ${response.statusCode()}")
                println("DEBUG: ResponseBody = ${response.body()}")

                Platform.runLater {
                    if (response.statusCode() == 200) {
                        // Response 200 pasti berisi satu PenggunaDTO
                        val pengguna = json.decodeFromString<PenggunaDTO>(response.body())
                        tblPengguna.items = FXCollections.observableArrayList(listOf(pengguna))
                    } else if (response.statusCode() == 404) {
                        tblPengguna.items.clear()
                        clientController?.showError("Pengguna tidak ditemukan.")
                    } else {
                        clientController?.showError("Server error ${response.statusCode()}")
                    }
                }
            } catch (ex: Exception) {
                Platform.runLater {
                    clientController?.showError(ex.message ?: "Gagal mencari pengguna")
                }
            }
        }.start()
    }

    fun penggunaTerpilih(dto: PenggunaDTO){
        txtNamaPengguna.text = dto.namaLengkap
        txtNamaAkun.text = dto.namaAkun
        txtPassword.clear()
        txtUlangPassword.clear()
        btnSimpan.text = "Ubah"
    }
    fun getPenggunaTerpilih(): PenggunaDTO? {
        return tblPengguna.selectionModel.selectedItem
    }

}
