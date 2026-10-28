package com.caswatt.app.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.caswatt.app.ChargingSessionManager
import com.caswatt.app.R
import java.util.Locale
import kotlin.math.abs

class DashboardFragment : Fragment() {

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
        tvStatus = view.findViewById(R.id.tv_main_status)
        tvWatt = view.findViewById(R.id.tv_main_watt)
        tvCurrent = view.findViewById(R.id.tv_main_current)
        tvVoltage = view.findViewById(R.id.tv_main_voltage)
        tvThermalThrottle = view.findViewById(R.id.tv_thermal_throttle)
        tvTrickleStatus = view.findViewById(R.id.tv_trickle_status)
        tvCableQuality = view.findViewById(R.id.tv_cable_quality)
        tvSmartEta80 = view.findViewById(R.id.tv_smart_eta_80)
        tvSmartEta100 = view.findViewById(R.id.tv_smart_eta_100)
        tvDischargeDuration = view.findViewById(R.id.tv_discharge_duration)
        tvDischargeSub = view.findViewById(R.id.tv_discharge_sub)
        tvPeakWatt = view.findViewById(R.id.tv_peak_watt)
        tvTotalMah = view.findViewById(R.id.tv_total_mah)
        tvEnergyWh = view.findViewById(R.id.tv_energy_wh)
        tvCostMyr = view.findViewById(R.id.tv_cost_myr)
        tvLevel = view.findViewById(R.id.tv_main_level)
        tvTemp = view.findViewById(R.id.tv_main_temp)
        tvPlug = view.findViewById(R.id.tv_main_plug)
        btnResetSession = view.findViewById(R.id.btn_reset_session)
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
            tvTotalMah.text = "0 mAh"
            tvEnergyWh.text = "0.00 Wh"
            tvCostMyr.text = "RM 0.00"
            Toast.makeText(ctx, "Meter sesi berjaya diset semula!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val ctx = context ?: return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(
            ctx,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        try {
            context?.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
    }

    private fun updateBatteryInfo(intent: Intent) {
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

        ChargingSessionManager.checkStateTransition(ctx, isCharging, pct)

        val currentNowMicro = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
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
    }
}
