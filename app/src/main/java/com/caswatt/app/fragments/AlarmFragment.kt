package com.caswatt.app.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.caswatt.app.BatteryVoiceAlert
import com.caswatt.app.ChargingService
import com.caswatt.app.ChargingSessionManager
import com.caswatt.app.R

class AlarmFragment : Fragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alarm, container, false)
        initViews(view)
        setupAlarmControls()
        return view
    }

    private fun initViews(view: View) {
        switchBatteryAlarm = view.findViewById(R.id.switch_battery_alarm)
        switchTempAlarm = view.findViewById(R.id.switch_temp_alarm)
        switchLowBatteryAlarm = view.findViewById(R.id.switch_low_battery_alarm)
        switchTrickleAlarm = view.findViewById(R.id.switch_trickle_alarm)
        tvBatteryAlarmLabel = view.findViewById(R.id.tv_battery_alarm_label)
        tvTempAlarmLabel = view.findViewById(R.id.tv_temp_alarm_label)
        btnThresh80 = view.findViewById(R.id.btn_thresh_80)
        btnThresh85 = view.findViewById(R.id.btn_thresh_85)
        btnThresh90 = view.findViewById(R.id.btn_thresh_90)
        btnThresh100 = view.findViewById(R.id.btn_thresh_100)
        btnTemp42 = view.findViewById(R.id.btn_temp_42)
        btnTemp45 = view.findViewById(R.id.btn_temp_45)
        btnTemp48 = view.findViewById(R.id.btn_temp_48)
        btnTestAlarm = view.findViewById(R.id.btn_test_alarm)
        btnToggleService = view.findViewById(R.id.btn_toggle_service)
    }

    private fun setupAlarmControls() {
        val ctx = context ?: return

        switchBatteryAlarm.isChecked = ChargingSessionManager.batteryAlarmEnabled
        switchTempAlarm.isChecked = ChargingSessionManager.tempAlarmEnabled
        switchLowBatteryAlarm.isChecked = ChargingSessionManager.lowBatteryAlarmEnabled
        switchTrickleAlarm.isChecked = ChargingSessionManager.trickleAlarmEnabled

        updateAlarmUiState()

        switchBatteryAlarm.setOnCheckedChangeListener { _, isChecked ->
            val c = context ?: return@setOnCheckedChangeListener
            ChargingSessionManager.batteryAlarmEnabled = isChecked
            ChargingSessionManager.saveSettings(c)
            updateAlarmUiState()
        }

        switchTempAlarm.setOnCheckedChangeListener { _, isChecked ->
            val c = context ?: return@setOnCheckedChangeListener
            ChargingSessionManager.tempAlarmEnabled = isChecked
            ChargingSessionManager.saveSettings(c)
            updateAlarmUiState()
        }

        switchLowBatteryAlarm.setOnCheckedChangeListener { _, isChecked ->
            val c = context ?: return@setOnCheckedChangeListener
            ChargingSessionManager.lowBatteryAlarmEnabled = isChecked
            ChargingSessionManager.saveSettings(c)
        }

        switchTrickleAlarm.setOnCheckedChangeListener { _, isChecked ->
            val c = context ?: return@setOnCheckedChangeListener
            ChargingSessionManager.trickleAlarmEnabled = isChecked
            ChargingSessionManager.saveSettings(c)
        }

        val threshButtons = listOf(
            btnThresh80 to 80,
            btnThresh85 to 85,
            btnThresh90 to 90,
            btnThresh100 to 100
        )
        for ((btn, thresh) in threshButtons) {
            btn.setOnClickListener {
                val c = context ?: return@setOnClickListener
                ChargingSessionManager.batteryThreshold = thresh
                ChargingSessionManager.saveSettings(c)
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
                val c = context ?: return@setOnClickListener
                ChargingSessionManager.tempThreshold = tempThresh
                ChargingSessionManager.saveSettings(c)
                updateAlarmUiState()
            }
        }

        btnTestAlarm.setOnClickListener {
            val c = context ?: return@setOnClickListener
            // 1. Bunyikan nada dering dan aktifkan getaran perkakasan
            ChargingSessionManager.triggerAlarm(c)

            // 2. Jalankan ujian suara pintar TTS
            BatteryVoiceAlert.testVoice(c)
        }

        btnToggleService.setOnClickListener {
            val c = context ?: return@setOnClickListener
            val serviceIntent = Intent(c, ChargingService::class.java)
            if (ChargingService.isRunning) {
                c.stopService(serviceIntent)
                btnToggleService.text = "Jalankan Servis Background"
            } else {
                try {
                    ContextCompat.startForegroundService(c, serviceIntent)
                    btnToggleService.text = "Hentikan Servis Background"
                } catch (e: Exception) {
                    Log.e("AlarmFragment", "Gagal memulakan servis: ${e.message}")
                    Toast.makeText(c, "Gagal memulakan servis: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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
                b.backgroundTintList = ColorStateList.valueOf(activeBg)
                b.setTextColor(activeText)
            } else {
                b.backgroundTintList = ColorStateList.valueOf(inactiveBg)
                b.setTextColor(inactiveText)
            }
        }

        val activeTempBg = 0xFFFF9100.toInt()
        val tempList = listOf(btnTemp42 to 42, btnTemp45 to 45, btnTemp48 to 48)
        for ((b, t) in tempList) {
            if (ChargingSessionManager.tempThreshold == t) {
                b.backgroundTintList = ColorStateList.valueOf(activeTempBg)
                b.setTextColor(activeText)
            } else {
                b.backgroundTintList = ColorStateList.valueOf(inactiveBg)
                b.setTextColor(inactiveText)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateAlarmUiState()
        btnToggleService.text = if (ChargingService.isRunning) "Hentikan Servis Background" else "Jalankan Servis Background"
    }
}
