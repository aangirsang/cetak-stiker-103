package com.girsang.client.controller

import client.controller.DataOrderanController
import client.controller.DataStikerController
import client.controller.DataUmkmController
import client.util.PesanPeringatan
import com.girsang.client.config.ClientConfig
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import kotlin.concurrent.thread
import kotlin.jvm.javaClass
import kotlin.let
import kotlin.text.removeSuffix
import kotlin.text.toByteArray
import kotlin.text.trim

class MainClientAppController : Initializable {

    private val client = HttpClient.newBuilder().build()

    @FXML private lateinit var mainPane: BorderPane
    @FXML lateinit var lblWaktu: Label
    @FXML private lateinit var lblStatusServer: Label
    @FXML private lateinit var lblURL: Label
    @FXML private lateinit var mnPengguna: MenuItem
    @FXML private lateinit var mnUMKM: MenuItem
    @FXML private lateinit var mnDataStiker: MenuItem
    @FXML private lateinit var mnOrderStiker: MenuItem

    val user = ClientConfig.getUser()
    val pass = ClientConfig.getPass()
    val ip = ClientConfig.getIP()
    val port = ClientConfig.getPort()
    val url = "http://$ip:$port"
    val baseUrl = this.url.trim().removeSuffix("/")   // gunakan this.url, bukan url lokal

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        mnPengguna.setOnAction { tampilFormPengguna() }
        mnUMKM.setOnAction { tampilFormUMKM() }
        mnDataStiker.setOnAction { tampilFormStiker() }
        mnOrderStiker.setOnAction { tampilOrdertiker() }
        lblURL.text = ""
        konekServer(baseUrl)
    }

    private fun tampilFormPengguna() {
        val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/pengguna.fxml"))
        val content: AnchorPane = fxmlLoader.load()
        val controller = fxmlLoader.getController<PenggunaController>()
        controller.setClientController(this)  // kirim parent controller
        controller.setParentController(this)     // sudah ada ✅
        mainPane.center = content
    }
    private fun tampilFormUMKM() {
        val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/data-umkm.fxml"))
        val content: AnchorPane = fxmlLoader.load()
        val controller = fxmlLoader.getController<DataUmkmController>()
        controller.setClientController(this)  // kirim parent controller
        controller.setParentController(this)     // sudah ada ✅
        mainPane.center = content
    }
    private fun tampilFormStiker() {
        val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/data-stiker.fxml"))
        val content: AnchorPane = fxmlLoader.load()
        val controller = fxmlLoader.getController<DataStikerController>()
        controller.setClientController(this)  // kirim parent controller
        controller.setParentController(this)     // sudah ada ✅
        mainPane.center = content
    }
    private fun tampilOrdertiker() {
        val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/data-orderan-stiker.fxml"))
        val content: AnchorPane = fxmlLoader.load()
        val controller = fxmlLoader.getController<DataOrderanController>()
        controller.setClientController(this)  // kirim parent controller
        controller.setParentController(this)     // sudah ada ✅
        mainPane.center = content
    }
    fun tutupForm() {
        mainPane.center = null
    }

    fun konekServer(baseUrl: String){
        lblStatusServer.text = "Mencoba terhubung ke server…"
        println("Server: $url")
        println("User: $user")
        println("Password: $pass")


        val builder = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/pengguna/ping"))
            .GET()

        buildAuthHeader()?.let { builder.header("Authorization", it) }
        val req = builder.build()

        try {
            val resp = makeRequest(req)

            Platform.runLater {
                when (resp.statusCode()) {
                    200 -> {
                        lblStatusServer.text = "✅ Aktif - Ping: ${resp.statusCode()} - ${resp.body()}"
                    }
                    401 -> {
                        lblStatusServer.text = "❌ Autentikasi gagal (401)"
                        showError("Username atau password salah.\nSilakan periksa pengaturan koneksi Anda.")
                    }
                    else -> {
                        lblStatusServer.text = "⚠️ Server merespons kode ${resp.statusCode()}"
                        showError("Koneksi ke server berhasil tapi tidak valid.\nKode: ${resp.statusCode()}")
                    }
                }
                lblURL.text = "URL Server: $url"
            }

        } catch (ex: Exception) {
            // misalnya: server mati, jaringan putus, atau timeout
            Platform.runLater {
                lblStatusServer.text = "❌ Server tidak dapat dihubungi"
                val confirm = PesanPeringatan.confirmWithSettings("Konfirmasi", "Koneksi ke server gagal.\nApakah ingin mencoba kembali?")

                when (confirm) {
                    "OK" -> {
                        konekServer(baseUrl)
                        lblStatusServer.text = "Mencoba terhubung ke server…"
                    }
                    "SETTING" -> {
                        tampilSettings()
                        val stage = lblWaktu.scene.window as javafx.stage.Stage
                        stage.close()
                    }
                    "CANCEL" -> {
                        println("User pilih tutup")
                        val stage = lblWaktu.scene.window as javafx.stage.Stage
                        stage.close()
                    }
                }
            }
        }
    }
    fun buildAuthHeader(): String? {
        val token = Base64.getEncoder().encodeToString("$user:$pass".toByteArray())
        return "Basic $token"
    }

    fun makeRequest(req: HttpRequest): HttpResponse<String> =
        client.send(req, HttpResponse.BodyHandlers.ofString())

    fun showError(pesan: String) {
        val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR)
        alert.title = "Perhatian"
        alert.headerText = null
        alert.contentText = pesan
        alert.showAndWait()

    }
    fun showInfo(pesan: String) {
        val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
        alert.title = "Informasi"
        alert.headerText = null
        alert.contentText = pesan
        alert.showAndWait()
    }
    private fun tampilSettings() {
        val stage = javafx.stage.Stage()
        val loader = FXMLLoader(javaClass.getResource("/fxml/settings.fxml"))
        stage.scene = javafx.scene.Scene(loader.load())
        stage.title = "Pengaturan Koneksi"
        stage.show()
    }
}
