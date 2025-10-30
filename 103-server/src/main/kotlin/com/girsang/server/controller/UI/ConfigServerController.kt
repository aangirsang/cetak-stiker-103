package com.girsang.server.controller.UI

import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.stage.Window
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class ConfigServerController {

    @FXML private lateinit var txtPort: TextField
    @FXML private lateinit var txtUser: TextField
    @FXML private lateinit var txtNewPass: PasswordField
    @FXML private lateinit var txtConfirmPass: PasswordField
    @FXML private lateinit var txtRoles: TextField

    private val configPath = Paths.get("config", "application.properties")
    private val encoder = BCryptPasswordEncoder()

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
                    setProperty("app.security.password", encoder.encode("admin"))
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

        } catch (e: Exception) {
            showError("Error", "Gagal membaca config: ${e.message}")
        }
    }

    @FXML
    fun saveConfig() {
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
}