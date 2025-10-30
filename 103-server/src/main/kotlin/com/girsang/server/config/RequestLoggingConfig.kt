package com.girsang.server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter

@Configuration
class RequestLoggingConfig {

    @Bean
    fun requestLogger(): CommonsRequestLoggingFilter {
        return CommonsRequestLoggingFilter().apply {
            setIncludeClientInfo(true)
            setIncludeQueryString(true)
            setIncludePayload(true)
            setIncludeHeaders(false)   // kalau mau header juga, ubah jadi true
            setMaxPayloadLength(10000) // limit size request body
        }
    }
}