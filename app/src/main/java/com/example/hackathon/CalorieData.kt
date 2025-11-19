package com.example.hackathon

data class CalorieDataPoint(
    val label: String,
    val calories: Int,
    val timestamp: Long
)

enum class Timeline {
    DAILY, WEEKLY, MONTHLY
}
