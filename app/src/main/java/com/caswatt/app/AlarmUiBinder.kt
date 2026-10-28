package com.caswatt.app

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import java.util.Locale

class AlarmUiBinder(
    private val rootView: View,
    private val context: Context,
    private val onPickRingtoneClicked: () -> Unit,
    private val onToggleServiceClicked: () -> Unit
) {
    // 1. Service Card
    val tvServiceStatus: TextView = rootView.findViewById(R.id.tv_service_status)
    val btnToggleService: Button = rootView.findViewById(R.id.btn_toggle_service)

    // 2. Battery Limit Card
    val switchBatteryAlarm: SwitchCompat = rootView.findViewById(R.id.switch_battery_alarm)
    val tvBatteryAlarmLabel: TextView = rootView.findViewById(R.id.tv_battery_alarm_label)
    val btnThresh80: Button = rootView.findViewById(R.id.btn_thresh_80)
    val btnThresh85: Button = rootView.findViewById(R.id.btn_thresh_85)
    val btnThresh90: Button = rootView.findViewById(R.id.btn_thresh_90)
    val btnThresh100: Button = rootView.findViewById(R.id.btn_thresh_100)

    // 3. Temperature Card
    val switchTempAlarm: SwitchCompat = rootView.findViewById(R.id.switch_temp_alarm)
    val tvTempAlarmLabel: TextView = rootView.findViewById(R.id.tv_temp_alarm_label)
    val btnTemp42: Button = rootView.findViewById(R.id.btn_temp_42)
    val btnTemp45: Button = rootView.findViewById(R.id.btn_temp_45)
    val btnTemp48: Button = rootView.findViewById(R.id.btn_temp_48)

    // 4. Status Bateri (Low & Saturation)
    val switchLowBattery: SwitchCompat = rootView.findViewById(R.id.switch_low_battery_alarm)
    val switchTrickle: SwitchCompat = rootView.findViewById(R.id.switch_trickle_alarm)

    // 5. Penderia Keselamatan Pintar
    val switchAntiTheft: SwitchCompat = rootView.findViewById(R.id.switch_anti_theft)
    val switchSlowCharge: SwitchCompat = rootView.findViewById(R.id.switch_slow_charge)
    val switchOverVoltage: SwitchCompat = rootView.findViewById(R.id.switch_over_voltage)
    val switchRapidDrain: SwitchCompat = rootView.findViewById(R.id.switch_rapid_drain)
    val btnCheckBatteryDrain: Button = rootView.findViewById(R.id.btn_check_battery_drain)

    // 6. Audio & Waktu Senyap
    val cbSound: CheckBox = rootView.findViewById(R.id.cb_alarm_sound)
    val cbVibrate: CheckBox = rootView.findViewById(R.id.cb_alarm_vibrate)
    val cbTts: CheckBox = rootView.findViewById(R.id.cb_alarm_tts)
    val tvCurrentRingtone: TextView = rootView.findViewById(R.id.tv_current_ringtone)
    val btnPickRingtone: Button = rootView.findViewById(R.id.btn_pick_ringtone)
    val switchQuietHours: SwitchCompat = rootView.findViewById(R.id.switch_quiet_hours)
    val switchLoopAlarm: SwitchCompat = rootView.findViewById(R.id.switch_loop_alarm)

    // 7. Butang Uji
    val btnTestAlarm: Button = rootView.findViewById(R.id.btn_test_alarm)

    fun bindAll() {
        bindSwitches()
        bindThresholdButtons()
        bindThermalButtons()
        bindAudioOptions()
        bindActions()
        updateRingtoneDisplay()
    }

    private fun bindSwitches() {
        switchBatteryAlarm.isChecked = AlarmPreferencesHelper.isHighLevelEnabled(context)
        switchBatteryAlarm.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setHighLevelEnabled(context, isChecked)
        }

        switchTempAlarm.isChecked = AlarmPreferencesHelper.isThermalEnabled(context)
        switchTempAlarm.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setThermalEnabled(context, isChecked)
        }

        switchLowBattery.isChecked = AlarmPreferencesHelper.isLowBatteryEnabled(context)
        switchLowBattery.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setLowBatteryEnabled(context, isChecked)
        }

        switchTrickle.isChecked = AlarmPreferencesHelper.isSaturationEnabled(context)
        switchTrickle.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setSaturationEnabled(context, isChecked)
        }

        switchAntiTheft.isChecked = AlarmPreferencesHelper.isAntiTheftEnabled(context)
        switchAntiTheft.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setAntiTheftEnabled(context, isChecked)
            AlarmMonitorHelper.isAntiTheftArmed = isChecked
        }

        switchSlowCharge.isChecked = AlarmPreferencesHelper.isSlowChargeEnabled(context)
        switchSlowCharge.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setSlowChargeEnabled(context, isChecked)
        }

        switchOverVoltage.isChecked = AlarmPreferencesHelper.isOverVoltageEnabled(context)
        switchOverVoltage.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setOverVoltageEnabled(context, isChecked)
        }

        switchRapidDrain.isChecked = AlarmPreferencesHelper.isRapidDrainEnabled(context)
        switchRapidDrain.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setRapidDrainEnabled(context, isChecked)
        }

        switchQuietHours.isChecked = AlarmPreferencesHelper.isQuietHoursEnabled(context)
        switchQuietHours.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setQuietHoursEnabled(context, isChecked)
        }

        switchLoopAlarm.isChecked = AlarmPreferencesHelper.isLoopAlarmEnabled(context)
        switchLoopAlarm.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setLoopAlarmEnabled(context, isChecked)
        }
    }

    private fun bindThresholdButtons() {
        val currentThresh = AlarmPreferencesHelper.getHighLevelThreshold(context)
        updateThresholdUi(currentThresh)

        btnThresh80.setOnClickListener { setThreshold(80) }
        btnThresh85.setOnClickListener { setThreshold(85) }
        btnThresh90.setOnClickListener { setThreshold(90) }
        btnThresh100.setOnClickListener { setThreshold(100) }
    }

    private fun setThreshold(pct: Int) {
        AlarmPreferencesHelper.setHighLevelThreshold(context, pct)
        updateThresholdUi(pct)
    }

    private fun updateThresholdUi(pct: Int) {
        tvBatteryAlarmLabel.text = "Berbunyi apabila bateri cecah $pct%"
        val activeColor = Color.parseColor("#00E676")
        val activeText = Color.parseColor("#121318")
        val inactiveColor = Color.parseColor("#21262D")
        val inactiveText = Color.parseColor("#CFD8DC")

        val map = mapOf(
            80 to btnThresh80,
            85 to btnThresh85,
            90 to btnThresh90,
            100 to btnThresh100
        )

        map.forEach { (level, btn) ->
            if (level == pct) {
                btn.setBackgroundColor(activeColor)
                btn.setTextColor(activeText)
            } else {
                btn.setBackgroundColor(inactiveColor)
                btn.setTextColor(inactiveText)
            }
        }
    }

    private fun bindThermalButtons() {
        val currentTemp = AlarmPreferencesHelper.getThermalThreshold(context).toInt()
        updateThermalUi(currentTemp)

        btnTemp42.setOnClickListener { setTemp(42) }
        btnTemp45.setOnClickListener { setTemp(45) }
        btnTemp48.setOnClickListener { setTemp(48) }
    }

    private fun setTemp(temp: Int) {
        AlarmPreferencesHelper.setThermalThreshold(context, temp.toFloat())
        updateThermalUi(temp)
    }

    private fun updateThermalUi(temp: Int) {
        tvTempAlarmLabel.text = String.format(Locale.US, "Amaran kecemasan jika suhu cecah %d.0 °C", temp)
        val activeColor = Color.parseColor("#FFA000")
        val activeText = Color.parseColor("#121318")
        val inactiveColor = Color.parseColor("#21262D")
        val inactiveText = Color.parseColor("#CFD8DC")

        val map = mapOf(
            42 to btnTemp42,
            45 to btnTemp45,
            48 to btnTemp48
        )

        map.forEach { (t, btn) ->
            if (t == temp) {
                btn.setBackgroundColor(activeColor)
                btn.setTextColor(activeText)
            } else {
                btn.setBackgroundColor(inactiveColor)
                btn.setTextColor(inactiveText)
            }
        }
    }

    private fun bindAudioOptions() {
        cbSound.isChecked = AlarmPreferencesHelper.isSoundEnabled(context)
        cbSound.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setSoundEnabled(context, isChecked)
        }

        cbVibrate.isChecked = AlarmPreferencesHelper.isVibrateEnabled(context)
        cbVibrate.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setVibrateEnabled(context, isChecked)
        }

        cbTts.isChecked = AlarmPreferencesHelper.isTtsEnabled(context)
        cbTts.setOnCheckedChangeListener { _, isChecked ->
            AlarmPreferencesHelper.setTtsEnabled(context, isChecked)
        }

        btnPickRingtone.setOnClickListener {
            onPickRingtoneClicked()
        }
    }

    private fun bindActions() {
        btnToggleService.setOnClickListener {
            onToggleServiceClicked()
        }

        btnCheckBatteryDrain.setOnClickListener {
            ChargingAlertHelper.openSystemBatterySettings(context)
        }

        btnTestAlarm.setOnClickListener {
            ChargingAlertHelper.triggerAlarm(
                context = context,
                customUriStr = AlarmPreferencesHelper.getCustomRingtoneUri(context),
                enableSound = AlarmPreferencesHelper.isSoundEnabled(context),
                enableVibrate = AlarmPreferencesHelper.isVibrateEnabled(context),
                ttsMessage = if (AlarmPreferencesHelper.isTtsEnabled(context)) {
                    "Ujian penggera CasWatt. Bateri dan keselamatan sistem berfungsi dengan baik."
                } else null
            )
        }
    }

    fun updateRingtoneDisplay() {
        tvCurrentRingtone.text = "Nada: " + AlarmRingtoneHelper.getRingtoneTitle(context)
    }

    fun updateServiceStatus(isRunning: Boolean) {
        if (isRunning) {
            tvServiceStatus.text = "● Sedang Berjalan"
            tvServiceStatus.setTextColor(Color.parseColor("#00E676"))
            btnToggleService.text = "Hentikan Servis Background"
        } else {
            tvServiceStatus.text = "○ Terhenti"
            tvServiceStatus.setTextColor(Color.parseColor("#FF5252"))
            btnToggleService.text = "Jalankan Servis Background"
        }
    }
}
