package com.caswatt.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.ceil

class ChargingChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 1. Grid Utama Mendatar (Setiap 5W - Ada Garis Putus & Label)
    private val majorGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#262C3D")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    }

    // 2. Grid Mikro Mendatar (Setiap 1W - Garisan Sangat Halus & Kemas)
    private val minorGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#171A24")
        strokeWidth = 0.9f
        style = Paint.Style.STROKE
    }

    // 3. Grid Menegak Masa (Setiap 1 Minit)
    private val timeGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C202C")
        strokeWidth = 1.0f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(4f, 6f), 0f)
    }

    // 4. Label Teks Paksi Kuasa (Watt - Kiri)
    private val wattTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        textSize = 21f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    // 5. Label Teks Paksi Suhu (°C - Kanan)
    private val tempTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9100")
        textSize = 21f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }

    // 6. Label Teks Minit Masa (Bawah)
    private val timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#78909C")
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    // 7. Teks Graf Kosong
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#546E7A")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    // 8. Keluk Garisan Watt (Dominan)
    private val wattPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        strokeWidth = 3.6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // 9. Kecerunan Kawasan di Bawah Keluk Watt
    private val wattFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 10. Keluk Garisan Suhu (Halus)
    private val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9100")
        strokeWidth = 2.2f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // 11. Penanda Hujung Titik Semasa
    private val glowHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3800E676")
        style = Paint.Style.FILL
    }

    private val glowDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        style = Paint.Style.FILL
    }

    private val tempDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9100")
        style = Paint.Style.FILL
    }

    // Kitar semula objek Path dalam memori
    private val wattPath = Path()
    private val wattFillPath = Path()
    private val tempPath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val paddingTop = 40f
            val paddingBottom = 60f
            val plotH = (h.toFloat() - paddingTop - paddingBottom).coerceAtLeast(10f)

            wattFillPaint.shader = LinearGradient(
                0f, paddingTop,
                0f, paddingTop + plotH,
                Color.parseColor("#3500E676"),
                Color.parseColor("#0000E676"),
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val paddingLeft = 75f
        val paddingRight = 75f
        val paddingTop = 40f
        val paddingBottom = 60f

        val plotW = w - paddingLeft - paddingRight
        val plotH = h - paddingTop - paddingBottom
        val bottomY = paddingTop + plotH

        if (plotW <= 0f || plotH <= 0f) return

        // 1. Ambil salinan sejarah pengecasan
        val data = synchronized(ChargingSessionManager) {
            ChargingSessionManager.history.toList()
        }

        if (data.size < 2) {
            canvas.drawText("⚡ Sambungkan pengecas untuk merekod keluk...", w / 2f, h / 2f, emptyPaint)
            return
        }

        // 2. Skala Paksi-Y (Gandaan 5W, minimum 20W)
        var maxObservedWatt = 15f
        for (p in data) {
            if (p.watt > maxObservedWatt) {
                maxObservedWatt = p.watt
            }
        }
        val maxWattAxis = (ceil((maxObservedWatt + 2f) / 5f) * 5f).coerceAtLeast(20f)
        val totalWattSteps = maxWattAxis.toInt()

        val minTemp = 20f
        val maxTemp = 60f

        // 3. LUKIS GRID TEPI SETIAP 1W & 5W
        for (wVal in 0..totalWattSteps) {
            val y = bottomY - (wVal.toFloat() / maxWattAxis) * plotH

            if (wVal % 5 == 0) {
                // Major Grid (Setiap 5W): Garisan putus-putus & ada nombor label
                canvas.drawLine(paddingLeft, y, w - paddingRight, y, majorGridPaint)
                canvas.drawText("${wVal}W", 10f, y + 7f, wattTextPaint)
            } else {
                // Minor Grid (Setiap 1W): Garisan kelabu sangat halus tanpa teks
                canvas.drawLine(paddingLeft, y, w - paddingRight, y, minorGridPaint)
            }
        }

        // Label Suhu Paksi Kanan (60°C, 40°C, 20°C)
        canvas.drawText("60°C", w - 10f, paddingTop + 10f, tempTextPaint)
        canvas.drawText("40°C", w - 10f, paddingTop + (plotH / 2f) + 7f, tempTextPaint)
        canvas.drawText("20°C", w - 10f, bottomY + 2f, tempTextPaint)

        // 4. LUKIS GRID BAWAH SETIAP 1 MINIT (Time-based scaling)
        val startTime = ChargingSessionManager.sessionStartTime
        val now = System.currentTimeMillis()
        val elapsedSec = if (startTime > 0L) ((now - startTime) / 1000L).coerceAtLeast(0L) else 0L

        // Tetingkap masa minimum 5 minit (300s). Kalau lebih 5 minit, luaskan automatik ke minit berikutnya
        val timeWindowSec = if (elapsedSec <= 300L) {
            300L
        } else {
            (ceil(elapsedSec / 60.0).toLong() * 60L).coerceAtLeast(300L)
        }

        val totalMinutes = (timeWindowSec / 60L).toInt()

        for (m in 0..totalMinutes) {
            val x = paddingLeft + (m.toFloat() / totalMinutes.toFloat()) * plotW

            // Garisan grid menegak setiap 1 minit
            canvas.drawLine(x, paddingTop, x, bottomY, timeGridPaint)

            // Label teks minit di bawah (Kawal selang label jika sesi terlalu panjang agar teks tak bertindih)
            val shouldDrawLabel = when {
                totalMinutes <= 10 -> true
                totalMinutes <= 25 -> m % 2 == 0
                else -> m % 5 == 0
            }

            if (shouldDrawLabel) {
                canvas.drawText("${m}m", x, h - 20f, timeTextPaint)
            }
        }

        // 5. PLOT KELUK BEZIER MENGIKUT MASA SEBENAR
        wattPath.reset()
        wattFillPath.reset()
        tempPath.reset()

        val firstPointTime = if (startTime > 0L) startTime else data.first().timestamp

        val firstElapsed = ((data[0].timestamp - firstPointTime) / 1000f).coerceAtLeast(0f)
        var prevX = paddingLeft + (firstElapsed / timeWindowSec.toFloat()).coerceIn(0f, 1f) * plotW

        val firstWattClamped = data[0].watt.coerceIn(0f, maxWattAxis)
        var prevYWatt = bottomY - (firstWattClamped / maxWattAxis) * plotH

        val firstTempClamped = data[0].temp.coerceIn(minTemp, maxTemp)
        var prevYTemp = bottomY - ((firstTempClamped - minTemp) / (maxTemp - minTemp)) * plotH

        wattPath.moveTo(prevX, prevYWatt)
        wattFillPath.moveTo(prevX, bottomY)
        wattFillPath.lineTo(prevX, prevYWatt)
        tempPath.moveTo(prevX, prevYTemp)

        for (i in 1 until data.size) {
            val p = data[i]
            val pElapsed = ((p.timestamp - firstPointTime) / 1000f).coerceAtLeast(0f)
            val currX = paddingLeft + (pElapsed / timeWindowSec.toFloat()).coerceIn(0f, 1f) * plotW

            val clampedWatt = p.watt.coerceIn(0f, maxWattAxis)
            val currYWatt = bottomY - (clampedWatt / maxWattAxis) * plotH

            val clampedTemp = p.temp.coerceIn(minTemp, maxTemp)
            val currYTemp = bottomY - ((clampedTemp - minTemp) / (maxTemp - minTemp)) * plotH

            val midX = (prevX + currX) / 2f
            val midYWatt = (prevYWatt + currYWatt) / 2f
            val midYTemp = (prevYTemp + currYTemp) / 2f

            wattPath.quadTo(prevX, prevYWatt, midX, midYWatt)
            wattFillPath.quadTo(prevX, prevYWatt, midX, midYWatt)
            tempPath.quadTo(prevX, prevYTemp, midX, midYTemp)

            prevX = currX
            prevYWatt = currYWatt
            prevYTemp = currYTemp
        }

        // Sambung ke titik penamat
        wattPath.lineTo(prevX, prevYWatt)
        wattFillPath.lineTo(prevX, prevYWatt)
        wattFillPath.lineTo(prevX, bottomY)
        wattFillPath.close()

        tempPath.lineTo(prevX, prevYTemp)

        // Lukis lapisan: Kecerunan Kawasan -> Keluk Watt -> Keluk Suhu -> Titik Bercahaya
        canvas.drawPath(wattFillPath, wattFillPaint)
        canvas.drawPath(wattPath, wattPaint)
        canvas.drawPath(tempPath, tempPaint)

        canvas.drawCircle(prevX, prevYWatt, 8f, glowHaloPaint)
        canvas.drawCircle(prevX, prevYWatt, 4f, glowDotPaint)
        canvas.drawCircle(prevX, prevYTemp, 3f, tempDotPaint)
    }
}
