package com.girsang.server

import javafx.application.Application
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class SpringApp


// helper if you want to run spring normally
fun main(args: Array<String>) {
    // Jalankan JavaFX App
    Application.launch(ServerUI::class.java, *args)
}