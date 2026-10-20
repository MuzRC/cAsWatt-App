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
                // Automatik hidupkan servis pantauan bila cucuk pengecas!
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                // Automatik matikan servis bila cabut kabel supaya bateri tidak terbazir
                context.stopService(serviceIntent)
                // Kemas kini widget ke mod discharging
                val updateIntent = Intent(ChargingWidget.ACTION_UPDATE_WIDGET)
                context.sendBroadcast(updateIntent)
            }
        }
    }
}
