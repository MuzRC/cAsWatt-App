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
                var serviceReady = ChargingService.isRunning

                // Jika servis belum hidup, cuba mulakan dahulu
                if (!serviceReady) {
                    try {
                        ContextCompat.startForegroundService(context, serviceIntent)
                        serviceReady = true
                    } catch (e: Exception) {
                        serviceReady = false
                        Log.e("PowerReceiver", "Disekat memulakan ChargingService di latar belakang: ${e.message}")
                    }
                }

                // Hanya mulakan sesi cas jika servis sah berjalan untuk elak rekod kosong 0 W / 0 mAh
                if (serviceReady) {
                    ChargingSessionManager.onChargingStarted(context, pct)
                }
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                // Catat penamatan sesi pengecasan dan simpan data terkumpul
                ChargingSessionManager.onChargingStopped(context, pct)

                // JANGAN panggil context.stopService() di sini.
                // ChargingService direka khas untuk kekal hidup memantau tempoh guna bateri (5s)
                // dan memastikan servis sedia ada di memori supaya tidak disekat Android bila dicucuk semula.
                if (!ChargingService.isRunning) {
                    try {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } catch (e: Exception) {
                        Log.w("PowerReceiver", "Servis pemantau santai gagal dimulakan: ${e.message}")
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                // Mulakan servis pemantauan serta-merta sebaik sahaja telefon selesai boot
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    Log.e("PowerReceiver", "Gagal memulakan ChargingService semasa boot: ${e.message}")
                }

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
                }
            }
        }

        // 3. Hantar arahan kemas kini ke Widget secara eksplisit
        try {
            val updateIntent = Intent(ChargingWidget.ACTION_UPDATE_WIDGET).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(updateIntent)
        } catch (_: Exception) {
            // Abaikan jika tiada widget dipasang di skrin utama
        }
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

        // Pelan sandaran menggunakan applicationContext
        return try {
            val stickyIntent = context.applicationContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = stickyIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = stickyIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100) / scale else 50
        } catch (_: Exception) {
            50
        }
    }
}
