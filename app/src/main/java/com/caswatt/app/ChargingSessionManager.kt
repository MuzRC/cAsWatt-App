package com.caswatt.app

import android.content.Context
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

object ChargingSessionManager {
    private const val PREFS_NAME = "caswatt_prefs"
    private const val KEY_BATTERY_ALARM = "battery_alarm_enabled"
    private const val KEY_BATTERY_THRESHOLD = "battery_threshold"
    private const val KEY_TEMP_ALARM = "temp_alarm_enabled"
    private const val KEY_TEMP_THRESHOLD = "temp_threshold"
    private const val KEY_UNPLUG_TIME = "unplug_timestamp"
    private const val KEY_UNPLUG_LEVEL = "unplug_level"
    private const val KEY_DAILY_LOGS = "daily_charging_logs"
    private const val KEY_CUMULATIVE_PCT = "cumulative_pct_charged"
    private const val KEY_HEALTH_STATUS = "last_health_status"

    // Sesi Semasa
    var peakWatt: Double = 0.0
    var totalMah: Double = 0.0
    private var lastTimestamp: Long = 0L

    // Nyahcas (Discharge) Tracker
    var lastUnplugTimestamp: Long = 0L
    var lastUnplugLevel: Int = 100

    // Kitaran & Kesihatan
    var cumulativePctCharged: Double = 0.0
    var healthCondition: String = "Baik (Good)"

    // Alarms
    var batteryAlarmEnabled: Boolean = true
    var batteryThreshold: Int = 80
    var tempAlarmEnabled: Boolean = true
    var tempThreshold: Int = 45
    private var batteryAlarmFired: Boolean = false
    private var tempAlarmFired: Boolean = false

    // Graf & History
    val history = mutableListOf<ChartPoint>()
    private const val MAX_HISTORY_POINTS = 300

    // Rekod Harian
    val dailyRecords = mutableListOf<DailyRecord>()

    private var previousChargeLevel: Int = -1

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        batteryAlarmEnabled = prefs.getBoolean(KEY_BATTERY_ALARM, true)
        batteryThreshold = prefs.getInt(KEY_BATTERY_THRESHOLD, 80)
        tempAlarmEnabled = prefs.getBoolean(KEY_TEMP_ALARM, true)
        tempThreshold = prefs.getInt(KEY_TEMP_THRESHOLD, 45)
        lastUnplugTimestamp = prefs.getLong(KEY_UNPLUG_TIME, System.currentTimeMillis())
        lastUnplugLevel = prefs.getInt(KEY_UNPLUG_LEVEL, 100)
        cumulativePctCharged = prefs.getFloat(KEY_CUMULATIVE_PCT, 0f).toDouble()
        healthCondition = prefs.getString(KEY_HEALTH_STATUS, "Baik (Good)") ?: "Baik (Good)"

