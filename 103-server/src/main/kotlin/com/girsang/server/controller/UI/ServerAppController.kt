package com.girsang.server.controller.UI

import com.girsang.server.SpringApp
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.stage.Stage
import kotlinx.coroutines.*
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.*

class ServerAppController : Initializable {

    @FXML private lateinit var lblStatusServer: Label
    @FXML private lateinit var lblIPServer: Label
    @FXML private lateinit var btnStartServer: Button
    @FXML private lateinit var btnStopServer: Button
    @FXML private lateinit var btnPengaturan: Button

    private var serverContext: ConfigurableApplicationContext? = null
    private var serverJob: Job? = null
    private val controllerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        lblStatusServer.text = "Server belum berjalan"
        lblIPServer.text = getLocalIPv4Address() ?: "Tidak ditemukan"
        btnStopServer.isDisable = true

        btnStartServer.setOnAction { startServer() }
        btnStopServer.setOnAction { stopServer() }
        btnPengaturan.setOnAction { tampilSettings() }
    }

    private fun startServer() {
        if (serverJob?.isActive == true) {
            updateStatus("Server sudah berjalan!")
            return
        }

        btnStartServer.isDisable = true
        btnStopServer.isDisable = false

        serverJob = controllerScope.launch {
            try {
                updateStatus("Menjalankan server...")
                serverContext = SpringApplication.run(SpringApp::class.java)
                updateStatus("Server berjalan di http://localhost:8080")
            } catch (e: Exception) {
                e.printStackTrace()
                updateStatus("Gagal menjalankan server: ${e.message}")
                Platform.runLater {
                    btnStartServer.isDisable = false
                    btnStopServer.isDisable = true
                }
            }
        }
    }

    private fun stopServer() {
        controllerScope.launch {
            try {
                serverContext?.close()
                serverJob?.cancelAndJoin()
                updateStatus("Server dihentikan")

                Platform.runLater {
                    btnStartServer.isDisable = false
                    btnStopServer.isDisable = true
                }
            } catch (e: Exception) {
                updateStatus("Gagal menghentikan server: ${e.message}")
            }
        }
    }

    private fun updateStatus(text: String) {
        Platform.runLater {
            lblStatusServer.text = text
        }
    }

    private fun getLocalIPv4Address(): String? {
        return NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }

    fun shutdown() {
        controllerScope.cancel()
        stopServer()
    }
    private fun tampilSettings() {
        val stage = Stage()
        stage.title = "Pengaturan Server"
        val loader = FXMLLoader(javaClass.getResource("/fxml/config_server.fxml"))
        stage.scene = Scene(loader.load())
        stage.show()
    }
}
