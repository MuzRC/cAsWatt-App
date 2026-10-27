package com.caswatt.app

import android.content.Context
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
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
    val cableQuality: String
)

object ChargingSessionManager {
    private const val PREFS_NAME = "caswatt_prefs"
    private const val KEY_BATTERY_ALARM = "battery_alarm_enabled"
    private const val KEY_BATTERY_THRESHOLD = "battery_threshold"
    private const val KEY_TEMP_ALARM = "temp_alarm_enabled"
    private const val KEY_TEMP_THRESHOLD = "temp_threshold"
    private const val KEY_LOW_BAT_ALARM = "low_battery_alarm_enabled"
    private const val KEY_TRICKLE_ALARM = "trickle_alarm_enabled"
    private const val KEY_UNPLUG_TIME = "unplug_timestamp"
    private const val KEY_UNPLUG_LEVEL = "unplug_level"
    private const val KEY_DAILY_LOGS = "daily_charging_logs"
    private const val KEY_SESSION_LOGS = "session_charging_logs"
    private const val KEY_CUMULATIVE_PCT = "cumulative_pct_charged"
    private const val KEY_HEALTH_STATUS = "last_health_status"

    // Penanda keselamatan init
    private var isInitialized = false

    // Sesi Semasa
    var peakWatt: Double = 0.0
    var totalMah: Double = 0.0
    var energyWh: Double = 0.0
    var costMyr: Double = 0.0
    private var lastTimestamp: Long = 0L

    var sessionStartTime: Long = 0L
    var sessionStartLevel: Int = 0

    // Nyahcas (Discharge) Tracker
    var lastUnplugTimestamp: Long = 0L
    var lastUnplugLevel: Int = 100

    // Diagnostik Kabel & Haba
    var cableQuality: String = "Mengukur..."
    var isThermalThrottling: Boolean = false

    // Smart ETA
    var etaTo80Min: Int = -1
    var etaTo100Min: Int = -1

    // Trickle & Full Saturation
    var isTrickleCharging: Boolean = false
    private var fullSaturationAlertFired: Boolean = false
    private var lowBatteryAlertFired: Boolean = false

    // Kitaran & Kesihatan
    var cumulativePctCharged: Double = 0.0
    var healthCondition: String = "Baik (Good)"

    // Alarms Configuration
    var batteryAlarmEnabled: Boolean = true
    var batteryThreshold: Int = 80
    var tempAlarmEnabled: Boolean = true
    var tempThreshold: Int = 45
    var lowBatteryAlarmEnabled: Boolean = true
    var trickleAlarmEnabled: Boolean = true

    private var batteryAlarmFired: Boolean = false
    private var tempAlarmFired: Boolean = false

    // Graf & History
    val history = mutableListOf<ChartPoint>()
    private const val MAX_HISTORY_POINTS = 300

    // Rekod Harian & Sesi
    val dailyRecords = mutableListOf<DailyRecord>()
    val sessionRecords = mutableListOf<SessionRecord>()

    private var previousChargeLevel: Int = -1
    private var wasCharging: Boolean? = null

    // Tarif Elektrik SESB Sabah Asas: RM 0.38 per kWh
    private const val TARIFF_PER_KWH = 0.38

    @Synchronized
    fun ensureInit(context: Context) {
        if (!isInitialized) {
            init(context.applicationContext)
        }
    }

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        batteryAlarmEnabled = prefs.getBoolean(KEY_BATTERY_ALARM, true)
        batteryThreshold = prefs.getInt(KEY_BATTERY_THRESHOLD, 80)
        tempAlarmEnabled = prefs.getBoolean(KEY_TEMP_ALARM, true)
        tempThreshold = prefs.getInt(KEY_TEMP_THRESHOLD, 45)
        lowBatteryAlarmEnabled = prefs.getBoolean(KEY_LOW_BAT_ALARM, true)
        trickleAlarmEnabled = prefs.getBoolean(KEY_TRICKLE_ALARM, true)

