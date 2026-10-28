package com.caswatt.app

import android.app.KeyguardManager
import android.content.Context

object AlarmMonitorHelper {

    private var lastAlarmTimestamp: Long = 0L
    private var lastTriggeredReason: String = ""

    // Pemantauan Cas Perlahan
    private var pluggedInTimestamp: Long = 0L
    private var hasAlertedSlowCharge: Boolean = false

    // Pemantauan Kering Mendadak (Rapid Drain)
    private var drainCheckTimestamp: Long = 0L
    private var drainStartLevel: Int = -1
    private var hasAlertedRapidDrain: Boolean = false

    // Status Anti-Kecurian
    var isAntiTheftArmed: Boolean = false

    fun onPowerConnected(context: Context) {
        pluggedInTimestamp = System.currentTimeMillis()
        hasAlertedSlowCharge = false
        drainCheckTimestamp = 0L
        drainStartLevel = -1
        hasAlertedRapidDrain = false

        // Jika suis anti-kecurian aktif, aktifkan kawalan berkawal (armed)
        if (AlarmPreferencesHelper.isAntiTheftEnabled(context)) {
            isAntiTheftArmed = true
        }
    }

    fun onPowerDisconnected(context: Context) {
        pluggedInTimestamp = 0L

        // 1. Semakan Penggera Anti-Kecurian (Unplug Alarm)
        if (isAntiTheftArmed && AlarmPreferencesHelper.isAntiTheftEnabled(context)) {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val isLocked = keyguardManager?.isKeyguardLocked ?: true

            if (isLocked) {
                // Wayar dicabut semasa skrin berkunci - bunyikan amaran kecemasan
                fireAlarm(
                    context = context,
                    reason = "ANTI_THEFT",
                    ttsMsg = "Perhatian! Pengecas telah dicabut tanpa kebenaran.",
                    forceBypassQuietHours = true
                )
            }
        }
        isAntiTheftArmed = false

        // Mulakan pemantauan nyahcas (discharging)
        drainCheckTimestamp = System.currentTimeMillis()
    }

