package com.caswatt.app

import java.util.Locale

object ChargingDiagnosticsHelper {

    // 1. Diagnostik Kualiti Kabel Berasaskan Kuasa (Watt)
    fun evaluateCableQuality(watt: Double, levelPct: Int): String {
        if (levelPct >= 85) return "Pengecasan Titisan (Trickle)"
        return when {
            watt >= 25.0 -> "Cemerlang (Super Fast Charge ⚡⚡)"
            watt >= 15.0 -> "Cemerlang (Fast Charge ⚡)"
            watt >= 9.0  -> "Baik (Standard Charge)"
            watt >= 4.5  -> "Sederhana (Arus Perlahan)"
            else         -> "Kabel Lemah / Rintangan Tinggi ⚠️"
        }
    }

    // 2. Pengesan Sekatan Haba (Thermal Throttling)
    fun checkThermalThrottling(tempC: Double, peakWatt: Double, currentWatt: Double): Boolean {
        return tempC >= 40.0 && peakWatt > 12.0 && currentWatt < (peakWatt * 0.65)
    }

    // 3. Pengiraan Smart ETA ke 80% dan 100%
    fun calculateEta(levelPct: Int, currentMa: Int, targetPct: Int): Int {
        if (levelPct >= targetPct) return 0
        if (currentMa <= 150) return -1

        val estimatedCap = 5000.0 // Anggaran kapasiti standard bateri
        val remMah = ((targetPct - levelPct) / 100.0) * estimatedCap
        return ((remMah / currentMa) * 60).toInt().coerceAtLeast(1)
    }

    // 4. Format Masa (Detik/Minit/Jam)
    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "Baru sahaja"
        val totalSec = millis / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return when {
            hours > 0 -> String.format(Locale.getDefault(), "%dj %dm", hours, minutes)
            minutes > 0 -> String.format(Locale.getDefault(), "%dm %ds", minutes, seconds)
            else -> String.format(Locale.getDefault(), "%ds", seconds)
        }
    }
}
