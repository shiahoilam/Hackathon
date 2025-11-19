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
        color = Color.GREEN  // Changed to green for better visibility
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f  // Slightly smaller for better fit
        style = Paint.Style.FILL
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.argb(220, 0, 0, 0) // More opaque black
        style = Paint.Style.FILL
    }

    private val textBounds = Rect()
    private val textRect = RectF()

    // FIX: Changed from List<CameraActivity.DetectionResult> to List<DetectionResult>
    private var detectionResults: List<DetectionResult> = emptyList()

    // FIX: Changed parameter type here as well
    fun setDetectionResults(results: List<DetectionResult>) {
        detectionResults = results
        Log.d("DetectionOverlay", "Received ${results.size} detections to draw")
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (detectionResults.isEmpty()) {
            return
        }

        for (result in detectionResults) {
            // Draw bounding box
            canvas.drawRect(result.boundingBox, detectionPaint)

            // Prepare text
            val confidencePercent = (result.confidence * 100).toInt()

            // FIX: Updated text to include the Unit (e.g., "120g") calculated by VolumeEstimator
            // result.unit will be "per 100g" if no depth, or "(150 g)" if depth worked.
            val text = "${result.foodName} ${confidencePercent}% - ${result.calories}kcal ${result.unit}"

            textPaint.getTextBounds(text, 0, text.length, textBounds)

            val padding = 10f
            val textWidth = textBounds.width().toFloat()
            val textHeight = textBounds.height().toFloat()

            // Position text above the box if there's room, otherwise inside
            val textTop = if (result.boundingBox.top > textHeight + padding * 2) {
                result.boundingBox.top - textHeight - padding * 2
            } else {
                result.boundingBox.top + padding
            }

            // Draw background rectangle for text
            textRect.set(
                result.boundingBox.left,
                textTop,
                result.boundingBox.left + textWidth + padding * 2,
                textTop + textHeight + padding * 2
            )

            canvas.drawRect(textRect, textBackgroundPaint)

            // Draw text
            canvas.drawText(
                text,
                result.boundingBox.left + padding,
                textTop + textHeight + padding,
                textPaint
            )
        }
    }
}