package com.example.bitesight.ui.mealhistory

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.bitesight.BaseActivity
import com.example.bitesight.R
import com.example.bitesight.data.local.db.AppDatabase
import com.example.bitesight.data.local.entity.Meal
import com.example.bitesight.data.local.repo.MealRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MealHistoryActivity : BaseActivity(), MealActionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MealAdapter
    private lateinit var repo: MealRepository
    private lateinit var tvTotalCalories: TextView
    private lateinit var btnToday: Button
    private lateinit var btnYesterday: Button
    private lateinit var btnSelectDate: Button

    private var currentFilter = FilterType.TODAY
    private var mealToRelog: Meal? = null
    private var lastCustomDateStart: Long? = null
    private var lastCustomDateEnd: Long? = null

    enum class FilterType {
        TODAY, YESTERDAY, CUSTOM
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_history)

        setupBottomNavigation()
        setSelectedNavItem(R.id.nav_history)

        initViews()
        setupRepository()
        setupFilterButtons()
        setupSwipeToDelete()

        filterByToday()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.mealRecyclerView)
        tvTotalCalories = findViewById(R.id.tvTotalCalories)
        btnToday = findViewById(R.id.btnToday)
        btnYesterday = findViewById(R.id.btnYesterday)
        btnSelectDate = findViewById(R.id.btnSelectDate)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MealAdapter(emptyList(), this)
        recyclerView.adapter = adapter
    }

    private fun setupRepository() {
        val dao = AppDatabase.getDatabase(this).mealDao()
        repo = MealRepository(dao)

        lifecycleScope.launch {
            if (repo.countMeals() == 0) {
                insertSampleData()
            }
        }
    }

    private suspend fun insertSampleData() {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 30)
        }.timeInMillis

        repo.insert(Meal(foodName = "Grilled Salmon", calories = 650, protein = 30f, fat = 20f, carbs = 10f, imagePath = "salmon", createdAt = today))
        repo.insert(Meal(foodName = "Grilled Chicken", calories = 450, protein = 40f, fat = 5f, carbs = 3f, imagePath = "chicken", createdAt = today))
        repo.insert(Meal(foodName = "Grilled Steak", calories = 720, protein = 50f, fat = 30f, carbs = 0f, imagePath = "steak", createdAt = yesterday))
    }

    private fun setupFilterButtons() {
        btnToday.setOnClickListener { filterByToday() }
        btnYesterday.setOnClickListener { filterByYesterday() }
        btnSelectDate.setOnClickListener { showDatePicker() }
    }

    private fun filterByToday() {
        currentFilter = FilterType.TODAY
        updateButtonStates()

        val (start, end) = repo.getTodayDateRange()
        lifecycleScope.launch {
            repo.observeMealsByDateRange(start, end).collect { meals ->
                adapter.updateMeals(meals)
                updateTotalCalories(meals)
            }
        }
    }

    private fun filterByYesterday() {
        currentFilter = FilterType.YESTERDAY
        updateButtonStates()

        val (start, end) = repo.getYesterdayDateRange()
        lifecycleScope.launch {
            repo.observeMealsByDateRange(start, end).collect { meals ->
                adapter.updateMeals(meals)
                updateTotalCalories(meals)
            }
        }
    }

    private fun filterByCustomDate(year: Int, month: Int, day: Int) {
        currentFilter = FilterType.CUSTOM
        updateButtonStates()

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 0, 0, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis

        lastCustomDateStart = start
        lastCustomDateEnd = end

        lifecycleScope.launch {
            repo.observeMealsByDateRange(start, end).collect { meals ->
                adapter.updateMeals(meals)
                updateTotalCalories(meals)
            }
        }
    }
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            filterByCustomDate(selectedYear, selectedMonth, selectedDay)
        }, year, month, day)

        dialog.datePicker.maxDate = System.currentTimeMillis()

        dialog.show()
    }

    private fun updateButtonStates() {
        // Reset all buttons
        btnToday.setBackgroundResource(R.drawable.btn_filter)
        btnToday.setTextColor(resources.getColor(R.color.purple_600, null))
        btnYesterday.setBackgroundResource(R.drawable.btn_filter)
        btnYesterday.setTextColor(resources.getColor(R.color.purple_600, null))
        btnSelectDate.setBackgroundResource(R.drawable.btn_filter)
        btnSelectDate.setTextColor(resources.getColor(R.color.purple_600, null))

        // Highlight selected button
        when (currentFilter) {
            FilterType.TODAY -> {
                btnToday.setBackgroundResource(R.drawable.btn_filter_selected)
                btnToday.setTextColor(android.graphics.Color.WHITE)
            }
            FilterType.YESTERDAY -> {
                btnYesterday.setBackgroundResource(R.drawable.btn_filter_selected)
                btnYesterday.setTextColor(android.graphics.Color.WHITE)
            }
            FilterType.CUSTOM -> {
                btnSelectDate.setBackgroundResource(R.drawable.btn_filter_selected)
                btnSelectDate.setTextColor(android.graphics.Color.WHITE)
            }
        }
    }

    private fun updateTotalCalories(meals: List<Meal>) {
        val totalCalories = meals.sumOf { it.calories }
        tvTotalCalories.text = "$totalCalories Calories logged"
    }


    override fun onLogMealClicked(meal: Meal) {
        mealToRelog = meal
        showRelogDatePicker()
    }


    private fun showRelogDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerListener = DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->

            calendar.set(selectedYear, selectedMonth, selectedDay,
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                Calendar.getInstance().get(Calendar.MINUTE),
                Calendar.getInstance().get(Calendar.SECOND))
            val newTimestamp = calendar.timeInMillis

            relogMeal(newTimestamp)
        }

        val dialog = DatePickerDialog(this, datePickerListener, year, month, day)

        dialog.datePicker.maxDate = System.currentTimeMillis()

        dialog.show()
    }

    private fun relogMeal(newTimestamp: Long) {
        mealToRelog?.let { originalMeal ->
            val newMeal = originalMeal.copy(id = 0, createdAt = newTimestamp)

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repo.insert(newMeal)
                    }

                    val dateString = SimpleDateFormat("MMM dd, yyyy").format(Date(newTimestamp))
                    Toast.makeText(this@MealHistoryActivity, "Logged ${originalMeal.foodName} for $dateString", Toast.LENGTH_LONG).show()

                    refreshCurrentView()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@MealHistoryActivity, "Error logging meal: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    mealToRelog = null
                }
            }
        }
    }

    private fun refreshCurrentView() {
        when (currentFilter) {
            FilterType.TODAY -> filterByToday()
            FilterType.YESTERDAY -> filterByYesterday()
            FilterType.CUSTOM -> {
                if (lastCustomDateStart != null && lastCustomDateEnd != null) {
                    lifecycleScope.launch {
                        repo.observeMealsByDateRange(lastCustomDateStart!!, lastCustomDateEnd!!).collect { meals ->
                            adapter.updateMeals(meals)
                            updateTotalCalories(meals)
                        }
                    }
                }
            }
        }
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val mealToDelete = adapter.meals[position]
                    deleteMeal(mealToDelete)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun deleteMeal(meal: Meal) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repo.delete(meal)
                }

                Toast.makeText(this@MealHistoryActivity, "${meal.foodName} deleted.", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@MealHistoryActivity, "Error deleting meal: ${e.message}", Toast.LENGTH_SHORT).show()
                refreshCurrentView()
            }
        }
    }
}