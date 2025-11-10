package com.girsang.server.config

import java.io.File
import java.util.Properties

object DatabasePathFromProperties {
    fun getDatabasePathFromProperties(): String {
        val props = Properties()
        val propFile = File("./103-server/src/main/resources/application.properties") // ubah sesuai lokasi file-mu

        if (!propFile.exists()) {
            println("⚠️ File application.properties tidak ditemukan: ${propFile.absolutePath}")
            return "./data/cetak-stiker.db" // fallback default
        }

        propFile.inputStream().use { props.load(it) }

        val url = props.getProperty("spring.datasource.url") ?: "jdbc:sqlite:./data/cetak-stiker.db"
        val dbPath = url.substringAfter("jdbc:sqlite:").trim()

        return dbPath
    }
}