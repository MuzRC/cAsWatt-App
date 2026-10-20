package com.caswatt.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
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
    private lateinit var tabStatsGraph: ScrollView
    private lateinit var tabHealth: ScrollView
    private lateinit var tabSettingsAlarm: ScrollView
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
    private lateinit var tvDischargeDuration: TextView
    private lateinit var tvDischargeSub: TextView
    private lateinit var btnResetSession: Button

    // Tab 2 Elements
    private lateinit var tvTodayChargeCount: TextView
    private lateinit var tvTodayTotalMah: TextView
    private lateinit var tvTodayPeakWatt: TextView
    private lateinit var llDailyHistoryContainer: LinearLayout
    private lateinit var chartView: ChargingChartView
    private lateinit var btnClearGraph: Button

    // Tab 3 Elements
    private lateinit var tvHealthCondition: TextView
    private lateinit var tvHealthCycles: TextView
    private lateinit var tvHealthCumulative: TextView
    private lateinit var tvHealthVoltageDetail: TextView
    private lateinit var tvHealthTempDetail: TextView

    // Tab 4 Elements
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
        tabStatsGraph = findViewById(R.id.view_tab_stats_graph)
        tabHealth = findViewById(R.id.view_tab_health)
        tabSettingsAlarm = findViewById(R.id.view_tab_settings_alarm)
        bottomNav = findViewById(R.id.bottom_navigation)

        // Tab 1
        tvStatus = findViewById(R.id.tv_main_status)
        tvWatt = findViewById(R.id.tv_main_watt)
        tvCurrent = findViewById(R.id.tv_main_current)
        tvVoltage = findViewById(R.id.tv_main_voltage)
        tvLevel = findViewById(R.id.tv_main_level)
        tvTemp = findViewById(R.id.tv_main_temp)
        tvPlug = findViewById(R.id.tv_main_plug)
        tvPeakWatt = findViewById(R.id.tv_peak_watt)
        tvTotalMah = findViewById(R.id.tv_total_mah)
        tvDischargeDuration = findViewById(R.id.tv_discharge_duration)
        tvDischargeSub = findViewById(R.id.tv_discharge_sub)
        btnResetSession = findViewById(R.id.btn_reset_session)

        // Tab 2
        tvTodayChargeCount = findViewById(R.id.tv_today_charge_count)
        tvTodayTotalMah = findViewById(R.id.tv_today_total_mah)
        tvTodayPeakWatt = findViewById(R.id.tv_today_peak_watt)
        llDailyHistoryContainer = findViewById(R.id.ll_daily_history_container)
        chartView = findViewById(R.id.charging_chart_view)
        btnClearGraph = findViewById(R.id.btn_clear_graph)

        // Tab 3
        tvHealthCondition = findViewById(R.id.tv_health_condition)
        tvHealthCycles = findViewById(R.id.tv_health_cycles)
        tvHealthCumulative = findViewById(R.id.tv_health_cumulative)
        tvHealthVoltageDetail = findViewById(R.id.tv_health_voltage_detail)
        tvHealthTempDetail = findViewById(R.id.tv_health_temp_detail)

        // Tab 4
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
            tabStatsGraph.visibility = View.GONE
            tabHealth.visibility = View.GONE
            tabSettingsAlarm.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_dashboard -> {
                    tabDashboard.visibility = View.VISIBLE
                    true
                }
                R.id.nav_stats_graph -> {
                    tabStatsGraph.visibility = View.VISIBLE
                    chartView.invalidate()
                    renderDailyHistory()
                    true
                }
                R.id.nav_health -> {
                    tabHealth.visibility = View.VISIBLE
                    true
                }
                R.id.nav_settings_alarm -> {
                    tabSettingsAlarm.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }
    }

    private fun renderDailyHistory() {
        llDailyHistoryContainer.removeAllViews()

        if (ChargingSessionManager.dailyRecords.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "Belum ada rekod cas lama. Cucuk pengecas untuk mula merekod!"
                setTextColor(Color.parseColor("#90A4AE"))
                textSize = 13f
            }
            llDailyHistoryContainer.addView(tvEmpty)
            return
        }

        for (record in ChargingSessionManager.dailyRecords) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8, 0, 8)
            }

            val tvDate = TextView(this).apply {
                text = "📅 ${record.date}"
                setTextColor(Color.parseColor("#00E676"))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val tvDetails = TextView(this).apply {
                text = "• Dicas: ${record.count} kali | Puncak: ${String.format("%.2f W", record.peakWatt)} | Tenaga: ${String.format("%.0f mAh", record.totalMah)}"
                setTextColor(Color.parseColor("#CFD8DC"))
                textSize = 12f
            }

            row.addView(tvDate)
            row.addView(tvDetails)

            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                    setMargins(0, 8, 0, 8)
                }
                setBackgroundColor(Color.parseColor("#263238"))
            }

            llDailyHistoryContainer.addView(row)
            llDailyHistoryContainer.addView(divider)
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
            val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) ?: 100
            ChargingSessionManager.onChargingStarted(this, level)
            tvPeakWatt.text = "0.00 W"
            tvTotalMah.text = "0 mAh"
            chartView.invalidate()
        }

        btnClearGraph.setOnClickListener {
            ChargingSessionManager.history.clear()
            chartView.invalidate()
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
        renderDailyHistory()
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

        // Formula Kuasa Watt = (V * mA) / 1,000,000
        val watts = (voltageMv.toDouble() * currentMa) / 1000000.0

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempC = tempTenths / 10.0

        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Pengecas Dinding (AC)"
            BatteryManager.BATTERY_PLUGGED_USB -> "Port USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Cas Tanpa Wayar (Wireless)"
            else -> "Menggunakan Bateri"
        }

        // ================= TAB 1: DASHBOARD =================
        tvStatus.text = if (isCharging) "SEDANG DICAS ⚡" else "TIDAK DICAS (NYAHCAS)"
        tvStatus.setTextColor(if (isCharging) 0xFF00E676.toInt() else 0xFFFFAB00.toInt())
        
        // 1. TUKAR NILAI WATT KEPADA 2 TEMPAT PERPULUHAN (cth: 18.45 W)
        tvWatt.text = String.format("%.2f W", watts)
        tvWatt.setTextColor(if (isCharging) 0xFF00E676.toInt() else 0xFFFFFFFF.toInt())
        tvCurrent.text = "Arus: $currentMa mA"
        tvVoltage.text = String.format("Voltan: %.2f V", voltageV)
        tvLevel.text = "Peratus Bateri: $pct%"
        tvTemp.text = String.format("Suhu Bateri: %.1f °C", tempC)
        tvPlug.text = "Sumber Cas: $plugType"

        tvPeakWatt.text = String.format("%.2f W", ChargingSessionManager.peakWatt)
        tvTotalMah.text = String.format("%.0f mAh", ChargingSessionManager.totalMah)

        // 4. TEMPOH NYAHCAS (DISCHARGE DURATION)
        if (isCharging) {
            tvDischargeDuration.text = "Sedang Dicas ⚡"
            tvDischargeDuration.setTextColor(0xFF00E676.toInt())
            tvDischargeSub.text = "Auto-reset watt puncak & mAh aktif."
        } else {
            val elapsed = System.currentTimeMillis() - ChargingSessionManager.lastUnplugTimestamp
            tvDischargeDuration.text = ChargingSessionManager.formatDuration(elapsed)
            tvDischargeDuration.setTextColor(0xFF29B6F6.toInt())
            val dropPct = (ChargingSessionManager.lastUnplugLevel - pct).coerceAtLeast(0)
            tvDischargeSub.text = "Bateri: $pct% (Turun ${dropPct}% dari ${ChargingSessionManager.lastUnplugLevel}%)"
        }

        // ================= TAB 2: STATISTIK HARIAN =================
        val todayRecord = ChargingSessionManager.dailyRecords.firstOrNull()
        tvTodayChargeCount.text = "Berapa Kali Dicas Hari Ini: ${todayRecord?.count ?: 0} kali"
        tvTodayTotalMah.text = String.format("Jumlah Tenaga Masuk Hari Ini: %.0f mAh", todayRecord?.totalMah ?: 0.0)
        tvTodayPeakWatt.text = String.format("Watt Puncak Hari Ini: %.2f W", todayRecord?.peakWatt ?: 0.0)

        if (tabStatsGraph.visibility == View.VISIBLE) {
            chartView.invalidate()
        }

        // ================= TAB 3: KESIHATAN BATERI =================
        tvHealthCondition.text = ChargingSessionManager.healthCondition
        tvHealthCycles.text = String.format("Anggaran Kitaran: %.1f kitaran", ChargingSessionManager.getEstimatedCycles())
        tvHealthCumulative.text = String.format("Jumlah Kumulatif Cas: %.0f%% cas", ChargingSessionManager.cumulativePctCharged)
        tvHealthVoltageDetail.text = String.format("Voltan Sel Semasa: %.2f V (Normal: 3.7V - 4.4V)", voltageV)
        val tempStatus = if (tempC < 40.0) "Zon Selamat (Selesa)" else if (tempC < 45.0) "Zon Suam" else "Zon Panas Bahaya!"
        tvHealthTempDetail.text = String.format("Suhu Sel: %.1f °C (%s)", tempC, tempStatus)
    }
}
