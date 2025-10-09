package com.girsang.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class SpringApp


// helper if you want to run spring normally
fun main(args: Array<String>) {
    runApplication<SpringApp>(*args)
}