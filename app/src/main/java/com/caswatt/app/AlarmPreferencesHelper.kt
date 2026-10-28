package com.caswatt.app

import android.content.Context
import java.util.Calendar

object AlarmPreferencesHelper {
    private const val PREFS_NAME = "caswatt_alarm_prefs"

    // Kunci Simpanan Tetapan Penggera
    private const val KEY_HIGH_LEVEL_ENABLED = "high_level_alarm_enabled"
    private const val KEY_HIGH_LEVEL_THRESHOLD = "high_level_threshold"

    private const val KEY_THERMAL_ENABLED = "thermal_alarm_enabled"
    private const val KEY_THERMAL_THRESHOLD = "thermal_threshold"

    private const val KEY_LOW_BATTERY_ENABLED = "low_battery_alarm_enabled"
    private const val KEY_LOW_BATTERY_THRESHOLD = "low_battery_threshold"

    private const val KEY_SATURATION_ENABLED = "saturation_alarm_enabled" // Pengecasan Tepu (Trickle)
    private const val KEY_ANTI_THEFT_ENABLED = "anti_theft_alarm_enabled"
    private const val KEY_SLOW_CHARGE_ENABLED = "slow_charge_alarm_enabled"
    private const val KEY_OVER_VOLTAGE_ENABLED = "over_voltage_alarm_enabled"
    private const val KEY_RAPID_DRAIN_ENABLED = "rapid_drain_alarm_enabled"

    // Output Audio & Getaran
    private const val KEY_SOUND_ENABLED = "alarm_sound_enabled"
    private const val KEY_VIBRATE_ENABLED = "alarm_vibrate_enabled"
    private const val KEY_TTS_ENABLED = "alarm_tts_enabled"
    private const val KEY_CUSTOM_RINGTONE_URI = "custom_ringtone_uri"

    // Waktu Senyap (Quiet Hours) & Ulangan
    private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
    private const val KEY_QUIET_START_HOUR = "quiet_start_hour"
    private const val KEY_QUIET_START_MIN = "quiet_start_min"
    private const val KEY_QUIET_END_HOUR = "quiet_end_hour"
    private const val KEY_QUIET_END_MIN = "quiet_end_min"

    private const val KEY_LOOP_ALARM_ENABLED = "loop_alarm_enabled"
    private const val KEY_LOOP_INTERVAL_SEC = "loop_interval_sec"

