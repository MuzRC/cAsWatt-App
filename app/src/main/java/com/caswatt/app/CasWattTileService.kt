package com.caswatt.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import java.util.Locale
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.N)
class CasWattTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        // Pastikan konfigurasi dan rekod sedia ada dimuat turun sebelum kemas kini jubin
        ChargingSessionManager.ensureInit(this)
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return

        // Baca status bateri secara selamat
        val batteryStatus: Intent? = try {
            applicationContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        } catch (_: Exception) {
            null
        }

        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        var watts = 0.0
        var isCharging = false
        var pct = 0

        if (batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            pct = if (level >= 0 && scale > 0) (level * 100) / scale else 0

            val voltageMv = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val currentNowMicro = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
            
            // Pembaikan had pemisah: gunakan 50,000 supaya arus 10A-12A tidak disalah anggap sebagai microampere
            val currentMa = if (abs(currentNowMicro) > 50000) {
                abs(currentNowMicro) / 1000
            } else {
                abs(currentNowMicro)
            }
            
            watts = (voltageMv.toDouble() * currentMa) / 1000000.0
        }

        if (isCharging) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = String.format(Locale.US, "%.1fW Cas", watts)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (ChargingSessionManager.peakWatt > 0.0) {
                    String.format(Locale.US, "Puncak: %.1fW", ChargingSessionManager.peakWatt)
                } else {
                    "Bateri $pct%"
                }
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            val elapsed = System.currentTimeMillis() - ChargingSessionManager.lastUnplugTimestamp
            tile.label = "Nyahcas " + ChargingSessionManager.formatDuration(elapsed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val dropPct = (ChargingSessionManager.lastUnplugLevel - pct).coerceAtLeast(0)
                tile.subtitle = "Bateri $pct% (-$dropPct%)"
            }
        }

        tile.updateTile()
    }
}