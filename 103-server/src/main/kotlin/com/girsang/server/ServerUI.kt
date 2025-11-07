package com.girsang.server

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage

class ServerUI : Application() {

    companion object {
        var springContext: org.springframework.context.ConfigurableApplicationContext? = null
    }

    override fun start(stage: Stage) {
        val fxml = javaClass.getResource("/FXML/server.fxml")
            ?: throw IllegalStateException("FXML tidak ditemukan!")

        val loader = javafx.fxml.FXMLLoader(fxml)
        val root = loader.load<AnchorPane>()
        val scene = Scene(root)

        stage.title = "Server Data Cetak Stiker"
        stage.scene = scene
        stage.icons.addAll(
            Image("/img/icon/app-16.png"),
            Image("/img/icon/app-32.png"),
            Image("/img/icon/app-64.png"),
            Image("/img/icon/app-128.png"),
            Image("/img/icon/app-256.png")
        )
        stage.show()

        stage.setOnCloseRequest {
            println("Aplikasi ditutup...")
            springContext?.close()
            Platform.exit()
            System.exit(0)
        }
    }
}
