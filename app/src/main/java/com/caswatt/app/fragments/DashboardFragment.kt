package com.caswatt.app.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.caswatt.app.ChargingSessionManager
import com.caswatt.app.R
import java.util.Locale
import kotlin.math.abs

class DashboardFragment : Fragment() {

    // Status Utama
    private lateinit var tvStatus: TextView
    private lateinit var tvWatt: TextView
    private lateinit var tvCurrent: TextView
    private lateinit var tvVoltage: TextView
    private lateinit var tvThermalThrottle: TextView
    private lateinit var tvTrickleStatus: TextView

    // Nisbah Aliran Kuasa (Ratio Bar)
    private lateinit var layoutPowerRatio: LinearLayout
    private lateinit var tvInputVal: TextView
    private lateinit var tvRatioDivider: TextView
    private lateinit var tvDrainVal: TextView
    private lateinit var tvEfficiencyBadge: TextView

    // Kualiti Kabel & Smart ETA
    private lateinit var tvCableQuality: TextView
    private lateinit var tvSmartEta80: TextView
    private lateinit var tvSmartEta100: TextView

    // Kad Statistik Sesi Semasa (Gabungan Tempoh & Tenaga)
    private lateinit var tvDischargeDuration: TextView
    private lateinit var tvDischargeSub: TextView
    private lateinit var tvPeakWatt: TextView
    private lateinit var tvTotalMah: TextView
    private lateinit var tvEnergyWh: TextView
    private lateinit var tvCostMyr: TextView
    private lateinit var btnResetSession: Button

    // Kad Maklumat Sistem Asas (Interaktif)
    private lateinit var layoutSystemInfo: LinearLayout
    private lateinit var tvLevel: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvPlug: TextView
    private lateinit var tvRuntime: TextView
    private lateinit var tvTech: TextView

    // Status Mod Paparan & Simpanan Intent
    private var isWattMode: Boolean = false
    private var lastKnownIntent: Intent? = null

    // Detik 1-Saat untuk Kemas Kini Masa Nyata
    private val tickerHandler = Handler(Looper.getMainLooper())
    private val tickerRunnable = object : Runnable {
        override fun run() {
            lastKnownIntent?.let { updateBatteryInfo(it) }
            tickerHandler.postDelayed(this, 1000L)
        }
    }

