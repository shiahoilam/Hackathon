package com.example.bitesight.data.local.repo

import com.example.bitesight.data.local.dao.MealDao
import com.example.bitesight.data.local.entity.Meal
import kotlinx.coroutines.flow.Flow
import java.util.*

class MealRepository(private val dao: MealDao) {

    fun observeMeals(): Flow<List<Meal>> = dao.observeAllMeals()

    fun observeTodayCalories() = dao.observeTodayCalories()

    fun observeMealsByDateRange(startTime: Long, endTime: Long): Flow<List<Meal>> {
        return dao.observeMealsByDateRange(startTime, endTime)
    }

    suspend fun insert(meal: Meal) = dao.insertMeal(meal)

    suspend fun insertAll(meals: List<Meal>) = dao.insertAll(meals)

    suspend fun countMeals() = dao.countMeals()

    suspend fun delete(meal: Meal) = dao.deleteMeal(meal) // Match the Activity call

    // Helper function to get today's date range
    fun getTodayDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfToday = calendar.timeInMillis

        return Pair(startOfToday, endOfToday)
    }

    fun getYesterdayDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfYesterday = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfYesterday = calendar.timeInMillis

        return Pair(startOfYesterday, endOfYesterday)
    }
}