package com.caswatt.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    // Tab Views
    private lateinit var tabDashboard: ScrollView
    private lateinit var tabGraph: ScrollView
    private lateinit var tabAlarm: ScrollView
    private lateinit var tabSettings: ScrollView
    private lateinit var bottomNav: BottomNavigationView

    // Tab 1 Elements
    private lateinit var tvStatus: TextView
    private lateinit var tvWatt: TextView
    private lateinit var tvCurrent: TextView
    private lateinit var tvVoltage: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvPlug: TextView
    private lateinit var tvPeakWatt: TextView
    private lateinit var tvTotalMah: TextView
    private lateinit var btnResetSession: Button

    // Tab 2 Elements
    private lateinit var chartView: ChargingChartView
    private lateinit var tvGraphStatPoints: TextView
    private lateinit var tvGraphStatPeak: TextView
    private lateinit var btnClearGraph: Button

    // Tab 3 Elements
    private lateinit var switchBatteryAlarm: SwitchCompat
    private lateinit var switchTempAlarm: SwitchCompat
    private lateinit var tvBatteryAlarmLabel: TextView
    private lateinit var tvTempAlarmLabel: TextView
    private lateinit var btnThresh80: Button
    private lateinit var btnThresh85: Button
    private lateinit var btnThresh90: Button
    private lateinit var btnThresh100: Button
    private lateinit var btnTemp42: Button
    private lateinit var btnTemp45: Button
    private lateinit var btnTemp48: Button
    private lateinit var btnTestAlarm: Button

    // Tab 4 Elements
    private lateinit var btnToggleService: Button

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateBatteryInfo(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ChargingSessionManager.init(this)

        initViews()
        setupBottomNav()
        setupAlarmControls()
        setupSessionControls()
        checkPermissions()
    }

    private fun initViews() {
        tabDashboard = findViewById(R.id.view_tab_dashboard)
        tabGraph = findViewById(R.id.view_tab_graph)
        tabAlarm = findViewById(R.id.view_tab_alarm)
        tabSettings = findViewById(R.id.view_tab_settings)
        bottomNav = findViewById(R.id.bottom_navigation)

        tvStatus = findViewById(R.id.tv_main_status)
        tvWatt = findViewById(R.id.tv_main_watt)
        tvCurrent = findViewById(R.id.tv_main_current)
        tvVoltage = findViewById(R.id.tv_main_voltage)
        tvLevel = findViewById(R.id.tv_main_level)
        tvTemp = findViewById(R.id.tv_main_temp)
        tvPlug = findViewById(R.id.tv_main_plug)
        tvPeakWatt = findViewById(R.id.tv_peak_watt)
        tvTotalMah = findViewById(R.id.tv_total_mah)
        btnResetSession = findViewById(R.id.btn_reset_session)

        chartView = findViewById(R.id.charging_chart_view)
        tvGraphStatPoints = findViewById(R.id.tv_graph_stat_points)
        tvGraphStatPeak = findViewById(R.id.tv_graph_stat_peak)
        btnClearGraph = findViewById(R.id.btn_clear_graph)

        switchBatteryAlarm = findViewById(R.id.switch_battery_alarm)
        switchTempAlarm = findViewById(R.id.switch_temp_alarm)
        tvBatteryAlarmLabel = findViewById(R.id.tv_battery_alarm_label)
        tvTempAlarmLabel = findViewById(R.id.tv_temp_alarm_label)
        btnThresh80 = findViewById(R.id.btn_thresh_80)
        btnThresh85 = findViewById(R.id.btn_thresh_85)
        btnThresh90 = findViewById(R.id.btn_thresh_90)
        btnThresh100 = findViewById(R.id.btn_thresh_100)
        btnTemp42 = findViewById(R.id.btn_temp_42)
        btnTemp45 = findViewById(R.id.btn_temp_45)
        btnTemp48 = findViewById(R.id.btn_temp_48)
        btnTestAlarm = findViewById(R.id.btn_test_alarm)

        btnToggleService = findViewById(R.id.btn_toggle_service)
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            tabDashboard.visibility = View.GONE
            tabGraph.visibility = View.GONE
            tabAlarm.visibility = View.GONE
            tabSettings.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_dashboard -> {
                    tabDashboard.visibility = View.VISIBLE
                    true
                }
                R.id.nav_graph -> {
                    tabGraph.visibility = View.VISIBLE
                    chartView.invalidate()
                    true
                }
                R.id.nav_alarm -> {
                    tabAlarm.visibility = View.VISIBLE
                    true
                }
                R.id.nav_settings -> {
                    tabSettings.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }
    }

    private fun setupAlarmControls() {
        switchBatteryAlarm.isChecked = ChargingSessionManager.batteryAlarmEnabled
        switchTempAlarm.isChecked = ChargingSessionManager.tempAlarmEnabled

        updateAlarmUiState()

        switchBatteryAlarm.setOnCheckedChangeListener { _, isChecked ->
            ChargingSessionManager.batteryAlarmEnabled = isChecked
            ChargingSessionManager.saveSettings(this)
            updateAlarmUiState()
        }

        switchTempAlarm.setOnCheckedChangeListener { _, isChecked ->
            ChargingSessionManager.tempAlarmEnabled = isChecked
            ChargingSessionManager.saveSettings(this)
            updateAlarmUiState()
        }

        val threshButtons = listOf(
            btnThresh80 to 80,
            btnThresh85 to 85,
            btnThresh90 to 90,
            btnThresh100 to 100
        )
        for ((btn, thresh) in threshButtons) {
            btn.setOnClickListener {
                ChargingSessionManager.batteryThreshold = thresh
                ChargingSessionManager.saveSettings(this)
                updateAlarmUiState()
            }
        }

        val tempButtons = listOf(
            btnTemp42 to 42,
            btnTemp45 to 45,
            btnTemp48 to 48
        )
        for ((btn, tempThresh) in tempButtons) {
            btn.setOnClickListener {
                ChargingSessionManager.tempThreshold = tempThresh
                ChargingSessionManager.saveSettings(this)
                updateAlarmUiState()
            }
        }

        btnTestAlarm.setOnClickListener {
            ChargingSessionManager.triggerAlarm(this)
        }
    }

    private fun updateAlarmUiState() {
        tvBatteryAlarmLabel.text = if (ChargingSessionManager.batteryAlarmEnabled) {
            "Berbunyi & bergetar apabila bateri cecah ${ChargingSessionManager.batteryThreshold}%"
        } else {
            "Penggera had bateri dimatikan"
        }

        tvTempAlarmLabel.text = if (ChargingSessionManager.tempAlarmEnabled) {
            "Amaran kecemasan jika suhu cecah ${ChargingSessionManager.tempThreshold}.0 °C"
        } else {
            "Penggera suhu panas dimatikan"
        }

        // Highlight selected threshold button
        val activeBg = 0xFF00E676.toInt()
        val inactiveBg = 0xFF263238.toInt()
        val activeText = 0xFF121318.toInt()
        val inactiveText = 0xFFFFFFFF.toInt()

        val threshList = listOf(btnThresh80 to 80, btnThresh85 to 85, btnThresh90 to 90, btnThresh100 to 100)
        for ((b, t) in threshList) {
            if (ChargingSessionManager.batteryThreshold == t) {
                b.backgroundTintList = android.content.res.ColorStateList.valueOf(activeBg)
                b.setTextColor(activeText)
            } else {
                b.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveBg)
                b.setTextColor(inactiveText)
            }
        }

        val activeTempBg = 0xFFFF9100.toInt()
        val tempList = listOf(btnTemp42 to 42, btnTemp45 to 45, btnTemp48 to 48)
        for ((b, t) in tempList) {
            if (ChargingSessionManager.tempThreshold == t) {
                b.backgroundTintList = android.content.res.ColorStateList.valueOf(activeTempBg)
                b.setTextColor(activeText)
            } else {
                b.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveBg)
                b.setTextColor(inactiveText)
            }
        }
    }

    private fun setupSessionControls() {
        btnResetSession.setOnClickListener {
            ChargingSessionManager.resetSession()
            tvPeakWatt.text = "0.0 W"
            tvTotalMah.text = "0 mAh"
            chartView.invalidate()
            updateGraphStats()
        }

        btnClearGraph.setOnClickListener {
            ChargingSessionManager.history.clear()
            chartView.invalidate()
            updateGraphStats()
        }

        btnToggleService.setOnClickListener {
            val serviceIntent = Intent(this, ChargingService::class.java)
            if (ChargingService.isRunning) {
                stopService(serviceIntent)
                btnToggleService.text = "Jalankan Servis Background"
            } else {
                ContextCompat.startForegroundService(this, serviceIntent)
                btnToggleService.text = "Hentikan Servis Background"
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        btnToggleService.text = if (ChargingService.isRunning) "Hentikan Servis Background" else "Jalankan Servis Background"
        updateGraphStats()
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
        val currentMa = if (abs(currentNowMicro) > 10000) {
            abs(currentNowMicro) / 1000
        } else {
            abs(currentNowMicro)
        }

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

        // Tab 1 Updates
        tvStatus.text = if (isCharging) "SEDANG DICAS ⚡" else "TIDAK DICAS"
        tvStatus.setTextColor(if (isCharging) 0xFF00E676.toInt() else 0xFFFFAB00.toInt())
        tvWatt.text = String.format("%.1f W", watts)
        tvWatt.setTextColor(if (isCharging) 0xFF00E676.toInt() else 0xFFFFFFFF.toInt())
        tvCurrent.text = "Arus: $currentMa mA"
        tvVoltage.text = String.format("Voltan: %.2f V", voltageV)
        tvLevel.text = "Peratus Bateri: $pct%"
        tvTemp.text = String.format("Suhu Bateri: %.1f °C", tempC)
        tvPlug.text = "Sumber Cas: $plugType"

        tvPeakWatt.text = String.format("%.1f W", ChargingSessionManager.peakWatt)
        tvTotalMah.text = String.format("%.0f mAh", ChargingSessionManager.totalMah)

        // Tab 2 Graph Updates
        if (tabGraph.visibility == View.VISIBLE) {
            chartView.invalidate()
            updateGraphStats()
        }
    }

    private fun updateGraphStats() {
        val count = ChargingSessionManager.history.size
        tvGraphStatPoints.text = "Jumlah Titik Data: $count titik"
        tvGraphStatPeak.text = String.format("Watt Puncak Sesi: %.1f W", ChargingSessionManager.peakWatt)
    }
}
