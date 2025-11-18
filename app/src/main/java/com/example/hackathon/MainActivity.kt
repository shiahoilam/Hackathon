package com.example.hackathon

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
       // val fabCamera = findViewById<FloatingActionButton>(R.id.fab_camera)

        // Handle bottom navigation item clicks
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_analysis -> {
                    Toast.makeText(this, "Analysis", Toast.LENGTH_SHORT).show()
                    true
                }
//                R.id.nav_history -> {
//                    Toast.makeText(this, "History", Toast.LENGTH_SHORT).show()
//                    true
//                }
                R.id.nav_history -> {
                    // 👉 Navigate to the MealHistoryActivity
                    val intent = Intent(this, MealHistoryActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Handle camera FAB click (Main feature!)
//        fabCamera.setOnClickListener {
//            Toast.makeText(this, "Camera opened!", Toast.LENGTH_SHORT).show()
//            // TODO: Open camera activity
//        }
    }
}

//package com.example.hackathon
//
//import android.os.Bundle
//import android.widget.Button
//import android.widget.LinearLayout
//import android.widget.ScrollView
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.lifecycleScope
//import com.example.hackathon.data.local.AppDatabase
//import com.example.hackathon.data.local.entity.Meal
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.launch
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var database: AppDatabase
//    private lateinit var statsText: TextView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Initialize database
//        database = AppDatabase.getDatabase(this)
//
//        // Create UI
//        val layout = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            setPadding(32, 32, 32, 32)
//        }
//
//        // Stats TextView
//        statsText = TextView(this).apply {
//            textSize = 16f
//            setPadding(0, 0, 0, 32)
//        }
//        layout.addView(statsText)
//
//        // Add Chicken Button
//        val btnChicken = Button(this).apply {
//            text = "Add Chicken (250 cal)"
//            setOnClickListener {
//                lifecycleScope.launch {
//                    val meal = Meal(
//                        foodName = "Chicken",
//                        calories = 250,
//                        protein = 30f,
//                        fat = 14f,
//                        carbs = 0f,
//                        confidence = 0.95f,
//                        imagePath = null
//                    )
//                    database.mealDao().insertMeal(meal)
//                    updateStats()
//                }
//            }
//        }
//        layout.addView(btnChicken)
//
//        // Add Rice Button
//        val btnRice = Button(this).apply {
//            text = "Add Rice (200 cal)"
//            setOnClickListener {
//                lifecycleScope.launch {
//                    val meal = Meal(
//                        foodName = "Rice",
//                        calories = 200,
//                        protein = 4f,
//                        fat = 0.4f,
//                        carbs = 44f,
//                        confidence = 0.92f,
//                        imagePath = null
//                    )
//                    database.mealDao().insertMeal(meal)
//                    updateStats()
//                }
//            }
//        }
//        layout.addView(btnRice)
//
//        // Clear Button
//        val btnClear = Button(this).apply {
//            text = "Clear All"
//            setOnClickListener {
//                lifecycleScope.launch {
//                    database.mealDao().deleteAllMeals()
//                    updateStats()
//                }
//            }
//        }
//        layout.addView(btnClear)
//
//        // Wrap in ScrollView
//        val scrollView = ScrollView(this).apply {
//            addView(layout)
//        }
//
//        setContentView(scrollView)
//
//        // Initial stats
//        updateStats()
//    }
//
//    private fun updateStats() {
//        lifecycleScope.launch {
//            val calories = database.mealDao().getTodayTotalCalories().first()
//            val protein = database.mealDao().getTodayTotalProtein().first()
//            val carbs = database.mealDao().getTodayTotalCarbs().first()
//            val fat = database.mealDao().getTodayTotalFat().first()
//            val count = database.mealDao().getTodayMealCount().first()
//
//            statsText.text = """
//                Room Database Test
//
//                Today's Totals:
//                Calories: $calories cal
//                Protein: ${String.format("%.1f", protein)}g
//                Carbs: ${String.format("%.1f", carbs)}g
//                Fat: ${String.format("%.1f", fat)}g
//                Meals: $count
//            """.trimIndent()
//        }
//    }
//}