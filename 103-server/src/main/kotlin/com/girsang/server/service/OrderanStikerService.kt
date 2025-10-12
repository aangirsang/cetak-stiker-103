package com.girsang.server.service

import com.girsang.server.model.OrderanStiker
import com.girsang.server.model.OrderanStikerRinci
import com.girsang.server.repository.OrderanStikerRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OrderanStikerService(private val repository: OrderanStikerRepository) {

    fun findAll(): List<OrderanStiker> = repository.findAll()

    fun findById(id: Long): OrderanStiker? = repository.findById(id).orElse(null)

    fun save(orderan: OrderanStiker): OrderanStiker {
        if (orderan.faktur.isBlank()) {
            orderan.faktur = generateFaktur()
        }
        orderan.rincian.forEach { it.orderan = orderan }
        updateTotal(orderan)
        return repository.save(orderan)
    }

    fun delete(orderan: OrderanStiker) = repository.delete(orderan)

    // Tambah rincian
    @Transactional
    fun addRinci(orderan: OrderanStiker, rincian: OrderanStikerRinci) {
        rincian.orderan = orderan
        orderan.rincian.add(rincian)
        updateTotal(orderan)
        repository.save(orderan)
    }

    // Hapus rincian
    @Transactional
    fun removeRinci(orderan: OrderanStiker, rincian: OrderanStikerRinci) {
        if (orderan.rincian.remove(rincian)) {
            rincian.stiker
            updateTotal(orderan)
            repository.save(orderan)
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
            repository.save(orderan)
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
        val lastFaktur = repository.findLastFakturByYear(tahunFull)

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