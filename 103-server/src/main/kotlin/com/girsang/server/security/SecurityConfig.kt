package com.girsang.server.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun userDetailsService(): UserDetailsService {
        val user = User.withUsername("admin")
            .password("{noop}secret") // penting: gunakan {noop}
            .roles("USER")
            .build()
        return InMemoryUserDetailsManager(user)
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .headers { it.frameOptions { frame -> frame.sameOrigin() } }
            .authorizeHttpRequests {
                it.requestMatchers("/h2-console/**").permitAll()
                it.requestMatchers("/api/pengguna/ping").permitAll()
                it.requestMatchers("/api/**").authenticated()
                it.anyRequest().permitAll()
            }
            .httpBasic { } // Basic Auth aktif

        return http.build()
    }
}
