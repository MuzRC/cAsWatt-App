package com.caswatt.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.core.content.ContextCompat

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, ChargingService::class.java)

        // Dapatkan peratus bateri terus daripada BatteryManager tanpa panggil context.registerReceiver (elak ReceiverCallNotAllowedException)
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100

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
