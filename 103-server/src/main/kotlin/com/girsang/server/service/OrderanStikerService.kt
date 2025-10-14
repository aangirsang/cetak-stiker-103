package com.girsang.server.service

import com.girsang.server.dto.OrderanStikerDTO
import com.girsang.server.model.DataStiker
import com.girsang.server.model.OrderanStiker
import com.girsang.server.model.OrderanStikerRinci
import com.girsang.server.repository.DataStikerRepository
import com.girsang.server.repository.OrderanStikerRepository
import com.girsang.server.repository.OrderanStikerRinciRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OrderanStikerService(
    private val repositoryOrderan: OrderanStikerRepository,
    private val repositoryRinci: OrderanStikerRinciRepository,
    private val repositoryStiker: DataStikerRepository) {

    fun findRinciByOrderanId(orderanId: Long): List<OrderanStikerRinci> {
        return repositoryRinci.findByOrderanId(orderanId)
    }

    fun findAll(): List<OrderanStiker> = repositoryOrderan.findAll()

    fun findById(id: Long): OrderanStiker? = repositoryOrderan.findById(id).orElse(null)

    fun save(orderan: OrderanStiker): OrderanStiker {
        if (orderan.faktur.isBlank()) {
            orderan.faktur = generateFaktur()
        }
        orderan.rincian.forEach { it.orderan = orderan }
        updateTotal(orderan)
        return repositoryOrderan.save(orderan)
    }

    fun update(id: Long, dto: OrderanStikerDTO): OrderanStiker {
        val existing = repositoryOrderan.findById(id)
            .orElseThrow { NoSuchElementException("Orderan tidak ditemukan") }

        existing.tanggal = dto.tanggal
        existing.totalStiker = dto.totalStiker
        existing.faktur = dto.faktur
        existing.umkm = dto.umkm

        // Hapus rincian lama (karena orphanRemoval = true)
        existing.rincian.clear()

        // Tambahkan rincian baru
        dto.rincian.forEach { rincianDTO ->
            val stiker = rincianDTO.stikerId?.let { stikerId ->
                repositoryStiker.findById(stikerId).orElseThrow {
                    IllegalArgumentException("Stiker dengan id $stikerId tidak ditemukan")
                }
            }

            val rincianBaru = OrderanStikerRinci(
                jumlah = rincianDTO.jumlah,
                stiker = rincianDTO.stiker,
                orderan = existing
            )
            println("Stiker disimpan nama stiker: ${rincianBaru.stiker}, jumlah: ${rincianBaru.jumlah}, Orderan: ${rincianBaru.orderan?.faktur}")
            existing.rincian.add(rincianBaru)
        }

        // Update total
        existing.totalStiker = existing.rincian.sumOf { it.jumlah }

        return repositoryOrderan.save(existing)
    }

    fun delete(orderan: OrderanStiker) = repositoryOrderan.delete(orderan)

    // Hitung ulang total stiker
    private fun updateTotal(orderan: OrderanStiker) {
        orderan.totalStiker = orderan.rincian.sumOf { it.jumlah }
    }

    fun generateFaktur(): String {
        val now = LocalDateTime.now()
        val tahunFull = now.year
        val tahunShort = tahunFull % 100  // contoh: 2025 -> 25
        val bulan = String.format("%02d", now.monthValue)

        // Ambil faktur terakhir untuk tahun ini
        val lastFaktur = repositoryOrderan.findLastFakturByYear(tahunFull)

        // Tentukan nomor urut berikutnya
        val nextUrut = if (lastFaktur != null && lastFaktur.length >= 9) {
            val lastUrut = lastFaktur.takeLast(3).toIntOrNull() ?: 0
            lastUrut + 1
        } else {
            1
        }

        val urutStr = String.format("%03d", nextUrut)
        return "RBBB-${tahunShort}${bulan}${urutStr}"
    }
}