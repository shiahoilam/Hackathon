package com.example.bitesight

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class SettingsActivity : BaseActivity() {

    private lateinit var etCalorieGoal: EditText
    private lateinit var etProteinGoal: EditText
    private lateinit var etCarbsGoal: EditText
    private lateinit var etFatGoal: EditText
    private lateinit var btnSaveGoal: Button

    private lateinit var etUserName: EditText

    companion object {
        const val PREFS_NAME = "BiteSightPrefs"
        const val KEY_CALORIE_GOAL = "calorieGoal"
        const val KEY_PROTEIN_GOAL = "proteinGoal"
        const val KEY_CARBS_GOAL = "carbsGoal"
        const val KEY_FAT_GOAL = "fatGoal"
        const val KEY_USER_NAME = "userName"
        const val DEFAULT_USER_NAME = "User"

        const val DEFAULT_CALORIE_GOAL = 2000
        const val DEFAULT_PROTEIN_GOAL = 60
        const val DEFAULT_CARBS_GOAL = 200
        const val DEFAULT_FAT_GOAL = 65
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        etCalorieGoal = findViewById(R.id.etCalorieGoal)
        etProteinGoal = findViewById(R.id.etProteinGoal)
        etCarbsGoal = findViewById(R.id.etCarbsGoal)
        etFatGoal = findViewById(R.id.etFatGoal)
        btnSaveGoal = findViewById(R.id.btnSaveGoal)
        etUserName = findViewById(R.id.etUserName)

        setupBottomNavigation()
        setSelectedNavItem(R.id.nav_settings)

        loadGoals()
        setupSaveButton()
    }

    private fun loadGoals() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        etUserName.setText(prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME))
        etCalorieGoal.setText(prefs.getInt(KEY_CALORIE_GOAL, DEFAULT_CALORIE_GOAL).toString())
        etProteinGoal.setText(prefs.getInt(KEY_PROTEIN_GOAL, DEFAULT_PROTEIN_GOAL).toString())
        etCarbsGoal.setText(prefs.getInt(KEY_CARBS_GOAL, DEFAULT_CARBS_GOAL).toString())
        etFatGoal.setText(prefs.getInt(KEY_FAT_GOAL, DEFAULT_FAT_GOAL).toString())
    }

    private fun setupSaveButton() {
        btnSaveGoal.setOnClickListener {
            if (saveAllGoals()) {
                Toast.makeText(this, "Daily Goals saved **successfully**!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter **valid numbers** for all goals and a **name**.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveAllGoals(): Boolean {
        val userName = etUserName.text.toString().trim()
        val calorie = etCalorieGoal.text.toString().toIntOrNull()
        val protein = etProteinGoal.text.toString().toIntOrNull()
        val carbs = etCarbsGoal.text.toString().toIntOrNull()
        val fat = etFatGoal.text.toString().toIntOrNull()

        if (userName.isEmpty() || calorie == null || protein == null || carbs == null || fat == null ||
            calorie <= 0 || protein <= 0 || carbs <= 0 || fat <= 0) {
            return false
        }

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_USER_NAME, userName)
            putInt(KEY_CALORIE_GOAL, calorie)
            putInt(KEY_PROTEIN_GOAL, protein)
            putInt(KEY_CARBS_GOAL, carbs)
            putInt(KEY_FAT_GOAL, fat)
            apply()
        }
        return true
    }
}