package com.example.bitesight.ui.mealhistory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bitesight.R
import com.example.bitesight.data.local.db.AppDatabase
import com.example.bitesight.data.local.entity.Meal
import com.example.bitesight.data.local.repo.MealRepository
import kotlinx.coroutines.launch

class MealHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_history)

        // RecyclerView setup
        val recyclerView = findViewById<RecyclerView>(R.id.mealRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Get DAO -> Repository
        val dao = AppDatabase.getDatabase(this).mealDao()
        val repo = MealRepository(dao)

        // Adapter setup
        val adapter = MealAdapter(emptyList())
        recyclerView.adapter = adapter

        // Observe meals from Room
        lifecycleScope.launch {
            repo.observeMeals().collect { meals ->
                adapter.updateMeals(meals)
            }
        }

        // Insert sample data (only once)
        lifecycleScope.launch {
            if (repo.countMeals() == 0) {
                repo.insert(
                    Meal(
                        foodName = "Grilled Salmon",
                        calories = 650,
                        protein = 30f,
                        fat = 20f,
                        carbs = 10f,
                        imagePath = "salmon"
                    )
                )
                repo.insert(
                    Meal(
                        foodName = "Grilled Chicken",
                        calories = 450,
                        protein = 40f,
                        fat = 5f,
                        carbs = 3f,
                        imagePath = "chicken"
                    )
                )
                repo.insert(
                    Meal(
                        foodName = "Grilled Steak",
                        calories = 720,
                        protein = 50f,
                        fat = 30f,
                        carbs = 0f,
                        imagePath = "steak"
                    )
                )
            }
        }
    }
}
