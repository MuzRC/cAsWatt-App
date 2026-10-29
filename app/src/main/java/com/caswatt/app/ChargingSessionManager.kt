package com.caswatt.app

import android.content.Context
import android.os.BatteryManager
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private const val KEY_CUMULATIVE_PCT = "cumulative_pct_charged"
    private const val KEY_HEALTH_STATUS = "last_health_status"
    private const val TARIFF_PER_KWH = 0.38
    
    // Muatan penimbal dinaikkan ke 400 titik dengan kawalan selang masa
    private const val MAX_HISTORY_POINTS = 400
    private const val HISTORY_SAMPLE_INTERVAL_MS = 2000L // 1 sampel graf setiap 2 saat (~13.3 minit rekod aktif)

    private var isInitialized = false

    // Sesi Semasa
    var peakWatt: Double = 0.0
    var totalMah: Double = 0.0
    var energyWh: Double = 0.0
    var costMyr: Double = 0.0
    private var lastTimestamp: Long = 0L
    private var lastHistoryTimestamp: Long = 0L

    // Pembolehubah Penapis Kehalusan Data (EMA & Anti-Spike)
    private var lastValidWatt: Double = 0.0
    private var smoothedWatt: Double = 0.0
    private var smoothedTemp: Double = 0.0

    var sessionStartTime: Long = 0L
    var sessionStartLevel: Int = 0
    var sessionStartTemp: Double = 0.0
    var sessionLastTemp: Double = 0.0

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

    // Konfigurasi Penggera
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
    val dailyRecords = mutableListOf<DailyRecord>()
    val sessionRecords = mutableListOf<SessionRecord>()

    private var previousChargeLevel: Int = -1
    private var wasCharging: Boolean? = null

    @Synchronized
    fun ensureInit(context: Context) {
        if (!isInitialized) init(context.applicationContext)
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
            val boot = System.currentTimeMillis() - SystemClock.elapsedRealtime()
            lastUnplugTimestamp = boot
            lastUnplugLevel = 100
            prefs.edit().putLong(KEY_UNPLUG_TIME, boot).putInt(KEY_UNPLUG_LEVEL, 100).apply()
        } else {
            lastUnplugTimestamp = prefs.getLong(KEY_UNPLUG_TIME, System.currentTimeMillis() - SystemClock.elapsedRealtime())
            lastUnplugLevel = prefs.getInt(KEY_UNPLUG_LEVEL, 100)
        }

        cumulativePctCharged = prefs.getFloat(KEY_CUMULATIVE_PCT, 0f).toDouble()
        healthCondition = prefs.getString(KEY_HEALTH_STATUS, "Baik (Good)") ?: "Baik (Good)"

        ChargingStorageHelper.loadDailyRecords(context, dailyRecords)
        ChargingStorageHelper.loadSessionRecords(context, sessionRecords)
        isInitialized = true
        BatteryVoiceAlert.init(context)
    }

    fun saveDailyRecords(context: Context) {
        ChargingStorageHelper.saveDailyRecords(context, dailyRecords)
    }

    fun saveSessionRecords(context: Context) {
        ChargingStorageHelper.saveSessionRecords(context, sessionRecords)
    }

    private fun getTodayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    @Synchronized
    fun checkStateTransition(context: Context, isCharging: Boolean, levelPct: Int) {
        ensureInit(context)
        if (wasCharging == null) {
            wasCharging = isCharging
            return
        }
        if (wasCharging == false && isCharging) onChargingStarted(context, levelPct)
        else if (wasCharging == true && !isCharging) onChargingStopped(context, levelPct)
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
        lastHistoryTimestamp = 0L
        sessionStartTime = now
        sessionStartLevel = currentLevel
        sessionStartTemp = 0.0
        sessionLastTemp = 0.0
        previousChargeLevel = currentLevel
        wasCharging = true
        cableQuality = "Mengukur kualiti kabel..."
        isThermalThrottling = false

        // Reset penapis kelicinan graf
        lastValidWatt = 0.0
        smoothedWatt = 0.0
        smoothedTemp = 0.0

        val today = getTodayDateString()
        var todayRecord = dailyRecords.find { it.date == today }
        if (todayRecord == null) {
            todayRecord = DailyRecord(today, 1, 0.0, 0.0)
            dailyRecords.add(0, todayRecord)
            if (dailyRecords.size > 3650) dailyRecords.removeAt(dailyRecords.size - 1)
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

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_UNPLUG_TIME, lastUnplugTimestamp)
            .putInt(KEY_UNPLUG_LEVEL, lastUnplugLevel)
            .putFloat(KEY_CUMULATIVE_PCT, cumulativePctCharged.toFloat())
            .apply()

        val today = getTodayDateString()
        val todayRecord = dailyRecords.find { it.date == today }
        if (todayRecord != null) {
            todayRecord.totalMah += totalMah
            if (peakWatt > todayRecord.peakWatt) todayRecord.peakWatt = peakWatt
            saveDailyRecords(context)
        }

        val durationSec = if (sessionStartTime > 0L) (now - sessionStartTime) / 1000 else 0
        val levelGain = (currentLevel - sessionStartLevel).coerceAtLeast(0)

        if (sessionStartTime > 1577836800000L && durationSec >= 45 && (levelGain >= 1 || totalMah >= 10.0)) {
            val avgWattVal = if (durationSec > 0 && energyWh > 0.0) ((energyWh * 3600.0) / durationSec).coerceAtMost(peakWatt) else peakWatt
            val sTemp = if (sessionStartTemp > 0.0) sessionStartTemp else 30.0
            val eTemp = if (sessionLastTemp > 0.0) sessionLastTemp else sTemp

            sessionRecords.add(0, SessionRecord(
                id = sessionStartTime,
                startTime = sessionStartTime,
                endTime = now,
                startLevel = sessionStartLevel,
                endLevel = currentLevel,
                peakWatt = peakWatt,
                totalMah = totalMah,
                energyWh = energyWh,
                costMyr = costMyr,
                cableQuality = cableQuality,
                avgWatt = avgWattVal,
                startTemp = sTemp,
                endTemp = eTemp
            ))
            if (sessionRecords.size > 150) sessionRecords.removeAt(sessionRecords.size - 1)
            saveSessionRecords(context)
        }

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
            if (sessionStartTemp == 0.0 && tempC > 0.0) sessionStartTemp = tempC
            sessionLastTemp = tempC

            // 1. Tapis Glitch / False-Drop (elak jarum terjunam ke 0W bila polling Android terlepas)
            val cleanWatt = if (watt < 0.6 && lastValidWatt > 2.0) {
                lastValidWatt
            } else {
                if (watt >= 0.6) lastValidWatt = watt
                watt
            }

            if (cleanWatt > peakWatt) peakWatt = cleanWatt

            // 2. Kira Integrasi Tenaga (Coulomb counting & Wh)
            if (lastTimestamp > 0L) {
                val dtSec = (now - lastTimestamp) / 1000.0
                if (dtSec >= 0.5 && currentMa > 0) {
                    val safeDt = dtSec.coerceAtMost(60.0)
                    totalMah += currentMa * (safeDt / 3600.0)
                    energyWh += (cleanWatt * safeDt) / 3600.0
                    costMyr = (energyWh / 1000.0) * TARIFF_PER_KWH
                }
            }

            if (previousChargeLevel in 0..100 && levelPct > previousChargeLevel) {
                cumulativePctCharged += (levelPct - previousChargeLevel)
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putFloat(KEY_CUMULATIVE_PCT, cumulativePctCharged.toFloat()).apply()
            }
            previousChargeLevel = levelPct

            // 3. Diagnostik Pintar
            cableQuality = ChargingDiagnosticsHelper.evaluateCableQuality(cleanWatt, levelPct)
            isThermalThrottling = ChargingDiagnosticsHelper.checkThermalThrottling(tempC, peakWatt, cleanWatt)
            etaTo80Min = ChargingDiagnosticsHelper.calculateEta(levelPct, currentMa, 80)
            etaTo100Min = ChargingDiagnosticsHelper.calculateEta(levelPct, currentMa, 100)

            // 4. Pengesanan Trickle & Pengecasan Penuh
            if (levelPct == 100) {
                isTrickleCharging = (currentMa in 20..250)
                if (currentMa in 1..40 && !fullSaturationAlertFired && trickleAlarmEnabled) {
                    fullSaturationAlertFired = true
                    triggerAlarm(context)
                    BatteryVoiceAlert.speak(context, "Pengecasan selesai. Bateri kini seratus peratus tepu.")
                }
            } else {
                isTrickleCharging = false
            }

            // 5. Penapisan Kelicinan Titik Graf (Exponential Moving Average / EMA)
            // Mengelakkan riak kecil denyutan CPU daripada mengotorkan visual graf
            smoothedWatt = if (smoothedWatt <= 0.0) {
                cleanWatt
            } else {
                (0.35 * cleanWatt) + (0.65 * smoothedWatt)
            }

            smoothedTemp = if (smoothedTemp <= 0.0) {
                tempC
            } else {
                (0.20 * tempC) + (0.80 * smoothedTemp)
            }

            // Rekod ke history graf dengan selang masa minimum (2 saat) agar data tidak padat berhimpit
            if (now - lastHistoryTimestamp >= HISTORY_SAMPLE_INTERVAL_MS || history.isEmpty()) {
                history.add(ChartPoint(smoothedWatt.toFloat(), smoothedTemp.toFloat(), now))
                if (history.size > MAX_HISTORY_POINTS) history.removeAt(0)
                lastHistoryTimestamp = now
            }

            val today = getTodayDateString()
            dailyRecords.find { it.date == today }?.let {
                if (peakWatt > it.peakWatt) it.peakWatt = peakWatt
            }

            checkAlarms(context, levelPct, tempC)
        } else {
            if (levelPct <= 20 && !lowBatteryAlertFired && lowBatteryAlarmEnabled) {
                lowBatteryAlertFired = true
                triggerAlarm(context)
                BatteryVoiceAlert.speak(context, "Amaran. Bateri lemah pada paras " + levelPct + " peratus. Sila cucuk pengecas.")
            }
        }
        lastTimestamp = now
    }

    private fun checkAlarms(context: Context, levelPct: Int, tempC: Double) {
        if (batteryAlarmEnabled && !batteryAlarmFired && levelPct >= batteryThreshold) {
            batteryAlarmFired = true
            triggerAlarm(context)
            BatteryVoiceAlert.speak(context, "Perhatian. Paras bateri sudah mencecah " + levelPct + " peratus. Sila cabut kabel pengecas.")
        }
        if (tempAlarmEnabled && !tempAlarmFired && tempC >= tempThreshold) {
            tempAlarmFired = true
            triggerAlarm(context)
            BatteryVoiceAlert.speak(context, "Amaran kecemasan! Suhu bateri terlalu tinggi, mencecah " + tempC.toInt() + " darjah celcius. Sila cabut pengecas segera!")
        }
    }

    fun triggerAlarm(context: Context) {
        ChargingAlertHelper.triggerAlarm(context)
    }

    fun formatDuration(millis: Long): String {
        return ChargingDiagnosticsHelper.formatDuration(millis)
    }

    fun getEstimatedCycles(): Double = cumulativePctCharged / 100.0

    fun getWeeklyStats(): WeeklyStats = WeeklyStatsHelper.calculate(dailyRecords)

    fun saveSettings(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_BATTERY_ALARM, batteryAlarmEnabled)
            .putInt(KEY_BATTERY_THRESHOLD, batteryThreshold)
            .putBoolean(KEY_TEMP_ALARM, tempAlarmEnabled)
            .putInt(KEY_TEMP_THRESHOLD, tempThreshold)
            .putBoolean(KEY_LOW_BAT_ALARM, lowBatteryAlarmEnabled)
            .putBoolean(KEY_TRICKLE_ALARM, trickleAlarmEnabled)
            .apply()
    }
}
