package com.example.bitesight

import android.graphics.RectF

data class FoodInfo(
    val calories: Int,  // kcal per 100g
    val density: Float = 0.85f, // g/cm³ (Default to roughly an apple/mixed food)
    val protein: Float = 0.0f,
    val fat: Float = 0.0f,
    val carbs: Float = 0.0f
)

data class DetectionResult(
    val foodName: String,
    val confidence: Float,
    val boundingBox: RectF,
    val calories: Int,
    val weightGrams: Int = 0, // Calculated weight
    val unit: String = "estimated"
)