package com.girsang.server.service

import com.girsang.server.model.OrderanStiker
import com.girsang.server.model.OrderanStikerRinci
import com.girsang.server.repository.OrderanStikerRepository
import com.girsang.server.repository.OrderanStikerRinciRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OrderanStikerService(
    private val repositoryOrderan: OrderanStikerRepository,
    private val repositoryRinci: OrderanStikerRinciRepository) {

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

    fun delete(orderan: OrderanStiker) = repositoryOrderan.delete(orderan)

    // Tambah rincian
    @Transactional
    fun addRinci(orderan: OrderanStiker, rincian: OrderanStikerRinci) {
        rincian.orderan = orderan
        orderan.rincian.add(rincian)
        updateTotal(orderan)
        repositoryOrderan.save(orderan)
    }

    // Hapus rincian
    @Transactional
    fun removeRinci(orderan: OrderanStiker, rincian: OrderanStikerRinci) {
        if (orderan.rincian.remove(rincian)) {
            rincian.stiker
            updateTotal(orderan)
            repositoryOrderan.save(orderan)
        }
    }
    // Edit rincian
    @Transactional
    fun updateRinci(orderan: OrderanStiker, rinciId: Long, rincianBaru: OrderanStikerRinci) {
        val rincianLama = orderan.rincian.find { it.id == rinciId }
        if (rincianLama != null) {
            rincianLama.stiker = rincianBaru.stiker ?: rincianLama.stiker
            rincianLama.jumlah = rincianBaru.jumlah
            updateTotal(orderan)
            repositoryOrderan.save(orderan)
        }
    }

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