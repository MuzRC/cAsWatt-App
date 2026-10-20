package com.caswatt.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, ChargingService::class.java)

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                context.stopService(serviceIntent)
                val updateIntent = Intent(ChargingWidget.ACTION_UPDATE_WIDGET)
                context.sendBroadcast(updateIntent)
            }
        }
    }
}