        if (!prefs.contains(KEY_UNPLUG_TIME)) {
            val bootTimestamp = System.currentTimeMillis() - SystemClock.elapsedRealtime()
            lastUnplugTimestamp = bootTimestamp
            lastUnplugLevel = 100
            prefs.edit()
                .putLong(KEY_UNPLUG_TIME, bootTimestamp)
                .putInt(KEY_UNPLUG_LEVEL, 100)
                .apply()
        } else {
            lastUnplugTimestamp = prefs.getLong(KEY_UNPLUG_TIME, System.currentTimeMillis() - SystemClock.elapsedRealtime())
            lastUnplugLevel = prefs.getInt(KEY_UNPLUG_LEVEL, 100)
        }

        cumulativePctCharged = prefs.getFloat(KEY_CUMULATIVE_PCT, 0f).toDouble()
        healthCondition = prefs.getString(KEY_HEALTH_STATUS, "Baik (Good)") ?: "Baik (Good)"

        loadDailyRecords(context)
        loadSessionRecords(context)
        isInitialized = true
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

    private fun loadSessionRecords(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_SESSION_LOGS, null) ?: return
        try {
            sessionRecords.clear()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                sessionRecords.add(
                    SessionRecord(
                        id = obj.getLong("id"),
                        startTime = obj.getLong("startTime"),
                        endTime = obj.getLong("endTime"),
                        startLevel = obj.getInt("startLevel"),
                        endLevel = obj.getInt("endLevel"),
                        peakWatt = obj.getDouble("peakWatt"),
                        totalMah = obj.getDouble("totalMah"),
                        energyWh = obj.getDouble("energyWh"),
                        costMyr = obj.getDouble("costMyr"),
                        cableQuality = obj.getString("cableQuality")
                    )
                )
            }
        } catch (_: Exception) {}
    }

    fun saveSessionRecords(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (rec in sessionRecords) {
            val obj = JSONObject().apply {
                put("id", rec.id)
                put("startTime", rec.startTime)
                put("endTime", rec.endTime)
                put("startLevel", rec.startLevel)
                put("endLevel", rec.endLevel)
                put("peakWatt", rec.peakWatt)
                put("totalMah", rec.totalMah)
                put("energyWh", rec.energyWh)
                put("costMyr", rec.costMyr)
                put("cableQuality", rec.cableQuality)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_SESSION_LOGS, array.toString()).apply()
    }

    @Synchronized
    fun checkStateTransition(context: Context, isCharging: Boolean, levelPct: Int) {
        ensureInit(context)
        if (wasCharging == null) {
            wasCharging = isCharging
            return
        }
        if (wasCharging == false && isCharging) {
            onChargingStarted(context, levelPct)
        } else if (wasCharging == true && !isCharging) {
            onChargingStopped(context, levelPct)
        }
        wasCharging = isCharging
    }

    @Synchronized
    fun onChargingStarted(context: Context, currentLevel: Int) {
        ensureInit(context)
        val now = System.currentTimeMillis()
        peakWatt = 0.0
        totalMah = 0.0
        energyWh = 0.0
        costMyr = 0.0
        history.clear()
        batteryAlarmFired = false
        tempAlarmFired = false
        fullSaturationAlertFired = false
        lowBatteryAlertFired = false
        lastTimestamp = now
        sessionStartTime = now
        sessionStartLevel = currentLevel
        previousChargeLevel = currentLevel
        wasCharging = true
        cableQuality = "Mengukur kualiti kabel..."
        isThermalThrottling = false

        val today = getTodayDateString()
        var todayRecord = dailyRecords.find { it.date == today }
        if (todayRecord == null) {
            todayRecord = DailyRecord(date = today, count = 1, totalMah = 0.0, peakWatt = 0.0)
            dailyRecords.add(0, todayRecord)
            if (dailyRecords.size > 3650) {
                dailyRecords.removeAt(dailyRecords.size - 1)
            }
        } else {
            todayRecord.count += 1
        }
        saveDailyRecords(context)
    }

    @Synchronized
    fun onChargingStopped(context: Context, currentLevel: Int) {
        ensureInit(context)
        val now = System.currentTimeMillis()
        lastUnplugTimestamp = now
        lastUnplugLevel = currentLevel
        batteryAlarmFired = false
        tempAlarmFired = false
        wasCharging = false
        isTrickleCharging = false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_UNPLUG_TIME, lastUnplugTimestamp)
            .putInt(KEY_UNPLUG_LEVEL, lastUnplugLevel)
            .putFloat(KEY_CUMULATIVE_PCT, cumulativePctCharged.toFloat())
            .apply()

        val today = getTodayDateString()
        val todayRecord = dailyRecords.find { it.date == today }
        if (todayRecord != null) {
            todayRecord.totalMah += totalMah
            if (peakWatt > todayRecord.peakWatt) {
                todayRecord.peakWatt = peakWatt
            }
            saveDailyRecords(context)
        }

        // Simpan sesi terperinci jika cas melebihi 20 saat
        val durationSec = (now - sessionStartTime) / 1000
        if (durationSec >= 20 && (totalMah >= 5.0 || (currentLevel - sessionStartLevel) >= 1)) {
            val session = SessionRecord(
                id = sessionStartTime,
                startTime = sessionStartTime,
                endTime = now,
                startLevel = sessionStartLevel,
                endLevel = currentLevel,
                peakWatt = peakWatt,
                totalMah = totalMah,
                energyWh = energyWh,
                costMyr = costMyr,
                cableQuality = cableQuality
            )
            sessionRecords.add(0, session)
            if (sessionRecords.size > 150) {
                sessionRecords.removeAt(sessionRecords.size - 1)
            }
            saveSessionRecords(context)
        }

        // Simpan statistik kesihatan bateri secara automatik
BatteryHealthManager.onChargingSessionFinished(context, sessionStartLevel, currentLevel, totalMah)
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
        ensureInit(context)
        checkStateTransition(context, isCharging, levelPct)
        val now = System.currentTimeMillis()

        healthCondition = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Sihat & Baik (Good)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Terlampau Panas (Overheat)"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Voltan Terlebih (Over Voltage)"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Bateri Rosak / Mati"
            BatteryManager.BATTERY_HEALTH_COLD -> "Terlampau Sejuk"
            else -> "Normal (Unspecified)"
        }

        if (isCharging) {
            if (watt > peakWatt) {
                peakWatt = watt
            }

            if (lastTimestamp > 0L) {
                val dtSec = (now - lastTimestamp) / 1000.0
                // Hadkan selang maksimum 60 saat semasa mod tidur/skrin tutup
                if (dtSec >= 0.5 && currentMa > 0) {
                    val safeDt = dtSec.coerceAtMost(60.0)
                    val dMah = currentMa * (safeDt / 3600.0)
                    totalMah += dMah
                    val dWh = (watt * safeDt) / 3600.0
                    energyWh += dWh
                    costMyr = (energyWh / 1000.0) * TARIFF_PER_KWH
                }
            }

            if (previousChargeLevel in 0..100 && levelPct > previousChargeLevel) {
                cumulativePctCharged += (levelPct - previousChargeLevel)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putFloat(KEY_CUMULATIVE_PCT, cumulativePctCharged.toFloat()).apply()
            }
            previousChargeLevel = levelPct

            // 1. Diagnostik Kualiti Kabel
            if (levelPct < 85) {
                cableQuality = when {
                    watt >= 25.0 -> "Cemerlang (Super Fast Charge ⚡⚡)"
                    watt >= 15.0 -> "Cemerlang (Fast Charge ⚡)"
                    watt >= 9.0 -> "Baik (Standard Charge)"
                    watt >= 4.5 -> "Sederhana (Arus Perlahan)"
                    else -> "Kabel Lemah / Rintangan Tinggi ⚠️"
                }
            }

            // 2. Pengesan Sekatan Suhu (Thermal Throttling)
            isThermalThrottling = (tempC >= 40.0 && peakWatt > 12.0 && watt < (peakWatt * 0.65))

            // 3. Smart ETA (Anggaran Masa Siap Cas)
            if (currentMa > 150) {
                val estimatedCap = 5000.0
                etaTo80Min = if (levelPct < 80) {
                    val remMah = ((80 - levelPct) / 100.0) * estimatedCap
                    ((remMah / currentMa) * 60).toInt().coerceAtLeast(1)
                } else 0

                etaTo100Min = if (levelPct < 100) {
                    val remMah = ((100 - levelPct) / 100.0) * estimatedCap
                    ((remMah / currentMa) * 60).toInt().coerceAtLeast(1)
                } else 0
            } else {
                etaTo80Min = if (levelPct >= 80) 0 else -1
                etaTo100Min = if (levelPct >= 100) 0 else -1
            }

            // 4. Trickle Charge & Full Saturation Alert
            if (levelPct == 100) {
                isTrickleCharging = (currentMa in 20..250)
                if (currentMa in 1..40 && !fullSaturationAlertFired && trickleAlarmEnabled) {
                    fullSaturationAlertFired = true
                    triggerAlarm(context)
                }
            } else {
                isTrickleCharging = false
            }

            history.add(ChartPoint(watt.toFloat(), tempC.toFloat(), now))
            if (history.size > MAX_HISTORY_POINTS) {
                history.removeAt(0)
            }

            val today = getTodayDateString()
            val todayRecord = dailyRecords.find { it.date == today }
            if (todayRecord != null) {
                if (peakWatt > todayRecord.peakWatt) {
                    todayRecord.peakWatt = peakWatt
                }
            }

            checkAlarms(context, levelPct, tempC)
        } else {
            if (levelPct <= 20 && !lowBatteryAlertFired && lowBatteryAlarmEnabled) {
                lowBatteryAlertFired = true
                triggerAlarm(context)
            }
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
            String.format(Locale.getDefault(), "%dj %dm", hours, minutes)
        } else if (minutes > 0) {
            String.format(Locale.getDefault(), "%dm %ds", minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%ds", seconds)
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
            .putBoolean(KEY_LOW_BAT_ALARM, lowBatteryAlarmEnabled)
            .putBoolean(KEY_TRICKLE_ALARM, trickleAlarmEnabled)
            .putString(KEY_HEALTH_STATUS, healthCondition)
            .apply()
    }

    fun generateCsvData(): String {
        val sb = StringBuilder()
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        sb.append("=== REKOD RINGKASAN HARIAN (10 TAHUN) ===\n")
        sb.append("Tarikh,Bilangan Cas (kali),Watt Puncak (W),Jumlah Tenaga (mAh)\n")
        for (rec in dailyRecords) {
            sb.append("${rec.date},${rec.count},${String.format(Locale.US, "%.2f", rec.peakWatt)},${String.format(Locale.US, "%.0f", rec.totalMah)}\n")
        }

        sb.append("\n=== LOG TERPERINCI SESI PENGCASAN ===\n")
        sb.append("Masa Mula,Masa Tamat,Durasi,Paras Bateri Mula (%),Paras Bateri Tamat (%),Watt Puncak (W),Jumlah Arus (mAh),Tenaga (Wh),Kos Elektrik (RM),Kualiti Kabel\n")
        for (s in sessionRecords) {
            val startStr = timeFormat.format(Date(s.startTime))
            val endStr = timeFormat.format(Date(s.endTime))
            val durStr = formatDuration(s.endTime - s.startTime)
            sb.append("${startStr},${endStr},\"${durStr}\",${s.startLevel}%,${s.endLevel}%,${String.format(Locale.US, "%.2f", s.peakWatt)},${String.format(Locale.US, "%.0f", s.totalMah)},${String.format(Locale.US, "%.3f", s.energyWh)},RM ${String.format(Locale.US, "%.4f", s.costMyr)},\"${s.cableQuality}\"\n")
        }

        return sb.toString()
    }
}
