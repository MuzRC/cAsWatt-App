package com.caswatt.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.ceil

class ChargingChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 1. Cat garisan grid berputus-putus
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2D3A")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    // 2. Cat label teks paksi Watt (Hijau)
    private val wattTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        textSize = 28f
        textAlign = Paint.Align.LEFT
    }

    // 3. Cat label teks paksi Suhu (Oren)
    private val tempTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9100")
        textSize = 28f
        textAlign = Paint.Align.RIGHT
    }

    // 4. Cat notis apabila data graf masih kosong (Diisytihar awal untuk elak alokasi dalam onDraw)
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#546E7A")
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    // 5. Cat garisan keluk Watt
    private val wattPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // 6. Cat garisan keluk Suhu
    private val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9100")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Objek Path dikitar semula untuk mengelakkan alokasi memori berulang
    private val wattPath = Path()
    private val tempPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val paddingLeft = 70f
        val paddingRight = 70f
        val paddingTop = 50f
        val paddingBottom = 60f

        val plotW = w - paddingLeft - paddingRight
        val plotH = h - paddingTop - paddingBottom

        if (plotW <= 0f || plotH <= 0f) return

        // Lukis 5 garisan grid mendatar
        for (i in 0..4) {
            val y = paddingTop + (plotH / 4f) * i
            canvas.drawLine(paddingLeft, y, w - paddingRight, y, gridPaint)
        }

        // Ambil salinan titik data secara selamat daripada ChargingSessionManager
        val data = synchronized(ChargingSessionManager) {
            ChargingSessionManager.history.toList()
        }

        if (data.size < 2) {
            canvas.drawText("⚡ Sambungkan pengecas untuk merekod keluk...", w / 2f, h / 2f, emptyPaint)
            return
        }

        // Cari bacaan watt tertinggi untuk skala automatik paksi Y
        var maxObservedWatt = 15f
        for (p in data) {
            if (p.watt > maxObservedWatt) {
                maxObservedWatt = p.watt
            }
        }
        val maxWattAxis = (ceil((maxObservedWatt + 5f) / 10f) * 10f).coerceAtLeast(20f)

        val minTemp = 20f
        val maxTemp = 60f

        // Lukis label teks paksi kuasa (Watt) di sebelah kiri
        canvas.drawText(String.format(Locale.US, "%.0fW", maxWattAxis), 10f, paddingTop + 20f, wattTextPaint)
        canvas.drawText("0W", 10f, h - paddingBottom, wattTextPaint)

        // Lukis label teks paksi suhu (°C) di sebelah kanan
        canvas.drawText("60°C", w - 10f, paddingTop + 20f, tempTextPaint)
        canvas.drawText("20°C", w - 10f, h - paddingBottom, tempTextPaint)

        wattPath.reset()
        tempPath.reset()

        val stepX = plotW / (data.size - 1).toFloat()

        for (i in data.indices) {
            val p = data[i]
            val x = paddingLeft + i * stepX

            val clampedWatt = p.watt.coerceIn(0f, maxWattAxis)
            val yWatt = (paddingTop + plotH) - (clampedWatt / maxWattAxis) * plotH

            val clampedTemp = p.temp.coerceIn(minTemp, maxTemp)
            val yTemp = (paddingTop + plotH) - ((clampedTemp - minTemp) / (maxTemp - minTemp)) * plotH

            if (i == 0) {
                wattPath.moveTo(x, yWatt)
                tempPath.moveTo(x, yTemp)
            } else {
                wattPath.lineTo(x, yWatt)
                tempPath.lineTo(x, yTemp)
            }
        }

        // Lukis garisan keluk di atas grid
        canvas.drawPath(wattPath, wattPaint)
        canvas.drawPath(tempPath, tempPaint)
    }
}