package com.girsang.server.service

import com.girsang.server.model.Pengguna
import com.girsang.server.repository.PenggunaRepository
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class PenggunaService(private val repo: PenggunaRepository) {

    fun semuaPengguna(): List<Pengguna> = repo.findAll()

    fun cariById(id: Long): Optional<Pengguna> = repo.findById(id)

    fun findByNamaAkun(namaAkun: String): Pengguna? = repo.findByNamaAkun(namaAkun)

    fun simpan(pengguna: Pengguna): Pengguna = repo.save(pengguna)

    fun update(id: Long, penggunaBaru: Pengguna): Pengguna {
        val existing = repo.findById(id).orElseThrow { NoSuchElementException() }
        existing.namaAkun = penggunaBaru.namaAkun
        existing.namaLengkap = penggunaBaru.namaLengkap
        existing.kataSandi = penggunaBaru.kataSandi
        return repo.save(existing)
    }

    fun hapus(id: Long) {
        if (repo.existsById(id)) {
            repo.deleteById(id)
        } else {
            throw NoSuchElementException("Pengguna dengan id $id tidak ditemukan")
        }
    }
}