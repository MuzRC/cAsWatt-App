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
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var tabDashboard: ScrollView
    private lateinit var tabStatsGraph: ScrollView
    private lateinit var tabHealth: ScrollView
    private lateinit var tabSettingsAlarm: ScrollView
    private lateinit var tabCredits: ScrollView
    private lateinit var bottomNav: BottomNavigationView

    // Tab 1: Utama
    private lateinit var tvStatus: TextView
    private lateinit var tvWatt: TextView
    private lateinit var tvCurrent: TextView
    private lateinit var tvVoltage: TextView
    private lateinit var tvThermalThrottle: TextView
    private lateinit var tvTrickleStatus: TextView
    private lateinit var tvCableQuality: TextView
    private lateinit var tvSmartEta80: TextView
    private lateinit var tvSmartEta100: TextView
    private lateinit var tvDischargeDuration: TextView
    private lateinit var tvDischargeSub: TextView
    private lateinit var tvPeakWatt: TextView
    private lateinit var tvTotalMah: TextView
    private lateinit var tvEnergyWh: TextView
    private lateinit var tvCostMyr: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvPlug: TextView
    private lateinit var btnResetSession: Button

    // Tab 2: Statistik & Sesi
    private lateinit var tvTodayChargeCount: TextView
    private lateinit var tvTodayTotalMah: TextView
    private lateinit var tvTodayPeakWatt: TextView
    private lateinit var btnExportCsv: Button
    private lateinit var llSessionHistoryContainer: LinearLayout
    private lateinit var btnToggleSessions: Button
    private var isSessionsExpanded: Boolean = false
    private lateinit var llDailyHistoryContainer: LinearLayout
    private lateinit var chartView: ChargingChartView
    private lateinit var btnClearGraph: Button

    // Tab 3: Kesihatan
    private lateinit var tvHealthCondition: TextView
    private lateinit var tvHealthCycles: TextView
    private lateinit var tvHealthCumulative: TextView
    private lateinit var tvHealthVoltageDetail: TextView
    private lateinit var tvHealthTempDetail: TextView

    // Tab 4: Penggera
    private lateinit var switchBatteryAlarm: SwitchCompat
    private lateinit var switchTempAlarm: SwitchCompat
    private lateinit var switchLowBatteryAlarm: SwitchCompat
    private lateinit var switchTrickleAlarm: SwitchCompat
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

        // Pastikan storan data sedia ada dimuat turun dengan selamat
        ChargingSessionManager.ensureInit(this)

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
        tabCredits = findViewById(R.id.view_tab_credits)
        bottomNav = findViewById(R.id.bottom_navigation)

        tvStatus = findViewById(R.id.tv_main_status)
        tvWatt = findViewById(R.id.tv_main_watt)
        tvCurrent = findViewById(R.id.tv_main_current)
        tvVoltage = findViewById(R.id.tv_main_voltage)
        tvThermalThrottle = findViewById(R.id.tv_thermal_throttle)
        tvTrickleStatus = findViewById(R.id.tv_trickle_status)
        tvCableQuality = findViewById(R.id.tv_cable_quality)
        tvSmartEta80 = findViewById(R.id.tv_smart_eta_80)
        tvSmartEta100 = findViewById(R.id.tv_smart_eta_100)
        tvDischargeDuration = findViewById(R.id.tv_discharge_duration)
        tvDischargeSub = findViewById(R.id.tv_discharge_sub)
        tvPeakWatt = findViewById(R.id.tv_peak_watt)
        tvTotalMah = findViewById(R.id.tv_total_mah)
        tvEnergyWh = findViewById(R.id.tv_energy_wh)
        tvCostMyr = findViewById(R.id.tv_cost_myr)
        tvLevel = findViewById(R.id.tv_main_level)
        tvTemp = findViewById(R.id.tv_main_temp)
        tvPlug = findViewById(R.id.tv_main_plug)
        btnResetSession = findViewById(R.id.btn_reset_session)

        tvTodayChargeCount = findViewById(R.id.tv_today_charge_count)
        tvTodayTotalMah = findViewById(R.id.tv_today_total_mah)
        tvTodayPeakWatt = findViewById(R.id.tv_today_peak_watt)
        btnExportCsv = findViewById(R.id.btn_export_csv)
        llSessionHistoryContainer = findViewById(R.id.ll_session_history_container)
        btnToggleSessions = findViewById(R.id.btn_toggle_sessions)
        btnToggleSessions.setOnClickListener {
            isSessionsExpanded = !isSessionsExpanded
            renderSessionHistory()
        }
        llDailyHistoryContainer = findViewById(R.id.ll_daily_history_container)
        chartView = findViewById(R.id.charging_chart_view)
        btnClearGraph = findViewById(R.id.btn_clear_graph)

        tvHealthCondition = findViewById(R.id.tv_health_condition)
        // Membolehkan pengguna menyentuh status kesihatan untuk memasukkan kapasiti asal kilang
        tvHealthCondition.setOnClickListener {
            showEditCapacityDialog()
        }

        tvHealthCycles = findViewById(R.id.tv_health_cycles)
        tvHealthCumulative = findViewById(R.id.tv_health_cumulative)
        tvHealthVoltageDetail = findViewById(R.id.tv_health_voltage_detail)
        tvHealthTempDetail = findViewById(R.id.tv_health_temp_detail)

        switchBatteryAlarm = findViewById(R.id.switch_battery_alarm)
        switchTempAlarm = findViewById(R.id.switch_temp_alarm)
        switchLowBatteryAlarm = findViewById(R.id.switch_low_battery_alarm)
        switchTrickleAlarm = findViewById(R.id.switch_trickle_alarm)
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
            tabCredits.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_dashboard -> {
                    tabDashboard.visibility = View.VISIBLE
                    true
                }
                R.id.nav_stats_graph -> {
                    tabStatsGraph.visibility = View.VISIBLE
                    chartView.invalidate()
                    renderSessionHistory()
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
                R.id.nav_credits -> {
                    tabCredits.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }
    }

    private fun renderSessionHistory() {
        llSessionHistoryContainer.removeAllViews()

        // Lindungi pembacaan senarai daripada perubahan serentak thread lain
        val allSessions = synchronized(ChargingSessionManager) {
            ChargingSessionManager.sessionRecords.toList()
        }

        val totalRecords = allSessions.size
        if (totalRecords == 0) {
            btnToggleSessions.visibility = View.GONE
            val tvEmpty = TextView(this).apply {
                text = "Belum ada rekod sesi cas. Cucuk dan cabut pengecas untuk mula merekod!"
                setTextColor(Color.parseColor("#90A4AE"))
                textSize = 13f
            }
            llSessionHistoryContainer.addView(tvEmpty)
            return
        }

        val displayLimit = if (isSessionsExpanded) 30 else 4
        val displayList = allSessions.take(displayLimit)

        if (totalRecords > 4) {
            btnToggleSessions.visibility = View.VISIBLE
            btnToggleSessions.text = if (isSessionsExpanded) {
                "Sembunyikan Sesi Lama ▲"
            } else {
                "Tampilkan Lebih Banyak (${totalRecords - 4} sesi lagi) ▼"
            }
        } else {
            btnToggleSessions.visibility = View.GONE
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

        for ((index, session) in displayList.withIndex()) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 6, 0, 6)
            }

            val startStr = "${dateFormat.format(Date(session.startTime))} ${timeFormat.format(Date(session.startTime))}"
            val endStr = timeFormat.format(Date(session.endTime))
            val durStr = ChargingSessionManager.formatDuration(session.endTime - session.startTime)
            val gained = session.endLevel - session.startLevel
            val gainStr = if (gained >= 0) "+$gained%" else "$gained%"

            val tvHeader = TextView(this).apply {
                text = "⚡ $startStr - $endStr ($durStr) • ${session.startLevel}% ➔ ${session.endLevel}% ($gainStr)"
                setTextColor(Color.parseColor("#00E676"))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val tvDetails = TextView(this).apply {
                val cableShort = if (session.cableQuality.contains("Fast", ignoreCase = true)) {
                    "⚡ Cepat"
                } else if (session.cableQuality.contains("Lemah", ignoreCase = true)) {
                    "⚠️ Rintangan Tinggi"
                } else {
                    "Standar"
                }
                text = "${String.format(Locale.US, "%.1fW", session.peakWatt)} • ${String.format(Locale.US, "%.0f mAh", session.totalMah)} • ${String.format(Locale.US, "%.2f Wh", session.energyWh)} | $cableShort"
                setTextColor(Color.parseColor("#B0BEC5"))
                textSize = 12f
                setPadding(0, 2, 0, 0)
            }

            row.addView(tvHeader)
            row.addView(tvDetails)

            if (index < displayList.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                        setMargins(0, 8, 0, 8)
                    }
                    setBackgroundColor(Color.parseColor("#21262D"))
                }
                llSessionHistoryContainer.addView(row)
                llSessionHistoryContainer.addView(divider)
            } else {
                llSessionHistoryContainer.addView(row)
            }
        }
    }

    private fun renderDailyHistory() {
        llDailyHistoryContainer.removeAllViews()

        val allDaily = synchronized(ChargingSessionManager) {
            ChargingSessionManager.dailyRecords.toList()
        }

        if (allDaily.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "Belum ada rekod cas harian. Cucuk pengecas untuk mula merekod!"
                setTextColor(Color.parseColor("#90A4AE"))
                textSize = 13f
            }
            llDailyHistoryContainer.addView(tvEmpty)
            return
        }

        for (record in allDaily) {
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
                text = "• Dicas: ${record.count} kali | Puncak: ${String.format(Locale.US, "%.2f W", record.peakWatt)} | Tenaga: ${String.format(Locale.US, "%.0f mAh", record.totalMah)}"
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
        switchLowBatteryAlarm.isChecked = ChargingSessionManager.lowBatteryAlarmEnabled
        switchTrickleAlarm.isChecked = ChargingSessionManager.trickleAlarmEnabled

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

        switchLowBatteryAlarm.setOnCheckedChangeListener { _, isChecked ->
            ChargingSessionManager.lowBatteryAlarmEnabled = isChecked
            ChargingSessionManager.saveSettings(this)
        }

        switchTrickleAlarm.setOnCheckedChangeListener { _, isChecked ->
            ChargingSessionManager.trickleAlarmEnabled = isChecked
            ChargingSessionManager.saveSettings(this)
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
            // 1. Bunyikan nada dering dan aktifkan getaran perkakasan
            ChargingSessionManager.triggerAlarm(this)

            // 2. Jalankan ujian suara pintar TTS
            BatteryVoiceAlert.testVoice(this)
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
        btnExportCsv.setOnClickListener {
            exportRecordsToCsv()
        }

        // Pembaikan: Set semula meter sesi tanpa menambah kiraan bilangan cas harian
        btnResetSession.setOnClickListener {
            val intent = try {
                registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            } catch (_: Exception) {
                null
            }
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) ?: 100

            synchronized(ChargingSessionManager) {
                ChargingSessionManager.peakWatt = 0.0
                ChargingSessionManager.totalMah = 0.0
                ChargingSessionManager.energyWh = 0.0
                ChargingSessionManager.costMyr = 0.0
                ChargingSessionManager.sessionStartTime = System.currentTimeMillis()
                ChargingSessionManager.sessionStartLevel = level
                ChargingSessionManager.history.clear()
            }

            tvPeakWatt.text = "0.00 W"
            tvTotalMah.text = "0 mAh"
            tvEnergyWh.text = "0.00 Wh"
            tvCostMyr.text = "RM 0.00"
            chartView.invalidate()
            Toast.makeText(this, "Meter sesi berjaya diset semula!", Toast.LENGTH_SHORT).show()
        }

        // Pembaikan: Pengosongan graf yang selamat daripada nahas multithreading
        btnClearGraph.setOnClickListener {
            synchronized(ChargingSessionManager) {
                ChargingSessionManager.history.clear()
            }
            chartView.invalidate()
            Toast.makeText(this, "Graf keluk telah dikosongkan.", Toast.LENGTH_SHORT).show()
        }

        btnToggleService.setOnClickListener {
            val serviceIntent = Intent(this, ChargingService::class.java)
            if (ChargingService.isRunning) {
                stopService(serviceIntent)
                btnToggleService.text = "Jalankan Servis Background"
            } else {
                try {
                    ContextCompat.startForegroundService(this, serviceIntent)
                    btnToggleService.text = "Hentikan Servis Background"
                } catch (e: Exception) {
                    Log.e("MainActivity", "Gagal memulakan servis: ${e.message}")
                    Toast.makeText(this, "Gagal memulakan servis: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
        // Pembaikan Android 14+ (API 34): Daftarkan receiver dengan bendera NOT_EXPORTED secara selamat
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        btnToggleService.text = if (ChargingService.isRunning) "Hentikan Servis Background" else "Jalankan Servis Background"
        renderSessionHistory()
        renderDailyHistory()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
    }

    private fun updateBatteryInfo(intent: Intent) {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val voltageV = voltageMv / 1000.0

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val currentNowMicro = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        
        // Pembaikan had pemisah (50,000) supaya arus pengecasan pantas 10A-12A tidak tersalah bahagi
        val currentMa = if (abs(currentNowMicro) > 50000) {
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

        tvStatus.text = if (isCharging) "SEDANG DICAS ⚡" else "TIDAK DICAS (NYAHCAS)"
        tvStatus.setTextColor(if (isCharging) 0xFF00E676.toInt() else 0xFFFFAB00.toInt())
        tvWatt.text = String.format(Locale.US, "%.2f W", watts)
        tvWatt.setTextColor(if (isCharging) 0xFF00E676.toInt() else 0xFFFFFFFF.toInt())
        tvCurrent.text = "Arus: $currentMa mA"
        tvVoltage.text = String.format(Locale.US, "Voltan: %.2f V", voltageV)
        tvLevel.text = "Peratus Bateri: $pct%"
        tvTemp.text = String.format(Locale.US, "Suhu Bateri: %.1f °C", tempC)
        tvPlug.text = "Sumber Cas: $plugType"

        // Live Badges
        tvThermalThrottle.visibility = if (isCharging && ChargingSessionManager.isThermalThrottling) View.VISIBLE else View.GONE
        tvTrickleStatus.visibility = if (isCharging && ChargingSessionManager.isTrickleCharging) View.VISIBLE else View.GONE

        // Kualiti Kabel & Smart ETA
        if (isCharging) {
            tvCableQuality.text = "Kualiti Kabel: ${ChargingSessionManager.cableQuality}"
            val eta80 = ChargingSessionManager.etaTo80Min
            tvSmartEta80.text = when {
                eta80 > 0 -> "⏱️ Anggaran ke 80%: ~$eta80 minit lagi"
                eta80 == 0 -> "⏱️ Anggaran ke 80%: Tercapai (Zon Selamat)"
                else -> "⏱️ Anggaran ke 80%: Mengira keluk arus..."
            }

            val eta100 = ChargingSessionManager.etaTo100Min
            tvSmartEta100.text = when {
                eta100 > 0 -> "⏱️ Anggaran ke 100%: ~$eta100 minit lagi"
                eta100 == 0 -> "⏱️ Anggaran ke 100%: Bateri Tepu 100%"
                else -> "⏱️ Anggaran ke 100%: Mengira keluk arus..."
            }
        } else {
            tvCableQuality.text = "Kualiti Kabel: Pengecas Tidak Bersambung"
            tvSmartEta80.text = "⏱️ Anggaran ke 80%: Pengecas tidak dicucuk"
            tvSmartEta100.text = "⏱️ Anggaran ke 100%: Menggunakan kuasa bateri"
        }

        // Metrik Kuasa & Tenaga Sesi
        tvPeakWatt.text = String.format(Locale.US, "%.2f W", ChargingSessionManager.peakWatt)
        tvTotalMah.text = String.format(Locale.US, "%.0f mAh", ChargingSessionManager.totalMah)
        tvEnergyWh.text = String.format(Locale.US, "%.2f Wh", ChargingSessionManager.energyWh)
        tvCostMyr.text = String.format(Locale.US, "RM %.4f", ChargingSessionManager.costMyr)

        if (isCharging) {
            tvDischargeDuration.text = "Sedang Dicas ⚡"
            tvDischargeDuration.setTextColor(0xFF00E676.toInt())
            tvDischargeSub.text = "Auto-reset watt, mAh & Wh aktif."
        } else {
            val elapsed = System.currentTimeMillis() - ChargingSessionManager.lastUnplugTimestamp
            tvDischargeDuration.text = ChargingSessionManager.formatDuration(elapsed)
            tvDischargeDuration.setTextColor(0xFF29B6F6.toInt())
            val dropPct = (ChargingSessionManager.lastUnplugLevel - pct).coerceAtLeast(0)
            tvDischargeSub.text = "Bateri: $pct% (Turun ${dropPct}% dari ${ChargingSessionManager.lastUnplugLevel}%)"
        }

        val todayRecord = synchronized(ChargingSessionManager) {
            ChargingSessionManager.dailyRecords.firstOrNull()
        }
        tvTodayChargeCount.text = "Berapa Kali Dicas Hari Ini: ${todayRecord?.count ?: 0} kali"
        tvTodayTotalMah.text = String.format(Locale.US, "Jumlah Tenaga Masuk Hari Ini: %.0f mAh", todayRecord?.totalMah ?: 0.0)
        tvTodayPeakWatt.text = String.format(Locale.US, "Watt Puncak Hari Ini: %.2f W", todayRecord?.peakWatt ?: 0.0)

        if (tabStatsGraph.visibility == View.VISIBLE) {
            chartView.invalidate()
        }

        // Paparkan status kesihatan sebenar dengan petunjuk boleh disentuh
        tvHealthCondition.text = "Kesihatan: " + BatteryHealthManager.getFormattedHealthSummary(this) + " (Sentuh untuk ubah)"
        tvHealthCycles.text = String.format(Locale.US, "Anggaran Kitaran: %.1f kitaran", ChargingSessionManager.getEstimatedCycles())
        tvHealthCumulative.text = String.format(Locale.US, "Jumlah Kumulatif Cas: %.0f%% cas", ChargingSessionManager.cumulativePctCharged)
        tvHealthVoltageDetail.text = String.format(Locale.US, "Voltan Sel Semasa: %.2f V (Normal: 3.7V - 4.4V)", voltageV)
        val tempStatus = if (tempC < 40.0) "Zon Selamat (Selesa)" else if (tempC < 45.0) "Zon Suam" else "Zon Panas Bahaya!"
        tvHealthTempDetail.text = String.format(Locale.US, "Suhu Sel: %.1f °C (%s)", tempC, tempStatus)
    }

    private fun showEditCapacityDialog() {
        val currentCapacity = BatteryHealthManager.getDesignCapacity(this)

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Contoh: 5000"
            setText(currentCapacity.toString())
            setSelection(text.length)
            setPadding(60, 40, 60, 20)
        }

        AlertDialog.Builder(this)
            .setTitle("Tetapkan Kapasiti Asal Kilang")
            .setMessage("Masukkan spesifikasi kapasiti bateri asal telefon (Design Capacity dalam unit mAh):")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val enteredValue = input.text.toString().toIntOrNull()
                if (enteredValue != null && enteredValue in 1000..15000) {
                    BatteryHealthManager.setDesignCapacity(this, enteredValue)
                    tvHealthCondition.text = "Kesihatan: " + BatteryHealthManager.getFormattedHealthSummary(this) + " (Sentuh untuk ubah)"
                    Toast.makeText(this, "Kapasiti berjaya disimpan: ${enteredValue} mAh", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Sila masukkan nilai mAh yang sah (1000 - 15000 mAh)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun exportRecordsToCsv() {
        if (ChargingSessionManager.dailyRecords.isEmpty() && ChargingSessionManager.sessionRecords.isEmpty()) {
            Toast.makeText(this, "Tiada rekod cas untuk dieksport lagi!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val csvData = ChargingSessionManager.generateCsvData()
            val file = File(cacheDir, "CasWatt_Pro_Log_Lengkap.csv")
            file.writeText(csvData)

            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "CasWatt Pro v5.0 - Rekod Sejarah Pengecasan Lengkap")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Simpan atau Kongsi Rekod CSV:"))
        } catch (e: Exception) {
            Toast.makeText(this, "Ralat mengeksport: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
