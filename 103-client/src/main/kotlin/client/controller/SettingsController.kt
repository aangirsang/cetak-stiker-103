package client.controller

import com.girsang.client.config.ClientConfig
import javafx.fxml.FXML
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.stage.Stage

class SettingsController {

    @FXML private lateinit var txtUrl: TextField
    @FXML private lateinit var txtUser: TextField
    @FXML private lateinit var txtPass: PasswordField

    @FXML
    fun initialize() {
        txtUrl.text = ClientConfig.getUrl()
        txtUser.text = ClientConfig.getUser()
        txtPass.text = ClientConfig.getPass()
    }

    @FXML
    fun simpan() {
        ClientConfig.setUrl(txtUrl.text)
        ClientConfig.setUser(txtUser.text)
        ClientConfig.setPass(txtPass.text)
        ClientConfig.save()

        javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION,
            "Konfigurasi disimpan.\nSilakan restart aplikasi."
        ).showAndWait()

        (txtUrl.scene.window as Stage).close()
    }
}