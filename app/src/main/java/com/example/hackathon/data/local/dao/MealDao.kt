package com.example.hackathon.data.local.dao

import androidx.room.*
import com.example.hackathon.data.local.entity.Meal
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    // Insert a meal
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: Meal): Long

    // Get all meals
    @Query("SELECT * FROM meal ORDER BY created_at DESC")
    fun getAllMeals(): Flow<List<Meal>>

    // Get today's meals
    @Query("""
        SELECT * FROM meal 
        WHERE DATE(created_at / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
        ORDER BY created_at DESC
    """)
    fun getTodayMeals(): Flow<List<Meal>>

    // Get today's total calories
    @Query("""
        SELECT COALESCE(SUM(calories), 0)
        FROM meal 
        WHERE DATE(created_at / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
    """)
    fun getTodayTotalCalories(): Flow<Int>

    // Get today's total protein
    @Query("""
        SELECT COALESCE(SUM(protein), 0.0)
        FROM meal 
        WHERE DATE(created_at / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
    """)
    fun getTodayTotalProtein(): Flow<Float>

    // Get today's total carbs
    @Query("""
        SELECT COALESCE(SUM(carbs), 0.0)
        FROM meal 
        WHERE DATE(created_at / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
    """)
    fun getTodayTotalCarbs(): Flow<Float>

    // Get today's total fat
    @Query("""
        SELECT COALESCE(SUM(fat), 0.0)
        FROM meal 
        WHERE DATE(created_at / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
    """)
    fun getTodayTotalFat(): Flow<Float>

    // Get meal count for today
    @Query("""
        SELECT COUNT(*) 
        FROM meal 
        WHERE DATE(created_at / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
    """)
    fun getTodayMealCount(): Flow<Int>

    // Delete a meal
    @Delete
    suspend fun deleteMeal(meal: Meal)

    // Delete all meals
    @Query("DELETE FROM meal")
    suspend fun deleteAllMeals()
}