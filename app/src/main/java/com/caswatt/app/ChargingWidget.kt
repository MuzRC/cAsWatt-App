package com.caswatt.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews
import java.util.Locale
import kotlin.math.abs

class ChargingWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        ChargingSessionManager.ensureInit(context)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            ChargingSessionManager.ensureInit(context)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, ChargingWidget::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.caswatt.app.ACTION_UPDATE_WIDGET"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            // 1. Pastikan rekod dan konfigurasi masa aktif sebelum dibaca
            ChargingSessionManager.ensureInit(context)

            val views = RemoteViews(context.packageName, R.layout.widget_charging)
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

            // Membaca sticky intent menggunakan applicationContext untuk elak ReceiverCallNotAllowedException
            val batteryStatus: Intent? = try {
                context.applicationContext.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
            } catch (_: Exception) {
                null
            }

            var watts = 0.0
            var currentMa = 0
            var voltageV = 0.0
            var pct = 0
            var isCharging = false

            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                pct = if (level >= 0 && scale > 0) (level * 100) / scale else 0

                val voltageMv = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                voltageV = voltageMv / 1000.0

                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val currentNowMicro = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
                
                // Pembaikan had pemisah (50,000) bagi mengelak salah anggap arus pengecasan pantas 10A-12A
                currentMa = if (abs(currentNowMicro) > 50000) {
                    abs(currentNowMicro) / 1000
                } else {
                    abs(currentNowMicro)
                }
                
                watts = (voltageMv.toDouble() * currentMa) / 1000000.0
            }

            views.setTextViewText(R.id.tv_widget_battery, "$pct%")

            if (isCharging) {
                views.setTextViewText(R.id.tv_widget_status, "CHARGING ⚡")
                views.setTextColor(R.id.tv_widget_status, 0xFF00E676.toInt())
                views.setTextViewText(R.id.tv_widget_watt, String.format(Locale.US, "%.2f W", watts))
                views.setTextColor(R.id.tv_widget_watt, 0xFF00E676.toInt())

                val peakStr = if (ChargingSessionManager.peakWatt > 0.0) {
                    String.format(Locale.US, " • Puncak: %.2fW", ChargingSessionManager.peakWatt)
                } else ""
                views.setTextViewText(
                    R.id.tv_widget_details,
                    String.format(Locale.US, "%d mA • %.2f V%s", currentMa, voltageV, peakStr)
                )
            } else {
                val elapsed = System.currentTimeMillis() - ChargingSessionManager.lastUnplugTimestamp
                val durationStr = ChargingSessionManager.formatDuration(elapsed)
                val dropPct = (ChargingSessionManager.lastUnplugLevel - pct).coerceAtLeast(0)

                views.setTextViewText(R.id.tv_widget_status, "NYAHCAS (-${dropPct}%)")
                views.setTextColor(R.id.tv_widget_status, 0xFF29B6F6.toInt())
                views.setTextViewText(R.id.tv_widget_watt, durationStr)
                views.setTextColor(R.id.tv_widget_watt, 0xFFFFFFFF.toInt())
                views.setTextViewText(
                    R.id.tv_widget_details,
                    String.format(Locale.US, "%.2f V • Sejak cabut pengecas", voltageV)
                )
            }

            // Tetapkan pembukaan MainActivity bila mana-mana komponen widget disentuh
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.tv_widget_watt, pendingIntent)
            views.setOnClickPendingIntent(R.id.tv_widget_battery, pendingIntent)
            views.setOnClickPendingIntent(R.id.tv_widget_status, pendingIntent)
            views.setOnClickPendingIntent(R.id.tv_widget_details, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}