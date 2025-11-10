package com.girsang.server.controller.UI

import com.girsang.server.ServerUI
import com.girsang.server.SpringApp
import com.girsang.server.config.DatabasePathFromProperties
import com.girsang.server.config.ServerPort
import com.girsang.server.service.DatabaseBackupService
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Component
class ServerAppController : Initializable {

    @FXML private lateinit var txtStatusServer: TextField
    @FXML private lateinit var txtIPServer: TextField
    @FXML private lateinit var txtPortServer: TextField
    @FXML private lateinit var txtURLServer: TextField
    @FXML private lateinit var txtConsole: TextArea
    @FXML private lateinit var btnStartServer: Button
    @FXML private lateinit var btnStopServer: Button
    @FXML private lateinit var btnPengaturan: Button


    private val controllerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isRunning = false
    private var springContext: ConfigurableApplicationContext? = null

    private var port = 0
    private val ip = getLocalIPv4Address()

    val dbService = DatabaseBackupService(DatabasePathFromProperties.getDatabasePathFromProperties())


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        txtStatusServer.text = "Server belum berjalan"
        txtIPServer.text = getLocalIPv4Address() ?: "Tidak ditemukan"
        txtPortServer.text = "-"
        txtURLServer.text = "-"
        btnStopServer.isDisable = false
        txtConsole.isEditable = false
        txtConsole.style = "-fx-control-inner-background: black; -fx-text-fill: white; -fx-font-family: Consolas; -fx-font-size: 12px;"


        btnStartServer.setOnAction { startServer() }
        btnStopServer.setOnAction { stopServer() }
        btnPengaturan.setOnAction { tampilSettings() }

        updateUI()
        redirectConsoleToTextArea()
        println("Aplikasi GUI siap...")
    }

    /** 🚀 Jalankan Spring Boot server */
    private fun startServer() {
        if (!isRunning) {
            txtStatusServer.text = "Starting Spring Server..."
            btnStartServer.isDisable = true

            controllerScope.launch {
                try {
                    appendConsole("🚀 Menjalankan Spring Boot...")
                    springContext = SpringApplication.run(SpringApp::class.java)
                    ServerUI.springContext = springContext
                    isRunning = true
                    port = ServerPort.port
                    appendConsole("✅ Server Spring Boot berjalan!")
                    dbService.backupOtomatis(6)
                } catch (e: Exception) {
                    appendConsole("❌ Gagal menjalankan server: ${e.message}")
                }
                Platform.runLater { updateUI() }
            }
        }
    }

    /** 🛑 Hentikan Spring Boot server */
    private fun stopServer() {
        if (isRunning) {
            txtStatusServer.text = "Stopping..."
            btnStopServer.isDisable = true

            controllerScope.launch {
                try {
                    appendConsole("🛑 Menghentikan Spring Boot...")
                    springContext?.close()
                    springContext = null
                    ServerUI.springContext = null
                    isRunning = false
                    updateUI()
                    appendConsole("✅ Server berhasil dihentikan.")
                } catch (e: Exception) {
                    appendConsole("❌ Error saat stop server: ${e.message}")
                }
                Platform.runLater { updateUI() }
            }
        }
    }


    private fun updateUI() {
        if (isRunning) {
            txtStatusServer.text = "Server Running"
            btnStartServer.isDisable = true
            btnStopServer.isDisable = false
            btnPengaturan.isDisable = true

            txtPortServer.text = "$port"
            txtURLServer.text = "http://$ip:$port"
        } else {
            txtStatusServer.text = "Server Stopped"
            btnStartServer.isDisable = false
            btnStopServer.isDisable = true
            btnPengaturan.isDisable = false
            txtPortServer.text = "-"
            txtURLServer.text = "-"
        }
    }

    private fun getLocalIPv4Address(): String? {
        return NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }

    private fun tampilSettings() {
        val stage = Stage()
        stage.title = "Pengaturan Server"
        val loader = FXMLLoader(javaClass.getResource("/fxml/config_server.fxml"))
        stage.scene = Scene(loader.load())
        stage.show()
    }

    //menampilkan isi consol kedalam txtConsol
    private fun redirectConsoleToTextArea() {
        val buffer = StringBuilder()

        val ps = java.io.PrintStream(object : java.io.OutputStream() {
            override fun write(b: Int) {
                val char = b.toChar()
                buffer.append(char)

                if (char == '\n') {
                    val line = buffer.toString()
                    buffer.clear()

                    Platform.runLater {
                        val clean = line.replace(Regex("\u001B\\[[;\\d]*m"), "")
                        txtConsole.appendText(clean)
                    }
                }
            }
        })

        System.setOut(ps)
        System.setErr(ps)
    }

    private fun appendConsole(msg: String) {
        Platform.runLater {
            txtConsole.appendText(msg + "\n")
        }
    }
}
