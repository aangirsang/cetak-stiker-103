package com.girsang.server

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import java.util.concurrent.atomic.AtomicReference

object ServerRunner {
    private val ctxRef = AtomicReference<ConfigurableApplicationContext?>()

    fun start(args: Array<String> = emptyArray()): Boolean {
        if (ctxRef.get() != null) return false
        val thread = Thread {
            val ctx = SpringApplicationBuilder(SpringApp::class.java)
                .headless(false)
                .run(*args)
            ctxRef.set(ctx)
            println("=== Environment properties ===")
            ctx?.environment?.propertySources?.forEach {
                println("Source: ${it.name}")
            }
            println("spring.datasource.url = ${ctx?.environment?.getProperty("spring.datasource.url")}")

        }
        thread.isDaemon = false
        thread.start()
        return true
    }

    fun stop(): Boolean {
        val ctx = ctxRef.getAndSet(null) ?: return false
        Thread { ctx.close() }.start()
        return true
    }

    fun isRunning(): Boolean = ctxRef.get() != null

    // 👉 Tambahkan fungsi ini (jika belum ada)
    fun getContext(): ConfigurableApplicationContext? = ctxRef.get()
}
