package com.example.bitesight

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val detectionPaint = Paint().apply {
        color = Color.TRANSPARENT
        style = Paint.Style.STROKE
        strokeWidth = 0f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 40f
        style = Paint.Style.FILL
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.argb(220, 255, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val ringBackgroundPaint = Paint().apply {
        color = Color.argb(100, 200, 200, 200)
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

    // Load calorie icon bitmap
    private val calorieIcon: Bitmap by lazy {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_fire)!!
        val size = 48 // icon size in pixels
        Bitmap.createScaledBitmap(
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                val canvas = Canvas(this)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
            }, size, size, true
        )
    }

    fun setDetectionResults(results: List<DetectionResult>) {
        detectionResults = results
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (detectionResults.isEmpty()) return

        for (result in detectionResults) {

            // Bounding box (invisible)
            canvas.drawRect(result.boundingBox, detectionPaint)

            // Prepare text
            val text = "${result.foodName} ${result.calories} kcal ${result.unit}"
            textPaint.getTextBounds(text, 0, text.length, textBounds)

            val padding = 18f
            val ringSize = 60f
            val spacing = 24f
            val ringSpacing = 48f // distance between text and donut
            val iconSpacing = 8f
            val iconWidth = calorieIcon.width.toFloat()
            val iconHeight = calorieIcon.height.toFloat()

            val textWidth = textBounds.width().toFloat()
            val textHeight = textBounds.height().toFloat()

            // Total width includes icon, text, spacing, and donut
            val totalBlockWidth = iconWidth + iconSpacing + textWidth + ringSpacing + ringSize + padding * 2
            val totalBlockHeight = maxOf(textHeight, ringSize, iconHeight) + padding * 2

            val blockLeft = result.boundingBox.left
            val blockTop = if (result.boundingBox.top > totalBlockHeight + 20) {
                result.boundingBox.top - totalBlockHeight - 10
            } else {
                result.boundingBox.top + 10
            }

            textRect.set(
                blockLeft,
                blockTop,
                blockLeft + totalBlockWidth,
                blockTop + totalBlockHeight
            )

            // Draw white rounded rectangle
            canvas.drawRoundRect(textRect, 20f, 20f, textBackgroundPaint)

            // Draw calorie icon
            val iconX = blockLeft + padding
            val iconY = blockTop + padding + (totalBlockHeight - padding * 2 - iconHeight) / 2
            canvas.drawBitmap(calorieIcon, iconX, iconY, null)

            // Draw text next to icon
            val textX = iconX + iconWidth + iconSpacing
            val textY = blockTop + padding + textHeight
            canvas.drawText(text, textX, textY, textPaint)

            // Draw portion ring inside rectangle
            if (result.weightGrams > 0) {
                val cx = textX + textWidth + ringSpacing + (ringSize / 2)
                val cy = blockTop + (totalBlockHeight / 2)
                val radius = ringSize / 2

                val percentage = result.weightGrams.toFloat() / result.standardPortion.toFloat()

                when {
                    percentage < 0.9f -> ringProgressPaint.color = Color.parseColor("#4CAF50")
                    percentage in 0.9f..1.1f -> ringProgressPaint.color = Color.parseColor("#00E676")
                    percentage in 1.1f..1.3f -> ringProgressPaint.color = Color.parseColor("#FFEB3B")
                    else -> ringProgressPaint.color = Color.parseColor("#FF5252")
                }

                val sweepAngle = (percentage * 360f).coerceAtMost(360f)

                canvas.drawCircle(cx, cy, radius, ringBackgroundPaint)
                canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, -90f, sweepAngle, false, ringProgressPaint)
            }
        }
    }
}
