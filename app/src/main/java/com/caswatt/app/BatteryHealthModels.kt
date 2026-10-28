package com.caswatt.app

// Sampel Sesi Pengecasan Berkualiti (Untuk Purata Bergerak)
data class CalibrationSample(
    val id: Long,
    val startLevel: Int,
    val endLevel: Int,
    val mahDelivered: Double,
    val calculatedCapacity: Double,
    val timestamp: Long
)

// Data Lengkap Kesihatan & Diagnostik Bateri
data class BatteryHealthData(
    val designCapacityMah: Double,
    val estimatedCapacityMah: Double,
    val healthPct: Double,
    val capacityLostMah: Double,
    val capacityLostPct: Double,
    val totalCycles: Double,
    val validSessionsCount: Int,
    val confidenceLevel: String,
    val confidenceColor: Int,
    val serviceStatus: String,
    val serviceColor: Int,
    val internalResistanceMilliOhm: Double
)
