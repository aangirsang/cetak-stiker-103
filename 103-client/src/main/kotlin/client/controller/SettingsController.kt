package client.controller

import client.util.PesanPeringatan
import com.girsang.client.config.ClientConfig
import javafx.fxml.FXML
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.mindrot.jbcrypt.BCrypt

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
        val pass = BCrypt.hashpw(txtPass.text, BCrypt.gensalt())
        ClientConfig.setUrl(txtUrl.text)
        ClientConfig.setUser(txtUser.text)
        ClientConfig.setPass(pass)
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