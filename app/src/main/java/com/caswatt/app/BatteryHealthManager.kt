package com.caswatt.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

object BatteryHealthManager {
    private const val PREFS_NAME = "caswatt_health_prefs"
    private const val KEY_DESIGN_CAPACITY = "design_capacity_mah"
    private const val KEY_ESTIMATED_SAMPLES = "estimated_capacity_samples"
    private const val DEFAULT_DESIGN_CAPACITY = 5000

    private val samplesCache = mutableListOf<CalibrationSample>()
    private var isCacheLoaded = false

    @Synchronized
    private fun ensureLoaded(context: Context) {
        if (isCacheLoaded) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ESTIMATED_SAMPLES, "[]") ?: "[]"
        samplesCache.clear()

        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val item = array.get(i)
                if (item is JSONObject) {
                    samplesCache.add(
                        CalibrationSample(
                            id = item.optLong("id", System.currentTimeMillis()),
                            startLevel = item.optInt("startLevel", 20),
                            endLevel = item.optInt("endLevel", 80),
                            mahDelivered = item.optDouble("mahDelivered", 0.0),
                            calculatedCapacity = item.optDouble("calculatedCapacity", 5000.0),
                            timestamp = item.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                } else if (item is Number) {
                    // Keserasian untuk data lama (format nombor Double sedia ada)
                    val cap = item.toDouble()
                    if (cap in 1000.0..10000.0) {
                        samplesCache.add(
                            CalibrationSample(
                                id = System.currentTimeMillis() - (i * 3600000L),
                                startLevel = 20,
                                endLevel = 80,
                                mahDelivered = cap * 0.6,
                                calculatedCapacity = cap,
                                timestamp = System.currentTimeMillis() - (i * 3600000L)
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        isCacheLoaded = true
    }

    private fun saveSamples(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (s in samplesCache) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("startLevel", s.startLevel)
                put("endLevel", s.endLevel)
                put("mahDelivered", s.mahDelivered)
                put("calculatedCapacity", s.calculatedCapacity)
                put("timestamp", s.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_ESTIMATED_SAMPLES, array.toString()).apply()
    }

    /**
     * Dipanggil setiap kali sesi cas tamat.
     * Hanya merekod jika ada kenaikan sekurang-kurangnya 20% bagi memastikan data stabil & jitu.
     */
    @Synchronized
    fun onChargingSessionFinished(context: Context, startLevel: Int, endLevel: Int, totalMah: Double) {
        ensureLoaded(context)
        val pctGained = endLevel - startLevel
        if (pctGained < 20 || totalMah <= 0.0) return

        // Anggaran kapasiti penuh daripada formula Coulomb Counting
        val estimatedCap = BatteryHealthCalculator.calculateSessionCapacity(startLevel, endLevel, totalMah)

        // Tapis bacaan pelik/mustahil (had selamat 1000mAh hingga 10000mAh)
        if (estimatedCap !in 1000.0..10000.0) return

        val now = System.currentTimeMillis()
        val sample = CalibrationSample(
            id = now,
            startLevel = startLevel,
            endLevel = endLevel,
            mahDelivered = totalMah,
            calculatedCapacity = estimatedCap,
            timestamp = now
        )

        samplesCache.add(0, sample)
        if (samplesCache.size > 15) {
            samplesCache.removeAt(samplesCache.size - 1)
        }
        saveSamples(context)
    }

    /**
     * Mendapatkan purata kapasiti penuh sebenar daripada sejarah pengecasan.
     */
    fun getEstimatedCapacityMah(context: Context): Double {
        ensureLoaded(context)
        if (samplesCache.isEmpty()) {
            return getDesignCapacity(context).toDouble()
        }
        return BatteryHealthCalculator.calculateEstimatedCapacity(
            samplesCache,
            getDesignCapacity(context).toDouble()
        )
    }

    /**
     * Mengira peratus kesihatan bateri sebenar (State of Health).
     */
    fun getHealthPercentage(context: Context): Double {
        val design = getDesignCapacity(context)
        if (design <= 0) return 100.0
        val estimated = getEstimatedCapacityMah(context)
        val health = (estimated / design.toDouble()) * 100.0
        return health.coerceIn(10.0, 100.0)
    }

    fun getDesignCapacity(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DESIGN_CAPACITY, DEFAULT_DESIGN_CAPACITY)
    }

    fun setDesignCapacity(context: Context, capacityMah: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DESIGN_CAPACITY, capacityMah).apply()
    }

    fun getFormattedHealthSummary(context: Context): String {
        val health = getHealthPercentage(context)
        val estimated = getEstimatedCapacityMah(context).roundToInt()
        val design = getDesignCapacity(context)

        return String.format(
            Locale.US,
            "%.1f%% (%d mAh / %d mAh)",
            health,
            estimated,
            design
        )
    }

    /**
     * Mengembalikan data lengkap untuk antaramuka tab Kesihatan baharu
     */
    fun getHealthData(context: Context): BatteryHealthData {
        ensureLoaded(context)
        val design = getDesignCapacity(context).toDouble()
        val cycles = ChargingSessionManager.getEstimatedCycles()

        return BatteryHealthCalculator.buildHealthData(
            designCapacity = design,
            samples = samplesCache,
            totalCycles = cycles
        )
    }

    /**
     * Butang Set Semula Bateri Baharu (Reset Calibration)
     */
    fun resetForNewBattery(context: Context) {
        ensureLoaded(context)
        samplesCache.clear()
        saveSamples(context)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ESTIMATED_SAMPLES)
            .apply()

        // Set semula kitaran dalam ChargingSessionManager
        val mainPrefs = context.getSharedPreferences("caswatt_prefs", Context.MODE_PRIVATE)
        mainPrefs.edit().putFloat("cumulative_pct_charged", 0f).apply()
        ChargingSessionManager.cumulativePctCharged = 0.0
    }
}
