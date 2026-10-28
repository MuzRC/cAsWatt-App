package com.caswatt.app

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import java.util.Locale

object ChargingAlertHelper {

    private var activeRingtone: Ringtone? = null
    private var activeVibrator: Vibrator? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    /**
     * Inisialisasi awal TTS (pilihan untuk dipanggil semasa app bermula)
     */
    fun initTts(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("ms", "MY"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                isTtsReady = true
            }
        }
    }

    /**
     * Membunyikan penggera penuh: Nada Alarm, Getaran, dan Suara TTS (Pilihan)
     */
    fun triggerAlarm(
        context: Context,
        customUriStr: String? = null,
        enableSound: Boolean = true,
        enableVibrate: Boolean = true,
        ttsMessage: String? = null
    ) {
        stopAlarm(context)

        // 1. Nada Dering Laluan STREAM_ALARM
        if (enableSound) {
            try {
                val uri = if (!customUriStr.isNullOrEmpty()) {
                    Uri.parse(customUriStr)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                activeRingtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                activeRingtone?.let { ringtone ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ringtone.audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    ringtone.play()
                }
            } catch (_: Exception) {}
        }

        // 2. Getaran Perkakasan Corak Denyutan Amaran
        if (enableVibrate) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vibrator != null && vibrator.hasVibrator()) {
                    activeVibrator = vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(longArrayOf(0, 450, 200, 450), -1)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(1000)
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Suara TTS Laluan ALARM (Pic 1.05x, Laju 1.15x)
        if (!ttsMessage.isNullOrEmpty()) {
            speakAlert(context, ttsMessage)
        }
    }

    /**
     * Membunyikan suara TTS tegas di laluan ALARM
     */
    fun speakAlert(context: Context, text: String) {
        val appContext = context.applicationContext
        if (tts == null) {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val res = tts?.setLanguage(Locale("ms", "MY"))
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.language = Locale.getDefault()
                    }
                    isTtsReady = true
                    playSpeech(text)
                }
            }
        } else if (isTtsReady) {
            playSpeech(text)
        }
    }

    private fun playSpeech(text: String) {
        try {
            tts?.setPitch(1.05f)
            tts?.setSpeechRate(1.15f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "CasWatt_Alert")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (_: Exception) {}
    }

    /**
     * Menghentikan serta-merta nada penggera, gegaran, dan suara TTS
     */
    fun stopAlarm(context: Context? = null) {
        try {
            if (activeRingtone?.isPlaying == true) {
                activeRingtone?.stop()
            }
            activeRingtone = null

            activeVibrator?.cancel()
            activeVibrator = null

            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (_: Exception) {}
    }

    /**
     * Kaedah 2: Pintasan terus ke paparan rasmi Penggunaan Bateri Sistem Android
     */
    fun openSystemBatterySettings(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }
}
