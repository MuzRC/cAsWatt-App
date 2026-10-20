package com.caswatt.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class ChargingChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2D3A")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#90A4AE")
        textSize = 28f
    }

    private val wattPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9100")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

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

        if (plotW <= 0 || plotH <= 0) return

        for (i in 0..4) {
            val y = paddingTop + (plotH / 4f) * i
            canvas.drawLine(paddingLeft, y, w - paddingRight, y, gridPaint)
        }

        val data = synchronized(ChargingSessionManager) {
            ChargingSessionManager.history.toList()
        }

        if (data.size < 2) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#546E7A")
                textSize = 32f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("⚡ Sambungkan pengecas untuk merekod keluk...", w / 2f, h / 2f, emptyPaint)
            return
        }

        var maxObservedWatt = 15f
        for (p in data) {
            if (p.watt > maxObservedWatt) maxObservedWatt = p.watt
        }
        val maxWattAxis = (kotlin.math.ceil((maxObservedWatt + 5f) / 10f) * 10f).coerceAtLeast(20f)

        val minTemp = 20f
        val maxTemp = 60f

        textPaint.color = Color.parseColor("#00E676")
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(String.format("%.0fW", maxWattAxis), 10f, paddingTop + 20f, textPaint)
        canvas.drawText("0W", 10f, h - paddingBottom, textPaint)

        textPaint.color = Color.parseColor("#FF9100")
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("60°C", w - 10f, paddingTop + 20f, textPaint)
        canvas.drawText("20°C", w - 10f, h - paddingBottom, textPaint)

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

        canvas.drawPath(wattPath, wattPaint)
        canvas.drawPath(tempPath, tempPaint)
    }
}
