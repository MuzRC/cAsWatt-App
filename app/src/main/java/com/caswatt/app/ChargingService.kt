package com.caswatt.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class ChargingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val CHANNEL_ID = "CasWattMonitorChannel"
    private val NOTIF_ID = 2026

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateMetricsAndWidget()
            handler.postDelayed(this, 1500) // Kemas kini setiap 1.5 saat
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIF_ID, createNotification("Memulakan bacaan cas..."))
        handler.post(updateRunnable)
    }

    private fun updateMetricsAndWidget() {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            registerReceiver(null, filter)
        }

        var watts = 0.0
        var currentMa = 0
        var isCharging = false

        if (batteryStatus != null) {
            val voltageMv = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val currentNowMicro = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            currentMa = if (abs(currentNowMicro) > 10000) abs(currentNowMicro) / 1000 else abs(currentNowMicro)
            watts = (voltageMv.toDouble() * currentMa) / 1000000.0
        }

        val text = if (isCharging) {
            String.format("Kuasa Cas: %.1f W (%d mA)", watts, currentMa)
        } else {
            "Kabel dicabut. Pemantauan santai."
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID, createNotification(text))

        // Hantar broadcast untuk refresh widget
        val updateIntent = Intent(ChargingWidget.ACTION_UPDATE_WIDGET)
        sendBroadcast(updateIntent)
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CasWatt Live Monitor")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pantauan Watt Bateri",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(updateRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        var isRunning = false
    }
}
