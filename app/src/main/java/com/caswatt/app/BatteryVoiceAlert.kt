package com.caswatt.app

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import java.util.Locale

object BatteryVoiceAlert : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingMessage: String? = null
    private var lastSpokenTimestamp = 0L

    /**
     * Memulakan enjin suara Android Text-to-Speech
     */
    @Synchronized
    fun init(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // 1. Cuba pasang suara Bahasa Melayu
            val msLocale = Locale("ms", "MY")
            val result = tts?.setLanguage(msLocale)

            // 2. Jika peranti tiada data suara Melayu, guna bahasa lalai telefon atau Bahasa Inggeris
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val fallbackResult = tts?.setLanguage(Locale.getDefault())
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
            }

            tts?.setPitch(1.0f)        // Ton suara normal
            tts?.setSpeechRate(0.95f)   // Kelajuan pertuturan jelas & santai
            isInitialized = true

            // Sebut mesej yang menunggu sebaik inisialisasi selesai
            pendingMessage?.let {
                speakText(it)
                pendingMessage = null
            }
        } else {
            Log.e("BatteryVoiceAlert", "Inisialisasi Android TTS gagal dijalankan.")
        }
    }

    /**
     * Sebut teks amaran harian (dilengkapi sekatan selang 5 saat untuk elak spam audio).
     */
    @Synchronized
    fun speak(context: Context, message: String) {
        init(context)

        val now = System.currentTimeMillis()
        if (now - lastSpokenTimestamp < 5000L) return
        lastSpokenTimestamp = now

        if (isInitialized) {
            speakText(message)
        } else {
            pendingMessage = message
        }
    }

    /**
     * Fungsi khas untuk menguji fungsi suara secara langsung.
     * Mengabaikan pemasa selang (cooldown) dan memaparkan notis Toast maklum balas.
     */
    @Synchronized
    fun testVoice(context: Context) {
        init(context)

        val testMessage = "Ujian penggera bersuara. CasWatt Pro bersedia memantau bateri anda."

        Toast.makeText(
            context,
            "🔊 Menguji suara TTS (Pastikan Kelantangan Media aktif)...",
            Toast.LENGTH_SHORT
        ).show()

        if (isInitialized) {
            speakText(testMessage)
        } else {
            pendingMessage = testMessage
        }
    }

    private fun speakText(text: String) {
        val utteranceId = "CasWattAlert_${System.currentTimeMillis()}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = Bundle()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
