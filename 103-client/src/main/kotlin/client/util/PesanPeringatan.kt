package client.util

import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.stage.Window

object PesanPeringatan {
    fun info(title: String, message: String, owner: Window? = null) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        owner?.let { alert.initOwner(it) }
        alert.showAndWait()
    }

    fun warning(title: String, message: String, owner: Window? = null) {
        val alert = Alert(Alert.AlertType.WARNING)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        owner?.let { alert.initOwner(it) }
        alert.showAndWait()
    }

    fun error(title: String, message: String, owner: Window? = null) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        owner?.let { alert.initOwner(it) }
        alert.showAndWait()
    }

    fun confirm(title: String, message: String, owner: Window? = null): Boolean {
        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        owner?.let { alert.initOwner(it) }

        val result = alert.showAndWait()
        return result.isPresent && result.get() == ButtonType.OK
    }

    fun confirmWithSettings(
        title: String,
        message: String,
        owner: Window? = null
    ): String? {
        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        owner?.let { alert.initOwner(it) }

        // Buat tombol custom
        val okButton = ButtonType("OK", ButtonBar.ButtonData.OK_DONE)
        val settingsButton = ButtonType("Setting", ButtonBar.ButtonData.OTHER)
        val cancelButton = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)

        alert.buttonTypes.setAll(okButton, settingsButton, cancelButton)

        val result = alert.showAndWait()

        return when(result.orElse(cancelButton)) {
            okButton -> "OK"
            settingsButton -> "SETTING"
            cancelButton -> "CANCEL"
            else -> null
        }
    }
}