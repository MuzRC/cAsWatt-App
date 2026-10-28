package com.caswatt.app

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri

object AlarmRingtoneHelper {

    /**
     * Hasilkan Intent untuk membuka Pemilih Nada Penggera Sistem (System Alarm Picker)
     */
    fun createRingtonePickerIntent(context: Context): Intent {
        val currentUriStr = AlarmPreferencesHelper.getCustomRingtoneUri(context)
        val existingUri = if (!currentUriStr.isNullOrEmpty()) {
            Uri.parse(currentUriStr)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

        return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Pilih Nada Penggera CasWatt")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
        }
    }

    /**
     * Dapatkan tajuk nada dering yang mesra pengguna daripada URI yang disimpan
     */
    fun getRingtoneTitle(context: Context): String {
        val uriStr = AlarmPreferencesHelper.getCustomRingtoneUri(context)
        if (uriStr.isNullOrEmpty()) {
            return "Nada Penggera Sistem Lalai"
        }
        return try {
            val uri = Uri.parse(uriStr)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.getTitle(context) ?: "Nada Pilihan Tersuai"
        } catch (_: Exception) {
            "Nada Pilihan Tersuai"
        }
    }

    /**
     * Simpan URI nada dering yang dipilih oleh pengguna
     */
    fun saveSelectedRingtone(context: Context, uri: Uri?) {
        if (uri != null) {
            AlarmPreferencesHelper.setCustomRingtoneUri(context, uri.toString())
        } else {
            AlarmPreferencesHelper.setCustomRingtoneUri(context, null)
        }
    }
}