    // --- 1. Had Paras Bateri Penuh ---
    fun isHighLevelEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_HIGH_LEVEL_ENABLED, true)

    fun setHighLevelEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_HIGH_LEVEL_ENABLED, enabled).apply()

    fun getHighLevelThreshold(context: Context): Int =
        getPrefs(context).getInt(KEY_HIGH_LEVEL_THRESHOLD, 80)

    fun setHighLevelThreshold(context: Context, pct: Int) =
        getPrefs(context).edit().putInt(KEY_HIGH_LEVEL_THRESHOLD, pct).apply()

    // --- 2. Had Suhu Panas ---
    fun isThermalEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_THERMAL_ENABLED, true)

    fun setThermalEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_THERMAL_ENABLED, enabled).apply()

    fun getThermalThreshold(context: Context): Float =
        getPrefs(context).getFloat(KEY_THERMAL_THRESHOLD, 45.0f)

    fun setThermalThreshold(context: Context, tempC: Float) =
        getPrefs(context).edit().putFloat(KEY_THERMAL_THRESHOLD, tempC).apply()

    // --- 3. Bateri Lemah (Low Battery) ---
    fun isLowBatteryEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_LOW_BATTERY_ENABLED, true)

    fun setLowBatteryEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_LOW_BATTERY_ENABLED, enabled).apply()

    fun getLowBatteryThreshold(context: Context): Int =
        getPrefs(context).getInt(KEY_LOW_BATTERY_THRESHOLD, 20)

    fun setLowBatteryThreshold(context: Context, pct: Int) =
        getPrefs(context).edit().putInt(KEY_LOW_BATTERY_THRESHOLD, pct).apply()

    // --- 4. Pengecasan Tepu (Saturation / Trickle) ---
    fun isSaturationEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SATURATION_ENABLED, true)

    fun setSaturationEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_SATURATION_ENABLED, enabled).apply()

    // --- 5. Keselamatan Tambahan (Anti-Theft, Slow Charge, Over-Voltage, Rapid Drain) ---
    fun isAntiTheftEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ANTI_THEFT_ENABLED, false)

    fun setAntiTheftEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_ANTI_THEFT_ENABLED, enabled).apply()

    fun isSlowChargeEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SLOW_CHARGE_ENABLED, true)

    fun setSlowChargeEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_SLOW_CHARGE_ENABLED, enabled).apply()

    fun isOverVoltageEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_OVER_VOLTAGE_ENABLED, true)

    fun setOverVoltageEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_OVER_VOLTAGE_ENABLED, enabled).apply()

    fun isRapidDrainEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_RAPID_DRAIN_ENABLED, true)

    fun setRapidDrainEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_RAPID_DRAIN_ENABLED, enabled).apply()

    // --- 6. Saluran Output Amaran ---
    fun isSoundEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SOUND_ENABLED, true)

    fun setSoundEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()

    fun isVibrateEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_VIBRATE_ENABLED, true)

    fun setVibrateEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_VIBRATE_ENABLED, enabled).apply()

    fun isTtsEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_TTS_ENABLED, true)

    fun setTtsEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()

    fun getCustomRingtoneUri(context: Context): String? =
        getPrefs(context).getString(KEY_CUSTOM_RINGTONE_URI, null)

    fun setCustomRingtoneUri(context: Context, uriStr: String?) =
        getPrefs(context).edit().putString(KEY_CUSTOM_RINGTONE_URI, uriStr).apply()

    // --- 7. Waktu Senyap (Quiet Hours) ---
    fun isQuietHoursEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_QUIET_HOURS_ENABLED, false)

    fun setQuietHoursEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_QUIET_HOURS_ENABLED, enabled).apply()

    fun getQuietStartTime(context: Context): Pair<Int, Int> =
        Pair(getPrefs(context).getInt(KEY_QUIET_START_HOUR, 23), getPrefs(context).getInt(KEY_QUIET_START_MIN, 0))

    fun setQuietStartTime(context: Context, hour: Int, min: Int) =
        getPrefs(context).edit()
            .putInt(KEY_QUIET_START_HOUR, hour)
            .putInt(KEY_QUIET_START_MIN, min)
            .apply()

    fun getQuietEndTime(context: Context): Pair<Int, Int> =
        Pair(getPrefs(context).getInt(KEY_QUIET_END_HOUR, 6), getPrefs(context).getInt(KEY_QUIET_END_MIN, 0))

    fun setQuietEndTime(context: Context, hour: Int, min: Int) =
        getPrefs(context).edit()
            .putInt(KEY_QUIET_END_HOUR, hour)
            .putInt(KEY_QUIET_END_MIN, min)
            .apply()

    /**
     * Semak adakah waktu sekarang berada dalam zon Waktu Senyap
     */
    fun isInQuietHours(context: Context): Boolean {
        if (!isQuietHoursEnabled(context)) return false
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val (startH, startM) = getQuietStartTime(context)
        val startMinutes = startH * 60 + startM

        val (endH, endM) = getQuietEndTime(context)
        val endMinutes = endH * 60 + endM

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            // Merentasi tengah malam (contoh: 23:00 malam hingga 06:00 pagi)
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }

    // --- 8. Corak Ulangan Penggera ---
    fun isLoopAlarmEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_LOOP_ALARM_ENABLED, true)

    fun setLoopAlarmEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_LOOP_ALARM_ENABLED, enabled).apply()

    fun getLoopIntervalSec(context: Context): Int =
        getPrefs(context).getInt(KEY_LOOP_INTERVAL_SEC, 60)

    fun setLoopIntervalSec(context: Context, seconds: Int) =
        getPrefs(context).edit().putInt(KEY_LOOP_INTERVAL_SEC, seconds).apply()

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