        loadDailyRecords(context)
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun loadDailyRecords(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_DAILY_LOGS, null) ?: return
        try {
            dailyRecords.clear()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                dailyRecords.add(
                    DailyRecord(
                        date = obj.getString("date"),
                        count = obj.getInt("count"),
                        totalMah = obj.getDouble("totalMah"),
                        peakWatt = obj.getDouble("peakWatt")
                    )
                )
            }
        } catch (_: Exception) {}
    }

    fun saveDailyRecords(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (rec in dailyRecords) {
            val obj = JSONObject().apply {
                put("date", rec.date)
                put("count", rec.count)
                put("totalMah", rec.totalMah)
                put("peakWatt", rec.peakWatt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_DAILY_LOGS, array.toString()).apply()
    }

    // 1. Auto-reset dipanggil bila kabel mula dicucuk
    @Synchronized
    fun onChargingStarted(context: Context, currentLevel: Int) {
        peakWatt = 0.0
        totalMah = 0.0
        history.clear()
        batteryAlarmFired = false
        tempAlarmFired = false
        lastTimestamp = System.currentTimeMillis()
        previousChargeLevel = currentLevel

        // Rekod bilangan cas hari ini
        val today = getTodayDateString()
        var todayRecord = dailyRecords.find { it.date == today }
        if (todayRecord == null) {
            todayRecord = DailyRecord(date = today, count = 1, totalMah = 0.0, peakWatt = 0.0)
            dailyRecords.add(0, todayRecord)
            // Simpan maksimum 14 hari rekod
            if (dailyRecords.size > 14) {
                dailyRecords.removeAt(dailyRecords.size - 1)
            }
        } else {
            todayRecord.count += 1
        }
        saveDailyRecords(context)
    }

    // 4. Dipanggil bila kabel dicabut (mula discharge)
    @Synchronized
    fun onChargingStopped(context: Context, currentLevel: Int) {
        lastUnplugTimestamp = System.currentTimeMillis()
        lastUnplugLevel = currentLevel
        batteryAlarmFired = false
        tempAlarmFired = false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_UNPLUG_TIME, lastUnplugTimestamp)
            .putInt(KEY_UNPLUG_LEVEL, lastUnplugLevel)
            .putFloat(KEY_CUMULATIVE_PCT, cumulativePctCharged.toFloat())
            .apply()

        // Kemas kini rekod harian dengan jumlah mAh dan peak watt
        val today = getTodayDateString()
        val todayRecord = dailyRecords.find { it.date == today }
        if (todayRecord != null) {
            todayRecord.totalMah += totalMah
            if (peakWatt > todayRecord.peakWatt) {
                todayRecord.peakWatt = peakWatt
            }
            saveDailyRecords(context)
        }
    }

    @Synchronized
    fun recordSample(
        context: Context,
        watt: Double,
        currentMa: Int,
        tempC: Double,
        levelPct: Int,
        isCharging: Boolean,
        healthCode: Int
    ) {
        val now = System.currentTimeMillis()

        // 3. Update Status Kesihatan Bateri dari OS
        healthCondition = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Sihat & Baik (Good)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Terlampau Panas (Overheat)"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Voltan Terlebih (Over Voltage)"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Bateri Rosak / Mati"
            BatteryManager.BATTERY_HEALTH_COLD -> "Terlampau Sejuk"
            else -> "Normal (Unspecified)"
        }

        if (isCharging) {
            // Track Peak Watt
            if (watt > peakWatt) {
                peakWatt = watt
            }

            // Integrasi mAh: mAh = mA * (dt_seconds / 3600)
            if (lastTimestamp > 0L) {
                val dtSec = (now - lastTimestamp) / 1000.0
                if (dtSec in 0.5..10.0 && currentMa > 0) {
                    val dMah = currentMa * (dtSec / 3600.0)
                    totalMah += dMah
                }
            }

            // Kira peningkatan peratus untuk kitaran (Cycle count)
            if (previousChargeLevel in 0..100 && levelPct > previousChargeLevel) {
                cumulativePctCharged += (levelPct - previousChargeLevel)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putFloat(KEY_CUMULATIVE_PCT, cumulativePctCharged.toFloat()).apply()
            }
            previousChargeLevel = levelPct

            // Rekod keluk graf
            history.add(ChartPoint(watt.toFloat(), tempC.toFloat(), now))
            if (history.size > MAX_HISTORY_POINTS) {
                history.removeAt(0)
            }

            // Kemas kini rekod harian secara langsung
            val today = getTodayDateString()
            val todayRecord = dailyRecords.find { it.date == today }
            if (todayRecord != null) {
                if (peakWatt > todayRecord.peakWatt) {
                    todayRecord.peakWatt = peakWatt
                }
            }

            // Semak penggera
            checkAlarms(context, levelPct, tempC)
        }

        lastTimestamp = now
    }

    private fun checkAlarms(context: Context, levelPct: Int, tempC: Double) {
        if (batteryAlarmEnabled && !batteryAlarmFired && levelPct >= batteryThreshold) {
            batteryAlarmFired = true
            triggerAlarm(context)
        }

        if (tempAlarmEnabled && !tempAlarmFired && tempC >= tempThreshold) {
            tempAlarmFired = true
            triggerAlarm(context)
        }
    }

    fun triggerAlarm(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            ringtone?.play()

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(1000)
                }
            }
        } catch (_: Exception) {}
    }

    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "Baru sahaja"
        val totalSec = millis / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return if (hours > 0) {
            String.format("%dj %dm", hours, minutes)
        } else if (minutes > 0) {
            String.format("%dm %ds", minutes, seconds)
        } else {
            String.format("%ds", seconds)
        }
    }

    fun getEstimatedCycles(): Double {
        return cumulativePctCharged / 100.0
    }

    fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_BATTERY_ALARM, batteryAlarmEnabled)
            .putInt(KEY_BATTERY_THRESHOLD, batteryThreshold)
            .putBoolean(KEY_TEMP_ALARM, tempAlarmEnabled)
            .putInt(KEY_TEMP_THRESHOLD, tempThreshold)
            .putString(KEY_HEALTH_STATUS, healthCondition)
            .apply()
    }
}
