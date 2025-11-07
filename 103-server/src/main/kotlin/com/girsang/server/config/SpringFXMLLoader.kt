package com.girsang.server.config

import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.util.Callback
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.net.URL

@Component
class SpringFXMLLoader(private val context: ApplicationContext) {

    fun load(url: URL): Any {
        val loader = FXMLLoader(url)
        loader.controllerFactory = Callback { clazz: Class<*> ->
            context.getBean(clazz)
        }
        return loader.load()
    }
}