    companion object {
        // Ingatan Arus Nyahcas Terakhir (Beban telefon sebelum dicucuk - Pilihan A)
        var lastDischargeMa: Int = 500
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateBatteryInfo(intent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        initViews(view)
        setupListeners()
        return view
    }

    private fun initViews(view: View) {
        // Bind Status Utama
        tvStatus = view.findViewById(R.id.tv_main_status)
        tvWatt = view.findViewById(R.id.tv_main_watt)
        tvCurrent = view.findViewById(R.id.tv_main_current)
        tvVoltage = view.findViewById(R.id.tv_main_voltage)
        tvThermalThrottle = view.findViewById(R.id.tv_thermal_throttle)
        tvTrickleStatus = view.findViewById(R.id.tv_trickle_status)

        // Bind Baris Aliran Nisbah Kuasa
        layoutPowerRatio = view.findViewById(R.id.layout_power_ratio)
        tvInputVal = view.findViewById(R.id.tv_input_val)
        tvRatioDivider = view.findViewById(R.id.tv_ratio_divider)
        tvDrainVal = view.findViewById(R.id.tv_drain_val)
        tvEfficiencyBadge = view.findViewById(R.id.tv_efficiency_badge)

        // Bind Kualiti Kabel & Smart ETA
        tvCableQuality = view.findViewById(R.id.tv_cable_quality)
        tvSmartEta80 = view.findViewById(R.id.tv_smart_eta_80)
        tvSmartEta100 = view.findViewById(R.id.tv_smart_eta_100)

        // Bind Kad Statistik Sesi Semasa
        tvDischargeDuration = view.findViewById(R.id.tv_discharge_duration)
        tvDischargeSub = view.findViewById(R.id.tv_discharge_sub)
        tvPeakWatt = view.findViewById(R.id.tv_peak_watt)
        tvTotalMah = view.findViewById(R.id.tv_total_mah)
        tvEnergyWh = view.findViewById(R.id.tv_energy_wh)
        tvCostMyr = view.findViewById(R.id.tv_cost_myr)
        btnResetSession = view.findViewById(R.id.btn_reset_session)

        // Bind Kad Maklumat Sistem Asas
        layoutSystemInfo = view.findViewById(R.id.layout_system_info)
        tvLevel = view.findViewById(R.id.tv_main_level)
        tvTemp = view.findViewById(R.id.tv_main_temp)
        tvPlug = view.findViewById(R.id.tv_main_plug)
        tvRuntime = view.findViewById(R.id.tv_main_runtime)
        tvTech = view.findViewById(R.id.tv_main_tech)
    }

    private fun setupListeners() {
        btnResetSession.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val intent = try {
                ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
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
            tvTotalMah.text = "+0 mAh"
            tvEnergyWh.text = "0.00 Wh"
            tvCostMyr.text = "RM 0.00"
            Toast.makeText(ctx, "Sesi berjaya diset semula!", Toast.LENGTH_SHORT).show()
        }

        // Sentuh Baris Aliran untuk Bertukar Unit (mA <-> Watt)
        layoutPowerRatio.setOnClickListener {
            isWattMode = !isWattMode
            val toastMsg = if (isWattMode) "Paparan Kuasa (Watt)" else "Paparan Arus (mA)"
            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
            lastKnownIntent?.let { updateBatteryInfo(it) }
        }

        // Sentuh Kad Maklumat Sistem untuk Pintasan Tetapan Bateri Android
        layoutSystemInfo.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            try {
                val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                ctx.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                    ctx.startActivity(fallbackIntent)
                } catch (_: Exception) {
                    Toast.makeText(ctx, "Pintasan tetapan bateri tidak disokong", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val ctx = context ?: return

        val stickyIntent = try {
            ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
            null
        }
        if (stickyIntent != null) {
            updateBatteryInfo(stickyIntent)
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(
            ctx,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Mulakan detik 1-saat semasa pengguna melihat dashboard
        tickerHandler.removeCallbacks(tickerRunnable)
        tickerHandler.postDelayed(tickerRunnable, 1000L)
    }

    override fun onPause() {
        super.onPause()
        // Hentikan detik untuk jimat bateri apabila pengguna keluar dari dashboard
        tickerHandler.removeCallbacks(tickerRunnable)
        try {
            context?.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
    }

    private fun updateBatteryInfo(intent: Intent) {
        lastKnownIntent = intent
        val ctx = context ?: return
        val batteryManager = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val voltageV = voltageMv / 1000.0

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val currentNowMicro = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        val currentMa = if (abs(currentNowMicro) > 50000) {
            abs(currentNowMicro) / 1000
        } else {
            abs(currentNowMicro)
        }

        val watts = (voltageMv.toDouble() * currentMa) / 1000000.0

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempC = tempTenths / 10.0
        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

        // =========================================================================
        // HUBUNGAN UTAMA: HANTAR SAMPEL BATERI TERUS KE ChargingSessionManager!
        // =========================================================================
        ChargingSessionManager.recordSample(
            context = ctx,
            watt = watts,
            currentMa = currentMa,
            tempC = tempC,
            levelPct = pct,
            isCharging = isCharging,
            healthCode = healthCode
        )

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Pengecas Dinding (AC)"
            BatteryManager.BATTERY_PLUGGED_USB -> "Port USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Cas Tanpa Wayar (Wireless)"
            else -> "Menggunakan Bateri"
        }

        // Status Cas & Voltan Utama
        tvStatus.text = if (isCharging) "SEDANG DICAS ⚡" else "TIDAK DICAS (NYAHCAS)"
        tvStatus.setTextColor(if (isCharging) 0xFF00E676.toInt() else 0xFFFFAB00.toInt())
        tvWatt.text = String.format(Locale.US, "%.2f W", watts)
        tvWatt.setTextColor(if (isCharging) 0xFF00E676.toInt() else 0xFFFFFFFF.toInt())
        tvCurrent.text = "Arus Bersih: $currentMa mA"
        tvVoltage.text = String.format(Locale.US, "Voltan: %.2f V", voltageV)

        // =========================================================================
        // 1. LOGIK ALIRAN KUASA (PILIHAN A + CARA B + mA/WATT TOGGLE)
        // =========================================================================
        if (isCharging) {
            val phoneDrainMa = if (lastDischargeMa > 100) lastDischargeMa else 500
            val inputMa = currentMa + phoneDrainMa
            val isDeficit = currentMa <= 50 && inputMa <= phoneDrainMa

            val efficiencyPct = if (inputMa > 0) {
                ((currentMa.toDouble() / inputMa.toDouble()) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }

            if (isWattMode) {
                val inputWatt = (inputMa.toDouble() * voltageV) / 1000.0
                val drainWatt = (phoneDrainMa.toDouble() * voltageV) / 1000.0
                tvInputVal.text = String.format(Locale.US, "↓ %.1f W", inputWatt)
                tvDrainVal.text = String.format(Locale.US, "↑ %.1f W", drainWatt)
            } else {
                tvInputVal.text = "↓ ${inputMa}mA"
                tvDrainVal.text = "↑ ${phoneDrainMa}mA"
            }

            val inputColor = when {
                inputMa >= 2500 -> 0xFF00E676.toInt() // Hijau: Pengecasan pantas
                inputMa >= 1000 -> 0xFFFFD54F.toInt() // Kuning: Standard
                else -> 0xFFCFD8DC.toInt()            // Kelabu: Pengecasan perlahan
            }
            tvInputVal.setTextColor(inputColor)

            val drainColor = when {
                phoneDrainMa >= 800 -> 0xFFFF5252.toInt() // Merah: Penggunaan berat
                phoneDrainMa >= 500 -> 0xFFFFD54F.toInt() // Kuning: Sederhana
                else -> 0xFFCFD8DC.toInt()               // Kelabu: Santai
            }
            tvDrainVal.setTextColor(drainColor)

            if (isDeficit) {
                tvEfficiencyBadge.text = "DEFISIT ⚠️"
                tvEfficiencyBadge.setTextColor(0xFFFF5252.toInt())
            } else {
                tvEfficiencyBadge.text = "$efficiencyPct% Bersih"
                tvEfficiencyBadge.setTextColor(0xFF80D8FF.toInt())
            }
        } else {
            if (currentMa > 100) {
                lastDischargeMa = currentMa
            }

            if (isWattMode) {
                val drainWatt = (currentMa.toDouble() * voltageV) / 1000.0
                tvInputVal.text = "↓ 0.0 W"
                tvDrainVal.text = String.format(Locale.US, "↑ %.1f W", drainWatt)
            } else {
                tvInputVal.text = "↓ 0mA"
                tvDrainVal.text = "↑ ${currentMa}mA"
            }

            tvInputVal.setTextColor(0xFF90A4AE.toInt())
            tvDrainVal.setTextColor(0xFFFFAB00.toInt())
            tvEfficiencyBadge.text = "Nyahcas"
            tvEfficiencyBadge.setTextColor(0xFF90A4AE.toInt())
        }

        // Live Badges
        tvThermalThrottle.visibility = if (isCharging && ChargingSessionManager.isThermalThrottling) View.VISIBLE else View.GONE
        tvTrickleStatus.visibility = if (isCharging && ChargingSessionManager.isTrickleCharging) View.VISIBLE else View.GONE

        // =========================================================================
        // KUALITI KABEL & SMART ETA (DIBACA TERUS DARI MANAGER)
        // =========================================================================
        if (isCharging) {
            val quality = ChargingSessionManager.cableQuality
            tvCableQuality.text = quality

            val color = when {
                quality.contains("Lemah", ignoreCase = true) || quality.contains("Tinggi", ignoreCase = true) -> 0xFFFF5252.toInt()
                quality.contains("Baik", ignoreCase = true) || quality.contains("Pantas", ignoreCase = true) || quality.contains("Cemerlang", ignoreCase = true) -> 0xFF00E676.toInt()
                quality.contains("Mengukur", ignoreCase = true) -> 0xFF90A4AE.toInt()
                else -> 0xFFFFD54F.toInt()
            }
            tvCableQuality.setTextColor(color)

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
            tvCableQuality.text = "Pengecas Tidak Bersambung"
            tvCableQuality.setTextColor(0xFF90A4AE.toInt())
            tvSmartEta80.text = "⏱️ Anggaran ke 80%: Pengecas tidak dicucuk"
            tvSmartEta100.text = "⏱️ Anggaran ke 100%: Menggunakan kuasa bateri"
        }

        // =========================================================================
        // 2. KAD STATISTIK SESI SEMASA (TEMPOH MASA & TENAGA)
        // =========================================================================
        if (isCharging) {
            val elapsed = System.currentTimeMillis() - ChargingSessionManager.sessionStartTime
            tvDischargeDuration.text = "${ChargingSessionManager.formatDuration(elapsed)} ⚡"
            tvDischargeDuration.setTextColor(0xFF00E676.toInt())
            val startLevel = ChargingSessionManager.sessionStartLevel
            val gainPct = (pct - startLevel).coerceAtLeast(0)
            tvDischargeSub.text = "Bateri: ${startLevel}% ➔ ${pct}% (+${gainPct}%)"
            tvTotalMah.text = String.format(Locale.US, "+%.0f mAh", ChargingSessionManager.totalMah)
            tvTotalMah.setTextColor(0xFF00E676.toInt())
        } else {
            val elapsed = System.currentTimeMillis() - ChargingSessionManager.lastUnplugTimestamp
            tvDischargeDuration.text = ChargingSessionManager.formatDuration(elapsed)
            tvDischargeDuration.setTextColor(0xFF29B6F6.toInt())
            val startLevel = ChargingSessionManager.lastUnplugLevel
            val dropPct = (startLevel - pct).coerceAtLeast(0)
            tvDischargeSub.text = "Bateri: ${startLevel}% ➔ ${pct}% (-${dropPct}%)"
            tvTotalMah.text = String.format(Locale.US, "%.0f mAh", ChargingSessionManager.totalMah)
            tvTotalMah.setTextColor(0xFF80D8FF.toInt())
        }

        tvPeakWatt.text = String.format(Locale.US, "%.2f W", ChargingSessionManager.peakWatt)
        tvEnergyWh.text = String.format(Locale.US, "%.2f Wh", ChargingSessionManager.energyWh)

        // =========================================================================
        // 3. KAD MAKLUMAT SISTEM ASAS (PERKAKASAN & RUNTIME)
        // =========================================================================
        // Suhu Bateri
        tvTemp.text = String.format(Locale.US, "%.1f °C", tempC)
        val tempColor = when {
            tempC >= 40.0 -> 0xFFFF5252.toInt() // Merah: Panas
            tempC >= 35.0 -> 0xFFFFD54F.toInt() // Kuning: Suam
            else -> 0xFF00E676.toInt()          // Hijau: Sejuk
        }
        tvTemp.setTextColor(tempColor)

        // Sumber Cas
        tvPlug.text = plugType
        val plugColor = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> 0xFF00E676.toInt()
            BatteryManager.BATTERY_PLUGGED_USB,
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> 0xFFFFD54F.toInt()
            else -> 0xFF29B6F6.toInt()
        }
        tvPlug.setTextColor(plugColor)

        // Baki Hayat Nyahcas (Runtime ETA)
        if (isCharging) {
            tvRuntime.text = "Sedang Dicas ⚡"
            tvRuntime.setTextColor(0xFF00E676.toInt())
        } else {
            val drainRate = if (currentMa > 50) currentMa else lastDischargeMa
            if (drainRate > 50 && pct > 0) {
                val remainingCapacityMah = (pct / 100.0) * 5000.0
                val hoursRemaining = remainingCapacityMah / drainRate.toDouble()
                val hours = hoursRemaining.toInt()
                val minutes = ((hoursRemaining - hours) * 60).toInt()

                tvRuntime.text = String.format(Locale.US, "~%dj %02dm lagi", hours, minutes)
                val runtimeColor = when {
                    hoursRemaining >= 5.0 -> 0xFF00E676.toInt() // Hijau: Tahan lama
                    hoursRemaining >= 2.0 -> 0xFFFFD54F.toInt() // Kuning: Sederhana
                    else -> 0xFFFF5252.toInt()                  //