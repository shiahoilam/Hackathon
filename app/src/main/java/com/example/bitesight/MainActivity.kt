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
import android.content.Intent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.Context

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

    // Constants retrieved from SettingsActivity for consistent access to SharedPreferences
    private val PREFS_NAME = SettingsActivity.PREFS_NAME
    private val KEY_CALORIE_GOAL = SettingsActivity.KEY_CALORIE_GOAL
    private val DEFAULT_CALORIE_GOAL = SettingsActivity.DEFAULT_CALORIE_GOAL

    // *** FIX: ADDED MISSING MACRO CONSTANTS AS PROPERTIES ***
    private val KEY_PROTEIN_GOAL = SettingsActivity.KEY_PROTEIN_GOAL
    private val KEY_CARBS_GOAL = SettingsActivity.KEY_CARBS_GOAL
    private val KEY_FAT_GOAL = SettingsActivity.KEY_FAT_GOAL
    private val DEFAULT_PROTEIN_GOAL = SettingsActivity.DEFAULT_PROTEIN_GOAL
    private val DEFAULT_CARBS_GOAL = SettingsActivity.DEFAULT_CARBS_GOAL
    private val DEFAULT_FAT_GOAL = SettingsActivity.DEFAULT_FAT_GOAL
    // ********************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupBottomNavigation()
        setSelectedNavItem(R.id.nav_home)

        // Add FAB camera button listener
        findViewById<FloatingActionButton>(R.id.fab_camera).setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
            finish()
        }

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
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Retrieve dynamic goals from SharedPreferences
        val goalCalories = prefs.getInt(KEY_CALORIE_GOAL, DEFAULT_CALORIE_GOAL)
        val goalProtein = prefs.getInt(KEY_PROTEIN_GOAL, DEFAULT_PROTEIN_GOAL).toFloat()
        val goalCarbs = prefs.getInt(KEY_CARBS_GOAL, DEFAULT_CARBS_GOAL).toFloat()
        val goalFat = prefs.getInt(KEY_FAT_GOAL, DEFAULT_FAT_GOAL).toFloat()

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
                    // Check for zero goal to prevent division by zero
                    val proteinProgress = if (goalProtein > 0) ((currentProtein / goalProtein) * 100).toInt().coerceIn(0, 100) else 0
                    proteinProgressBar.progress = proteinProgress
                }
            }
        }

        // Observe today's carbs
        lifecycleScope.launch {
            database.mealDao().getTodayTotalCarbs().collect { currentCarbs ->
                runOnUiThread {
                    tvCarbsValue.text = "${currentCarbs.toInt()}/${goalCarbs.toInt()}g"
                    // Check for zero goal to prevent division by zero
                    val carbsProgress = if (goalCarbs > 0) ((currentCarbs / goalCarbs) * 100).toInt().coerceIn(0, 100) else 0
                    carbsProgressBar.progress = carbsProgress
                }
            }
        }

        // Observe today's fat
        lifecycleScope.launch {
            database.mealDao().getTodayTotalFat().collect { currentFat ->
                runOnUiThread {
                    tvFatValue.text = "${currentFat.toInt()}/${goalFat.toInt()}g"
                    // Check for zero goal to prevent division by zero
                    val fatProgress = if (goalFat > 0) ((currentFat / goalFat) * 100).toInt().coerceIn(0, 100) else 0
                    fatProgressBar.progress = fatProgress
                }
            }
        }
    }
}