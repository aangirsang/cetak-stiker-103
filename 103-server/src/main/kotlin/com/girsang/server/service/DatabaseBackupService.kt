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
        val dir = File("./backup/SQL-File")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun timestamp(): String {
        val fmt = SimpleDateFormat("yyyyMMdd-HHmmss")
        return fmt.format(Date())
    }

    suspend fun backupOtomatis(limit: Int) {
        val ts = timestamp() // ✅ timestamp tunggal

        backupSQLiteCopy(ts)
        backupSQLiteDataOnly()
        hapusFileLama(limit)
    }


    /**
     * 🔹 Backup cepat — hanya copy file .db
     */
    suspend fun backupSQLiteCopy(ts: String): File = withContext(Dispatchers.IO) {
        val dbFile = getDatabaseFile()
        val backupFile = File(getBackupDir(), "backup-sqlite-$ts.db")

        if (!dbFile.exists()) throw IllegalStateException("Database file tidak ditemukan: ${dbFile.absolutePath}")

        try {
            val conn = DriverManager.getConnection("jdbc:sqlite:$dbUrl")
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA wal_checkpoint(FULL);")
                stmt.execute("PRAGMA optimize;")
            }
            conn.close()
        } catch (e: Exception) {
            println("⚠️ Gagal flush SQLite sebelum backup: ${e.message}")
        }

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
        val backupFile = File(getBackupDir(), "backup-sql-${timestamp()}.sql")

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
     * ✅ Backup hanya DATA ke file SQL
     *    Tidak termasuk struktur tabel (CREATE TABLE)
     */
    suspend fun backupSQLiteDataOnly(): File = withContext(Dispatchers.IO) {
        val url = "jdbc:sqlite:$dbUrl"
        val backupFile = File(getBackupDirSQL(), "backup-sql-data-only-${timestamp()}.sql")
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

    /**
     * 🔹 Hapus file backup lama per tipe (db dan sql),
     *    hanya simpan sejumlah 'limit' file terbaru untuk masing-masing.
     */
    private fun hapusFileLama(limit: Int = 5) {
        val dirDb = getBackupDir()
        val dirSql = getBackupDirSQL()

        // === Hapus file .db lama ===
        val dbFiles = dirDb.listFiles { file -> file.extension == "db" }
            ?.sortedBy { it.lastModified() } ?: emptyList() // 🔹 Urut paling lama → paling baru

        if (dbFiles.size > limit) {
            val toDelete = dbFiles.take(dbFiles.size - limit) // 🔹 ambil yang paling lama untuk dihapus
            toDelete.forEach {
                if (it.delete()) println("🗑️ Hapus backup DB lama: ${it.name}")
                else println("⚠️ Gagal hapus backup DB: ${it.name}")
            }
        } else {
            println("✅ Tidak ada backup DB lama yang perlu dihapus (${dbFiles.size}/$limit).")
        }

        // === Hapus file .sql lama ===
        val sqlFiles = dirSql.listFiles { file -> file.extension == "sql" }
            ?.sortedBy { it.lastModified() } ?: emptyList() // 🔹 Urut paling lama → paling baru

        if (sqlFiles.size > limit) {
            val toDelete = sqlFiles.take(sqlFiles.size - limit)
            toDelete.forEach {
                if (it.delete()) println("🗑️ Hapus backup SQL lama: ${it.name}")
                else println("⚠️ Gagal hapus backup SQL: ${it.name}")
            }
        } else {
            println("✅ Tidak ada backup SQL lama yang perlu dihapus (${sqlFiles.size}/$limit).")
        }
    }


}
