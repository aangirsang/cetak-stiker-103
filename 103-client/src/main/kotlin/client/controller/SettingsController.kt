package client.controller

import client.util.PesanPeringatan
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
        val confirm = PesanPeringatan.confirm(
            "Simpan Setting Server",
            "Apakah anda yakin ingin menyimpan settingan ini?\n\n" +
                    "Settingan yang salah dapat menyebabkan aplikasi tidak bisa terhubung ke server!"
        )
        if(confirm){
            ClientConfig.save()
            PesanPeringatan.info("Simpan Setting Server","Konfigurasi disimpan.\nSilakan restart aplikasi.")
            (txtUrl.scene.window as Stage).close()
        }
    }
    @FXML
    fun batal() {
        (txtUrl.scene.window as Stage).close()
    }
}