    fun checkBatteryState(
        context: Context,
        level: Int,
        tempC: Float,
        voltageV: Double,
        powerWatts: Double,
        isCharging: Boolean
    ) {
        val now = System.currentTimeMillis()

        // Semakan semasa sedang dicas
        if (isCharging) {
            // A. Penggera Suhu Panas
            if (AlarmPreferencesHelper.isThermalEnabled(context)) {
                val thermalLimit = AlarmPreferencesHelper.getThermalThreshold(context)
                if (tempC >= thermalLimit) {
                    dispatchPeriodicAlarm(
                        context,
                        "THERMAL",
                        "Amaran suhu tinggi! Suhu bateri mencecah ${tempC.toInt()} darjah celcius. Sila rehatkan telefon."
                    )
                    return
                }
            }

            // B. Penggera Voltan Terlebih (> 4.45V)
            if (AlarmPreferencesHelper.isOverVoltageEnabled(context) && voltageV >= 4.45) {
                dispatchPeriodicAlarm(
                    context,
                    "OVER_VOLTAGE",
                    "Amaran lonjakan voltan! Voltan sel melebihi 4.45 volt. Sila cabut pengecas untuk keselamatan."
                )
                return
            }

            // C. Penggera Cas Terlalu Perlahan / Sambungan Longgar (Selepas 60 saat dicas)
            if (AlarmPreferencesHelper.isSlowChargeEnabled(context) && !hasAlertedSlowCharge) {
                if (pluggedInTimestamp > 0L && (now - pluggedInTimestamp) >= 60_000L) {
                    if (powerWatts in 0.05..2.2 && level < 95) {
                        hasAlertedSlowCharge = true
                        fireAlarm(
                            context = context,
                            reason = "SLOW_CHARGE",
                            ttsMsg = "Pengecasan terlalu perlahan dikesan. Sila pastikan kabel dan soket dipasang dengan rapat."
                        )
                    }
                }
            }

            // D. Penggera Pengecasan Tepu (Saturation / Trickle di 100%)
            if (AlarmPreferencesHelper.isSaturationEnabled(context) && level >= 100) {
                if (powerWatts in 0.01..2.0) {
                    dispatchPeriodicAlarm(
                        context,
                        "SATURATION",
                        "Pengecasan tepu selesai. Bateri kini seratus peratus penuh sempurna."
                    )
                    return
                }
            }

            // E. Penggera Had Paras Bateri Penuh (Contoh: 80%, 85%, 90%)
            if (AlarmPreferencesHelper.isHighLevelEnabled(context)) {
                val targetLevel = AlarmPreferencesHelper.getHighLevelThreshold(context)
                if (level >= targetLevel && level < 100) {
                    dispatchPeriodicAlarm(
                        context,
                        "HIGH_LEVEL",
                        "Bateri telah mencapai had $targetLevel peratus. Sila cabut pengecas."
                    )
                    return
                }
            }

        } else {
            // Semakan semasa TIDAK dicas (Bateri digunakan)

            // F. Penggera Bateri Lemah (Low Battery)
            if (AlarmPreferencesHelper.isLowBatteryEnabled(context)) {
                val lowLimit = AlarmPreferencesHelper.getLowBatteryThreshold(context)
                if (level <= lowLimit) {
                    dispatchPeriodicAlarm(
                        context,
                        "LOW_BATTERY",
                        "Bateri lemah, tinggal $level peratus. Sila sambungkan pengecas."
                    )
                    return
                }
            }

            // G. Pengesan Bateri Kering Mendadak (Rapid Drain: Turun >= 5% dalam masa <= 10 minit)
            if (AlarmPreferencesHelper.isRapidDrainEnabled(context) && !hasAlertedRapidDrain) {
                if (drainStartLevel == -1) {
                    drainStartLevel = level
                    drainCheckTimestamp = now
                } else {
                    val timeDiffMinutes = (now - drainCheckTimestamp) / 60000.0
                    val drop = drainStartLevel - level

                    if (timeDiffMinutes <= 10.0 && drop >= 5) {
                        hasAlertedRapidDrain = true
                        fireAlarm(
                            context = context,
                            reason = "RAPID_DRAIN",
                            ttsMsg = "Amaran! Penggunaan bateri mendadak dikesan di latar belakang."
                        )
                    } else if (timeDiffMinutes > 10.0) {
                        // Reset tetingkap masa pemantauan
                        drainStartLevel = level
                        drainCheckTimestamp = now
                    }
                }
            }
        }
    }

    private fun dispatchPeriodicAlarm(context: Context, reason: String, ttsMsg: String) {
        val now = System.currentTimeMillis()
        val loopEnabled = AlarmPreferencesHelper.isLoopAlarmEnabled(context)
        val intervalMs = AlarmPreferencesHelper.getLoopIntervalSec(context) * 1000L

        if (lastTriggeredReason == reason) {
            if (!loopEnabled) return
            if (now - lastAlarmTimestamp < intervalMs) return
        }

        lastAlarmTimestamp = now
        lastTriggeredReason = reason
        fireAlarm(context, reason, ttsMsg)
    }

    private fun fireAlarm(
        context: Context,
        reason: String,
        ttsMsg: String,
        forceBypassQuietHours: Boolean = false
    ) {
        // Semak Waktu Senyap (Quiet Hours)
        val isQuiet = AlarmPreferencesHelper.isInQuietHours(context)
        val allowSound = if (forceBypassQuietHours) true else (!isQuiet && AlarmPreferencesHelper.isSoundEnabled(context))
        val allowTts = if (forceBypassQuietHours) true else (!isQuiet && AlarmPreferencesHelper.isTtsEnabled(context))
        val allowVibrate = AlarmPreferencesHelper.isVibrateEnabled(context)

        val ringtoneUri = AlarmPreferencesHelper.getCustomRingtoneUri(context)

        ChargingAlertHelper.triggerAlarm(
            context = context,
            customUriStr = ringtoneUri,
            enableSound = allowSound,
            enableVibrate = allowVibrate,
            ttsMessage = if (allowTts) ttsMsg else null
        )
    }

    fun resetAlarmState() {
        lastTriggeredReason = ""
        lastAlarmTimestamp = 0L
    }
}
