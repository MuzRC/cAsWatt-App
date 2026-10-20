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
import kotlin.math.abs

class ChargingWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
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
            val views = RemoteViews(context.packageName, R.layout.widget_charging)

            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
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

                val currentNowMicro = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                currentMa = if (abs(currentNowMicro) > 10000) abs(currentNowMicro) / 1000 else abs(currentNowMicro)
                watts = (voltageMv.toDouble() * currentMa) / 1000000.0
            }

            views.setTextViewText(R.id.tv_widget_battery, "$pct%")
            views.setTextViewText(R.id.tv_widget_watt, String.format("%.1f W", watts))
            views.setTextViewText(R.id.tv_widget_details, String.format("%d mA • %.2f V", currentMa, voltageV))

            if (isCharging) {
                views.setTextViewText(R.id.tv_widget_status, "CHARGING ⚡")
                views.setTextColor(R.id.tv_widget_status, 0xFF00E676.toInt())
                views.setTextColor(R.id.tv_widget_watt, 0xFF00E676.toInt())
            } else {
                views.setTextViewText(R.id.tv_widget_status, "DISCHARGING")
                views.setTextColor(R.id.tv_widget_status, 0xFF9E9E9E.toInt())
                views.setTextColor(R.id.tv_widget_watt, 0xFFFFFFFF.toInt())
            }

            // Klik widget untuk buka aplikasi
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_widget_watt, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
