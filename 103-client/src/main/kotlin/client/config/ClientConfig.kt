package com.girsang.client.config

import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.util.*

object ClientConfig {

    private const val FILE_PATH = "config.properties" // file external di folder aplikasi

    private val props = Properties()

    init {
        load()
    }

    fun load() {
        try {
            val file = File(FILE_PATH)
            if (file.exists()) {
                FileInputStream(file).use { props.load(it) }
            } else {
                // fallback ke classpath
                val defaultProps = Properties()
                javaClass.classLoader.getResourceAsStream("application.properties")?.use {
                    defaultProps.load(it)
                }

                props.putAll(defaultProps)
                save() // simpan sebagai config pertama kali
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun save() {
        FileWriter(FILE_PATH).use { writer ->
            props.store(writer, "Client Configuration")
        }
    }

    fun getUrl(): String = props.getProperty("client.server.url", "")
    fun getUser(): String = props.getProperty("client.server.user", "")
    fun getPass(): String = props.getProperty("client.server.pass", "")

    fun setUrl(v: String) { props.setProperty("client.server.url", v) }
    fun setUser(v: String) { props.setProperty("client.server.user", v) }
    fun setPass(v: String) { props.setProperty("client.server.pass", v) }
}