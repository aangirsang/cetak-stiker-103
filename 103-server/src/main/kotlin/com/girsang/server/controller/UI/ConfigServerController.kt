package com.girsang.server.controller.UI

import com.girsang.server.config.DatabasePathFromProperties
import com.girsang.server.service.DatabaseBackupService
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.stage.FileChooser
import javafx.stage.Window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ConfigServerController {

    @FXML private lateinit var txtPort: TextField
    @FXML private lateinit var txtUser: TextField
    @FXML private lateinit var txtNewPass: PasswordField
    @FXML private lateinit var txtConfirmPass: PasswordField
    @FXML private lateinit var txtRoles: TextField

    @FXML private lateinit var btnBackUp: Button
    @FXML private lateinit var btnRestore: Button
    @FXML private lateinit var btnSimpanConfig: Button

    private val configPath = Paths.get("config", "server-config.properties")
    private val encoder = BCryptPasswordEncoder()
    private val controllerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    val dbService = DatabaseBackupService(DatabasePathFromProperties.getDatabasePathFromProperties())


    @FXML
    fun initialize() {
        try {
            // Buat folder & file jika belum ada
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.parent)

                Files.createFile(configPath)

                val defaultProps = Properties().apply {
                    setProperty("server.port", "8080")
                    setProperty("app.security.user", "admin")
                    setProperty("app.security.password", encoder.encode("secret"))
                    setProperty("app.security.roles", "USER")
                }

                Files.newOutputStream(configPath).use {
                    defaultProps.store(it, "Default Config Created")
                }
            }

            val props = Properties().apply {
                Files.newInputStream(configPath).use { load(it) }
            }

            txtPort.text = props.getProperty("server.port", "8080")
            txtUser.text = props.getProperty("app.security.user", "admin")
            txtRoles.text = props.getProperty("app.security.roles", "USER")

            btnSimpanConfig.setOnAction { simpanConfig() }
            btnBackUp.setOnAction { manualBackup() }
            btnRestore.setOnAction { pilihRestoreFile() }

        } catch (e: Exception) {
            showError("Error", "Gagal membaca config: ${e.message}")
        }
    }

    fun simpanConfig() {
        val props = Properties().apply {
            Files.newInputStream(configPath).use { load(it) }
        }

        if (txtNewPass.text.isNotEmpty()) {
            if (txtNewPass.text != txtConfirmPass.text) {
                showError("Gagal", "Password tidak sama!")
                return
            }
            props["app.security.password"] = encoder.encode(txtNewPass.text)
        }

        props["server.port"] = txtPort.text
        props["app.security.user"] = txtUser.text
        props["app.security.roles"] = txtRoles.text

        Files.newOutputStream(configPath).use { props.store(it, "Updated by UI") }

        showInfo("Sukses", "Konfigurasi berhasil disimpan.\nRestart server untuk menerapkan.")
    }


    fun showInfo(title: String, message: String, owner: Window? = null) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        owner?.let { alert.initOwner(it) }
        alert.showAndWait()
    }

    fun showError(title: String, message: String, owner: Window? = null) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        owner?.let { alert.initOwner(it) }
        alert.showAndWait()
    }


    /** 💾 Manual backup SQLLite */
    private fun manualBackup() {
        controllerScope.launch {
            println("🔄 Manual backup Sqlite berjalan...")

            try {
                // Jalankan backup data-only SQL di background thread
                val backupFileSQLLengkap = withContext(Dispatchers.IO) { dbService.backupSQLiteToSQL() }
                val backupFileSQL = withContext(Dispatchers.IO) { dbService.backupSQLiteDataOnly() }
                val sqliteFile = File("./data/cetak-stiker.db")

                if (!backupFileSQL.exists() || !sqliteFile.exists() || !backupFileSQLLengkap.exists()) {
                    println("❌ Backup gagal: file sumber tidak ditemukan.")
                    return@launch
                }

                // 🗂️ Pilih lokasi file ZIP pakai FileChooser (jalan di JavaFX thread)
                val selectedFile = showSaveFileChooser() ?: run {
                    println("⚠️ Backup dibatalkan oleh pengguna.")
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

                println("✅ Backup ZIP berhasil disimpan di: ${zipFile.absolutePath}")

            } catch (e: Exception) {
                println("❌ Backup gagal: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /** ♻️ Pilih file SQL untuk restore */
    private fun pilihRestoreFile() {

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
                println("ℹ️ Restore dibatalkan oleh pengguna.")
                return@runLater
            }

            // Jalankan proses restore di coroutine
            controllerScope.launch {
                try {
                    val fileName = file.name.lowercase()
                    val dbPath = DatabasePathFromProperties.getDatabasePathFromProperties()
                    val dbFile = File(dbPath)

                    println("♻️ Memulai proses restore dari file: ${file.name}")

                    // Jika file restore ".sql"
                    if (fileName.endsWith(".sql")) {
                        // 🔄 Restore data-only SQL
                        println("🧩 Mode: Restore SQL (data-only)")
                        dbService.restoreSQLiteDataOnly(file)
                        println("✅ Restore SQL selesai dari file: ${file.name}")

                    }
                    // Jika file restore nya ".db" copy langsung ke folder database, timpa database lama
                    else if (fileName.endsWith(".db") || fileName.endsWith(".sqlite")) {
                        // 🔄 Restore file database SQLite (replace file lama)
                        println("       🧩 Mode: Restore SQLite file")

                        if (!dbFile.exists()) {
                            dbFile.parentFile?.mkdirs()
                            dbFile.createNewFile()
                        }

                        Files.copy(
                            file.toPath(),
                            dbFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                        )

                        println("✅ File SQLite aktif telah diganti dari backup: ${file.name}")
                    } else {
                        println("❌ Jenis file tidak dikenali. Hanya mendukung .sql, .db, atau .sqlite")
                        return@launch
                    }

                    println("🔁 Silakan jalankan kembali server untuk menerapkan perubahan.")

                } catch (e: Exception) {
                    println("❌ Gagal restore database: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
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