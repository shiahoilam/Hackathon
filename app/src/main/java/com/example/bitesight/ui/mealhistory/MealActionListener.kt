package com.example.bitesight.ui.mealhistory

import com.example.bitesight.data.local.entity.Meal

interface MealActionListener {
    /**
     * Called when the "Log Meal" button is clicked for a specific meal item.
     */
    fun onLogMealClicked(meal: Meal)
}