package com.caswatt.app

data class ChartPoint(
    val watt: Float,
    val temp: Float,
    val timestamp: Long
)

data class DailyRecord(
    val date: String,
    var count: Int,
    var totalMah: Double,
    var peakWatt: Double
)

data class SessionRecord(
    val id: Long,
    val startTime: Long,
    val endTime: Long,
    val startLevel: Int,
    val endLevel: Int,
    val peakWatt: Double,
    val totalMah: Double,
    val energyWh: Double,
    val costMyr: Double,
    val cableQuality: String,
    val avgWatt: Double = 0.0,
    val startTemp: Double = 0.0,
    val endTemp: Double = 0.0
)

data class WeeklyStats(
    val totalCharges: Int,
    val totalMah: Double,
    val totalWh: Double,
    val avgChargesPerDay: Double,
    val mostActiveDay: String
)
