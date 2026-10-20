package com.caswatt.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvWatt: TextView
    private lateinit var tvCurrent: TextView
    private lateinit var tvVoltage: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvPlug: TextView
    private lateinit var btnToggleService: Button

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateBatteryInfo(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_main_status)
        tvWatt = findViewById(R.id.tv_main_watt)
        tvCurrent = findViewById(R.id.tv_main_current)
        tvVoltage = findViewById(R.id.tv_main_voltage)
        tvLevel = findViewById(R.id.tv_main_level)
        tvTemp = findViewById(R.id.tv_main_temp)
        tvPlug = findViewById(R.id.tv_main_plug)
        btnToggleService = findViewById(R.id.btn_toggle_service)

        // Minta kebenaran Notifikasi untuk Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        btnToggleService.setOnClickListener {
            val serviceIntent = Intent(this, ChargingService::class.java)
            if (ChargingService.isRunning) {
                stopService(serviceIntent)
                btnToggleService.text = "Jalankan Servis Pantauan Background"
            } else {
                ContextCompat.startForegroundService(this, serviceIntent)
                btnToggleService.text = "Hentikan Servis Background"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        btnToggleService.text = if (ChargingService.isRunning) "Hentikan Servis Background" else "Jalankan Servis Pantauan Background"
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
    }

    private fun updateBatteryInfo(intent: Intent) {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val voltageV = voltageMv / 1000.0

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val currentNowMicro = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        // Kebanyakan vendor guna uA (microamperes), sebahagian kecil guna mA terus
        val currentMa = if (abs(currentNowMicro) > 10000) {
            abs(currentNowMicro) / 1000
        } else {
            abs(currentNowMicro)
        }

        // Kira Watt = (V * mA) / 1,000,000
        val watts = (voltageMv.toDouble() * currentMa) / 1000000.0

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempC = tempTenths / 10.0

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Pengecas Dinding (AC)"
            BatteryManager.BATTERY_PLUGGED_USB -> "Port USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Cas Tanpa Wayar (Wireless)"
            else -> "Menggunakan Bateri"
        }

        tvStatus.text = if (isCharging) "SEDANG DICAS ⚡" else "TIDAK DICAS"
        tvStatus.setTextColor(if (isCharging) 0xFF00E676.toInt() else 0xFFFFAB00.toInt())
        tvWatt.text = String.format("%.1f W", watts)
        tvWatt.setTextColor(if (isCharging) 0xFF00E676.toInt() else 0xFFFFFFFF.toInt())
        tvCurrent.text = "Arus: $currentMa mA"
        tvVoltage.text = String.format("Voltan: %.2f V", voltageV)
        tvLevel.text = "Peratus Bateri: $pct%"
        tvTemp.text = String.format("Suhu Bateri: %.1f °C", tempC)
        tvPlug.text = "Sumber Cas: $plugType"
    }
}
