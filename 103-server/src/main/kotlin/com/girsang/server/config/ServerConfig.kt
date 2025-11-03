package com.girsang.server.config

import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.util.*

object ServerConfig {

    private const val FILE_PATH = "./config/server-config.properties" // file external di folder aplikasi

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
                javaClass.classLoader.getResourceAsStream("./config/client-config.properties")?.use {
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
            props.store(writer, "Server Configuration")
        }
    }

    fun getUrl(): String = props.getProperty("server.server.url", "")
    fun getUser(): String = props.getProperty("server.server.user", "")
    fun getPass(): String = props.getProperty("server.server.pass", "")

    fun setUrl(v: String) { props.setProperty("server.server.url", v) }
    fun setUser(v: String) { props.setProperty("server.server.user", v) }
    fun setPass(v: String) { props.setProperty("server.server.pass", v) }
}