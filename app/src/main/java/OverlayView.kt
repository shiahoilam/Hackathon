//package com.example.hackathon
//
//import android.content.Context
//import android.graphics.*
//import android.util.AttributeSet
//import android.view.View
//import kotlin.math.sin
//
//class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
//
//    private var boxes: List<RectF> = emptyList()
//    private var labels: List<String> = emptyList()
//
//    // Soft glowing circle paint
//    private val circlePaint = Paint().apply {
//        color = Color.argb(120, 0, 200, 255) // semi-transparent cyan
//        style = Paint.Style.FILL
//        isAntiAlias = true
//    }
//
//    // Text paint with shadow
//    private val textPaint = Paint().apply {
//        color = Color.WHITE
//        textSize = 60f
//        style = Paint.Style.FILL
//        isAntiAlias = true
//        setShadowLayer(10f, 0f, 0f, Color.BLACK) // soft glow
//    }
//
//    // Call this from MainActivity to update results
//    fun setResults(boxes: List<RectF>, labels: List<String>) {
//        this.boxes = boxes
//        this.labels = labels
//        invalidate() // redraw the view
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//
//        if (boxes.isEmpty() || labels.isEmpty()) return
//
//        val currentTime = System.currentTimeMillis()
//
//        for (i in boxes.indices) {
//            val box = boxes[i]
//            val label = labels[i]
//
//            // Center of detected object
//            val cx = box.centerX()
//            val cy = box.centerY()
//
//            // Optional floating animation
//            val floatOffset = (sin(currentTime / 300.0 + i) * 10).toFloat()
//
//            // Draw soft circle at center
//            canvas.drawCircle(cx, cy, 40f, circlePaint)
//
//            // Draw label slightly above the circle
//            canvas.drawText(label, cx - textPaint.measureText(label) / 2, cy - 60f - floatOffset, textPaint)
//        }
//
//        // Force redraw for animation
//        postInvalidateOnAnimation()
//    }
//}



package com.example.hackathon

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.collections.emptyList


class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var boxes: List<RectF> = emptyList()
    private var labels: List<String> = emptyList()

    // Soft glowing circle paint (fallback)
    private val circlePaint = Paint().apply {
        color = Color.argb(120, 0, 200, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Text paint with shadow
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(10f, 0f, 0f, Color.BLACK)
    }

    // Cute icons for classes
    private val icons: Map<String, Bitmap> = mapOf(
        "banana" to BitmapFactory.decodeResource(resources, R.drawable.banana_emoji)
        // Add more class icons here
    )

    /** Update detection results from MainActivity */
    fun setResults(boxes: List<RectF>, labels: List<String>) {
        this.boxes = boxes
        this.labels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (boxes.isEmpty() || labels.isEmpty()) return

        val currentTime = System.currentTimeMillis()

        for (i in boxes.indices) {
            val box = boxes[i]
            val label = labels[i]

            val cx = box.centerX()
            val cy = box.centerY()

            // Floating offset animation
            val floatOffset = (sin(currentTime / 300.0 + i) * 20).toFloat()

            // Scale animation for pulsing effect
            val scale = 1f + 0.1f * sin(currentTime / 200.0 + i).toFloat()

            // Draw cute icon if exists
            val className = label.split(" ")[0] // get class name
            val icon = icons[className]
            val iconSize = 120f

            if (icon != null) {
                val left = cx - iconSize / 2
                val top = cy - iconSize / 2 - floatOffset
                val right = cx + iconSize / 2
                val bottom = cy + iconSize / 2 - floatOffset

                canvas.save()
                canvas.scale(scale, scale, cx, cy - floatOffset)
                canvas.drawBitmap(icon, null, RectF(left, top, right, bottom), null)
                canvas.restore()
            } else {
                // Fallback: soft glowing circle
                canvas.drawCircle(cx, cy, 40f, circlePaint)
            }

            // Draw label above icon/circle
            canvas.drawText(label, cx - textPaint.measureText(label) / 2, cy - 80f - floatOffset, textPaint)
        }

        // Keep animation going
        postInvalidateOnAnimation()
    }
}
