package com.caswatt.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.core.content.ContextCompat

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        // 1. Pastikan rekod sesi lama selamat dimuat turun ke memori dahulu
        ChargingSessionManager.ensureInit(context)

        // 2. Dapatkan bacaan peratus bateri yang jitu dan stabil
        val pct = getBatteryPercentage(context)
        val serviceIntent = Intent(context, ChargingService::class.java)

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                ChargingSessionManager.onChargingStarted(context, pct)
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    // Lindungi daripada ForegroundServiceStartNotAllowedException pada Android 12/14+
                    Log.e("PowerReceiver", "Gagal memulakan ChargingService: ${e.message}")
                }
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                ChargingSessionManager.onChargingStopped(context, pct)
                try {
                    context.stopService(serviceIntent)
                } catch (e: Exception) {
                    Log.e("PowerReceiver", "Gagal menghentikan ChargingService: ${e.message}")
                }
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                // Semak jika kabel pengecas memang sedia bercucuk sebaik telefon selesai boot
                val batteryStatus = try {
                    context.applicationContext.registerReceiver(
                        null,
                        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                    )
                } catch (_: Exception) {
                    null
                }

                val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isPlugged = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                if (isPlugged) {
                    ChargingSessionManager.onChargingStarted(context, pct)
                    try {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } catch (e: Exception) {
                        Log.e("PowerReceiver", "Gagal memulakan ChargingService semasa boot: ${e.message}")
                    }
                }
            }
        }

        // 3. Hantar arahan kemas kini ke Widget secara eksplisit supaya tidak disekat sistem
        val updateIntent = Intent(ChargingWidget.ACTION_UPDATE_WIDGET).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(updateIntent)
    }

    /**
     * Mengambil peratus bateri tanpa mencetuskan ReceiverCallNotAllowedException.
     * Menggunakan applicationContext sekiranya BatteryManager gagal memberi nilai sah.
     */
    private fun getBatteryPercentage(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val capacity = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

        // Jika nilai dalam julat sah 0% hingga 100%, gunakan terus
        if (capacity in 0..100) {
            return capacity
        }

        // Pelan sandaran menggunakan applicationContext (sah di sisi Android)
        return try {
            val stickyIntent = context.applicationContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = stickyIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = stickyIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100) / scale else 50
        } catch (_: Exception) {
            50 // Nilai tengah yang selamat sekiranya peranti langsung tidak memberi respon
        }
    }
}
