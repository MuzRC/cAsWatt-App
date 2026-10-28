package com.caswatt.app.fragments

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.caswatt.app.BatteryHealthManager
import com.caswatt.app.ChargingSessionManager
import com.caswatt.app.R
import java.util.Locale
import kotlin.math.roundToInt

class HealthFragment : Fragment() {

    private lateinit var tvHealthPercentage: TextView
    private lateinit var tvHealthServiceBadge: TextView
    private lateinit var pbHealthBar: ProgressBar
    private lateinit var tvHealthCapacityDetail: TextView
    private lateinit var btnEditCapacity: TextView
    private lateinit var tvHealthConfidence: TextView
    private lateinit var tvHealthWearLoss: TextView
    private lateinit var tvHealthResistance: TextView
    private lateinit var tvHealthCycles: TextView
    private lateinit var tvHealthCumulative: TextView
    private lateinit var tvHealthAdvice: TextView
    private lateinit var btnResetBattery: Button

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
        tvHealthPercentage = view.findViewById(R.id.tv_health_percentage)
        tvHealthServiceBadge = view.findViewById(R.id.tv_health_service_badge)
        pbHealthBar = view.findViewById(R.id.pb_health_bar)
        tvHealthCapacityDetail = view.findViewById(R.id.tv_health_capacity_detail)
        btnEditCapacity = view.findViewById(R.id.btn_edit_capacity)
        tvHealthConfidence = view.findViewById(R.id.tv_health_confidence)
        tvHealthWearLoss = view.findViewById(R.id.tv_health_wear_loss)
        tvHealthResistance = view.findViewById(R.id.tv_health_resistance)
        tvHealthCycles = view.findViewById(R.id.tv_health_cycles)
        tvHealthCumulative = view.findViewById(R.id.tv_health_cumulative)
        tvHealthAdvice = view.findViewById(R.id.tv_health_advice)
        btnResetBattery = view.findViewById(R.id.btn_reset_battery)
    }

    private fun setupListeners() {
        btnEditCapacity.setOnClickListener {
            showEditCapacityDialog()
        }

        btnResetBattery.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        updateHealthDisplay()
    }

    fun updateHealthDisplay() {
        val ctx = context ?: return
        val data = BatteryHealthManager.getHealthData(ctx)

        // 1. Peratus & Warna Tolok Kesihatan
        val healthInt = data.healthPct.coerceIn(0.0, 100.0).roundToInt()
        tvHealthPercentage.text = String.format(Locale.US, "%.1f%%", data.healthPct)

        val healthColor = when {
            data.healthPct >= 80.0 -> Color.parseColor("#00E676")
            data.healthPct >= 70.0 -> Color.parseColor("#FFD54F")
            else -> Color.parseColor("#FF5252")
        }
        tvHealthPercentage.setTextColor(healthColor)
        pbHealthBar.progress = healthInt
        pbHealthBar.progressTintList = ColorStateList.valueOf(healthColor)

        // 2. Lencana Status Servis
        tvHealthServiceBadge.text = data.serviceStatus
        tvHealthServiceBadge.setTextColor(data.serviceColor)

        // 3. Kapasiti Sebenar vs Kilang
        tvHealthCapacityDetail.text = String.format(
            Locale.US,
            "Kapasiti: %.0f mAh / %.0f mAh",
            data.estimatedCapacityMah,
            data.designCapacityMah
        )

        // 4. Tahap Keyakinan Kalibrasi
        tvHealthConfidence.text = "Keyakinan Kalibrasi: " + data.confidenceLevel
        tvHealthConfidence.setTextColor(data.confidenceColor)

        // 5. Diagnostik Penuaan & Elektrik
        tvHealthWearLoss.text = String.format(
            Locale.US,
            "• Kehilangan Kapasiti: -%.0f mAh (-%.1f%%)",
            data.capacityLostMah,
            data.capacityLostPct
        )

        val resStatus = when {
            data.internalResistanceMilliOhm <= 80.0 -> "Cemerlang"
            data.internalResistanceMilliOhm <= 140.0 -> "Normal"
            else -> "Tinggi (Sel Berusia)"
        }
        tvHealthResistance.text = String.format(
            Locale.US,
            "• Rintangan Dalaman Sel: %.0f mΩ (%s)",
            data.internalResistanceMilliOhm,
            resStatus
        )

        tvHealthCycles.text = String.format(
            Locale.US,
            "• Kitaran Dikesan: %.1f kitaran (Sejak CasWatt dipasang)",
            data.totalCycles
        )

        tvHealthCumulative.text = String.format(
            Locale.US,
            "• Jumlah Kumulatif Cas: %.0f%% cas",
            ChargingSessionManager.cumulativePctCharged
        )

        // 6. Nasihat Klinikal Dinamik
        tvHealthAdvice.text = when {
            data.healthPct >= 80.0 ->
                "Bateri anda beroperasi pada tahap kapasiti optimum. Teruskan mengecas secara santai dan elakkan peranti terbiar terlalu lama pada 0% untuk mengekalkan kesegaran kimia sel."
            data.healthPct >= 70.0 ->
                "Kapasiti sel bateri anda telah menurun sekitar " + String.format(Locale.US, "%.1f%%", data.capacityLostPct) + ". Anda mungkin menyedari tempoh penggunaan berkurang. Masih selamat digunakan untuk tugasan harian."
            else ->
                "Kesihatan bateri berada pada zon penuaan ketara (< 70%). Sel bateri berisiko mengalami kejatuhan voltan mendadak semasa beban tinggi. Servis atau penggantian bateri disyorkan."
        }
    }

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
                    Toast.makeText(ctx, "Kapasiti disimpan: " + enteredValue + " mAh", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(ctx, "Sila masukkan nilai mAh yang sah (1000 - 15000 mAh)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showResetConfirmationDialog() {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle("Pasang Bateri Baharu?")
            .setMessage("Adakah anda telah menggantikan bateri telefon ini? Tindakan ini akan mengosongkan semua sampel penuaan lama dan menetapkan semula kalibrasi ke 100%.")
            .setPositiveButton("Ya, Set Semula") { _, _ ->
                BatteryHealthManager.resetForNewBattery(ctx)
                updateHealthDisplay()
                Toast.makeText(ctx, "Kalibrasi bateri telah diset semula.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
