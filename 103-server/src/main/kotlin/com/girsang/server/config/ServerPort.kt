package com.girsang.server.config

import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class ServerPort : ApplicationListener<WebServerInitializedEvent> {

    companion object {
        var port: Int = 0
    }

    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        port = event.webServer.port
        println("✅ Spring Boot berjalan di port: $port")
    }
}