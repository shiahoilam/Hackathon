package com.example.hackathon

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Float = 0.82f // 82% filled
    private val strokeWidth = 20f

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@CircularProgressView.strokeWidth
        color = Color.parseColor("#1A2332")
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@CircularProgressView.strokeWidth
        strokeCap = Paint.Cap.ROUND
    }

    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (Math.min(width, height) / 2f) - strokeWidth

        rect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // Draw background circle
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // Draw progress arc with gradient effect
        val sweepAngle = 360 * progress

        // Create gradient from green to yellow
        val greenColor = Color.parseColor("#4CAF50")
        val yellowColor = Color.parseColor("#FFC107")

        // Simple color interpolation based on progress
        val red = (Color.red(greenColor) + (Color.red(yellowColor) - Color.red(greenColor)) * progress).toInt()
        val green = (Color.green(greenColor) + (Color.green(yellowColor) - Color.green(greenColor)) * progress).toInt()
        val blue = (Color.blue(greenColor) + (Color.blue(yellowColor) - Color.blue(greenColor)) * progress).toInt()

        progressPaint.color = Color.rgb(red, green, blue)

        canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
    }

    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = 240 * resources.displayMetrics.density.toInt()
        setMeasuredDimension(size, size)
    }
}