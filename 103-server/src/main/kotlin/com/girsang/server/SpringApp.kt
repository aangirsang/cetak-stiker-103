package com.girsang.server

import javafx.application.Application
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class SpringApp

fun main(args: Array<String>) {
    Application.launch(ServerUI::class.java, *args)
}