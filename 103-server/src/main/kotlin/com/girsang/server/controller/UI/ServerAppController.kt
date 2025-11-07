package com.girsang.server.controller.UI

import com.girsang.server.ServerUI
import com.girsang.server.SpringApp
import com.girsang.server.config.ServerPort
import com.girsang.server.config.SpringFXMLLoader
import com.girsang.server.service.DatabaseBackupService
import javafx.application.Platform
import javafx.fxml.FXML
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

    @Autowired
    lateinit var fxmlLoader: SpringFXMLLoader

    @FXML private lateinit var txtStatusServer: TextField
    @FXML private lateinit var txtIPServer: TextField
    @FXML private lateinit var txtPortServer: TextField
    @FXML private lateinit var txtURLServer: TextField
    @FXML private lateinit var txtConsole: TextArea
    @FXML private lateinit var btnStartServer: Button
    @FXML private lateinit var btnStopServer: Button
    @FXML private lateinit var btnPengaturan: Button
    @FXML private lateinit var btnBackUp: Button
    @FXML private lateinit var btnRestore: Button


    private val controllerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isRunning = false
    private var springContext: ConfigurableApplicationContext? = null

    private var port = 0
    private val ip = getLocalIPv4Address()

    val dbService = DatabaseBackupService(getDatabasePathFromProperties())


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
        btnBackUp.setOnAction { manualBackup() }
        btnRestore.setOnAction { pilihRestoreFile() }

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
                    dbService.backupSQLiteCopy()
                    dbService.backupSQLiteDataOnly()
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

    /** 💾 Manual backup PostgreSQL */
    private fun manualBackup() {
        controllerScope.launch {
            appendConsole("🔄 Manual backup Sqlite berjalan...")

            try {
                // Jalankan backup data-only SQL di background thread
                val backupFileSQLLengkap = withContext(Dispatchers.IO) { dbService.backupSQLiteToSQL() }
                val backupFileSQL = withContext(Dispatchers.IO) { dbService.backupSQLiteDataOnly() }
                val sqliteFile = File("./data/cetak-stiker.db")

                if (!backupFileSQL.exists() || !sqliteFile.exists() || !backupFileSQLLengkap.exists()) {
                    appendConsole("❌ Backup gagal: file sumber tidak ditemukan.")
                    return@launch
                }

                // 🗂️ Pilih lokasi file ZIP pakai FileChooser (jalan di JavaFX thread)
                val selectedFile = showSaveFileChooser() ?: run {
                    appendConsole("⚠️ Backup dibatalkan oleh pengguna.")
                    return@launch
                }

                val zipFile = if (selectedFile.name.endsWith(".zip")) selectedFile else File(selectedFile.absolutePath + ".zip")

                // 🔧 Buat ZIP di background thread
                withContext(Dispatchers.IO) {
                    zipFiles(
                        zipFile,
                        mapOf(
                            "database/cetak-stiker.db" to sqliteFile,
                            "sql/data-backup-dataOnly.sql" to backupFileSQL,
                            "sql/data-backup-lengkap.sql" to backupFileSQLLengkap
                        )
                    )
                }

                appendConsole("✅ Backup ZIP berhasil disimpan di: ${zipFile.absolutePath}")

            } catch (e: Exception) {
                appendConsole("❌ Backup gagal: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /** ♻️ Pilih file SQL untuk restore */
    private fun pilihRestoreFile() {
        if (springContext != null) {
            appendConsole("⚠️ Matikan server dulu sebelum restore database.")
            return
        }

        // Jalankan FileChooser di JavaFX thread
        Platform.runLater {
            val chooser = FileChooser()
            chooser.title = "Pilih File Backup untuk Restore"
            chooser.extensionFilters.addAll(
                FileChooser.ExtensionFilter("Backup File (*.sql, *.db, *.sqlite)", "*.sql", "*.db", "*.sqlite"),
                FileChooser.ExtensionFilter("SQL File", "*.sql"),
                FileChooser.ExtensionFilter("SQLite File", "*.db", "*.sqlite")
            )

            val file = chooser.showOpenDialog(null)
            if (file == null) {
                appendConsole("ℹ️ Restore dibatalkan oleh pengguna.")
                return@runLater
            }

            // Jalankan proses restore di coroutine
            controllerScope.launch {
                try {
                    val fileName = file.name.lowercase()
                    val dbPath = getDatabasePathFromProperties()
                    val dbFile = File(dbPath)

                    appendConsole("♻️ Memulai proses restore dari file: ${file.name}")

                    if (fileName.endsWith(".sql")) {
                        // 🔄 Restore data-only SQL
                        appendConsole("🧩 Mode: Restore SQL (data-only)")
                        dbService.restoreSQLiteDataOnly(file)
                        appendConsole("✅ Restore SQL selesai dari file: ${file.name}")

                    } else if (fileName.endsWith(".db") || fileName.endsWith(".sqlite")) {
                        // 🔄 Restore file database SQLite (replace file lama)
                        appendConsole("🧩 Mode: Restore SQLite file")

                        if (!dbFile.exists()) {
                            dbFile.parentFile?.mkdirs()
                            dbFile.createNewFile()
                        }

                        // Tutup koneksi aktif (jika ada)
                        // dbService.closeConnection() // jika kamu punya fungsi close DB

                        java.nio.file.Files.copy(
                            file.toPath(),
                            dbFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                        )

                        appendConsole("✅ File SQLite aktif telah diganti dari backup: ${file.name}")
                    } else {
                        appendConsole("❌ Jenis file tidak dikenali. Hanya mendukung .sql, .db, atau .sqlite")
                        return@launch
                    }

                    appendConsole("🔁 Silakan jalankan kembali server untuk menerapkan perubahan.")

                } catch (e: Exception) {
                    appendConsole("❌ Gagal restore database: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }


    private fun updateUI() {
        if (isRunning) {
            txtStatusServer.text = "Server Running"
            btnStartServer.isDisable = true
            btnStopServer.isDisable = false
            btnBackUp.isDisable = false
            btnRestore.isDisable = true

            txtPortServer.text = "$port"
            txtURLServer.text = "http://$ip:$port"
        } else {
            txtStatusServer.text = "Server Stopped"
            btnStartServer.isDisable = false
            btnStopServer.isDisable = true
            btnBackUp.isDisable = false
            btnRestore.isDisable = false

            txtPortServer.text = "-"
            txtURLServer.text = "-"
        }
    }

    private fun updateStatus(text: String) {
        Platform.runLater {
            txtStatusServer.text = text
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
        val url = javaClass.getResource("/fxml/config_server.fxml")
        val root = fxmlLoader.load(url!!)
        stage.title = "Pengaturan Server"
        stage.scene = Scene(root as Parent?)
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
    fun getDatabasePathFromProperties(): String {
        val props = Properties()
        val propFile = File("./103-server/src/main/resources/application.properties") // ubah sesuai lokasi file-mu

        if (!propFile.exists()) {
            println("⚠️ File application.properties tidak ditemukan: ${propFile.absolutePath}")
            return "./data/cetak-stiker.db" // fallback default
        }

        propFile.inputStream().use { props.load(it) }

        val url = props.getProperty("spring.datasource.url") ?: "jdbc:sqlite:./data/cetak-stiker.db"
        val dbPath = url.substringAfter("jdbc:sqlite:").trim()

        return dbPath
    }

    fun zipFiles(zipFile: File, files: Map<String, File>) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
            files.forEach { (pathInZip, file) ->
                if (file.exists()) {
                    FileInputStream(file).use { fis ->
                        val entry = ZipEntry(pathInZip)
                        zipOut.putNextEntry(entry)
                        fis.copyTo(zipOut, 1024)
                        zipOut.closeEntry()
                    }
                }
            }
        }
    }
    private suspend fun showSaveFileChooser(): File? = suspendCoroutine { cont ->
        Platform.runLater {
            try {
                val chooser = FileChooser()
                chooser.title = "Simpan File Backup"
                chooser.initialDirectory = File(System.getProperty("user.dir"), "backup").apply { mkdirs() }
                chooser.extensionFilters.add(FileChooser.ExtensionFilter("ZIP Backup File", "*.zip"))

                val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
                chooser.initialFileName = "manual-backup-$timestamp.zip"

                val file = chooser.showSaveDialog(null)
                cont.resume(file)
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
    }
}
