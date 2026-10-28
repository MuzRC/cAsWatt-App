package com.caswatt.app

import android.graphics.Color

object BatteryHealthCalculator {

    // 1. Kira Anggaran Kapasiti daripada Sesi Pengecasan Sah (Coulomb Counting)
    fun calculateSessionCapacity(startLevel: Int, endLevel: Int, mahDelivered: Double): Double {
        val gainPct = endLevel - startLevel
        if (gainPct <= 0 || mahDelivered <= 0.0) return 0.0
        return (mahDelivered / gainPct.toDouble()) * 100.0
    }

    // 2. Kira Purata Bergerak (Rolling Average) Kapasiti Sebenar
    fun calculateEstimatedCapacity(
        samples: List<CalibrationSample>,
        fallbackCapacity: Double
    ): Double {
        if (samples.isEmpty()) return fallbackCapacity
        val sum = samples.sumOf { it.calculatedCapacity }
        return sum / samples.size.toDouble()
    }

    // 3. Tahap Keyakinan Kalibrasi
    fun getConfidenceInfo(sampleCount: Int): Pair<String, Int> {
        return when {
            sampleCount >= 5 -> Pair("Tinggi & Stabil ($sampleCount sesi)", Color.parseColor("#00E676"))
            sampleCount in 2..4 -> Pair("Sederhana ($sampleCount/5 sesi)", Color.parseColor("#80D8FF"))
            sampleCount == 1 -> Pair("Rendah (1/5 sesi)", Color.parseColor("#FFD54F"))
            else -> Pair("Belum Ditentukur (0/5)", Color.parseColor("#90A4AE"))
        }
    }

    // 4. Status Klinikal Kesihatan Bateri
    fun getServiceStatus(healthPct: Double): Pair<String, Int> {
        return when {
            healthPct >= 80.0 -> Pair("Prestasi Puncak (Sihat & Baik)", Color.parseColor("#00E676"))
            healthPct >= 70.0 -> Pair("Prestasi Mula Susut (Masih Selamat)", Color.parseColor("#FFD54F"))
            else -> Pair("Servis Disyorkan ⚠️ (Penuaan Ketara)", Color.parseColor("#FF5252"))
        }
    }

    // 5. Anggaran Rintangan Dalaman (Internal Resistance - mOhm)
    fun estimateInternalResistance(deltaV: Double, deltaI_Amps: Double): Double {
        if (deltaI_Amps <= 0.05 || deltaV <= 0.0) return 0.0
        val rOhm = deltaV / deltaI_Amps
        val rMilliOhm = rOhm * 1000.0
        return rMilliOhm.coerceIn(20.0, 450.0)
    }

    // 6. Bina Objek Rumusan Penuh Kesihatan
    fun buildHealthData(
        designCapacity: Double,
        samples: List<CalibrationSample>,
        totalCycles: Double,
        lastKnownResistance: Double = 65.0
    ): BatteryHealthData {
        val estimatedCap = calculateEstimatedCapacity(samples, designCapacity)
        val healthPct = ((estimatedCap / designCapacity) * 100.0).coerceIn(10.0, 100.0)
        val lostMah = (designCapacity - estimatedCap).coerceAtLeast(0.0)
        val lostPct = (100.0 - healthPct).coerceAtLeast(0.0)

        val (confText, confColor) = getConfidenceInfo(samples.size)
        val (serviceText, serviceColor) = getServiceStatus(healthPct)

        return BatteryHealthData(
            designCapacityMah = designCapacity,
            estimatedCapacityMah = estimatedCap,
            healthPct = healthPct,
            capacityLostMah = lostMah,
            capacityLostPct = lostPct,
            totalCycles = totalCycles,
            validSessionsCount = samples.size,
            confidenceLevel = confText,
            confidenceColor = confColor,
            serviceStatus = serviceText,
            serviceColor = serviceColor,
            internalResistanceMilliOhm = if (lastKnownResistance > 0.0) lastKnownResistance else 65.0
        )
    }
}
