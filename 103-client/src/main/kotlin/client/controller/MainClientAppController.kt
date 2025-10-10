package com.girsang.client.controller

import client.controller.UmkmController
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
    @FXML private lateinit var mnPengguna: MenuItem
    @FXML private lateinit var mnUMKM: MenuItem

    val user = "admin"
    val pass = "secret"
    var url: String = "http://localhost:8080"

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        mnPengguna.setOnAction { tampilFormPengguna() }
        mnUMKM.setOnAction { tampilFormUMKM() }
        pingServer()
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
        val controller = fxmlLoader.getController<UmkmController>()
        controller.setClientController(this)  // kirim parent controller
        controller.setParentController(this)     // sudah ada ✅
        mainPane.center = content
    }
    fun tutupForm() {
        mainPane.center = null
    }

    fun pingServer() {
        val baseUrl = this.url.trim().removeSuffix("/")   // gunakan this.url, bukan url lokal
        lblStatusServer.text = "Pinging..."
        thread {
            try {
                val builder = HttpRequest.newBuilder()
                    .uri(URI.create("$baseUrl/api/pengguna/ping"))
                    .GET()
                buildAuthHeader()?.let { builder.header("Authorization", it) }
                val req = builder.build()
                val resp = makeRequest(req)
                Platform.runLater { lblStatusServer.text = "Aktif - Ping: ${resp.statusCode()} - ${resp.body()}" }
            } catch (ex: Exception) {
                Platform.runLater {
                    lblStatusServer.text = "Ping failed"
                    showError(ex.message ?: "Error")
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
}
