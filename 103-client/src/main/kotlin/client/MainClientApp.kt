package com.girsang.client

import com.girsang.client.controller.MainClientAppController
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.util.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import kotlin.jvm.java
import kotlin.jvm.javaClass

class MainClientApp : Application() {
    override fun start(stage: Stage) {
        val icons = listOf(
            Image("/img/icon/app-16.png"),
            Image("/img/icon/app-32.png"),
            Image("/img/icon/app-64.png"),
            Image("/img/icon/app-128.png"),
            Image("/img/icon/app-256.png")
        )
        val loader = FXMLLoader(javaClass.getResource("/fxml/main-client-app.fxml"))
        val root = loader.load<BorderPane>()
        val controller = loader.getController<MainClientAppController>()

        // Label waktu
        val timeLabel = controller.lblWaktu
        val formatJam = DateTimeFormatter.ofPattern("HH:mm:ss")
        val formatTanggal = DateTimeFormatter.ofPattern("dd MMMM yyyy")
        val tanggal = LocalDate.now().format(formatTanggal)

        // Update jam real-time
        val timeline = Timeline(
            KeyFrame(
                Duration.seconds(1.0),
                EventHandler<ActionEvent> {
                    val jam = LocalTime.now().format(formatJam)
                    timeLabel.text = "$tanggal   $jam"
                }
            )
        )
        timeline.cycleCount = Timeline.INDEFINITE
        timeline.play()

        // Tampilkan stage utama
        stage.title = "Aplikasi Client Data Cetak Stiker"
        stage.scene = Scene(root)
        stage.isMaximized = true
        stage.icons.addAll(icons)
        stage.show()
    }
}

fun main() = Application.launch(MainClientApp::class.java)
