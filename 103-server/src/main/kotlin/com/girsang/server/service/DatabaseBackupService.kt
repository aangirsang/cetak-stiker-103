package com.girsang.server.service

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.copyTo

@Service
class DatabaseBackupService(
    @Value("\${spring.datasource.url}") private val dbUrl: String,
    @Value("\${spring.datasource.username}") private val dbUser: String,
    @Value("\${spring.datasource.password}") private val dbPass: String
) {

    private val backupDir = File("./backup")
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    private val dbFile = File("./data/serverdb.mv.db")

    init { if (!backupDir.exists()) backupDir.mkdirs() }

    /** ✅ Backup otomatis saat aplikasi mulai */
    @EventListener(ApplicationReadyEvent::class)
    fun backupOnStart() {
        backupSql()
        backupFile()
        hapusBackUpFileMax(6)
    }

    /** ✅ Backup setiap hari jam 02:00 */
    @Scheduled(cron = "0 0 2 * * *")
    fun scheduledBackup() {
        backupSql()
        backupFile()
        hapusBackUpFileMax(6)
    }

    /** ✅ Backup SQL */
    fun backupSql(): String {
        val fileName = "backup-${now()}.sql"
        val filePath = File(backupDir, fileName).absolutePath
        try {
            Class.forName("org.h2.Driver")
            DriverManager.getConnection(dbUrl, dbUser, dbPass).use {
                it.createStatement().execute("SCRIPT TO '$filePath'")
            }
            println("✅ SQL Backup: $fileName")
            return filePath
        } catch (e: Exception) {
            println("❌ Gagal SQL backup: ${e.message}")
            return ""
        }
    }

    /** ✅ Copy file .mv.db */
    fun backupFile(): String {
        val fileName = "serverdb-backup-${now()}.zip"
        val backupPath = File(backupDir, fileName).absolutePath

        return try {
            Class.forName("org.h2.Driver")
            DriverManager.getConnection(dbUrl, dbUser, dbPass).use { conn ->
                conn.createStatement().execute("BACKUP TO '$backupPath'")
            }
            println("✅ H2 Backup: $fileName")
            backupPath
        } catch (e: Exception) {
            println("❌ Gagal H2 BACKUP: ${e.message}")
            ""
        }
    }

    fun hapusBackUpFileMax(maxFiles: Int = 5) {
        // Ambil semua file backup .sql & .zip
        val files = backupDir.listFiles { file ->
            file.extension == "sql" || file.extension == "zip"
        } ?: return

        // Jika jumlah sudah <= batas, tidak perlu hapus
        if (files.size <= maxFiles) return

        // Sort file berdasarkan lastModified() → terbaru dulu
        val sorted = files.sortedByDescending { it.lastModified() }

        // File yang harus dihapus (mulai dari urutan ke-6 dst)
        val toDelete = sorted.drop(maxFiles)

        toDelete.forEach { file ->
            if (file.delete()) {
                println("🗑️ Hapus backup lama (exceeded max): ${file.name}")
            } else {
                println("⚠️ Gagal hapus backup: ${file.name}")
            }
        }
    }

    private fun now() = LocalDateTime.now().format(formatter)

    /** ✅ Restore jika corrupt */
    fun restoreFrom(file: File) {
        DriverManager.getConnection(dbUrl, dbUser, dbPass).use {
            it.createStatement().execute("RUNSCRIPT FROM '${file.absolutePath}'")
        }
        println("✅ Database berhasil direstore dari ${file.name}")
    }
}