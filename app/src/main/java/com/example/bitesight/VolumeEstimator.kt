package com.example.bitesight

import android.graphics.RectF
import android.media.Image
import com.google.ar.core.CameraIntrinsics
import java.nio.ByteOrder
import kotlin.math.min

object VolumeEstimator {

    // --- CALIBRATION SETTINGS ---
    // 1. Reduce volume because food doesn't fill the entire bounding box (corners are empty)
    // 2. Further reduce because depth sensor might overshoot slightly
    // Try 0.4f or 0.5f. If still too high, lower it. If too low, raise it.
    private const val CALIBRATION_FACTOR = 0.45f

    /**
     * Calculates the estimated weight of a detected object.
     */
    fun calculateWeight(
        box: RectF,
        depthImage: Image,
        cameraIntrinsics: CameraIntrinsics,
        density: Float,
        screenWidth: Int,
        screenHeight: Int
    ): Int {
        val depthWidth = depthImage.width
        val depthHeight = depthImage.height

        // 1. Smart Depth Sampling
        // Instead of 1 pixel, sample a 5x5 grid at the center and pick the NEAREST value.
        // This prevents "punching through" the object to the table behind it.
        val centerX = box.centerX()
        val centerY = box.centerY()

        // Convert screen coords to depth coords
        val centerDepthX = (centerX / screenWidth * depthWidth).toInt()
        val centerDepthY = (centerY / screenHeight * depthHeight).toInt()

        val depthMm = getSafeAverageDepth(depthImage, centerDepthX, centerDepthY)

        if (depthMm == 0) return 0

        val depthMeters = depthMm / 1000f

        // 2. Correct for Device Rotation (Portrait Mode)
        // ARCore intrinsics are usually for the landscape camera sensor (e.g., 640x480).
        // But your screen is portrait (e.g., 1080x2400).
        // We must swap dimensions if the aspect ratios are flipped.
        val imageW = cameraIntrinsics.imageDimensions[0].toFloat()
        val imageH = cameraIntrinsics.imageDimensions[1].toFloat()

        val fx: Float
        val fy: Float
        val scaleX: Float
        val scaleY: Float

        // Check if screen is portrait but image is landscape
        if (screenHeight > screenWidth && imageW > imageH) {
            // Swap focal lengths and scales for portrait math
            fx = cameraIntrinsics.focalLength[1] // Use fy as fx
            fy = cameraIntrinsics.focalLength[0] // Use fx as fy

            // Map Screen Width -> Image Height
            // Map Screen Height -> Image Width
            scaleX = imageH / screenWidth
            scaleY = imageW / screenHeight
        } else {
            fx = cameraIntrinsics.focalLength[0]
            fy = cameraIntrinsics.focalLength[1]
            scaleX = imageW / screenWidth
            scaleY = imageH / screenHeight
        }

        // 3. Calculate Real World Dimensions
        val boxWidthPx = box.width() * scaleX
        val boxHeightPx = box.height() * scaleY

        // Size = (Pixels * Depth) / Focal_Length
        val realWidthM = (boxWidthPx * depthMeters) / fx
        val realHeightM = (boxHeightPx * depthMeters) / fy

        // 4. Volume Estimation
        // Heuristic: Thickness is roughly 60% of the smaller dimension
        val estimatedThicknessM = min(realWidthM, realHeightM) * 0.6f

        val rawVolumeM3 = realWidthM * realHeightM * estimatedThicknessM

        // Apply Calibration Factor (The "Sphere in a Box" correction)
        val calibratedVolumeM3 = rawVolumeM3 * CALIBRATION_FACTOR

        val volumeCm3 = calibratedVolumeM3 * 1_000_000

        // 5. Weight
        val weightGrams = volumeCm3 * density

        return weightGrams.toInt()
    }

    /**
     * Samples a grid of pixels and returns the closest valid depth (min value).
     * This helps ignore background noise if the center pixel misses the object.
     */
    private fun getSafeAverageDepth(depthImage: Image, cx: Int, cy: Int): Int {
        val plane = depthImage.planes[0]
        val buffer = plane.buffer.order(ByteOrder.nativeOrder())
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val width = depthImage.width
        val height = depthImage.height

        var minDepth = Int.MAX_VALUE
        var validSamples = 0

        // Sample a small grid (radius 3 = 7x7 grid)
        val radius = 3

        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val x = (cx + dx).coerceIn(0, width - 1)
                val y = (cy + dy).coerceIn(0, height - 1)

                val byteIndex = x * pixelStride + y * rowStride

                try {
                    // Depth is 16-bit integer in millimeters
                    val depthSample = buffer.getShort(byteIndex).toInt() and 0xFFFF

                    // Filter out 0 (invalid) and extremely far values (>2 meters)
                    if (depthSample in 50..2000) {
                        // We take the MINIMUM depth because the object is closer than the table.
                        if (depthSample < minDepth) {
                            minDepth = depthSample
                        }
                        validSamples++
                    }
                } catch (e: Exception) {
                    // Ignore buffer errors
                }
            }
        }

        return if (validSamples > 0) minDepth else 0
    }
}