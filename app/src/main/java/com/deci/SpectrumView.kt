package com.deci

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.LinkedList

class SpectrumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Y-axis: 0-120 dB
    private val minDb = 0f
    private val maxDb = 120f

    // Store dB values with timestamps
    private data class BarData(val db: Float, val timestamp: Long)
    private val barDataList = LinkedList<BarData>()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var accentHex = "CCFF00"

    fun setAccentColor(color: Int) {
        accentHex = String.format("%06X", color and 0xFFFFFF)
        invalidate()
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666")
        textSize = 10f * context.resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.RIGHT
    }

    private val labelPadding = 4f * context.resources.displayMetrics.density
    private val yAxisLabels = listOf(20, 40, 60, 80, 100, 120)

    init {
        // Add left padding for Y-axis labels and top/bottom padding to prevent clipping
        setPadding(
            (30f * context.resources.displayMetrics.density).toInt(),
            (16f * context.resources.displayMetrics.density).toInt(),
            paddingRight,
            (8f * context.resources.displayMetrics.density).toInt()
        )
        // Don't start random updates - only use real audio data from updateWithAmplitude()
    }


    fun updateWithAmplitude(amplitude: Float) {
        // Convert amplitude to dB (approximate)
        val db = if (amplitude > 0) {
            val normalized = (amplitude / 32768f).coerceIn(0.001f, 1f)
            (20 * kotlin.math.log10(normalized.toDouble())).toFloat() + 90f // Adjust to 0-120 range
        } else {
            minDb
        }

        val clampedDb = db.coerceIn(minDb, maxDb)
        barDataList.add(BarData(clampedDb, System.currentTimeMillis()))

        // Keep only bars that fit on screen
        val maxBars = (width / 8f).toInt().coerceAtLeast(1)
        while (barDataList.size > maxBars) {
            barDataList.removeFirst()
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val maxHeight = height.toFloat()
        val leftPadding = paddingLeft.toFloat()

        // Draw Y-axis labels
        yAxisLabels.forEach { labelDb ->
            val normalizedPosition = (labelDb - minDb) / (maxDb - minDb)
            val y = maxHeight - (normalizedPosition * maxHeight)

            canvas.drawText(
                "${labelDb}dB",
                leftPadding - labelPadding,
                y + 4f, // Slight offset for better alignment
                textPaint
            )
        }

        if (barDataList.isEmpty()) return

        val barWidth = 6f // Fixed bar width
        val barGap = 2f
        val totalBarWidth = barWidth + barGap

        // Calculate how many bars can fit (account for left padding)
        val availableWidth = width - leftPadding
        val maxBars = (availableWidth / totalBarWidth).toInt()

        barDataList.takeLast(maxBars).forEachIndexed { index, barData ->
            val x = leftPadding + (index * totalBarWidth)

            // Map dB value (0-120) to height (0-maxHeight)
            val normalizedHeight = (barData.db - minDb) / (maxDb - minDb)
            val barHeight = (normalizedHeight * maxHeight).coerceIn(0f, maxHeight)

            // Calculate opacity based on dB value
            val opacity = ((barData.db / maxDb) * 200 + 55).toInt().coerceIn(50, 255)
            val alpha = opacity.toString(16).padStart(2, '0')

            barPaint.color = Color.parseColor("#${alpha}${accentHex}")

            canvas.drawRect(
                x,
                maxHeight - barHeight,
                x + barWidth,
                maxHeight,
                barPaint
            )
        }
    }
}
