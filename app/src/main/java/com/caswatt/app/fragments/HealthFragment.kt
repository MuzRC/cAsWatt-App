package com.caswatt.app.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.caswatt.app.BatteryHealthManager
import com.caswatt.app.ChargingSessionManager
import com.caswatt.app.R
import java.util.Locale

class HealthFragment : Fragment() {

    private lateinit var tvHealthCondition: TextView
    private lateinit var tvHealthCycles: TextView
    private lateinit var tvHealthCumulative: TextView
    private lateinit var tvHealthVoltageDetail: TextView
    private lateinit var tvHealthTempDetail: TextView

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateHealthMetrics(intent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_health, container, false)
        initViews(view)
        setupListeners()
        return view
    }

    private fun initViews(view: View) {
        tvHealthCondition = view.findViewById(R.id.tv_health_condition)
        tvHealthCycles = view.findViewById(R.id.tv_health_cycles)
        tvHealthCumulative = view.findViewById(R.id.tv_health_cumulative)
        tvHealthVoltageDetail = view.findViewById(R.id.tv_health_voltage_detail)
        tvHealthTempDetail = view.findViewById(R.id.tv_health_temp_detail)
    }

    private fun setupListeners() {
        // Sentuh status kesihatan untuk tukar kapasiti asal kilang
        tvHealthCondition.setOnClickListener {
            showEditCapacityDialog()
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
        updateHealthDisplay()
    }

    override fun onPause() {
        super.onPause()
        try {
            context?.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
    }

    /**
     * Kemas kini bacaan voltan, suhu sel, dan kitaran semasa bateri
     */
    private fun updateHealthMetrics(intent: Intent) {
        val ctx = context ?: return

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val voltageV = voltageMv / 1000.0

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempC = tempTenths / 10.0

        updateHealthDisplay()

        tvHealthCycles.text = String.format(Locale.US, "Anggaran Kitaran: %.1f kitaran", ChargingSessionManager.getEstimatedCycles())
        tvHealthCumulative.text = String.format(Locale.US, "Jumlah Kumulatif Cas: %.0f%% cas", ChargingSessionManager.cumulativePctCharged)
        tvHealthVoltageDetail.text = String.format(Locale.US, "Voltan Sel Semasa: %.2f V (Normal: 3.7V - 4.4V)", voltageV)

        val tempStatus = when {
            tempC < 40.0 -> "Zon Selamat (Selesa)"
            tempC < 45.0 -> "Zon Suam"
            else -> "Zon Panas Bahaya!"
        }
        tvHealthTempDetail.text = String.format(Locale.US, "Suhu Sel: %.1f °C (%s)", tempC, tempStatus)
    }

    /**
     * Format paparan kesihatan: Peratus besar hijau, kapasiti mAh & butang "Ubah ✏️" biru menyala
     */
    private fun updateHealthDisplay() {
        val ctx = context ?: return
        val healthPct = BatteryHealthManager.getHealthPercentage(ctx)
        val estMah = BatteryHealthManager.getEstimatedCapacityMah(ctx).toInt()
        val desMah = BatteryHealthManager.getDesignCapacity(ctx)

        val healthHtml = """
            <big><b><font color="#00E676">%.1f%%</font></b></big> <font color="#CFD8DC">Kesihatan Bateri</font><br/>
            <small><font color="#90A4AE">%d mAh / %d mAh &nbsp;•&nbsp;</font> <font color="#29B6F6"><b><u>Ubah</u> ✏️</b></font></small>
        """.trimIndent()

        tvHealthCondition.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(
                String.format(Locale.US, healthHtml, healthPct, estMah, desMah),
                Html.FROM_HTML_MODE_LEGACY
            )
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(String.format(Locale.US, healthHtml, healthPct, estMah, desMah))
        }
    }

    /**
     * Dialog untuk memasukkan kapasiti bateri asal kilang (mAh)
     */
    private fun showEditCapacityDialog() {
        val ctx = context ?: return
        val currentCapacity = BatteryHealthManager.getDesignCapacity(ctx)

        val input = EditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Contoh: 5000"
            setText(currentCapacity.toString())
            setSelection(text.length)
            setPadding(60, 40, 60, 20)
        }

        AlertDialog.Builder(ctx)
            .setTitle("Tetapkan Kapasiti Asal Kilang")
            .setMessage("Masukkan spesifikasi kapasiti bateri asal telefon (Design Capacity dalam unit mAh):")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val enteredValue = input.text.toString().toIntOrNull()
                if (enteredValue != null && enteredValue in 1000..15000) {
                    BatteryHealthManager.setDesignCapacity(ctx, enteredValue)
                    updateHealthDisplay()
                    Toast.makeText(ctx, "Kapasiti berjaya disimpan: ${enteredValue} mAh", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(ctx, "Sila masukkan nilai mAh yang sah (1000 - 15000 mAh)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
