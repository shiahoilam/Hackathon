package com.example.bitesight

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.bitesight.data.local.db.AppDatabase
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private lateinit var database: AppDatabase
    // TextViews from your XML layout
    private lateinit var tvCurrentCalories: TextView
    private lateinit var tvGoalCalories: TextView
    private lateinit var tvRemainingCalories: TextView
    private lateinit var tvProteinValue: TextView
    private lateinit var tvCarbsValue: TextView
    private lateinit var tvFatValue: TextView

    // Progress bars
    private lateinit var proteinProgressBar: ProgressBar
    private lateinit var carbsProgressBar: ProgressBar
    private lateinit var fatProgressBar: ProgressBar
    private lateinit var circularProgress: CircularProgressView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupBottomNavigation()
        setSelectedNavItem(R.id.nav_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize database
        database = AppDatabase.getDatabase(this)

        // Initialize Views
        initializeViews()

        // Setup bottom nav
        setupBottomNavigation()

        // observe database changes and update UI
        observeDatabaseChanges()
    }

        private fun initializeViews() {
            // TextViews
            tvCurrentCalories = findViewById(R.id.currentCalories)
            tvGoalCalories = findViewById(R.id.goalCalories)
            tvRemainingCalories = findViewById(R.id.remainingCalories)
            tvProteinValue = findViewById(R.id.proteinValue)
            tvCarbsValue = findViewById(R.id.carbsValue)
            tvFatValue = findViewById(R.id.fatValue)

            // Progress bars
            proteinProgressBar = findViewById(R.id.proteinProgress)
            carbsProgressBar = findViewById(R.id.carbsProgress)
            fatProgressBar = findViewById(R.id.fatProgress)
            circularProgress = findViewById(R.id.circularProgress)
        }
        private fun observeDatabaseChanges() {
            // User goals (hardcoded for now - later from settings)
            val goalCalories = 2000
            val goalProtein = 60f
            val goalCarbs = 200f
            val goalFat = 65f

            // Observe today's total calories
            lifecycleScope.launch {
                database.mealDao().getTodayTotalCalories().collect { currentCalories ->
                    val remaining = goalCalories - currentCalories

                    runOnUiThread {
                        tvCurrentCalories.text = String.format("%,d", currentCalories)
                        tvGoalCalories.text = String.format("%,d", goalCalories)
                        tvRemainingCalories.text = if (remaining >= 0) {
                            "$remaining cal remaining"
                        } else {
                            "${Math.abs(remaining)} cal over"
                        }

                        // Update circular progress
                        val progress = currentCalories.toFloat() / goalCalories
                        circularProgress.setProgress(progress)
                    }
                }
            }

            // Observe today's protein
            lifecycleScope.launch {
                database.mealDao().getTodayTotalProtein().collect { currentProtein ->
                    runOnUiThread {
                        tvProteinValue.text = "${currentProtein.toInt()}/${goalProtein.toInt()}g"
                        val proteinProgress = ((currentProtein / goalProtein) * 100).toInt().coerceIn(0, 100)
                        proteinProgressBar.progress = proteinProgress
                    }
                }
            }

            // Observe today's carbs
            lifecycleScope.launch {
                database.mealDao().getTodayTotalCarbs().collect { currentCarbs ->
                    runOnUiThread {
                        tvCarbsValue.text = "${currentCarbs.toInt()}/${goalCarbs.toInt()}g"
                        val carbsProgress = ((currentCarbs / goalCarbs) * 100).toInt().coerceIn(0, 100)
                        carbsProgressBar.progress = carbsProgress
                    }
                }
            }

            // Observe today's fat
            lifecycleScope.launch {
                database.mealDao().getTodayTotalFat().collect { currentFat ->
                    runOnUiThread {
                        tvFatValue.text = "${currentFat.toInt()}/${goalFat.toInt()}g"
                        val fatProgress = ((currentFat / goalFat) * 100).toInt().coerceIn(0, 100)
                        fatProgressBar.progress = fatProgress
                    }
                }
            }
        }
        // Setup Bottom Navigation
//        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
       // val fabCamera = findViewById<FloatingActionButton>(R.id.fab_camera)

        // Handle bottom navigation item clicks
//        bottomNav.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.nav_home -> {
//                    Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
//                    true
//                }
//                R.id.nav_analysis -> {
//                    Toast.makeText(this, "Analysis", Toast.LENGTH_SHORT).show()
//                    true
//                }
//                R.id.nav_history -> {
//                    // 👉 Navigate to the MealHistoryActivity
//                    val intent = Intent(this, MealHistoryActivity::class.java)
//                    startActivity(intent)
//                    true
//                }
//                else -> false
//            }
//        }

        // Handle camera FAB click (Main feature!)
//        fabCamera.setOnClickListener {
//            Toast.makeText(this, "Camera opened!", Toast.LENGTH_SHORT).show()
//            // TODO: Open camera activity
//        }
//    }
}

//package com.example.bitesight
//
//import android.os.Bundle
//import android.widget.Button
//import android.widget.LinearLayout
//import android.widget.ScrollView
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.lifecycleScope
//import com.example.bitesight.data.local.AppDatabase
//import com.example.bitesight.data.local.entity.Meal
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