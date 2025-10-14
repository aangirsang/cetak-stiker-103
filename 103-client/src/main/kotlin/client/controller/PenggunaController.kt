package com.girsang.client.controller

import client.DTO.PenggunaDTO
import client.util.PesanPeringatan
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
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
                PesanPeringatan.warning("Peringatan","Semua field harus diisi!")
                return
            }
            if (password != ulangPassword) {
                PesanPeringatan.warning("Peringatan","Kata sandi tidak cocok!")
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
                            PesanPeringatan.info("Simpan Data","Data pengguna berhasil disimpan.")
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
                        PesanPeringatan.error("Simpan Data",ex.message ?:"Error saat menyimpan data" )
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
                PesanPeringatan.error("Udah Data", "Data ID tidak ditemukan!")
                return
            }
            if (namaPengguna.isEmpty() || namaAkun.isEmpty() || password.isEmpty() || ulangPassword.isEmpty()) {
                PesanPeringatan.warning("Peringatan","Semua field harus diisi!")
                return
            }
            if (password != ulangPassword) {
                PesanPeringatan.error("Udah Data", "Kata sandi tidak cocok!")
                return
            }

            val konfirm = PesanPeringatan.confirm("Ubah Data Pengguna", "Anda yakin ingi menyimpan perubahan data?")
            if(konfirm) {
                Thread {
                    try {
                        val dto =
                            PenggunaDTO(id = id, namaLengkap = namaPengguna, namaAkun = namaAkun, kataSandi = password)
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
                                PesanPeringatan.info("Udah Data", "Data pengguna berhasil diperbarui.")
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
    fun hapusData(){
        val pengguna = getPenggunaTerpilih()
        if (pengguna == null) {
            PesanPeringatan.error("Hapus Data", "Tidak ada pengguna yang dipilih.")
            return
        }
        val id = pengguna.id
        if (id == null) {
            PesanPeringatan.error("Hapus Data", "ID pengguna tidak tersedia.")
            return
        }

        val konfirm = PesanPeringatan.confirm("Hapus Data","Anda yakin ingin menghapus data ini?")
        if (konfirm) {
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
                            PesanPeringatan.info("Hapus Data", "Pengguna berhasil dihapus.")
                            bersih()
                        } else {
                            PesanPeringatan.error(
                                "Hapus Data",
                                "Server returned ${response.statusCode()} : ${response.body()}"
                            )
                        }
                    }
                } catch (ex: Exception) {
                    Platform.runLater {
                        PesanPeringatan.error("Hapus Data", ex.message ?: "Gagal menghapus pengguna")
                    }
                }
            }.start()
        }
    }
    fun loadData() {
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
                        PesanPeringatan.error("Load Data","Server error ${response.statusCode()}")
                    }
                }
            } catch (ex: Exception) {
                Platform.runLater {
                    PesanPeringatan.error("Load Data", ex.message ?: "Gagal menghapus pengguna")
                }
            }
        }.start()
    }
    fun cariPenggunaByNamaAkun(namaAkun: String) {
        if (clientController?.url.isNullOrBlank()) {
            Platform.runLater {
                PesanPeringatan.error("Data Pengguna", "URL server belum di set")
            }
            return
        }

        Thread {
            try {
                // 🔹 Encode namaAkun agar URL valid, ganti + menjadi %20 supaya lebih rapi
                val encodedNamaAkun = URLEncoder.encode(namaAkun, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20")
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("${clientController?.url}/api/pengguna/cari?namaAkun=$encodedNamaAkun"))
                    .GET()
                    .header("Content-Type", "application/json")

                // Tambahkan header Authorization jika ada
                clientController?.buildAuthHeader()?.let {
                    builder.header("Authorization", it)
                }

                val request = builder.build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                Platform.runLater {
                    if (response.statusCode() == 200) {
                        // Response 200 pasti berisi satu PenggunaDTO
                        val pengguna = json.decodeFromString<List<PenggunaDTO>>(response.body())
                        tblPengguna.items = FXCollections.observableArrayList(pengguna)
                    } else if (response.statusCode() == 404) {
                        tblPengguna.items.clear()
                        PesanPeringatan.error("Data Pengguna", "Pengguna tidak ditemukan.")
                    } else {
                        PesanPeringatan.error("Data Pengguna", "Server error ${response.statusCode()}")
                    }
                }
            } catch (ex: Exception) {
                Platform.runLater {
                    PesanPeringatan.error("Data Pengguna", ex.message ?: "Gagal mencari pengguna")
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
