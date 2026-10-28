package com.caswatt.app.fragments

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
import com.caswatt.app.ChargingSessionManager
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

        btnClearGraph.setOnClickListener {
            synchronized(ChargingSessionManager) {
                ChargingSessionManager.history.clear()
            }
            chartView.invalidate()
            context?.let { ctx ->
                Toast.makeText(ctx, "Graf keluk telah dikosongkan.", Toast.LENGTH_SHORT).show()
            }
        }

        btnExportCsv.setOnClickListener {
            exportRecordsToCsv()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    /**
     * Kemas kini keseluruhan paparan statistik dan sejarah
     */
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
        tvTodayChargeCount.text = "Berapa Kali Dicas Hari Ini: ${todayRecord?.count ?: 0} kali"
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
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 6, 0, 6)
            }

            val startStr = "${dateFormat.format(Date(session.startTime))} ${timeFormat.format(Date(session.startTime))}"
            val endStr = timeFormat.format(Date(session.endTime))
            val durStr = ChargingSessionManager.formatDuration(session.endTime - session.startTime)
            val gained = session.endLevel - session.startLevel
            val gainStr = if (gained >= 0) "+$gained%" else "$gained%"

            val tvHeader = TextView(ctx).apply {
                text = "⚡ $startStr - $endStr ($durStr) • ${session.startLevel}% ➔ ${session.endLevel}% ($gainStr)"
                setTextColor(Color.parseColor("#00E676"))
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
            }

            val tvDetails = TextView(ctx).apply {
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
                val divider = View(ctx).apply {
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

        for (record in allDaily) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8, 0, 8)
            }

            val tvDate = TextView(ctx).apply {
                text = "📅 ${record.date}"
                setTextColor(Color.parseColor("#00E676"))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
            }

            val tvDetails = TextView(ctx).apply {
                text = "• Dicas: ${record.count} kali | Puncak: ${String.format(Locale.US, "%.2f W", record.peakWatt)} | Tenaga: ${String.format(Locale.US, "%.0f mAh", record.totalMah)}"
                setTextColor(Color.parseColor("#CFD8DC"))
                textSize = 12f
            }

            row.addView(tvDate)
            row.addView(tvDetails)

            val divider = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                    setMargins(0, 8, 0, 8)
                }
                setBackgroundColor(Color.parseColor("#263238"))
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
            val csvData = ChargingSessionManager.generateCsvData()
            val file = File(ctx.cacheDir, "CasWatt_Pro_Log_Lengkap.csv")
            file.writeText(csvData)

            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "CasWatt Pro v5.0 - Rekod Sejarah Pengecasan Lengkap")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Simpan atau Kongsi Rekod CSV:"))
        } catch (e: Exception) {
            Toast.makeText(ctx, "Ralat mengeksport: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
