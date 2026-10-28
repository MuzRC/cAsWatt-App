package com.caswatt.app

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object WeeklyStatsHelper {

    fun calculate(dailyRecords: List<DailyRecord>): WeeklyStats {
        val recentRecords = dailyRecords.take(7)
        if (recentRecords.isEmpty()) {
            return WeeklyStats(
                totalCharges = 0,
                totalMah = 0.0,
                totalWh = 0.0,
                avgChargesPerDay = 0.0,
                mostActiveDay = "Tiada Data"
            )
        }

        var totalCount = 0
        var sumMah = 0.0
        var maxCount = -1
        var peakDayStr = ""

        val dayNames = arrayOf("Ahad", "Isnin", "Selasa", "Rabu", "Khamis", "Jumaat", "Sabtu")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (rec in recentRecords) {
            totalCount += rec.count
            sumMah += rec.totalMah
            if (rec.count > maxCount) {
                maxCount = rec.count
                try {
                    val date = sdf.parse(rec.date)
                    if (date != null) {
                        val cal = Calendar.getInstance()
                        cal.time = date
                        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
                        peakDayStr = dayNames.getOrElse(dayOfWeek) { rec.date }
                    } else {
                        peakDayStr = rec.date
                    }
                } catch (_: Exception) {
                    peakDayStr = rec.date
                }
            }
        }

        val daysCount = recentRecords.size.coerceAtLeast(1)
        val avgCount = totalCount.toDouble() / daysCount.toDouble()
        // Anggaran tenaga Wh daripada mAh berasaskan voltan nominal bateri 3.85V
        val approxWh = (sumMah * 3.85) / 1000.0

        return WeeklyStats(
            totalCharges = totalCount,
            totalMah = sumMah,
            totalWh = approxWh,
            avgChargesPerDay = avgCount,
            mostActiveDay = if (peakDayStr.isNotBlank()) peakDayStr else "Tiada Data"
        )
    }
}
