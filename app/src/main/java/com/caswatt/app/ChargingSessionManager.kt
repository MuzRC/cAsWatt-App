package com.caswatt.app

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.max

data class ChartPoint(
    val watt: Float,
    val temp: Float,
    val timestamp: Long
)

object ChargingSessionManager {
    private const val PREFS_NAME = "caswatt_prefs"
    private const val KEY_BATTERY_ALARM = "battery_alarm_enabled"
    private const val KEY_BATTERY_THRESHOLD = "battery_threshold"
    private const val KEY_TEMP_ALARM = "temp_alarm_enabled"
    private const val KEY_TEMP_THRESHOLD = "temp_threshold"

    var peakWatt: Double = 0.0
    var totalMah: Double = 0.0
    private var lastTimestamp: Long = 0L

    val history = mutableListOf<ChartPoint>()
    private const val MAX_HISTORY_POINTS = 300

    var batteryAlarmEnabled: Boolean = true
    var batteryThreshold: Int = 80
    var tempAlarmEnabled: Boolean = true
    var tempThreshold: Int = 45

    private var batteryAlarmFired: Boolean = false
    private var tempAlarmFired: Boolean = false

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        batteryAlarmEnabled = prefs.getBoolean(KEY_BATTERY_ALARM, true)
        batteryThreshold = prefs.getInt(KEY_BATTERY_THRESHOLD, 80)
        tempAlarmEnabled = prefs.getBoolean(KEY_TEMP_ALARM, true)
        tempThreshold = prefs.getInt(KEY_TEMP_THRESHOLD, 45)
    }

    fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_BATTERY_ALARM, batteryAlarmEnabled)
            .putInt(KEY_BATTERY_THRESHOLD, batteryThreshold)
            .putBoolean(KEY_TEMP_ALARM, tempAlarmEnabled)
            .putInt(KEY_TEMP_THRESHOLD, tempThreshold)
            .apply()
    }

    @Synchronized
    fun recordSample(context: Context, watt: Double, currentMa: Int, tempC: Double, levelPct: Int, isCharging: Boolean) {
        val now = System.currentTimeMillis()

        if (isCharging) {
            // Track Peak Watt
            if (watt > peakWatt) {
                peakWatt = watt
            }

            // Accumulate mAh: mAh = mA * (dt_seconds / 3600)
            if (lastTimestamp > 0L) {
                val dtSec = (now - lastTimestamp) / 1000.0
                if (dtSec in 0.5..10.0 && currentMa > 0) {
                    val dMah = currentMa * (dtSec / 3600.0)
                    totalMah += dMah
                }
            }

            // Add to chart history
            history.add(ChartPoint(watt.toFloat(), tempC.toFloat(), now))
            if (history.size > MAX_HISTORY_POINTS) {
                history.removeAt(0)
            }

            // Check Alarms
            checkAlarms(context, levelPct, tempC)
        } else {
            // Reset alarm triggers when unplugged
            batteryAlarmFired = false
            tempAlarmFired = false
        }

        lastTimestamp = now
    }

    private fun checkAlarms(context: Context, levelPct: Int, tempC: Double) {
        // Battery limit alarm
        if (batteryAlarmEnabled && !batteryAlarmFired && levelPct >= batteryThreshold) {
            batteryAlarmFired = true
            triggerAlarm(context)
        }

        // Temperature limit alarm
        if (tempAlarmEnabled && !tempAlarmFired && tempC >= tempThreshold) {
            tempAlarmFired = true
            triggerAlarm(context)
        }
    }

    fun triggerAlarm(context: Context) {
        try {
            // Bunyi penggera ringkas
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            ringtone?.play()

            // Getaran
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

    fun resetSession() {
        peakWatt = 0.0
        totalMah = 0.0
        history.clear()
        batteryAlarmFired = false
        tempAlarmFired = false
        lastTimestamp = System.currentTimeMillis()
    }
}
