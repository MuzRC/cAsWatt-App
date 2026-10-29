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

    // 1. Grid Garisan Putus-putus
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F2430")
        strokeWidth = 1.8f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }

    // 2. Label Teks Paksi Kuasa (Watt - Kiri)
    private val wattTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    // 3. Label Teks Paksi Suhu (°C - Kanan)
    private val tempTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9100")
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }

    // 4. Label Teks Paksi Masa (Bawah)
    private val timeTextLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#78909C")
        textSize = 22f
        textAlign = Paint.Align.LEFT
    }

    private val timeTextCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#546E7A")
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    private val timeTextRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#78909C")
        textSize = 22f
        textAlign = Paint.Align.RIGHT
    }

    // 5. Teks Mesej Graf Kosong
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#546E7A")
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    // 6. Garisan Keluk Watt (Dominan & Kemas)
    private val wattPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        strokeWidth = 3.8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // 7. Kecerunan Kawasan di Bawah Garisan Watt
    private val wattFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 8. Garisan Keluk Suhu (Halus & Elegan)
    private val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9100")
        strokeWidth = 2.4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // 9. Titik Bercahaya (Glow Marker) pada Titik Terkini
    private val glowHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4400E676")
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

    // Objek Path dikitar semula untuk mengelakkan alokasi memori berulang
    private val wattPath = Path()
    private val wattFillPath = Path()
    private val tempPath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val paddingTop = 45f
            val paddingBottom = 65f
            val plotH = (h.toFloat() - paddingTop - paddingBottom).coerceAtLeast(10f)

            // Pasang kecerunan hijau lut sinar di bawah garisan watt
            wattFillPaint.shader = LinearGradient(
                0f, paddingTop,
                0f, paddingTop + plotH,
                Color.parseColor("#3800E676"),
                Color.parseColor("#0000E676"),
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val paddingLeft = 80f
        val paddingRight = 80f
        val paddingTop = 45f
        val paddingBottom = 65f

        val plotW = w - paddingLeft - paddingRight
        val plotH = h - paddingTop - paddingBottom

        if (plotW <= 0f || plotH <= 0f) return

        // Lukis 5 garisan grid mendatar
        for (i in 0..4) {
            val y = paddingTop + (plotH / 4f) * i
            canvas.drawLine(paddingLeft, y, w - paddingRight, y, gridPaint)
        }

        // Ambil data sejarah pengecasan secara selamat
        val data = synchronized(ChargingSessionManager) {
            ChargingSessionManager.history.toList()
        }

        if (data.size < 2) {
            canvas.drawText("⚡ Sambungkan pengecas untuk merekod keluk...", w / 2f, h / 2f, emptyPaint)
            return
        }

        // Skala automatik Pintar Paksi-Y (gandaan 5W lebih stabil berbanding 10W)
        var maxObservedWatt = 15f
        for (p in data) {
            if (p.watt > maxObservedWatt) {
                maxObservedWatt = p.watt
            }
        }
        val maxWattAxis = (ceil((maxObservedWatt + 3f) / 5f) * 5f).coerceAtLeast(20f)

        val minTemp = 20f
        val maxTemp = 60f

        // Label Paksi Kuasa (Kiri): Puncak, Tengah, dan Sifar
        canvas.drawText(String.format(Locale.US, "%.0fW", maxWattAxis), 12f, paddingTop + 18f, wattTextPaint)
        canvas.drawText(String.format(Locale.US, "%.0fW", maxWattAxis / 2f), 12f, paddingTop + (plotH / 2f) + 8f, wattTextPaint)
        canvas.drawText("0W", 12f, h - paddingBottom, wattTextPaint)

        // Label Paksi Suhu (Kanan): 60°C, 40°C, dan 20°C
        canvas.drawText("60°C", w - 12f, paddingTop + 18f, tempTextPaint)
        canvas.drawText("40°C", w - 12f, paddingTop + (plotH / 2f) + 8f, tempTextPaint)
        canvas.drawText("20°C", w - 12f, h - paddingBottom, tempTextPaint)

        // Reset semua Path untuk lukisan bingkai semasa
        wattPath.reset()
        wattFillPath.reset()
        tempPath.reset()

        val stepX = plotW / (data.size - 1).toFloat()
        val bottomY = paddingTop + plotH

        var prevX = paddingLeft
        val firstWattClamped = data[0].watt.coerceIn(0f, maxWattAxis)
        var prevYWatt = bottomY - (firstWattClamped / maxWattAxis) * plotH

        val firstTempClamped = data[0].temp.coerceIn(minTemp, maxTemp)
        var prevYTemp = bottomY - ((firstTempClamped - minTemp) / (maxTemp - minTemp)) * plotH

        wattPath.moveTo(prevX, prevYWatt)
        wattFillPath.moveTo(prevX, bottomY)
        wattFillPath.lineTo(prevX, prevYWatt)
        tempPath.moveTo(prevX, prevYTemp)

        // Algoritma Midpoint Bezier: Tukar sambungan bersegi menjadi lengkung lembut
        for (i in 1 until data.size) {
            val p = data[i]
            val currX = paddingLeft + i * stepX

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

        // Sambung titik akhir
        wattPath.lineTo(prevX, prevYWatt)
        wattFillPath.lineTo(prevX, prevYWatt)
        wattFillPath.lineTo(prevX, bottomY)
        wattFillPath.close()

        tempPath.lineTo(prevX, prevYTemp)

        // 1. Lukis kecerunan kawasan hijau di bawah keluk watt
        canvas.drawPath(wattFillPath, wattFillPaint)

        // 2. Lukis garisan keluk watt dan suhu
        canvas.drawPath(wattPath, wattPaint)
        canvas.drawPath(tempPath, tempPaint)

        // 3. Lukis titik bercahaya pada bacaan terkini (Hujung graf)
        canvas.drawCircle(prevX, prevYWatt, 9f, glowHaloPaint)
        canvas.drawCircle(prevX, prevYWatt, 4.5f, glowDotPaint)
        canvas.drawCircle(prevX, prevYTemp, 3.5f, tempDotPaint)

        // 4. Lukis Penanda Garis Masa Paksi-X (Bawah Graf)
        val startTime = ChargingSessionManager.sessionStartTime
        val elapsedSec = if (startTime > 0L) ((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(0L) else 0L
        val elapsedMin = elapsedSec / 60L
        val remainSec = elapsedSec % 60L

        val durationLabel = if (elapsedMin > 0) String.format(Locale.US, "%dm %ds", elapsedMin, remainSec) else String.format(Locale.US, "%ds", remainSec)

        val timeY = h - 22f
        canvas.drawText("0m", paddingLeft, timeY, timeTextLeft)
        canvas.drawText("Masa Pengecasan", paddingLeft + (plotW / 2f), timeY, timeTextCenter)
        canvas.drawText(durationLabel, w - paddingRight, timeY, timeTextRight)
    }
}
