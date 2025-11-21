package com.example.bitesight

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.bitesight.ui.mealhistory.MealHistoryActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.bitesight.R

open class BaseActivity : AppCompatActivity() {

    protected fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setOnItemSelectedListener { item ->
            handleNavigation(item.itemId)
        }
    }

    private fun handleNavigation(itemId: Int): Boolean {
        // Don't navigate if already on that page
        if (isCurrentActivity(itemId)) {
            return true
        }

        when (itemId) {
            R.id.nav_home -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return true
            }
            R.id.nav_analysis -> {
                startActivity(Intent(this, AnalysisActivity::class.java))
                finish()
                return true
            }
            R.id.nav_history -> {
                startActivity(Intent(this, MealHistoryActivity::class.java))
                finish()
                return true
            }
            R.id.nav_camera -> {
                startActivity(Intent(this, CameraActivity::class.java))
                finish()
                return true
            }
            else -> return false
        }
    }

    private fun isCurrentActivity(itemId: Int): Boolean {
        return when (itemId) {
            R.id.nav_home -> this is MainActivity
            R.id.nav_analysis -> this is AnalysisActivity
            R.id.nav_history -> this is MealHistoryActivity
            R.id.nav_camera -> this is CameraActivity
            else -> false
        }
    }

    protected fun setSelectedNavItem(itemId: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.selectedItemId = itemId
    }
}