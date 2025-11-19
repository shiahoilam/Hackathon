package com.example.hackathon

import android.graphics.RectF

data class FoodInfo(
    val calories: Int,      // kcal per 100g
    val density: Float,     // g/cm³
    val standardPortion: Int = 150, // Target portion in grams (default 150g)
    val protein: Float = 0.0f,
    val fat: Float = 0.0f,
    val carbs: Float = 0.0f
)

data class DetectionResult(
    val foodName: String,
    val confidence: Float,
    val boundingBox: RectF,
    val calories: Int,
    val weightGrams: Int = 0,
    val standardPortion: Int = 150, // Pass this down to the view
    val unit: String = ""
)