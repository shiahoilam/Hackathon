package com.example.hackathon.data.local.repo

import com.example.hackathon.data.local.dao.MealDao
import com.example.hackathon.data.local.entity.Meal

class MealRepository(private val dao: MealDao) {

    fun observeMeals() = dao.observeAllMeals()

    fun observeTodayCalories() = dao.observeTodayCalories()

    suspend fun insert(meal: Meal) = dao.insert(meal)

    suspend fun insertAll(meals: List<Meal>) = dao.insertAll(meals)

    suspend fun countMeals() = dao.countMeals()

    suspend fun deleteMeal(meal: Meal) = dao.deleteMeal(meal)
}
