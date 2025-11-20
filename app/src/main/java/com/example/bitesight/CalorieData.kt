package com.example.bitesight

data class CalorieDataPoint(
    val label: String,
    val calories: Int,
    val timestamp: Long
)

enum class Timeline {
    DAILY, WEEKLY, MONTHLY
}
