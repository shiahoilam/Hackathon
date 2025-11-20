package com.example.bitesight

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bitesight.data.local.db.AppDatabase
import com.example.bitesight.data.local.entity.Meal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : BaseActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var swipeIndicator: LinearLayout
    private lateinit var analysisOverlay: CardView
    private lateinit var addToTrackerButton: Button
    private lateinit var overlayCalories: TextView
    private lateinit var overlayProteinProgress: ProgressBar
    private lateinit var overlayCarbsProgress: ProgressBar
    private lateinit var overlayFatProgress: ProgressBar
    private lateinit var overlayFoodImage: ImageView
    private lateinit var database: AppDatabase

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private var capturedImagePath: String? = null
    private var capturedTimestamp: Long = 0L

    private var isOverlayVisible = false
    private val swipeThreshold = 100f // Minimum swipe distance in pixels

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera)

        setupBottomNavigation()
        setSelectedNavItem(R.id.nav_camera)

        database = AppDatabase.getDatabase(this)

        previewView = findViewById(R.id.previewView)
        swipeIndicator = findViewById(R.id.swipeIndicator)
        analysisOverlay = findViewById(R.id.analysisOverlay)
        addToTrackerButton = findViewById(R.id.addToTrackerButton)
        overlayCalories = findViewById(R.id.overlayCalories)
        overlayProteinProgress = findViewById(R.id.overlayProteinProgress)
        overlayCarbsProgress = findViewById(R.id.overlayCarbsProgress)
        overlayFatProgress = findViewById(R.id.overlayFatProgress)
        overlayFoodImage = findViewById(R.id.overlayFoodImage)

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupOutputDirectory()

        setupSwipeGesture()
        setupOverlay()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupOutputDirectory() {
        outputDirectory = File(externalMediaDirs.firstOrNull() ?: filesDir, "FoodImages")
        outputDirectory.mkdirs()
    }

    private fun setupSwipeGesture() {
        var initialY = 0f
        var initialX = 0f
        
        val touchListener = View.OnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = event.y
                    initialX = event.x
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaY = initialY - event.y
                    val deltaX = initialX - event.x
                    
                    // Check if it's a vertical swipe (not horizontal)
                    if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > swipeThreshold) {
                        if (deltaY > 0 && !isOverlayVisible) {
                            // Swipe up detected - capture photo first
                            captureImage()
                            return@OnTouchListener true
                        } else if (deltaY < 0 && isOverlayVisible) {
                            // Swipe down detected
                            hideOverlay()
                            return@OnTouchListener true
                        }
                    }
                    false
                }
                else -> false
            }
        }

        swipeIndicator.setOnTouchListener { view, event ->
            if (!isOverlayVisible) {
                touchListener.onTouch(view, event)
            } else {
                false
            }
        }

        // Also allow swiping on the preview view
        previewView.setOnTouchListener { view, event ->
            if (!isOverlayVisible) {
                touchListener.onTouch(view, event)
            } else {
                false
            }
        }

        // Allow swiping down on the overlay to close it
        analysisOverlay.setOnTouchListener { view, event ->
            if (isOverlayVisible) {
                touchListener.onTouch(view, event)
            } else {
                false
            }
        }
    }

    private fun setupOverlay() {
        // Set initial state - overlay is hidden
        analysisOverlay.visibility = View.GONE
        analysisOverlay.alpha = 0f

        // Setup button click
        addToTrackerButton.setOnClickListener {
            saveMealsToTracker()
        }

        // Close button
        val closeButton = findViewById<View>(R.id.closeOverlayButton)
        closeButton?.setOnClickListener {
            hideOverlay()
        }
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: run {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity, "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedImagePath = photoFile.absolutePath
                    capturedTimestamp = System.currentTimeMillis() // Store the capture timestamp
                    displayCapturedImage(photoFile)
                    showOverlay()
                }
            }
        )
    }

    private fun displayCapturedImage(imageFile: File) {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        overlayFoodImage.setImageBitmap(bitmap)
    }

    private fun showOverlay() {
        if (isOverlayVisible) return
        isOverlayVisible = true

        analysisOverlay.visibility = View.VISIBLE
        analysisOverlay.alpha = 0f

        // Animate slide up from bottom
        val screenHeight = resources.displayMetrics.heightPixels
        analysisOverlay.translationY = screenHeight.toFloat()

        val animator = ValueAnimator.ofFloat(screenHeight.toFloat(), 0f)
        animator.duration = 300
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            analysisOverlay.translationY = value
            analysisOverlay.alpha = 1f - (value / screenHeight)
        }
        animator.start()

        // Update nutritional data (using mock data for now)
        updateNutritionalData()
    }

    private fun hideOverlay() {
        if (!isOverlayVisible) return
        isOverlayVisible = false

        val screenHeight = resources.displayMetrics.heightPixels

        val animator = ValueAnimator.ofFloat(0f, screenHeight.toFloat())
        animator.duration = 300
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            analysisOverlay.translationY = value
            analysisOverlay.alpha = value / screenHeight
            if (value >= screenHeight) {
                analysisOverlay.visibility = View.GONE
            }
        }
        animator.start()
    }

    private fun updateNutritionalData() {
        // Mock data - replace with actual detected food data
        val totalCalories = 2230
        val proteinPercent = 83
        val carbsPercent = 50
        val fatPercent = 63

        overlayCalories.text = "${String.format("%,d", totalCalories)} kcal"
        overlayProteinProgress.progress = proteinPercent
        overlayCarbsProgress.progress = carbsPercent
        overlayFatProgress.progress = fatPercent
    }

    private fun saveMealsToTracker() {
        lifecycleScope.launch {
            try {
                // Mock meals - replace with actual detected food data
                val meals = listOf(
                    Meal(
                        foodName = "Grilled Steak",
                        calories = 100,
                        protein = 30f,
                        carbs = 0f,
                        fat = 5f,
                        confidence = 0.85f,
                        imagePath = capturedImagePath,
                        createdAt = capturedTimestamp
                    ),
                    Meal(
                        foodName = "French Fries",
                        calories = 100,
                        protein = 30f,
                        carbs = 0f,
                        fat = 5f,
                        confidence = 0.85f,
                        imagePath = capturedImagePath,
                        createdAt = capturedTimestamp
                    )
                )

                withContext(Dispatchers.IO) {
                    meals.forEach { meal ->
                        database.mealDao().insertMeal(meal)
                    }
                }

                Toast.makeText(this@CameraActivity, "Meals added to tracker!", Toast.LENGTH_SHORT).show()
                hideOverlay()
            } catch (e: Exception) {
                Toast.makeText(this@CameraActivity, "Error saving meals: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

