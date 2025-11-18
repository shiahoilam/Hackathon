package com.example.hackathon.data.local.dao

import androidx.room.*
import com.example.hackathon.data.local.entity.Meal
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    // Insert one
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: Meal): Long

    // Insert multiple
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meals: List<Meal>)

    // Observe all meals
    @Query("SELECT * FROM meal ORDER BY created_at DESC")
    fun observeAllMeals(): Flow<List<Meal>>

    // Count meals
    @Query("SELECT COUNT(*) FROM meal")
    suspend fun countMeals(): Int

    // TODAY — All meals
    @Query("""
        SELECT * FROM meal
        WHERE date(created_at/1000, 'unixepoch','localtime') = date('now','localtime')
        ORDER BY created_at DESC
    """)
    fun getTodayMeals(): Flow<List<Meal>>

    // TODAY — Total calories
    @Query("""
        SELECT COALESCE(SUM(calories), 0)
        FROM meal
        WHERE date(created_at/1000, 'unixepoch','localtime') = date('now','localtime')
    """)
    fun observeTodayCalories(): Flow<Int>

    // Delete one
    @Delete
    suspend fun deleteMeal(meal: Meal)

    // Delete all
    @Query("DELETE FROM meal")
    suspend fun deleteAll()
}
