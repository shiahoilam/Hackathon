package com.example.hackathon

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import android.widget.Button
import android.graphics.Color
import com.example.hackathon.data.local.AppDatabase
import com.example.hackathon.data.local.entity.Meal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AnalysisActivity : BaseActivity() {
    private lateinit var calorieChart: LineChart
    private lateinit var btnDaily: Button
    private lateinit var btnWeekly: Button
    private lateinit var btnMonthly: Button

    private var currentTimeline = Timeline.DAILY
    private val calorieGoal = 2000 // You can make this dynamic later

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_analysis)

        // Setup bottom navigation
        setupBottomNavigation()
        setSelectedNavItem(R.id.nav_analysis)

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        initViews()
        setupToggleButtons()
        loadChartData(Timeline.DAILY)
    }

    private fun initViews() {
        calorieChart = findViewById(R.id.calorieChart)
        btnDaily = findViewById(R.id.btnDaily)
        btnWeekly = findViewById(R.id.btnWeekly)
        btnMonthly = findViewById(R.id.btnMonthly)

        setupChart()
    }

    private fun setupToggleButtons() {
        btnDaily.setOnClickListener {
            selectTimeline(Timeline.DAILY, btnDaily)
        }

        btnWeekly.setOnClickListener {
            selectTimeline(Timeline.WEEKLY, btnWeekly)
        }

        btnMonthly.setOnClickListener {
            selectTimeline(Timeline.MONTHLY, btnMonthly)
        }
    }

    private fun selectTimeline(timeline: Timeline, selectedButton: Button) {
        currentTimeline = timeline

        // Reset all buttons
        btnDaily.setBackgroundResource(R.drawable.toggle_button_unselected)
        btnDaily.setTextColor(Color.parseColor("#5B6B9E"))

        btnWeekly.setBackgroundResource(R.drawable.toggle_button_unselected)
        btnWeekly.setTextColor(Color.parseColor("#5B6B9E"))

        btnMonthly.setBackgroundResource(R.drawable.toggle_button_unselected)
        btnMonthly.setTextColor(Color.parseColor("#5B6B9E"))

        // Highlight selected button
        selectedButton.setBackgroundResource(R.drawable.toggle_button_selected)
        selectedButton.setTextColor(Color.WHITE)

        // Load data for selected timeline
        loadChartData(timeline)
    }

    private fun setupChart() {
        calorieChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            legend.isEnabled = false

            // X-axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#1C1B1F")
                granularity = 1f
            }

            // Left Y-axis
            axisLeft.apply {
                textColor = Color.parseColor("#1C1B1F")
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
            }

            // Right Y-axis
            axisRight.isEnabled = false
        }
    }

    private fun loadChartData(timeline: Timeline) {
        lifecycleScope.launch {
            val meals = withContext(Dispatchers.IO) {
                // Get your database instance and fetch meals
                val db = AppDatabase.getDatabase(applicationContext)
                db.mealDao().getAllMeals()
            }

            val dataPoints = processDataByTimeline(meals, timeline)
            updateChart(dataPoints, timeline)
        }
    }

    private fun processDataByTimeline(meals: List<Meal>, timeline: Timeline): List<CalorieDataPoint> {
        val calendar = Calendar.getInstance()
        val dataMap = mutableMapOf<String, MutableList<Meal>>()

        when (timeline) {
            Timeline.DAILY -> {
                // Group by day of week (last 7 days)
                val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())

                for (i in 0..6) {
                    calendar.timeInMillis = System.currentTimeMillis()
                    calendar.add(Calendar.DAY_OF_YEAR, -i)
                    val dayLabel = dateFormat.format(calendar.time)
                    dataMap[dayLabel] = mutableListOf()
                }

                meals.forEach { meal ->
                    calendar.timeInMillis = meal.createdAt
                    val dayLabel = dateFormat.format(calendar.time)
                    dataMap[dayLabel]?.add(meal)
                }
            }

            Timeline.WEEKLY -> {
                // Group by week (last 12 weeks)
                for (i in 0..11) {
                    calendar.timeInMillis = System.currentTimeMillis()
                    calendar.add(Calendar.WEEK_OF_YEAR, -i)
                    val weekLabel = "W${calendar.get(Calendar.WEEK_OF_YEAR)}"
                    dataMap[weekLabel] = mutableListOf()
                }

                meals.forEach { meal ->
                    calendar.timeInMillis = meal.createdAt
                    val weekLabel = "W${calendar.get(Calendar.WEEK_OF_YEAR)}"
                    dataMap[weekLabel]?.add(meal)
                }
            }

            Timeline.MONTHLY -> {
                // Group by month (last 12 months)
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

                for (i in 0..11) {
                    calendar.timeInMillis = System.currentTimeMillis()
                    calendar.add(Calendar.MONTH, -i)
                    val monthLabel = monthFormat.format(calendar.time)
                    dataMap[monthLabel] = mutableListOf()
                }

                meals.forEach { meal ->
                    calendar.timeInMillis = meal.createdAt
                    val monthLabel = monthFormat.format(calendar.time)
                    dataMap[monthLabel]?.add(meal)
                }
            }
        }

        // Convert to data points
        return dataMap.map { (label, mealList) ->
            val totalCalories = mealList.sumOf { it.calories }
            val avgCalories = if (mealList.isNotEmpty()) totalCalories / mealList.size else 0
            CalorieDataPoint(label, avgCalories, mealList.firstOrNull()?.createdAt ?: 0L)
        }.sortedBy { it.timestamp }.reversed().take(7)
    }

    private fun updateChart(dataPoints: List<CalorieDataPoint>, timeline: Timeline) {
        val entries = dataPoints.mapIndexed { index, point ->
            Entry(index.toFloat(), point.calories.toFloat())
        }

        val labels = dataPoints.map { it.label }

        // Create dataset
        val dataSet = LineDataSet(entries, "Calories").apply {
            color = Color.parseColor("#8B7FB8")
            setCircleColor(Color.parseColor("#8B7FB8"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }

        // Add goal line
        val goalEntries = dataPoints.mapIndexed { index, _ ->
            Entry(index.toFloat(), calorieGoal.toFloat())
        }

        val goalDataSet = LineDataSet(goalEntries, "Goal").apply {
            color = Color.GRAY
            lineWidth = 1f
            setDrawCircles(false)
            setDrawValues(false)
            enableDashedLine(10f, 5f, 0f)
        }

        val lineData = LineData(dataSet, goalDataSet)
        calorieChart.data = lineData

        // Set X-axis labels
        calorieChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value.toInt() < labels.size) {
                    labels[value.toInt()]
                } else ""
            }
        }

        calorieChart.invalidate() // Refresh chart
    }
}