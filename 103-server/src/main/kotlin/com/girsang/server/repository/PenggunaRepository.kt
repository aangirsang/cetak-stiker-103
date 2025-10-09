package com.girsang.server.repository

import com.girsang.server.model.Pengguna
import org.springframework.data.jpa.repository.JpaRepository

interface PenggunaRepository : JpaRepository<Pengguna, Long> {
    fun findByNamaAkun(namaAkun: String): Pengguna?
}