package com.girsang.server.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.util.*

class DatabaseBackupService(
    private val dbUrl: String,   // contoh: "jdbc:sqlite:./data/cetak-stiker.db"
) {

    private fun getDatabaseFile(): File {
        val path = dbUrl.removePrefix("jdbc:sqlite:")
        return File(path)
    }

    private fun getBackupDir(): File {
        val dir = File("./backup")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    private fun getBackupDirSQL(): File {
        val dir = File("./backup/sql-file")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun timestamp(): String {
        val fmt = SimpleDateFormat("yyyyMMdd-HHmmss")
        return fmt.format(Date())
    }

    /**
     * 🔹 Backup cepat — hanya copy file .db apa adanya
     */
    suspend fun backupSQLiteCopy(): File = withContext(Dispatchers.IO) {
        val dbFile = getDatabaseFile()
        val backupFile = File(getBackupDir(), "sqlite-backup-${timestamp()}.db")

        if (!dbFile.exists()) throw IllegalStateException("Database file tidak ditemukan: ${dbFile.absolutePath}")

        Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        println("✅ Backup SQLite sukses: ${backupFile.name}")
        return@withContext backupFile
    }

    /**
     * 🔹 Backup dalam bentuk SQL Dump (struktur + data)
     *    Bisa dibuka & dibaca manual, atau restore ke DB lain.
     */
    suspend fun backupSQLiteToSQL(): File = withContext(Dispatchers.IO) {
        val dbFile = getDatabaseFile()
        val backupFile = File(getBackupDirSQL(), "sqlite-backup-${timestamp()}.sql")

        if (!dbFile.exists()) throw IllegalStateException("Database file tidak ditemukan: ${dbFile.absolutePath}")

        val url = "jdbc:sqlite:$dbUrl"
        println(url)
        val conn = DriverManager.getConnection(url)
        val stmt = conn.createStatement()

        val writer = FileWriter(backupFile)

        // Tulis header
        writer.write("-- SQLite Backup Dump\n")
        writer.write("-- Tanggal: ${Date()}\n\n")

        // Dapatkan daftar tabel
        val tables = mutableListOf<String>()
        val rsTables = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';")
        while (rsTables.next()) tables.add(rsTables.getString("name"))
        rsTables.close()

        // Untuk setiap tabel, tulis CREATE TABLE dan INSERT
        for (table in tables) {
            // Struktur tabel
            val rsCreate = stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='$table';")
            if (rsCreate.next()) {
                writer.write("${rsCreate.getString("sql")};\n\n")
            }
            rsCreate.close()

            // Data tabel
            val rsData = stmt.executeQuery("SELECT * FROM $table;")
            val meta = rsData.metaData
            val columnCount = meta.columnCount

            while (rsData.next()) {
                val values = (1..columnCount).joinToString(", ") { i ->
                    val value = rsData.getObject(i)
                    if (value == null) "NULL"
                    else "'${value.toString().replace("'", "''")}'"
                }
                writer.write("INSERT INTO $table VALUES ($values);\n")
            }
            writer.write("\n")
            rsData.close()
        }

        writer.flush()
        writer.close()
        stmt.close()
        conn.close()

        println("✅ Backup SQL sukses: ${backupFile.name}")
        return@withContext backupFile
    }

    /**
     * 🔹 Restore database dari file .db (copy langsung)
     */
    suspend fun restoreSQLite(dbBackupFile: File): Unit = withContext(Dispatchers.IO) {
        val dbFile = getDatabaseFile()

        if (!dbBackupFile.exists()) throw IllegalArgumentException("File backup tidak ditemukan: ${dbBackupFile.absolutePath}")

        try {
            if (dbFile.exists()) dbFile.delete()
            Files.copy(dbBackupFile.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            println("✅ Restore SQLite berhasil dari file: ${dbBackupFile.name}")
        } catch (e: Exception) {
            println("❌ Gagal restore SQLite: ${e.message}")
            throw e
        }
    }

    /**
     * 🔹 Restore database dari file SQL dump
     */
    suspend fun restoreSQLiteFromSQL(sqlFile: File): Unit = withContext(Dispatchers.IO) {
        if (!sqlFile.exists()) throw IllegalArgumentException("File SQL tidak ditemukan: ${sqlFile.absolutePath}")

        val url = "jdbc:sqlite:$dbUrl"
        val conn = DriverManager.getConnection(url)
        val stmt = conn.createStatement()
        val sqlText = sqlFile.readText()

        // Pisahkan per baris per perintah SQL
        val sqlCommands = sqlText.split(";")
        var count = 0

        conn.autoCommit = false
        try {
            for (cmd in sqlCommands) {
                val trimmed = cmd.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("--")) {
                    stmt.execute(trimmed)
                    count++
                }
            }
            conn.commit()
            println("✅ Restore SQL selesai ($count perintah).")
        } catch (e: Exception) {
            conn.rollback()
            println("❌ Gagal restore SQL: ${e.message}")
            throw e
        } finally {
            stmt.close()
            conn.close()
        }
    }

    /**
     * ✅ Backup hanya DATA ke file SQL
     *    Tidak termasuk struktur tabel (CREATE TABLE)
     */
    suspend fun backupSQLiteDataOnly(): File = withContext(Dispatchers.IO) {
        val url = "jdbc:sqlite:$dbUrl"
        val backupFile = File(getBackupDirSQL(), "sqlite-data-backup-${timestamp()}.sql")
        val conn = DriverManager.getConnection(url)
        val stmt = conn.createStatement()
        val writer = FileWriter(backupFile)

        writer.write("-- SQLite Data Backup\n")
        writer.write("-- Generated: ${Date()}\n\n")

        // Ambil semua nama tabel user (bukan internal sqlite)
        val tables = mutableListOf<String>()
        val rsTables = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';"
        )
        while (rsTables.next()) tables.add(rsTables.getString("name"))
        rsTables.close()

        // Tulis semua INSERT
        for (table in tables) {
            val rs = stmt.executeQuery("SELECT * FROM $table;")
            val meta = rs.metaData
            val colCount = meta.columnCount

            while (rs.next()) {
                val values = (1..colCount).joinToString(", ") { i ->
                    val value = rs.getObject(i)
                    if (value == null) "NULL"
                    else "'${value.toString().replace("'", "''")}'"
                }

                // gunakan INSERT OR IGNORE agar duplikat di-skip saat restore
                writer.write("INSERT OR IGNORE INTO $table VALUES ($values);\n")
            }
            writer.write("\n")
            rs.close()
        }

        writer.flush()
        writer.close()
        stmt.close()
        conn.close()

        println("✅ Backup data-only SQL sukses: ${backupFile.name}")
        return@withContext backupFile
    }

    /**
     * ✅ Restore data dari file SQL
     *    Tidak hapus data lama, hanya tambahkan data baru
     *    Duplikat ID otomatis diabaikan (karena pakai INSERT OR IGNORE)
     */
    suspend fun restoreSQLiteDataOnly(sqlFile: File): Unit = withContext(Dispatchers.IO) {
        if (!sqlFile.exists()) throw IllegalArgumentException("File SQL tidak ditemukan: ${sqlFile.absolutePath}")

        val url = "jdbc:sqlite:$dbUrl"
        val conn = DriverManager.getConnection(url)
        val stmt = conn.createStatement()
        val sqlText = sqlFile.readText()

        val sqlCommands = sqlText.split(";")
        var count = 0

        conn.autoCommit = false
        try {
            for (cmd in sqlCommands) {
                val trimmed = cmd.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("--")) {
                    stmt.execute(trimmed)
                    count++
                }
            }
            conn.commit()
            println("✅ Restore data-only SQL selesai ($count perintah).")
        } catch (e: Exception) {
            conn.rollback()
            println("❌ Gagal restore data SQL: ${e.message}")
            throw e
        } finally {
            stmt.close()
            conn.close()
        }
    }
}
