package com.caswatt.app.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.caswatt.app.ChargingChartView
import com.caswatt.app.ChargingDiagnosticsHelper
import com.caswatt.app.ChargingSessionManager
import com.caswatt.app.ChargingStorageHelper
import com.caswatt.app.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsFragment : Fragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)
        initViews(view)
        setupListeners()
        return view
    }

    private fun initViews(view: View) {
        tvTodayChargeCount = view.findViewById(R.id.tv_today_charge_count)
        tvTodayTotalMah = view.findViewById(R.id.tv_today_total_mah)
        tvTodayPeakWatt = view.findViewById(R.id.tv_today_peak_watt)
        btnExportCsv = view.findViewById(R.id.btn_export_csv)
        llSessionHistoryContainer = view.findViewById(R.id.ll_session_history_container)
        btnToggleSessions = view.findViewById(R.id.btn_toggle_sessions)
        llDailyHistoryContainer = view.findViewById(R.id.ll_daily_history_container)
        chartView = view.findViewById(R.id.charging_chart_view)
        btnClearGraph = view.findViewById(R.id.btn_clear_graph)
    }

    private fun setupListeners() {
        btnToggleSessions.setOnClickListener {
            isSessionsExpanded = !isSessionsExpanded
            renderSessionHistory()
        }

        // Kotak dialog amaran supaya graf tidak terpadam secara tidak sengaja
        btnClearGraph.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            AlertDialog.Builder(ctx)
                .setTitle("Kosongkan Graf?")
                .setMessage("Adakah anda pasti mahu memadam semua rekod keluk graf semasa?")
                .setPositiveButton("Ya, Kosongkan") { _, _ ->
                    synchronized(ChargingSessionManager) {
                        ChargingSessionManager.history.clear()
                    }
                    chartView.invalidate()
                    Toast.makeText(ctx, "Graf keluk telah dikosongkan.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        btnExportCsv.setOnClickListener {
            exportRecordsToCsv()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    fun refreshData() {
        updateTodaySummary()
        chartView.invalidate()
        renderSessionHistory()
        renderDailyHistory()
    }

    private fun updateTodaySummary() {
        val todayRecord = synchronized(ChargingSessionManager) {
            ChargingSessionManager.dailyRecords.firstOrNull()
        }
        tvTodayChargeCount.text = "Berapa Kali Dicas Hari Ini: " + (todayRecord?.count ?: 0) + " kali"
        tvTodayTotalMah.text = String.format(Locale.US, "Jumlah Tenaga Masuk Hari Ini: %.0f mAh", todayRecord?.totalMah ?: 0.0)
        tvTodayPeakWatt.text = String.format(Locale.US, "Watt Puncak Hari Ini: %.2f W", todayRecord?.peakWatt ?: 0.0)
    }

    private fun renderSessionHistory() {
        val ctx = context ?: return
        llSessionHistoryContainer.removeAllViews()

        val allSessions = synchronized(ChargingSessionManager) {
            ChargingSessionManager.sessionRecords.toList()
        }

        val totalRecords = allSessions.size
        if (totalRecords == 0) {
            btnToggleSessions.visibility = View.GONE
            val tvEmpty = TextView(ctx).apply {
                text = "Belum ada rekod sesi cas sah. Cucuk dan cabut pengecas (>45s) untuk mula merekod!"
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
                "Tampilkan Lebih Banyak (" + (totalRecords - 4) + " sesi lagi) ▼"
            }
        } else {
            btnToggleSessions.visibility = View.GONE
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

        for (session in displayList) {
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)
                setBackgroundColor(Color.parseColor("#171A21"))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 8)
                }
                layoutParams = lp
            }

            val startStr = dateFormat.format(Date(session.startTime)) + " " + timeFormat.format(Date(session.startTime))
            val endStr = timeFormat.format(Date(session.endTime))
            val durMs = session.endTime - session.startTime
            val durStr = ChargingDiagnosticsHelper.formatDuration(durMs)
            val gained = session.endLevel - session.startLevel
            val gainStr = if (gained >= 0) "+$gained%" else "$gained%"

            val tvHeader = TextView(ctx).apply {
                text = "⚡ " + startStr + " - " + endStr + " (" + durStr + ") • " + session.startLevel + "% ➔ " + session.endLevel + "% (" + gainStr + ")"
                setTextColor(Color.parseColor("#00E676"))
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
            }

            // 1. Pengiraan Kelajuan Pengecasan (Charging Velocity)
            val durMin = durMs / 60000.0
            val speedStr = if (durMin > 0.5 && gained > 0) {
                val rate = gained / durMin
                String.format(Locale.US, " | Kelajuan: %.1f%%/min", rate)
            } else {
                ""
            }

            // 2. Kuasa Watt Puncak & Purata
            val tvSpeed = TextView(ctx).apply {
                val pWatt = String.format(Locale.US, "%.1fW", session.peakWatt)
                val aWatt = if (session.avgWatt > 0.0) String.format(Locale.US, " (Purata %.1fW)", session.avgWatt) else ""
                val mah = String.format(Locale.US, "%.0f mAh", session.totalMah)
                val wh = String.format(Locale.US, "%.2f Wh", session.energyWh)

                text = pWatt + aWatt + " • " + mah + " (" + wh + ")" + speedStr
                setTextColor(Color.parseColor("#CFD8DC"))
                textSize = 12f
                setPadding(0, 4, 0, 2)
            }

            // 3. Kualiti Kabel & Lompatan Suhu
            val tvDetails = TextView(ctx).apply {
                val cableBadge = when {
                    session.cableQuality.contains("Super", ignoreCase = true) -> "⚡⚡ Super Fast"
                    session.cableQuality.contains("Fast", ignoreCase = true) -> "⚡ Fast Charge"
                    session.cableQuality.contains("Lemah", ignoreCase = true) || session.cableQuality.contains("Tinggi", ignoreCase = true) -> "⚠️ Rintangan Tinggi"
                    else -> "Standard"
                }

                val tempStr = if (session.startTemp > 0.0 && session.endTemp > 0.0) {
                    val delta = session.endTemp - session.startTemp
                    val sign = if (delta >= 0) "+" else ""
                    String.format(Locale.US, " | Suhu: %.0f°C ➔ %.0f°C (%s%.1f°C)", session.startTemp, session.endTemp, sign, delta)
                } else {
                    ""
                }

                text = "Kabel: " + cableBadge + tempStr
                val cableColor = when {
                    cableBadge.contains("Rintangan", ignoreCase = true) -> Color.parseColor("#FF5252")
                    cableBadge.contains("Fast", ignoreCase = true) -> Color.parseColor("#00E676")
                    else -> Color.parseColor("#FFD54F")
                }
                setTextColor(cableColor)
                textSize = 11f
            }

            card.addView(tvHeader)
            card.addView(tvSpeed)
            card.addView(tvDetails)

            llSessionHistoryContainer.addView(card)
        }
    }

    private fun renderDailyHistory() {
        val ctx = context ?: return
        llDailyHistoryContainer.removeAllViews()

        val allDaily = synchronized(ChargingSessionManager) {
            ChargingSessionManager.dailyRecords.toList()
        }

        if (allDaily.isEmpty()) {
            val tvEmpty = TextView(ctx).apply {
                text = "Belum ada rekod cas harian. Cucuk pengecas untuk mula merekod!"
                setTextColor(Color.parseColor("#90A4AE"))
                textSize = 13f
            }
            llDailyHistoryContainer.addView(tvEmpty)
            return
        }

        // Kad Analitik Trend 7 Hari (Gantikan Pengulangan Data Hari Ini)
        val weekly = ChargingSessionManager.getWeeklyStats()
        if (weekly.totalCharges > 0) {
            val trendCard = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)
                setBackgroundColor(Color.parseColor("#1E222B"))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 12)
                }
                layoutParams = lp
            }

            val tvTrendTitle = TextView(ctx).apply {
                text = "📊 RINGKASAN TREND 7 HARI TERKINI"
                setTextColor(Color.parseColor("#80D8FF"))
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
            }

            val tvTrendStats = TextView(ctx).apply {
                text = "• Jumlah Sesi Cas: " + weekly.totalCharges + " kali\n" +
                       "• Tenaga Terkumpul: " + String.format(Locale.US, "%.0f mAh (%.1f Wh)", weekly.totalMah, weekly.totalWh) + "\n" +
                       "• Purata Harian: " + String.format(Locale.US, "%.1f kali / hari", weekly.avgChargesPerDay) + "\n" +
                       "• Hari Paling Kerap: " + weekly.mostActiveDay
                setTextColor(Color.parseColor("#ECEFF1"))
                textSize = 12f
                setLineSpacing(4f, 1f)
                setPadding(0, 6, 0, 0)
            }

            trendCard.addView(tvTrendTitle)
            trendCard.addView(tvTrendStats)
            llDailyHistoryContainer.addView(trendCard)
        }

        // Senarai Rekod Harian Lepas
        for (record in allDaily) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 6, 0, 6)
            }

            val tvDate = TextView(ctx).apply {
                text = "📅 " + record.date
                setTextColor(Color.parseColor("#00E676"))
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
            }

            val tvDetails = TextView(ctx).apply {
                text = "• Dicas: " + record.count + " kali | Puncak: " + String.format(Locale.US, "%.2f W", record.peakWatt) + " | Tenaga: " + String.format(Locale.US, "%.0f mAh", record.totalMah)
                setTextColor(Color.parseColor("#CFD8DC"))
                textSize = 12f
            }

            row.addView(tvDate)
            row.addView(tvDetails)

            val divider = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                    setMargins(0, 6, 0, 6)
                }
                setBackgroundColor(Color.parseColor("#21262D"))
            }

            llDailyHistoryContainer.addView(row)
            llDailyHistoryContainer.addView(divider)
        }
    }

    private fun exportRecordsToCsv() {
        val ctx = context ?: return

        if (ChargingSessionManager.dailyRecords.isEmpty() && ChargingSessionManager.sessionRecords.isEmpty()) {
            Toast.makeText(ctx, "Tiada rekod cas untuk dieksport lagi!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val csvData = ChargingStorageHelper.generateCsvData(
                ChargingSessionManager.sessionRecords,
                ChargingSessionManager.dailyRecords
            )
            val file = File(ctx.cacheDir, "CasWatt_Pro_Log_Lengkap.csv")
            file.writeText(csvData)

            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "CasWatt Pro - Rekod Sejarah Pengecasan Lengkap")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Simpan atau Kongsi Rekod CSV:"))
        } catch (e: Exception) {
            Toast.makeText(ctx, "Ralat mengeksport: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }
}
