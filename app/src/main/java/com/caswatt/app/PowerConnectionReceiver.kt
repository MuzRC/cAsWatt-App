package com.caswatt.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, ChargingService::class.java)

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 100

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                ChargingSessionManager.onChargingStarted(context, pct)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                ChargingSessionManager.onChargingStopped(context, pct)
                context.stopService(serviceIntent)
            }
        }

        val updateIntent = Intent(ChargingWidget.ACTION_UPDATE_WIDGET)
        context.sendBroadcast(updateIntent)
    }
}
