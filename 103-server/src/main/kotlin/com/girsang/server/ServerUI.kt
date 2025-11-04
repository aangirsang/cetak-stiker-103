package com.girsang.server

import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.springframework.context.ConfigurableApplicationContext

class ServerUI : Application() {


    override fun start(primaryStage: Stage) {
        val icons = listOf(
            Image("/img/icon/app-16.png"),
            Image("/img/icon/app-32.png"),
            Image("/img/icon/app-64.png"),
            Image("/img/icon/app-128.png"),
            Image("/img/icon/app-256.png")
        )
        val fxml = javaClass.getResource("/FXML/server.fxml")
        val root = FXMLLoader.load<AnchorPane>(fxml)
        val scene = Scene(root)
        primaryStage.title = "Server Data Cetak Stiker"
        primaryStage.scene = scene
        primaryStage.icons.addAll(icons)
        primaryStage.show()
        primaryStage.setOnCloseRequest {
            println("Aplikasi ditutup, menjalankan cleanup...")

            // contoh: simpan log, tutup koneksi DB, dll
            tutupKoneksiDatabase()

            // keluar dari aplikasi
            System.exit(0)
        }
    }
}
private fun tutupKoneksiDatabase(){
    var serverContext: ConfigurableApplicationContext? = null
    var serverJob: Job? = null
    GlobalScope.launch(Dispatchers.IO) {
        try {
            serverContext?.close()
            serverJob?.cancel()
        } catch (e: Exception) {
            Platform.runLater {e.message}
        }
    }
}

fun main() {
    Application.launch(ServerUI::class.java)
}