package com.caswatt.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale
import kotlin.math.abs

class ChargingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val CHANNEL_ID = "CasWattMonitorChannel"
    private val NOTIF_ID = 2026

    // Pengurusan kitaran masa kemas kini
    private var lastTileUpdateTimestamp = 0L
    private var lastWidgetUpdateTimestamp = 0L
    private var cachedPendingIntent: PendingIntent? = null

    private val updateRunnable = object : Runnable {
        override fun run() {
            val isCharging = updateMetricsAndWidget()

            // Jika sedang dicas, pantau laju (1.5s). Jika guna bateri biasa, lambatkan ke 5s untuk jimat kuasa.
            val nextDelay = if (isCharging) 1500L else 5000L
            handler.postDelayed(this, nextDelay)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        ChargingSessionManager.ensureInit(this)
        createNotificationChannel()

        // Bina PendingIntent sekali sahaja dalam memori
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        cachedPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mulakan Foreground Service dengan sokongan jenis servis Android 14+
        startForegroundCompat("Memulakan bacaan cas jitu...")
        handler.post(updateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Pastikan servis dihidupkan semula secara automatik jika ditamatkan sistem akibat kekurangan RAM
        return START_STICKY
    }

    private fun updateMetricsAndWidget(): Boolean {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        
        // Membaca sticky intent secara selamat tanpa kebocoran pendaftaran
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            registerReceiver(null, filter)
        }

        var watts = 0.0
        var currentMa = 0
        var isCharging = false
        var tempC = 0.0
        var levelPct = 0
        var healthCode = BatteryManager.BATTERY_HEALTH_UNKNOWN

        if (batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            levelPct = if (level >= 0 && scale > 0) (level * 100) / scale else 0

            val voltageMv = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val tempTenths = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            tempC = tempTenths / 10.0

            healthCode = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

            val currentNowMicro = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
            
            // Pembaikan had pemisah (threshold): Gunakan 50,000 supaya arus pengecasan pantas 10A-12A tidak disalah anggap sebagai microampere
            currentMa = if (abs(currentNowMicro) > 50000) {
                abs(currentNowMicro) / 1000
            } else {
                abs(currentNowMicro)
            }
            
            watts = (voltageMv.toDouble() * currentMa) / 1000000.0

            ChargingSessionManager.recordSample(this, watts, currentMa, tempC, levelPct, isCharging, healthCode)
        }

        // Susun teks notifikasi
        val text = if (isCharging) {
            val etaStr = if (ChargingSessionManager.etaTo80Min > 0) {
                " • ~${ChargingSessionManager.etaTo80Min}m ke 80%"
            } else ""
            String.format(
                Locale.US,
                "⚡ %.2f W | Puncak: %.2f W | %.2f Wh%s",
                watts,
                ChargingSessionManager.peakWatt,
                ChargingSessionManager.energyWh,
                etaStr
            )
        } else {
            val duration = ChargingSessionManager.formatDuration(
                System.currentTimeMillis() - ChargingSessionManager.lastUnplugTimestamp
            )
            String.format(Locale.getDefault(), "Kabel dicabut. Tempoh guna: %s", duration)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIF_ID, createNotification(text))

        val now = System.currentTimeMillis()

        // Hadkan kemas kini widget (maksimum setiap 5 saat sekali untuk elak skrin tersangkut)
        if (now - lastWidgetUpdateTimestamp >= 5000L) {
            lastWidgetUpdateTimestamp = now
            val updateIntent = Intent(ChargingWidget.ACTION_UPDATE_WIDGET).apply {
                setPackage(packageName)
            }
            sendBroadcast(updateIntent)
        }

        // Hadkan kemas kini Tile (maksimum setiap 10 saat sekali untuk elak disekat Android)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && (now - lastTileUpdateTimestamp >= 10000L)) {
            lastTileUpdateTimestamp = now
            try {
                TileService.requestListeningState(this, ComponentName(this, CasWattTileService::class.java))
            } catch (e: Exception) {
                Log.w("ChargingService", "TileService listening state request throttle: ${e.message}")
            }
        }

        return isCharging
    }

    private fun startForegroundCompat(initialText: String) {
        val notification = createNotification(initialText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Untuk Android 14+ (API 34), tentukan jenis foreground service yang bersesuaian jika didaftarkan dalam Manifest
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } catch (_: Exception) {
                startForeground(NOTIF_ID, notification)
            }
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CasWatt Pro Live Monitor")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(cachedPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pantauan Watt Bateri",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Saluran notifikasi untuk paparan status cas bateri langsung"
                setShowBadge(false)
            }
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