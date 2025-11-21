package com.example.bitesight.ui.mealhistory

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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
import android.widget.ImageButton
import android.content.Context
import com.example.bitesight.SettingsActivity // Used to access SharedPreferences constants

class MealHistoryActivity : BaseActivity(), MealActionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MealAdapter
    private lateinit var repo: MealRepository
    private lateinit var tvTotalCalories: TextView
    private lateinit var btnToday: Button
    private lateinit var btnYesterday: Button
    private lateinit var btnSelectDate: Button

    // PROPERTIES for Calorie Balance
    private lateinit var tvCalorieBalance: TextView

    // Constants copied from SettingsActivity for consistent SharedPreferences access
    private val PREFS_NAME = SettingsActivity.PREFS_NAME
    private val KEY_CALORIE_GOAL = SettingsActivity.KEY_CALORIE_GOAL
    private val DEFAULT_CALORIE_GOAL = SettingsActivity.DEFAULT_CALORIE_GOAL

    private var currentFilter = FilterType.TODAY
    private var mealToRelog: Meal? = null
    private var lastCustomDateStart: Long? = null
    private var lastCustomDateEnd: Long? = null

    enum class FilterType {
        TODAY, YESTERDAY, CUSTOM
    }

    // *** NEW FUNCTION: Get the dynamic calorie goal from SharedPreferences ***
    private fun getCalorieGoal(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CALORIE_GOAL, DEFAULT_CALORIE_GOAL)
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

        tvCalorieBalance = findViewById(R.id.tvCalorieBalance)

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
        val currentGoal = getCalorieGoal() // <-- GET DYNAMIC GOAL HERE

        // 1. Update Total Logged Calories (Large text)
        tvTotalCalories.text = "${String.format("%,d", totalCalories.toInt())} kcal"

        // 2. Calculate and Display Balance
        val remaining = currentGoal - totalCalories.toInt()
        val balanceText: String
        val balanceColor: Int

        if (remaining >= 0) {
            // Under or met goal (Remaining)
            balanceText = "${String.format("%,d", remaining)} kcal remaining (Goal: ${String.format("%,d", currentGoal)})"
            balanceColor = ContextCompat.getColor(this, R.color.progress_green)
        } else {
            // Over goal (Exceeded)
            val exceeded = Math.abs(remaining)
            balanceText = "${String.format("%,d", exceeded)} kcal over (Goal: ${String.format("%,d", currentGoal)})"
            balanceColor = ContextCompat.getColor(this, R.color.warning_color)

            showOverGoalAdvice(exceeded)
        }

        tvCalorieBalance.text = balanceText
        tvCalorieBalance.setTextColor(balanceColor)
    }


    override fun onLogMealClicked(meal: Meal) {
        mealToRelog = meal
        showRelogDatePicker()
    }

    override fun onViewStatsClicked(meal: Meal) {
        showMealStatsDialog(meal)
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

    private fun showOverGoalAdvice(exceededAmount: Int) {
        val adviceList = if (exceededAmount <= 300) {
            listOf(
                "It happens! Focus on mindful eating tomorrow. Try setting a timer for 20 minutes to slow down.",
                "Choose a lighter dinner or snack next time, like fresh vegetables or a piece of fruit.",
                "Plan a 30-minute brisk walk today. Increased movement helps balance the extra energy."
            )
        } else {
            listOf(
                "Don't worry about yesterday; today is a new start! Focus on high-fiber, low-calorie foods today.",
                "Review your biggest meal from yesterday. Could you swap out a high-fat side for steamed vegetables next time?",
                "Ensure you're drinking enough water (8 glasses). Thirst is often mistaken for hunger.",
                "Incorporate a 45-minute moderate exercise session into your routine this week."
            )
        }

        val advice = adviceList.random()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_advice_warning, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val tvExceeded = dialogView.findViewById<TextView>(R.id.tvExceededAmount)
        val tvAdvice = dialogView.findViewById<TextView>(R.id.tvAdviceMessage)
        val closeButton = dialogView.findViewById<Button>(R.id.adviceCloseButton)
        val closeImageButton = dialogView.findViewById<ImageButton>(R.id.closeAdviceButton)

        tvTitle.text = "Goal Exceeded"
        tvExceeded.text = "⚠️ You are ${String.format("%,d", exceededAmount)} kcal over your goal."
        tvAdvice.text = "💡 $advice\n\nRemember, consistency matters more than perfection!"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        closeButton.setOnClickListener { dialog.dismiss() }
        closeImageButton.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
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

    private fun showMealStatsDialog(meal: Meal) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_meal_stats, null)

        val tvName = dialogView.findViewById<TextView>(R.id.dialogMealName)
        val tvCalories = dialogView.findViewById<TextView>(R.id.dialogCalories)
        val tvProtein = dialogView.findViewById<TextView>(R.id.dialogProteinGrams)
        val tvCarbs = dialogView.findViewById<TextView>(R.id.dialogCarbsGrams)
        val tvFat = dialogView.findViewById<TextView>(R.id.dialogFatGrams)
        val closeButton = dialogView.findViewById<Button>(R.id.dialogCloseButton)
        val closeImageButton = dialogView.findViewById<ImageButton>(R.id.closeStatsButton)


        tvName.text = meal.foodName
        tvCalories.text = "${meal.calories} kcal"
        tvProtein.text = String.format("%.1fg", meal.protein)
        tvCarbs.text = String.format("%.1fg", meal.carbs)
        tvFat.text = String.format("%.1fg", meal.fat)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        closeButton.setOnClickListener { dialog.dismiss() }
        closeImageButton.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }
}