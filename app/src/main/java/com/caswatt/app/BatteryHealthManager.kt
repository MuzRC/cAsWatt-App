package com.caswatt.app

import android.content.Context
import org.json.JSONArray
import java.util.Locale
import kotlin.math.roundToInt

object BatteryHealthManager {
    private const val PREFS_NAME = "caswatt_health_prefs"
    private const val KEY_DESIGN_CAPACITY = "design_capacity_mah"
    private const val KEY_ESTIMATED_SAMPLES = "estimated_capacity_samples"

    // Kapasiti lalai bateri telefon moden (boleh diubah pengguna)
    private const val DEFAULT_DESIGN_CAPACITY = 5000

    /**
     * Dipanggil setiap kali sesi cas tamat.
     * Hanya merekod jika ada kenaikan sekurang-kurangnya 20% bagi memastikan data stabil & jitu.
     */
    @Synchronized
    fun onChargingSessionFinished(context: Context, startLevel: Int, endLevel: Int, totalMah: Double) {
        val pctGained = endLevel - startLevel
        if (pctGained < 20 || totalMah <= 0.0) return

        // Anggaran kapasiti penuh daripada pecahan tenaga yang masuk
        val estimatedFullCapacity = (totalMah / (pctGained / 100.0))

        // Tapis bacaan pelik/mustahil (had selamat 1000mAh hingga 10000mAh)
        if (estimatedFullCapacity !in 1000.0..10000.0) return

        saveCapacitySample(context, estimatedFullCapacity)
    }

    /**
     * Menyimpan sehingga 15 sampel cas terkini untuk purata stabil.
     */
    private fun saveCapacitySample(context: Context, sampleMah: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ESTIMATED_SAMPLES, "[]") ?: "[]"
        val samples = mutableListOf<Double>()

        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                samples.add(array.getDouble(i))
            }
        } catch (_: Exception) {}

        samples.add(0, sampleMah)
        if (samples.size > 15) {
            samples.removeAt(samples.size - 1)
        }

        val outArray = JSONArray()
        for (s in samples) {
            outArray.put(s)
        }
        prefs.edit().putString(KEY_ESTIMATED_SAMPLES, outArray.toString()).apply()
    }

    /**
     * Mendapatkan purata kapasiti penuh sebenar daripada sejarah pengecasan.
     */
    fun getEstimatedCapacityMah(context: Context): Double {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ESTIMATED_SAMPLES, "[]") ?: "[]"
        val samples = mutableListOf<Double>()

        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                samples.add(array.getDouble(i))
            }
        } catch (_: Exception) {}

        if (samples.isEmpty()) {
            return getDesignCapacity(context).toDouble()
        }

        return samples.average()
    }

    /**
     * Mengira peratus kesihatan bateri sebenar (State of Health).
     */
    fun getHealthPercentage(context: Context): Double {
        val design = getDesignCapacity(context)
        if (design <= 0) return 100.0
        val estimated = getEstimatedCapacityMah(context)
        val health = (estimated / design) * 100.0
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
}
