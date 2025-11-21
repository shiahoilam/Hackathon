package com.example.bitesight

import android.os.Bundle

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {  // Bundle with capital B and add ?
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupBottomNavigation()
        setSelectedNavItem(R.id.nav_settings)
    }
}