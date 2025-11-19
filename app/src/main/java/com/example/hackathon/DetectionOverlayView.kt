package com.example.hackathon

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View

class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val detectionPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        style = Paint.Style.FILL
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // --- Paints for Portion Ring ---
    private val ringBackgroundPaint = Paint().apply {
        color = Color.argb(100, 200, 200, 200) // Faint gray
        style = Paint.Style.STROKE
        strokeWidth = 12f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val ringProgressPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 12f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val textBounds = Rect()
    private val textRect = RectF()
    private var detectionResults: List<DetectionResult> = emptyList()

    fun setDetectionResults(results: List<DetectionResult>) {
        detectionResults = results
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (detectionResults.isEmpty()) return

        for (result in detectionResults) {
            // 1. Draw Bounding Box
            canvas.drawRect(result.boundingBox, detectionPaint)

            // 2. Prepare Text
            val text = "${result.foodName} ${result.calories}kcal ${result.unit}"
            textPaint.getTextBounds(text, 0, text.length, textBounds)

            val padding = 16f
            val ringSize = 60f // Diameter of the ring
            val spacing = 20f  // Space between text and ring

            val textWidth = textBounds.width().toFloat()
            val textHeight = textBounds.height().toFloat()

            // Total width of the background block = Text + Spacing + Ring + Padding
            val totalBlockWidth = textWidth + spacing + ringSize + (padding * 2)
            val totalBlockHeight = maxOf(textHeight, ringSize) + (padding * 2)

            // Position above the box
            val blockLeft = result.boundingBox.left
            val blockTop = if (result.boundingBox.top > totalBlockHeight + 20) {
                result.boundingBox.top - totalBlockHeight - 10
            } else {
                result.boundingBox.top + 10
            }

            // 3. Draw Background Rectangle
            textRect.set(
                blockLeft,
                blockTop,
                blockLeft + totalBlockWidth,
                blockTop + totalBlockHeight
            )
            canvas.drawRoundRect(textRect, 16f, 16f, textBackgroundPaint)

            // 4. Draw Text
            // Center vertically within the block
            val textY = blockTop + padding + textHeight - (textHeight/4) // visual adjustment
            canvas.drawText(text, blockLeft + padding, textY, textPaint)

            // 5. Draw Portion Ring (The "Donut Chart")
            if (result.weightGrams > 0) {
                val cx = blockLeft + padding + textWidth + spacing + (ringSize / 2)
                val cy = blockTop + (totalBlockHeight / 2)
                val radius = ringSize / 2

                // Determine Percentage
                val percentage = result.weightGrams.toFloat() / result.standardPortion.toFloat()

                // Color Logic
                when {
                    percentage < 0.9f -> ringProgressPaint.color = Color.parseColor("#4CAF50") // Green (Good)
                    percentage in 0.9f..1.1f -> ringProgressPaint.color = Color.parseColor("#00E676") // Bright Green (Perfect)
                    percentage in 1.1f..1.3f -> ringProgressPaint.color = Color.parseColor("#FFEB3B") // Yellow (Warning)
                    else -> ringProgressPaint.color = Color.parseColor("#FF5252") // Red (Too much)
                }

                val sweepAngle = (percentage * 360f).coerceAtMost(360f)

                // Draw gray background ring
                canvas.drawCircle(cx, cy, radius, ringBackgroundPaint)

                // Draw colored progress arc
                // -90 start angle so it starts at top (12 o'clock)
                canvas.drawArc(
                    cx - radius, cy - radius, cx + radius, cy + radius,
                    -90f, sweepAngle, false, ringProgressPaint
                )
            }
        }
    }
}