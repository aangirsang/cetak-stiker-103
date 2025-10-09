package com.girsang.server.controller.UI

import com.girsang.server.SpringApp
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.Label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import java.net.InetAddress
import java.net.URL
import java.util.ResourceBundle

class ServerAppController : Initializable {

    @FXML private lateinit var lblStatusServer: Label
    @FXML private lateinit var lblIPServer: Label
    @FXML private lateinit var btnStartServer: Button
    @FXML private lateinit var btnStopServer: Button

    private var serverContext: ConfigurableApplicationContext? = null
    private var serverJob: Job? = null


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        val localHost = InetAddress.getLocalHost()
        lblStatusServer.text = "Server belum berjalan"
        lblIPServer.text = localHost.hostAddress
        btnStartServer.setOnAction { startServer() }
        btnStopServer.setOnAction { stopServer() }
    }

    private fun startServer() {
        if (serverJob?.isActive == true) {
            Platform.runLater { lblStatusServer.text = "Server sudah berjalan!" }
            return
        }

        serverJob = GlobalScope.launch(Dispatchers.IO) {
            try {
                Platform.runLater { lblStatusServer.text = "Menjalankan server..." }
                serverContext = SpringApplication.run(SpringApp::class.java)
                Platform.runLater { lblStatusServer.text = "Server berjalan di http://localhost:8080" }
            } catch (e: Exception) {
                e.printStackTrace()
                Platform.runLater { lblStatusServer.text = "Gagal menjalankan server: ${e.message}" }
            }
        }
    }

    private fun stopServer() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                serverContext?.close()
                serverJob?.cancel()
                Platform.runLater { lblStatusServer.text = "Server dihentikan" }
            } catch (e: Exception) {
                Platform.runLater { lblStatusServer.text = "Gagal menghentikan server: ${e.message}" }
            }
        }
    }

}
