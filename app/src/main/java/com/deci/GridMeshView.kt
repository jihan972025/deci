package com.deci

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GridMeshView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.FILL
    }

    private val gridSize = 20f * context.resources.displayMetrics.density // 20dp
    private val dotRadius = 1f * context.resources.displayMetrics.density // 1dp

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Draw grid dots
        var y = 0f
        while (y <= height) {
            var x = 0f
            while (x <= width) {
                canvas.drawCircle(x, y, dotRadius, dotPaint)
                x += gridSize
            }
            y += gridSize
        }
    }
}
