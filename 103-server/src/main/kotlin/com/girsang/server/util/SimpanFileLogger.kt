package com.girsang.server.util

import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object SimpanFileLogger {


    // Folder log (bisa kamu ubah sesuai kebutuhan)
    private const val LOG_DIR = "logs"

    // Formatter untuk nama file dan waktu log
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    // Lock agar aman dipakai multithread (banyak request)
    private val lock = ReentrantLock()

    /**
     * Tulis pesan ke file log harian
     */
    fun log(pesan: String) {
        lock.withLock() {
            try {
                val tanggal = LocalDate.now()
                val waktu = LocalDateTime.now().format(timeFormatter)

                // Buat folder kalau belum ada
                val folder = File(LOG_DIR)
                if (!folder.exists()) folder.mkdirs()

                // Buat file log berdasarkan tanggal
                val logFile = File("$LOG_DIR/log-$tanggal.txt")

                // Format baris log
                val baris = "$waktu | $pesan\n"

                // Tulis ke file (append)
                FileWriter(logFile, true).use { it.write(baris) }

            } catch (e: IOException) {
                println("Gagal menulis log ke file: ${e.message}")
            }
        }
    }

    /**
     * Log dengan kategori (misal "INFO", "ERROR", dsb)
     */
    fun log(level: String, pesan: String) {
        log("[$level] $pesan")
    }

    /**
     * Shortcut untuk log info
     */
    fun info(pesan: String) = log("INFO", pesan)

    /**
     * Shortcut untuk log error
     */
    fun error(pesan: String) = log("ERROR", pesan)
}