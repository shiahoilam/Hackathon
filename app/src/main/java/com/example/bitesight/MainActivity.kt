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
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : BaseActivity() {
    private lateinit var database: AppDatabase

    private lateinit var tvCurrentCalories: TextView
    private lateinit var tvGoalCalories: TextView
    private lateinit var tvRemainingCalories: TextView
    private lateinit var tvProteinValue: TextView
    private lateinit var tvCarbsValue: TextView
    private lateinit var tvFatValue: TextView

    private lateinit var tvGreeting: TextView
    private lateinit var tvCurrentDate: TextView

    private lateinit var proteinProgressBar: ProgressBar
    private lateinit var carbsProgressBar: ProgressBar
    private lateinit var fatProgressBar: ProgressBar
    private lateinit var circularProgress: CircularProgressView

    private val PREFS_NAME = SettingsActivity.PREFS_NAME
    private val KEY_CALORIE_GOAL = SettingsActivity.KEY_CALORIE_GOAL
    private val DEFAULT_CALORIE_GOAL = SettingsActivity.DEFAULT_CALORIE_GOAL

    private val KEY_PROTEIN_GOAL = SettingsActivity.KEY_PROTEIN_GOAL
    private val KEY_CARBS_GOAL = SettingsActivity.KEY_CARBS_GOAL
    private val KEY_FAT_GOAL = SettingsActivity.KEY_FAT_GOAL
    private val DEFAULT_PROTEIN_GOAL = SettingsActivity.DEFAULT_PROTEIN_GOAL
    private val DEFAULT_CARBS_GOAL = SettingsActivity.DEFAULT_CARBS_GOAL
    private val DEFAULT_FAT_GOAL = SettingsActivity.DEFAULT_FAT_GOAL

    private val KEY_USER_NAME = SettingsActivity.KEY_USER_NAME
    private val DEFAULT_USER_NAME = SettingsActivity.DEFAULT_USER_NAME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupBottomNavigation()
        setSelectedNavItem(R.id.nav_home)

        findViewById<FloatingActionButton>(R.id.fab_camera).setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = AppDatabase.getDatabase(this)

        initializeViews()

        setupBottomNavigation()

        displayGreetingAndDate()

        observeDatabaseChanges()
    }

    override fun onResume() {
        super.onResume()
        displayGreetingAndDate()
    }

    private fun initializeViews() {
        tvCurrentCalories = findViewById(R.id.currentCalories)
        tvGoalCalories = findViewById(R.id.goalCalories)
        tvRemainingCalories = findViewById(R.id.remainingCalories)
        tvProteinValue = findViewById(R.id.proteinValue)
        tvCarbsValue = findViewById(R.id.carbsValue)
        tvFatValue = findViewById(R.id.fatValue)
        tvGreeting = findViewById(R.id.greeting)
        tvCurrentDate = findViewById(R.id.dateTxt)
        proteinProgressBar = findViewById(R.id.proteinProgress)
        carbsProgressBar = findViewById(R.id.carbsProgress)
        fatProgressBar = findViewById(R.id.fatProgress)
        circularProgress = findViewById(R.id.circularProgress)
    }

    private fun displayGreetingAndDate() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userName = prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val timeOfDay = when (hour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }

        tvGreeting.text = "Good $timeOfDay, $userName!"
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        tvCurrentDate.text = dateFormat.format(calendar.time)
    }

    private fun observeDatabaseChanges() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val goalCalories = prefs.getInt(KEY_CALORIE_GOAL, DEFAULT_CALORIE_GOAL)
        val goalProtein = prefs.getInt(KEY_PROTEIN_GOAL, DEFAULT_PROTEIN_GOAL).toFloat()
        val goalCarbs = prefs.getInt(KEY_CARBS_GOAL, DEFAULT_CARBS_GOAL).toFloat()
        val goalFat = prefs.getInt(KEY_FAT_GOAL, DEFAULT_FAT_GOAL).toFloat()

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

                    val progress = currentCalories.toFloat() / goalCalories
                    circularProgress.setProgress(progress)
                }
            }
        }

        lifecycleScope.launch {
            database.mealDao().getTodayTotalProtein().collect { currentProtein ->
                runOnUiThread {
                    tvProteinValue.text = String.format("%.1f/%.1fg", currentProtein, goalProtein)
                    val proteinProgress = if (goalProtein > 0) ((currentProtein / goalProtein) * 100).toInt().coerceIn(0, 100) else 0
                    proteinProgressBar.progress = proteinProgress
                }
            }
        }

        lifecycleScope.launch {
            database.mealDao().getTodayTotalCarbs().collect { currentCarbs ->
                runOnUiThread {
                    tvCarbsValue.text = String.format("%.1f/%.1fg", currentCarbs, goalCarbs)
                    val carbsProgress = if (goalCarbs > 0) ((currentCarbs / goalCarbs) * 100).toInt().coerceIn(0, 100) else 0
                    carbsProgressBar.progress = carbsProgress
                }
            }
        }

        lifecycleScope.launch {
            database.mealDao().getTodayTotalFat().collect { currentFat ->
                runOnUiThread {
                    tvFatValue.text = String.format("%.1f/%.1fg", currentFat, goalFat)
                    val fatProgress = if (goalFat > 0) ((currentFat / goalFat) * 100).toInt().coerceIn(0, 100) else 0
                    fatProgressBar.progress = fatProgress
                }
            }
        }
    }
}