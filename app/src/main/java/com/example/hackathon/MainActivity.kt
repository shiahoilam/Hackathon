//
//package com.example.hackathon
//
//import android.Manifest
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.graphics.*
//import android.os.Bundle
//import android.view.View
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.example.hackathon.databinding.ActivityMainBinding
//import java.io.ByteArrayOutputStream
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private val CAMERA_REQUEST_CODE = 1001
//    private lateinit var modelHelper: TFLiteModelHelper
//
//    // optional: if you want to use smoothing later
//    private var lastBoxes = mutableListOf<RectF>()
//    private var lastLabels = mutableListOf<String>()
//    private val smoothingFactor = 0.5f
//
//    // prevent launching AR screen many times
//    private var appleArLaunched = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Button: start camera
//        binding.btnUseCamera.setOnClickListener {
//            if (hasCameraPermission()) {
//                // Hide buttons
//                binding.btnUseCamera.visibility = View.GONE
//                binding.btnTestImage.visibility = View.GONE
//                // Show camera preview and overlay
//                binding.previewView.visibility = View.VISIBLE
//                binding.overlayView.visibility = View.VISIBLE
//                // Start camera
//                openCamera()
//            } else {
//                requestCameraPermission()
//            }
//        }
//
//        // Button: test with static banana image
//        binding.btnTestImage.setOnClickListener {
//            binding.btnUseCamera.visibility = View.GONE
//            binding.btnTestImage.visibility = View.GONE
//            binding.testImageView.visibility = View.VISIBLE
//            testModelWithImage()
//        }
//    }
//
//    /** Check if camera permission is granted */
//    private fun hasCameraPermission(): Boolean {
//        return ContextCompat.checkSelfPermission(
//            this,
//            Manifest.permission.CAMERA
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    /** Request camera permission at runtime */
//    private fun requestCameraPermission() {
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(Manifest.permission.CAMERA),
//            CAMERA_REQUEST_CODE
//        )
//    }
//
//    /** Called when permission is granted, starts camera preview */
//    private fun openCamera() {
//        binding.btnUseCamera.visibility = View.GONE
//        binding.previewView.visibility = View.VISIBLE
//        binding.overlayView.visibility = View.VISIBLE
//        startCamera()
//    }
//
//    /** CameraX setup */
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//
//            val preview = Preview.Builder()
//                .build()
//                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
//
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            val imageAnalyzer = ImageAnalysis.Builder()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//                .also {
//                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
//                        analyzeImage(imageProxy)
//                    }
//                }
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    this,
//                    cameraSelector,
//                    preview,
//                    imageAnalyzer
//                )
//            } catch (exc: Exception) {
//                exc.printStackTrace()
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    /** Handle permission result */
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == CAMERA_REQUEST_CODE) {
//            if (grantResults.isNotEmpty() &&
//                grantResults[0] == PackageManager.PERMISSION_GRANTED
//            ) {
//                openCamera()
//            } else {
//                Toast.makeText(
//                    this,
//                    "Camera permission is required to use this feature",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
//
//    /**
//     * Run YOLO on a bitmap and return boxes + labels.
//     * This function ONLY detects and does NOT launch AR.
//     */
//    private fun runDetection(bitmap: Bitmap): Pair<List<RectF>, List<String>> {
//        // Get input shape
//        val inputShape = modelHelper.interpreter.getInputTensor(0).shape()
//        val height = inputShape[1]
//        val width = inputShape[2]
//
//        // Resize to model input
//        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//
//        // Prepare input array (NHWC)
//        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(3) } } }
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = inputBitmap.getPixel(x, y)
//                inputArray[0][y][x][0] = Color.red(pixel) / 255f
//                inputArray[0][y][x][1] = Color.green(pixel) / 255f
//                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
//            }
//        }
//
//        // Prepare output
//        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
//        val outputShape = outputTensor.shape()
//        val numAttributes = outputShape[1]  // 116
//        val numPredictions = outputShape[2] // 8400
//        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }
//
//        // Run inference
//        modelHelper.interpreter.run(inputArray, outputArray)
//
//        // COCO classes
//        val classNames = arrayOf(
//            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
//            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
//            "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
//            "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
//            "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
//            "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl",
//            "banana", "apple", "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza",
//            "donut", "cake", "chair", "couch", "potted plant", "bed", "dining table", "toilet",
//            "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave", "oven",
//            "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors",
//            "teddy bear", "hair drier", "toothbrush"
//        )
//
//        val boxes = mutableListOf<RectF>()
//        val labels = mutableListOf<String>()
//
//        for (i in 0 until numPredictions) {
//            val x = outputArray[0][0][i]
//            val y = outputArray[0][1][i]
//            val w = outputArray[0][2][i]
//            val h = outputArray[0][3][i]
//
//            val classScores = FloatArray(80) { classIdx ->
//                outputArray[0][4 + classIdx][i]
//            }
//
//            val (classIndex, maxScore) =
//                classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value }
//                    ?: (0 to 0f)
//
//            if (maxScore > 0.5f) {
//                val xPixel = x * binding.previewView.width
//                val yPixel = y * binding.previewView.height
//                val wPixel = w * binding.previewView.width
//                val hPixel = h * binding.previewView.height
//
//                val left = xPixel - wPixel / 2
//                val top = yPixel - hPixel / 2
//                val right = xPixel + wPixel / 2
//                val bottom = yPixel + hPixel / 2
//
//                boxes.add(RectF(left, top, right, bottom))
//
//                val className = classNames[classIndex]
//                val label = "$className ${(maxScore * 100).toInt()}%"
//                labels.add(label)
//            }
//        }
//
//        return boxes to labels
//    }
//
//    /** Decide whether to launch Apple AR screen based on labels */
//    private fun maybeLaunchAppleAr(labels: List<String>) {
//        if (appleArLaunched) return
//
//        // labels look like "apple 87%" so we use startsWith("apple")
//        val hasApple = labels.any { it.startsWith("apple", ignoreCase = true) }
//
//        if (hasApple) {
//            appleArLaunched = true
//            runOnUiThread {
//                startActivity(Intent(this, AppleARActivity::class.java))
//            }
//        }
//    }
//
//    /** Analyze each camera frame */
//    private fun analyzeImage(imageProxy: ImageProxy) {
//        if (!::modelHelper.isInitialized) {
//            modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//        }
//
//        val bitmap = imageProxy.toBitmap()
//        val (boxes, labels) = runDetection(bitmap)
//
//        // Update your overlay view
//        binding.overlayView.setResults(boxes, labels)
//
//        // NEW: launch AR screen once if apple detected
//        maybeLaunchAppleAr(labels)
//
//        imageProxy.close()
//    }
//
//    /** Test model on a static banana.jpeg from assets */
//    private fun testModelWithImage() {
//        val bitmap: Bitmap = assets.open("banana.jpeg").use {
//            BitmapFactory.decodeStream(it)
//        }
//
//        if (!::modelHelper.isInitialized) {
//            modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//        }
//
//        val inputTensor = modelHelper.interpreter.getInputTensor(0)
//        val inputShape = inputTensor.shape()
//        val isNHWC = inputShape.size == 4 && inputShape[3] == 3
//        val height = if (isNHWC) inputShape[1] else inputShape[2]
//        val width = if (isNHWC) inputShape[2] else inputShape[3]
//        val channels = if (isNHWC) inputShape[3] else inputShape[1]
//
//        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//
//        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(channels) } } }
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = inputBitmap.getPixel(x, y)
//                inputArray[0][y][x][0] = Color.red(pixel) / 255f
//                inputArray[0][y][x][1] = Color.green(pixel) / 255f
//                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
//            }
//        }
//
//        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
//        val outputShape = outputTensor.shape()
//        val numAttributes = outputShape[1]
//        val numPredictions = outputShape[2]
//        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }
//
//        modelHelper.interpreter.run(inputArray, outputArray)
//
//        val classNames = arrayOf(
//            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
//            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
//            "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
//            "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
//            "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
//            "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl",
//            "banana", "apple", "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza",
//            "donut", "cake", "chair", "couch", "potted plant", "bed", "dining table", "toilet",
//            "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave", "oven",
//            "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors",
//            "teddy bear", "hair drier", "toothbrush"
//        )
//
//        val canvas = Canvas(inputBitmap)
//        val boxPaint = Paint().apply {
//            color = Color.GREEN
//            style = Paint.Style.STROKE
//            strokeWidth = 6f
//            isAntiAlias = true
//        }
//        val textPaint = Paint().apply {
//            color = Color.WHITE
//            textSize = 40f
//            style = Paint.Style.FILL
//            isAntiAlias = true
//        }
//        val textBgPaint = Paint().apply {
//            color = Color.argb(180, 0, 0, 0)
//            style = Paint.Style.FILL
//        }
//
//        var detectionCount = 0
//
//        for (i in 0 until numPredictions) {
//            val x = outputArray[0][0][i]
//            val y = outputArray[0][1][i]
//            val w = outputArray[0][2][i]
//            val h = outputArray[0][3][i]
//
//            val classScores = FloatArray(80) { classIdx ->
//                outputArray[0][4 + classIdx][i]
//            }
//
//            val (classIndex, maxScore) =
//                classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value }
//                    ?: (0 to 0f)
//
//            if (maxScore > 0.5f) {
//                detectionCount++
//
//                val xPixel = x * inputBitmap.width
//                val yPixel = y * inputBitmap.height
//                val wPixel = w * inputBitmap.width
//                val hPixel = h * inputBitmap.height
//
//                val left = xPixel - wPixel / 2
//                val top = yPixel - hPixel / 2
//                val right = xPixel + wPixel / 2
//                val bottom = yPixel + hPixel / 2
//
//                canvas.drawRect(left, top, right, bottom, boxPaint)
//
//                val label = "${classNames[classIndex]} ${(maxScore * 100).toInt()}%"
//                val textWidth = textPaint.measureText(label)
//                canvas.drawRect(left, top - 50, left + textWidth + 20, top, textBgPaint)
//                canvas.drawText(label, left + 10, top - 10, textPaint)
//            }
//        }
//
//        runOnUiThread {
//            binding.testImageView.setImageBitmap(inputBitmap)
//            binding.debugTextView.text = "Detections: $detectionCount"
//            if (detectionCount == 0) {
//                Toast.makeText(
//                    this,
//                    "No objects detected above 50% confidence",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
//}
//
///** Convert ImageProxy (YUV) to Bitmap */
//private fun ImageProxy.toBitmap(): Bitmap {
//    val yBuffer = planes[0].buffer
//    val uBuffer = planes[1].buffer
//    val vBuffer = planes[2].buffer
//
//    val ySize = yBuffer.remaining()
//    val uSize = uBuffer.remaining()
//    val vSize = vBuffer.remaining()
//
//    val nv21 = ByteArray(ySize + uSize + vSize)
//
//    yBuffer.get(nv21, 0, ySize)
//    vBuffer.get(nv21, ySize, vSize)
//    uBuffer.get(nv21, ySize + vSize, uSize)
//
//    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
//    val out = ByteArrayOutputStream()
//    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
//    val imageBytes = out.toByteArray()
//    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//}






















//
//
//

//package com.example.hackathon
//
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.view.View
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.example.hackathon.databinding.ActivityMainBinding
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import androidx.camera.core.ImageProxy.PlaneProxy
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.RectF
//
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private val CAMERA_REQUEST_CODE = 1001
//    private lateinit var modelHelper: TFLiteModelHelper
//    private var lastBoxes = mutableListOf<RectF>()
//    private var lastLabels = mutableListOf<String>()
//    private val smoothingFactor =
//        0.5f  // how much to smooth movements (0 = no smoothing, 1 = instant)
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//// Add this in your onCreate method, after the existing buttons
//        binding.btnStartAR.setOnClickListener {
//            val intent = Intent(this, ARFoodDetectionActivity::class.java)
//            startActivity(intent)
//        }
//        // Button to start camera
//        binding.btnUseCamera.setOnClickListener {
//            if (hasCameraPermission()) {
//                // Hide buttons
//                binding.btnUseCamera.visibility = View.GONE
//                binding.btnTestImage.visibility = View.GONE
//                // Show camera preview and overlay
//                binding.previewView.visibility = View.VISIBLE
//                binding.overlayView.visibility = View.VISIBLE
//
//                // Start camera
//                openCamera()
//            } else {
//                requestCameraPermission()
//            }
//        }
//
//        // Button to test image
//        binding.btnTestImage.setOnClickListener {
//            // Hide buttons
//            binding.btnUseCamera.visibility = View.GONE
//            binding.btnTestImage.visibility = View.GONE
//            // Show test image
//            binding.testImageView.visibility = View.VISIBLE
//
//            // Run TFLite inference on test image
//            testModelWithImage()
//        }
//    }
//
//
//    /** Check if camera permission is granted */
//    private fun hasCameraPermission(): Boolean {
//        return ContextCompat.checkSelfPermission(
//            this,
//            android.Manifest.permission.CAMERA
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    /** Request camera permission at runtime */
//    private fun requestCameraPermission() {
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(android.Manifest.permission.CAMERA),
//            CAMERA_REQUEST_CODE
//        )
//    }
//
//    /** Called when permission is granted, starts camera preview */
//    private fun openCamera() {
//        // Hide the button
//        binding.btnUseCamera.visibility = View.GONE
//        // Show camera preview and overlay
//        binding.previewView.visibility = View.VISIBLE
//        binding.overlayView.visibility = View.VISIBLE
//        // Start CameraX
//        startCamera()
//    }
//
//    /** CameraX setup */
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//
//            val preview = Preview.Builder()
//                .build()
//                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
//
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            // Add ImageAnalysis for AI model
//            val imageAnalyzer = ImageAnalysis.Builder()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//                .also {
//                    it.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy ->
//                        analyzeImage(imageProxy)
//                    })
//                }
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
//            } catch (exc: Exception) {
//                exc.printStackTrace()
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    /** Handle permission result */
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == CAMERA_REQUEST_CODE) {
//            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                openCamera()
//            } else {
//                Toast.makeText(
//                    this,
//                    "Camera permission is required to use this feature",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
//
//
//
//    private fun runDetection(bitmap: Bitmap): Pair<List<RectF>, List<String>> {
//        // Resize bitmap to model input
//        val inputShape = modelHelper.interpreter.getInputTensor(0).shape()
//        val height = inputShape[1]
//        val width = inputShape[2]
//        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//
//        // Prepare input array (NHWC)
//        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(3) } } }
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = inputBitmap.getPixel(x, y)
//                inputArray[0][y][x][0] = Color.red(pixel) / 255f
//                inputArray[0][y][x][1] = Color.green(pixel) / 255f
//                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
//            }
//        }
//
//        // Prepare output array
//        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
//        val outputShape = outputTensor.shape()
//        val numAttributes = outputShape[1]
//        val numPredictions = outputShape[2]
//        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }
//
//        // Run inference
//        modelHelper.interpreter.run(inputArray, outputArray)
//
//        //        // 8️⃣ COCO classes
//        val classNames = arrayOf(
//            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
//            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
//            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
//            "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
//            "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
//            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
//            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
//            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
//            "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
//            "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
//        )
//        val boxes = mutableListOf<RectF>()
//        val labels = mutableListOf<String>()
//
//        for (i in 0 until numPredictions) {
//            val x = outputArray[0][0][i]
//            val y = outputArray[0][1][i]
//            val w = outputArray[0][2][i]
//            val h = outputArray[0][3][i]
//
//            val classScores = FloatArray(80) { classIdx -> outputArray[0][4 + classIdx][i] }
//            val (classIndex, maxScore) = classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value } ?: (0 to 0f)
//
//            if (maxScore > 0.5f) {
//                val xPixel = x * binding.previewView.width
//                val yPixel = y * binding.previewView.height
//                val wPixel = w * binding.previewView.width
//                val hPixel = h * binding.previewView.height
//
//                boxes.add(RectF(
//                    xPixel - wPixel / 2,
//                    yPixel - hPixel / 2,
//                    xPixel + wPixel / 2,
//                    yPixel + hPixel / 2
//                ))
//
//                labels.add("${classNames[classIndex]} ${(maxScore * 100).toInt()}%")
//            }
//        }
//
//        return boxes to labels
//    }
//
//    private fun analyzeImage(imageProxy: ImageProxy) {
//        if (!::modelHelper.isInitialized) {
//            modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//        }
//
//        val bitmap = imageProxy.toBitmap()
//        val (boxes, labels) = runDetection(bitmap)
//
//        // Update overlay
//        binding.overlayView.setResults(boxes, labels)
//
//        imageProxy.close()
//    }
//
//
//
//    private fun testModelWithImage() {
//        // 1️⃣ Load test image from assets
//        val bitmap: Bitmap = assets.open("banana.jpeg").use {
//            BitmapFactory.decodeStream(it)
//        }
//
//        // 2️⃣ Initialize model if not yet
//        if (!::modelHelper.isInitialized) {
//            modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//        }
//
//        // 3️⃣ Get model input shape
//        val inputTensor = modelHelper.interpreter.getInputTensor(0)
//        val inputShape = inputTensor.shape()
//        val isNHWC = inputShape.size == 4 && inputShape[3] == 3
//        val height = if (isNHWC) inputShape[1] else inputShape[2]
//        val width = if (isNHWC) inputShape[2] else inputShape[3]
//        val channels = if (isNHWC) inputShape[3] else inputShape[1]
//
//        // 4️⃣ Resize bitmap to model input
//        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//
//        // 5️⃣ Prepare input array (NHWC)
//        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(channels) } } }
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = inputBitmap.getPixel(x, y)
//                inputArray[0][y][x][0] = Color.red(pixel) / 255f
//                inputArray[0][y][x][1] = Color.green(pixel) / 255f
//                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
//            }
//        }
//
//        // 6️⃣ Prepare output array
//        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
//        val outputShape = outputTensor.shape()
//        val numAttributes = outputShape[1] // 116
//        val numPredictions = outputShape[2] // 8400
//        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }
//
//        // 7️⃣ Run inference
//        modelHelper.interpreter.run(inputArray, outputArray)
//
////        // 8️⃣ COCO classes
//        val classNames = arrayOf(
//            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
//            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
//            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
//            "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
//            "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
//            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
//            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
//            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
//            "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
//            "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
//        )
//
//        // 9️⃣ Draw on canvas
//        val canvas = Canvas(inputBitmap)
//        val boxPaint = Paint().apply {
//            color = Color.GREEN
//            style = Paint.Style.STROKE
//            strokeWidth = 6f
//            isAntiAlias = true
//        }
//        val textPaint = Paint().apply {
//            color = Color.WHITE
//            textSize = 40f
//            style = Paint.Style.FILL
//            isAntiAlias = true
//        }
//        val textBgPaint = Paint().apply {
//            color = Color.argb(180, 0, 0, 0) // semi-transparent black
//            style = Paint.Style.FILL
//        }
//
//        var detectionCount = 0
//
//        // 10️⃣ Parse predictions
//        for (i in 0 until numPredictions) {
//            val x = outputArray[0][0][i]   // x center
//            val y = outputArray[0][1][i]   // y center
//            val w = outputArray[0][2][i]   // width
//            val h = outputArray[0][3][i]   // height
//
//            // Class scores
//            val classScores = FloatArray(80) { classIdx -> outputArray[0][4 + classIdx][i] }
//            val (classIndex, maxScore) = classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value } ?: (0 to 0f)
//
//            if (maxScore > 0.5f) {
//                detectionCount++
//
//                val xPixel = x * inputBitmap.width
//                val yPixel = y * inputBitmap.height
//                val wPixel = w * inputBitmap.width
//                val hPixel = h * inputBitmap.height
//
//                val left = xPixel - wPixel / 2
//                val top = yPixel - hPixel / 2
//                val right = xPixel + wPixel / 2
//                val bottom = yPixel + hPixel / 2
//
//                // Draw bounding box
//                canvas.drawRect(left, top, right, bottom, boxPaint)
//
//                // Draw label background
//                val label = "${classNames[classIndex]} ${(maxScore * 100).toInt()}%"
//                val textWidth = textPaint.measureText(label)
//                canvas.drawRect(left, top - 50, left + textWidth + 20, top, textBgPaint)
//
//                // Draw label text
//                canvas.drawText(label, left + 10, top - 10, textPaint)
//            }
//        }
//
//        // 11️⃣ Display result
//        runOnUiThread {
//            binding.testImageView.setImageBitmap(inputBitmap)
//            binding.debugTextView.text = "Detections: $detectionCount"
//
//            if (detectionCount == 0) {
//                Toast.makeText(this, "No objects detected above 50% confidence", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//
//}
//
//
//
//
//
//private fun ImageProxy.toBitmap(): Bitmap {
//    val yBuffer = planes[0].buffer
//    val uBuffer = planes[1].buffer
//    val vBuffer = planes[2].buffer
//
//    val ySize = yBuffer.remaining()
//    val uSize = uBuffer.remaining()
//    val vSize = vBuffer.remaining()
//
//    val nv21 = ByteArray(ySize + uSize + vSize)
//
//    yBuffer.get(nv21, 0, ySize)
//    vBuffer.get(nv21, ySize, vSize)
//    uBuffer.get(nv21, ySize + vSize, uSize)
//
//    val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
//    val out = java.io.ByteArrayOutputStream()
//    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
//    val imageBytes = out.toByteArray()
//    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//}







//
//package com.example.hackathon
//
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.view.View
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.example.hackathon.databinding.ActivityMainBinding
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.RectF
//import android.opengl.GLSurfaceView
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private val CAMERA_REQUEST_CODE = 1001
//    private lateinit var modelHelper: TFLiteModelHelper
//    private lateinit var glSurfaceView: GLSurfaceView
//    private lateinit var renderer: Model3DRenderer
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Initialize OpenGL Surface View
//        glSurfaceView = GLSurfaceView(this)
//        glSurfaceView.setEGLContextClientVersion(2)
//        renderer = Model3DRenderer(this)
//        glSurfaceView.setRenderer(renderer)
//        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
//        glSurfaceView.visibility = View.GONE
//
//        // Add GLSurfaceView to layout (it should be on top of preview)
//        binding.root.addView(glSurfaceView)
//
//        // Button to start camera
//        binding.btnUseCamera.setOnClickListener {
//            if (hasCameraPermission()) {
//                // Hide buttons
//                binding.btnUseCamera.visibility = View.GONE
//                binding.btnTestImage.visibility = View.GONE
//                // Show camera preview, overlay, and 3D view
//                binding.previewView.visibility = View.VISIBLE
//                binding.overlayView.visibility = View.VISIBLE
//                glSurfaceView.visibility = View.VISIBLE
//
//                // Start camera
//                openCamera()
//            } else {
//                requestCameraPermission()
//            }
//        }
//
//        // Button to test image
//        binding.btnTestImage.setOnClickListener {
//            // Hide buttons
//            binding.btnUseCamera.visibility = View.GONE
//            binding.btnTestImage.visibility = View.GONE
//            // Show test image
//            binding.testImageView.visibility = View.VISIBLE
//
//            // Run TFLite inference on test image
//            testModelWithImage()
//        }
//    }
//
//    private fun hasCameraPermission(): Boolean {
//        return ContextCompat.checkSelfPermission(
//            this,
//            android.Manifest.permission.CAMERA
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun requestCameraPermission() {
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(android.Manifest.permission.CAMERA),
//            CAMERA_REQUEST_CODE
//        )
//    }
//
//    private fun openCamera() {
//        binding.btnUseCamera.visibility = View.GONE
//        binding.previewView.visibility = View.VISIBLE
//        binding.overlayView.visibility = View.VISIBLE
//        glSurfaceView.visibility = View.VISIBLE
//        startCamera()
//    }
//
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//
//            val preview = Preview.Builder()
//                .build()
//                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
//
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            val imageAnalyzer = ImageAnalysis.Builder()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//                .also {
//                    it.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy ->
//                        analyzeImage(imageProxy)
//                    })
//                }
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
//            } catch (exc: Exception) {
//                exc.printStackTrace()
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == CAMERA_REQUEST_CODE) {
//            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                openCamera()
//            } else {
//                Toast.makeText(
//                    this,
//                    "Camera permission is required to use this feature",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
//
//    private fun runDetection(bitmap: Bitmap): Pair<List<RectF>, List<String>> {
//        val inputShape = modelHelper.interpreter.getInputTensor(0).shape()
//        val height = inputShape[1]
//        val width = inputShape[2]
//        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//
//        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(3) } } }
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = inputBitmap.getPixel(x, y)
//                inputArray[0][y][x][0] = Color.red(pixel) / 255f
//                inputArray[0][y][x][1] = Color.green(pixel) / 255f
//                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
//            }
//        }
//
//        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
//        val outputShape = outputTensor.shape()
//        val numAttributes = outputShape[1]
//        val numPredictions = outputShape[2]
//        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }
//
//        modelHelper.interpreter.run(inputArray, outputArray)
//
//        val classNames = arrayOf(
//            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
//            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
//            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
//            "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
//            "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
//            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
//            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
//            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
//            "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
//            "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
//        )
//
//        val boxes = mutableListOf<RectF>()
//        val labels = mutableListOf<String>()
//
//        for (i in 0 until numPredictions) {
//            val x = outputArray[0][0][i]
//            val y = outputArray[0][1][i]
//            val w = outputArray[0][2][i]
//            val h = outputArray[0][3][i]
//
//            val classScores = FloatArray(80) { classIdx -> outputArray[0][4 + classIdx][i] }
//            val (classIndex, maxScore) = classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value } ?: (0 to 0f)
//
//            if (maxScore > 0.5f) {
//                val xPixel = x * binding.previewView.width
//                val yPixel = y * binding.previewView.height
//                val wPixel = w * binding.previewView.width
//                val hPixel = h * binding.previewView.height
//
//                boxes.add(RectF(
//                    xPixel - wPixel / 2,
//                    yPixel - hPixel / 2,
//                    xPixel + wPixel / 2,
//                    yPixel + hPixel / 2
//                ))
//
//                labels.add("${classNames[classIndex]} ${(maxScore * 100).toInt()}%")
//            }
//        }
//
//        return boxes to labels
//    }
//
//    private fun analyzeImage(imageProxy: ImageProxy) {
//        if (!::modelHelper.isInitialized) {
//            modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//        }
//
//        val bitmap = imageProxy.toBitmap()
//        val (boxes, labels) = runDetection(bitmap)
//
//        // Update overlay
//        binding.overlayView.setResults(boxes, labels)
//
//        // Update 3D renderer with detected boxes
//        runOnUiThread {
//            renderer.detectedBoxes = boxes
//            renderer.surfaceWidth = binding.previewView.width
//            renderer.surfaceHeight = binding.previewView.height
//            glSurfaceView.requestRender()
//        }
//
//        imageProxy.close()
//    }
//
//    private fun testModelWithImage() {
//        val bitmap: Bitmap = assets.open("banana.jpeg").use {
//            BitmapFactory.decodeStream(it)
//        }
//
//        if (!::modelHelper.isInitialized) {
//            modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//        }
//
//        val inputTensor = modelHelper.interpreter.getInputTensor(0)
//        val inputShape = inputTensor.shape()
//        val isNHWC = inputShape.size == 4 && inputShape[3] == 3
//        val height = if (isNHWC) inputShape[1] else inputShape[2]
//        val width = if (isNHWC) inputShape[2] else inputShape[3]
//        val channels = if (isNHWC) inputShape[3] else inputShape[1]
//
//        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//
//        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(channels) } } }
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = inputBitmap.getPixel(x, y)
//                inputArray[0][y][x][0] = Color.red(pixel) / 255f
//                inputArray[0][y][x][1] = Color.green(pixel) / 255f
//                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
//            }
//        }
//
//        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
//        val outputShape = outputTensor.shape()
//        val numAttributes = outputShape[1]
//        val numPredictions = outputShape[2]
//        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }
//
//        modelHelper.interpreter.run(inputArray, outputArray)
//
//        val classNames = arrayOf(
//            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
//            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
//            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
//            "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
//            "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
//            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
//            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
//            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
//            "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
//            "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
//        )
//
//        val canvas = Canvas(inputBitmap)
//        val boxPaint = Paint().apply {
//            color = Color.GREEN
//            style = Paint.Style.STROKE
//            strokeWidth = 6f
//            isAntiAlias = true
//        }
//        val textPaint = Paint().apply {
//            color = Color.WHITE
//            textSize = 40f
//            style = Paint.Style.FILL
//            isAntiAlias = true
//        }
//        val textBgPaint = Paint().apply {
//            color = Color.argb(180, 0, 0, 0)
//            style = Paint.Style.FILL
//        }
//
//        var detectionCount = 0
//
//        for (i in 0 until numPredictions) {
//            val x = outputArray[0][0][i]
//            val y = outputArray[0][1][i]
//            val w = outputArray[0][2][i]
//            val h = outputArray[0][3][i]
//
//            val classScores = FloatArray(80) { classIdx -> outputArray[0][4 + classIdx][i] }
//            val (classIndex, maxScore) = classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value } ?: (0 to 0f)
//
//            if (maxScore > 0.5f) {
//                detectionCount++
//
//                val xPixel = x * inputBitmap.width
//                val yPixel = y * inputBitmap.height
//                val wPixel = w * inputBitmap.width
//                val hPixel = h * inputBitmap.height
//
//                val left = xPixel - wPixel / 2
//                val top = yPixel - hPixel / 2
//                val right = xPixel + wPixel / 2
//                val bottom = yPixel + hPixel / 2
//
//                canvas.drawRect(left, top, right, bottom, boxPaint)
//
//                val label = "${classNames[classIndex]} ${(maxScore * 100).toInt()}%"
//                val textWidth = textPaint.measureText(label)
//                canvas.drawRect(left, top - 50, left + textWidth + 20, top, textBgPaint)
//                canvas.drawText(label, left + 10, top - 10, textPaint)
//            }
//        }
//
//        runOnUiThread {
//            binding.testImageView.setImageBitmap(inputBitmap)
//            binding.debugTextView.text = "Detections: $detectionCount"
//
//            if (detectionCount == 0) {
//                Toast.makeText(this, "No objects detected above 50% confidence", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        glSurfaceView.onPause()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        glSurfaceView.onResume()
//    }
//}
//
//private fun ImageProxy.toBitmap(): Bitmap {
//    val yBuffer = planes[0].buffer
//    val uBuffer = planes[1].buffer
//    val vBuffer = planes[2].buffer
//
//    val ySize = yBuffer.remaining()
//    val uSize = uBuffer.remaining()
//    val vSize = vBuffer.remaining()
//
//    val nv21 = ByteArray(ySize + uSize + vSize)
//
//    yBuffer.get(nv21, 0, ySize)
//    vBuffer.get(nv21, ySize, vSize)
//    uBuffer.get(nv21, ySize + vSize, uSize)
//
//    val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
//    val out = java.io.ByteArrayOutputStream()
//    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
//    val imageBytes = out.toByteArray()
//    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//}
















//
//package com.example.hackathon
//
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.view.View
//import android.widget.FrameLayout
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.example.hackathon.databinding.ActivityMainBinding
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.RectF
//import android.opengl.GLSurfaceView
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private val CAMERA_REQUEST_CODE = 1001
//    private lateinit var modelHelper: TFLiteModelHelper
//    private lateinit var glSurfaceView: GLSurfaceView
//    private lateinit var renderer: Model3DRenderer
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Button to start camera
//        binding.btnUseCamera.setOnClickListener {
//            if (hasCameraPermission()) {
//                initializeGLSurfaceView()
//                // Hide buttons
//                binding.btnUseCamera.visibility = View.GONE
//                binding.btnTestImage.visibility = View.GONE
//                // Show camera preview and overlay
//                binding.previewView.visibility = View.VISIBLE
//                binding.overlayView.visibility = View.VISIBLE
//                glSurfaceView.visibility = View.VISIBLE
//
//                // Start camera
//                openCamera()
//            } else {
//                requestCameraPermission()
//            }
//        }
//
//        // Button to test image
//        binding.btnTestImage.setOnClickListener {
//            // Hide buttons
//            binding.btnUseCamera.visibility = View.GONE
//            binding.btnTestImage.visibility = View.GONE
//            // Show test image
//            binding.testImageView.visibility = View.VISIBLE
//
//            // Run TFLite inference on test image
//            testModelWithImage()
//        }
//    }
//
//    private fun initializeGLSurfaceView() {
//        if (!::glSurfaceView.isInitialized) {
//            glSurfaceView = GLSurfaceView(this)
//            glSurfaceView.setEGLContextClientVersion(2)
//            glSurfaceView.setZOrderOnTop(true) // Make it overlay on top
//            glSurfaceView.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT) // Make background transparent
//
//            renderer = Model3DRenderer(this)
//            glSurfaceView.setRenderer(renderer)
//            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
//
//            // Add to root layout
//            val params = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT
//            )
//            binding.root.addView(glSurfaceView, params)
//            glSurfaceView.visibility = View.GONE
//        }
//    }
//
//    private fun hasCameraPermission(): Boolean {
//        return ContextCompat.checkSelfPermission(
//            this,
//            android.Manifest.permission.CAMERA
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun requestCameraPermission() {
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(android.Manifest.permission.CAMERA),
//            CAMERA_REQUEST_CODE
//        )
//    }
//    private var isCameraOpened = false
//    private fun openCamera() {
//        if (!isCameraOpened) {
//            startCamera()
//            isCameraOpened = true
//        }
//    }
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//
//            val preview = Preview.Builder()
//                .build()
//                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
//
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            val imageAnalyzer = ImageAnalysis.Builder()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//                .also {
//                    it.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy ->
//                        analyzeImage(imageProxy)
//                    })
//                }
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
//            } catch (exc: Exception) {
//                exc.printStackTrace()
//                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == CAMERA_REQUEST_CODE) {
//            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                initializeGLSurfaceView()
//                openCamera()
//            } else {
//                Toast.makeText(
//                    this,
//                    "Camera permission is required to use this feature",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
//
//    private fun runDetection(bitmap: Bitmap): Pair<List<RectF>, List<String>> {
//        val inputShape = modelHelper.interpreter.getInputTensor(0).shape()
//        val height = inputShape[1]
//        val width = inputShape[2]
//        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//
//        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(3) } } }
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = inputBitmap.getPixel(x, y)
//                inputArray[0][y][x][0] = Color.red(pixel) / 255f
//                inputArray[0][y][x][1] = Color.green(pixel) / 255f
//                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
//            }
//        }
//
//        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
//        val outputShape = outputTensor.shape()
//        val numAttributes = outputShape[1]
//        val numPredictions = outputShape[2]
//        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }
//
//        modelHelper.interpreter.run(inputArray, outputArray)
//
//        val classNames = arrayOf(
//            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
//            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
//            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
//            "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
//            "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
//            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
//            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
//            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
//            "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
//            "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
//        )
//
//        val boxes = mutableListOf<RectF>()
//        val labels = mutableListOf<String>()
//
//        for (i in 0 until numPredictions) {
//            val x = outputArray[0][0][i]
//            val y = outputArray[0][1][i]
//            val w = outputArray[0][2][i]
//            val h = outputArray[0][3][i]
//
//            val classScores = FloatArray(80) { classIdx -> outputArray[0][4 + classIdx][i] }
//            val (classIndex, maxScore) = classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value } ?: (0 to 0f)
//
//            if (maxScore > 0.5f) {
//                val xPixel = x * binding.previewView.width
//                val yPixel = y * binding.previewView.height
//                val wPixel = w * binding.previewView.width
//                val hPixel = h * binding.previewView.height
//
//                boxes.add(RectF(
//                    xPixel - wPixel / 2,
//                    yPixel - hPixel / 2,
//                    xPixel + wPixel / 2,
//                    yPixel + hPixel / 2
//                ))
//
//                labels.add("${classNames[classIndex]} ${(maxScore * 100).toInt()}%")
//            }
//        }
//
//        return boxes to labels
//    }
//
//    private fun analyzeImage(imageProxy: ImageProxy) {
//        if (!::modelHelper.isInitialized) {
//            modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//        }
//
//        val bitmap = imageProxy.toBitmap()
//        val (boxes, labels) = runDetection(bitmap)
//
//        // Update overlay
//        binding.overlayView.setResults(boxes, labels)
//
//        // Update 3D renderer with detected boxes
//        if (::renderer.isInitialized && boxes.isNotEmpty()) {
//            runOnUiThread {
//                renderer.detectedBoxes = boxes
//                renderer.surfaceWidth = binding.previewView.width
//                renderer.surfaceHeight = binding.previewView.height
//            }
//        }
//
//        imageProxy.close()
//    }
//
//    private fun testModelWithImage() {
//        val bitmap: Bitmap = assets.open("banana.jpeg").use {
//            BitmapFactory.decodeStream(it)
//        }
//
//        if (!::modelHelper.isInitialized) {
//            modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//        }
//
//        val inputTensor = modelHelper.interpreter.getInputTensor(0)
//        val inputShape = inputTensor.shape()
//        val isNHWC = inputShape.size == 4 && inputShape[3] == 3
//        val height = if (isNHWC) inputShape[1] else inputShape[2]
//        val width = if (isNHWC) inputShape[2] else inputShape[3]
//        val channels = if (isNHWC) inputShape[3] else inputShape[1]
//
//        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//
//        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(channels) } } }
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = inputBitmap.getPixel(x, y)
//                inputArray[0][y][x][0] = Color.red(pixel) / 255f
//                inputArray[0][y][x][1] = Color.green(pixel) / 255f
//                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
//            }
//        }
//
//        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
//        val outputShape = outputTensor.shape()
//        val numAttributes = outputShape[1]
//        val numPredictions = outputShape[2]
//        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }
//
//        modelHelper.interpreter.run(inputArray, outputArray)
//
//        val classNames = arrayOf(
//            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
//            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
//            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
//            "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
//            "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
//            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
//            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
//            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
//            "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
//            "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
//        )
//
//        val canvas = Canvas(inputBitmap)
//        val boxPaint = Paint().apply {
//            color = Color.GREEN
//            style = Paint.Style.STROKE
//            strokeWidth = 6f
//            isAntiAlias = true
//        }
//        val textPaint = Paint().apply {
//            color = Color.WHITE
//            textSize = 40f
//            style = Paint.Style.FILL
//            isAntiAlias = true
//        }
//        val textBgPaint = Paint().apply {
//            color = Color.argb(180, 0, 0, 0)
//            style = Paint.Style.FILL
//        }
//
//        var detectionCount = 0
//
//        for (i in 0 until numPredictions) {
//            val x = outputArray[0][0][i]
//            val y = outputArray[0][1][i]
//            val w = outputArray[0][2][i]
//            val h = outputArray[0][3][i]
//
//            val classScores = FloatArray(80) { classIdx -> outputArray[0][4 + classIdx][i] }
//            val (classIndex, maxScore) = classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value } ?: (0 to 0f)
//
//            if (maxScore > 0.5f) {
//                detectionCount++
//
//                val xPixel = x * inputBitmap.width
//                val yPixel = y * inputBitmap.height
//                val wPixel = w * inputBitmap.width
//                val hPixel = h * inputBitmap.height
//
//                val left = xPixel - wPixel / 2
//                val top = yPixel - hPixel / 2
//                val right = xPixel + wPixel / 2
//                val bottom = yPixel + hPixel / 2
//
//                canvas.drawRect(left, top, right, bottom, boxPaint)
//
//                val label = "${classNames[classIndex]} ${(maxScore * 100).toInt()}%"
//                val textWidth = textPaint.measureText(label)
//                canvas.drawRect(left, top - 50, left + textWidth + 20, top, textBgPaint)
//                canvas.drawText(label, left + 10, top - 10, textPaint)
//            }
//        }
//
//        runOnUiThread {
//            binding.testImageView.setImageBitmap(inputBitmap)
//            binding.debugTextView.text = "Detections: $detectionCount"
//
//            if (detectionCount == 0) {
//                Toast.makeText(this, "No objects detected above 50% confidence", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        if (::glSurfaceView.isInitialized) {
//            glSurfaceView.onPause()
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (::glSurfaceView.isInitialized) {
//            glSurfaceView.onResume()
//        }
//    }
//}
//
//private fun ImageProxy.toBitmap(): Bitmap {
//    val yBuffer = planes[0].buffer
//    val uBuffer = planes[1].buffer
//    val vBuffer = planes[2].buffer
//
//    val ySize = yBuffer.remaining()
//    val uSize = uBuffer.remaining()
//    val vSize = vBuffer.remaining()
//
//    val nv21 = ByteArray(ySize + uSize + vSize)
//
//    yBuffer.get(nv21, 0, ySize)
//    vBuffer.get(nv21, ySize, vSize)
//    uBuffer.get(nv21, ySize + vSize, uSize)
//
//    val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
//    val out = java.io.ByteArrayOutputStream()
//    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
//    val imageBytes = out.toByteArray()
//    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//}









//
//
//package com.example.hackathon
//
//import android.content.pm.PackageManager
//import android.graphics.*
//import android.os.Bundle
//import android.view.View
//import android.widget.FrameLayout
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.*
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.example.hackathon.databinding.ActivityMainBinding
//import android.opengl.GLSurfaceView
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private val CAMERA_REQUEST_CODE = 1001
//
//    private lateinit var modelHelper: TFLiteModelHelper
//    private lateinit var glSurfaceView: GLSurfaceView
//    private lateinit var renderer: Model3DRenderer
//
//    private var isCameraOpened = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        binding.btnUseCamera.setOnClickListener {
//            if (hasCameraPermission()) {
//                initializeGLSurfaceView()
//                showCameraUI()
//            } else {
//                requestCameraPermission()
//            }
//        }
//
//        binding.btnTestImage.setOnClickListener {
//            showTestImageUI()
//            testModelWithImage()
//        }
//    }
//
//    /** --- Permissions --- **/
//    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
//        this, android.Manifest.permission.CAMERA
//    ) == PackageManager.PERMISSION_GRANTED
//
//    private fun requestCameraPermission() {
//        ActivityCompat.requestPermissions(
//            this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE
//        )
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == CAMERA_REQUEST_CODE) {
//            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                initializeGLSurfaceView()
//                showCameraUI()
//            } else {
//                Toast.makeText(
//                    this,
//                    "Camera permission is required",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
//
//    /** --- UI Handling --- **/
//    private fun showCameraUI() {
//        binding.btnUseCamera.visibility = View.GONE
//        binding.btnTestImage.visibility = View.GONE
//        binding.previewView.visibility = View.VISIBLE
//        binding.overlayView.visibility = View.VISIBLE
//        glSurfaceView.visibility = View.VISIBLE
//
//        // Ensure previewView is measured before starting camera
//        binding.previewView.post {
//            openCamera()
//        }
//    }
//
//    private fun testModelWithImage() {
//        val bitmap: Bitmap = assets.open("banana.jpeg").use {
//            BitmapFactory.decodeStream(it)
//        }
//
//        if (!::modelHelper.isInitialized) {
//            modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//        }
//
//        val inputTensor = modelHelper.interpreter.getInputTensor(0)
//        val inputShape = inputTensor.shape()
//        val isNHWC = inputShape.size == 4 && inputShape[3] == 3
//        val height = if (isNHWC) inputShape[1] else inputShape[2]
//        val width = if (isNHWC) inputShape[2] else inputShape[3]
//        val channels = if (isNHWC) inputShape[3] else inputShape[1]
//
//        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//
//        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(channels) } } }
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = inputBitmap.getPixel(x, y)
//                inputArray[0][y][x][0] = Color.red(pixel) / 255f
//                inputArray[0][y][x][1] = Color.green(pixel) / 255f
//                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
//            }
//        }
//
//        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
//        val outputShape = outputTensor.shape()
//        val numAttributes = outputShape[1]
//        val numPredictions = outputShape[2]
//        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }
//
//        modelHelper.interpreter.run(inputArray, outputArray)
//
//        val classNames = arrayOf(
//            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
//            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
//            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
//            "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
//            "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
//            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
//            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
//            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
//            "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
//            "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
//        )
//
//        val canvas = Canvas(inputBitmap)
//        val boxPaint = Paint().apply {
//            color = Color.GREEN
//            style = Paint.Style.STROKE
//            strokeWidth = 6f
//            isAntiAlias = true
//        }
//        val textPaint = Paint().apply {
//            color = Color.WHITE
//            textSize = 40f
//            style = Paint.Style.FILL
//            isAntiAlias = true
//        }
//        val textBgPaint = Paint().apply {
//            color = Color.argb(180, 0, 0, 0)
//            style = Paint.Style.FILL
//        }
//
//        var detectionCount = 0
//
//        for (i in 0 until numPredictions) {
//            val x = outputArray[0][0][i]
//            val y = outputArray[0][1][i]
//            val w = outputArray[0][2][i]
//            val h = outputArray[0][3][i]
//
//            val classScores = FloatArray(80) { classIdx -> outputArray[0][4 + classIdx][i] }
//            val (classIndex, maxScore) = classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value } ?: (0 to 0f)
//
//            if (maxScore > 0.5f) {
//                detectionCount++
//
//                val xPixel = x * inputBitmap.width
//                val yPixel = y * inputBitmap.height
//                val wPixel = w * inputBitmap.width
//                val hPixel = h * inputBitmap.height
//
//                val left = xPixel - wPixel / 2
//                val top = yPixel - hPixel / 2
//                val right = xPixel + wPixel / 2
//                val bottom = yPixel + hPixel / 2
//
//                canvas.drawRect(left, top, right, bottom, boxPaint)
//
//                val label = "${classNames[classIndex]} ${(maxScore * 100).toInt()}%"
//                val textWidth = textPaint.measureText(label)
//                canvas.drawRect(left, top - 50, left + textWidth + 20, top, textBgPaint)
//                canvas.drawText(label, left + 10, top - 10, textPaint)
//            }
//        }
//
//        runOnUiThread {
//            binding.testImageView.setImageBitmap(inputBitmap)
//            binding.debugTextView.text = "Detections: $detectionCount"
//
//            if (detectionCount == 0) {
//                Toast.makeText(this, "No objects detected above 50% confidence", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    private fun showTestImageUI() {
//        binding.btnUseCamera.visibility = View.GONE
//        binding.btnTestImage.visibility = View.GONE
//        binding.testImageView.visibility = View.VISIBLE
//    }
//
//    /** --- GLSurfaceView setup --- **/
//    private fun initializeGLSurfaceView() {
//        if (!::glSurfaceView.isInitialized) {
//            glSurfaceView = GLSurfaceView(this)
//            glSurfaceView.setEGLContextClientVersion(2)
//            glSurfaceView.setZOrderOnTop(true)
//            glSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
//
//            renderer = Model3DRenderer(this)
//            glSurfaceView.setRenderer(renderer)
//            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
//
//            val params = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT
//            )
//            binding.root.addView(glSurfaceView, params)
//            glSurfaceView.visibility = View.GONE
//        }
//    }
//
//    /** --- CameraX --- **/
//    private fun openCamera() {
//        if (!isCameraOpened) {
//            startCamera()
//            isCameraOpened = true
//        }
//    }
//
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//            val preview = Preview.Builder()
//                .build()
//                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
//
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            val imageAnalyzer = ImageAnalysis.Builder()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//                .also {
//                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
//                        analyzeImage(imageProxy)
//                    }
//                }
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
//            } catch (exc: Exception) {
//                exc.printStackTrace()
//                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
//            }
//
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    /** --- Image Analysis --- **/
//    private fun analyzeImage(imageProxy: ImageProxy) {
//        if (!::modelHelper.isInitialized) {
//            modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//        }
//
//        val bitmap = imageProxy.toBitmap()
//        val (boxes, labels) = runDetection(bitmap)
//
//        // Update overlay
//        binding.overlayView.setResults(boxes, labels)
//
//        // Update 3D renderer
//        if (::renderer.isInitialized && boxes.isNotEmpty()) {
//            runOnUiThread {
//                renderer.detectedBoxes = boxes
//                renderer.surfaceWidth = binding.previewView.width
//                renderer.surfaceHeight = binding.previewView.height
//            }
//        }
//
//        imageProxy.close()
//    }
//
//    /** --- Safe YUV to Bitmap conversion --- **/
//    private fun ImageProxy.toBitmap(): Bitmap {
//        val yBuffer = planes[0].buffer
//        val uBuffer = planes[1].buffer
//        val vBuffer = planes[2].buffer
//
//        val ySize = yBuffer.remaining()
//        val uSize = uBuffer.remaining()
//        val vSize = vBuffer.remaining()
//
//        val nv21 = ByteArray(ySize + uSize + vSize)
//        yBuffer.get(nv21, 0, ySize)
//        vBuffer.get(nv21, ySize, vSize)
//        uBuffer.get(nv21, ySize + vSize, uSize)
//
//        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
//        val out = java.io.ByteArrayOutputStream()
//        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
//        val imageBytes = out.toByteArray()
//        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//    }
//
//    /** --- Model detection --- **/
//    private fun runDetection(bitmap: Bitmap): Pair<List<RectF>, List<String>> {
//        val inputShape = modelHelper.interpreter.getInputTensor(0).shape()
//        val height = inputShape[1]
//        val width = inputShape[2]
//
//        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(3) } } }
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = inputBitmap.getPixel(x, y)
//                inputArray[0][y][x][0] = Color.red(pixel) / 255f
//                inputArray[0][y][x][1] = Color.green(pixel) / 255f
//                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
//            }
//        }
//
//        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
//        val outputShape = outputTensor.shape()
//        val numAttributes = outputShape[1]
//        val numPredictions = outputShape[2]
//        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }
//
//        modelHelper.interpreter.run(inputArray, outputArray)
//
//        val classNames = arrayOf(
//            "person","bicycle","car","motorcycle","airplane","bus","train","truck","boat",
//            "traffic light","fire hydrant","stop sign","parking meter","bench","bird","cat","dog",
//            "horse","sheep","cow","elephant","bear","zebra","giraffe","backpack","umbrella",
//            "handbag","tie","suitcase","frisbee","skis","snowboard","sports ball","kite",
//            "baseball bat","baseball glove","skateboard","surfboard","tennis racket","bottle",
//            "wine glass","cup","fork","knife","spoon","bowl","banana","apple","sandwich",
//            "orange","broccoli","carrot","hot dog","pizza","donut","cake","chair","couch",
//            "potted plant","bed","dining table","toilet","tv","laptop","mouse","remote",
//            "keyboard","cell phone","microwave","oven","toaster","sink","refrigerator","book",
//            "clock","vase","scissors","teddy bear","hair drier","toothbrush"
//        )
//
//        val boxes = mutableListOf<RectF>()
//        val labels = mutableListOf<String>()
//
//        for (i in 0 until numPredictions) {
//            val x = outputArray[0][0][i]
//            val y = outputArray[0][1][i]
//            val w = outputArray[0][2][i]
//            val h = outputArray[0][3][i]
//
//            val classScores = FloatArray(80) { classIdx -> outputArray[0][4 + classIdx][i] }
//            val (classIndex, maxScore) = classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value } ?: (0 to 0f)
//
//            if (maxScore > 0.5f) {
//                val xPixel = x * binding.previewView.width
//                val yPixel = y * binding.previewView.height
//                val wPixel = w * binding.previewView.width
//                val hPixel = h * binding.previewView.height
//
//                boxes.add(RectF(
//                    xPixel - wPixel / 2,
//                    yPixel - hPixel / 2,
//                    xPixel + wPixel / 2,
//                    yPixel + hPixel / 2
//                ))
//
//                labels.add("${classNames[classIndex]} ${(maxScore * 100).toInt()}%")
//            }
//        }
//
//        return boxes to labels
//    }
//
//    override fun onPause() {
//        super.onPause()
//        if (::glSurfaceView.isInitialized) glSurfaceView.onPause()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (::glSurfaceView.isInitialized) glSurfaceView.onResume()
//    }
//}











//
//package com.example.hackathon
//
//import android.content.pm.PackageManager
//import android.graphics.*
//import android.os.Bundle
//import android.view.View
//import android.widget.FrameLayout
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.*
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.example.hackathon.databinding.ActivityMainBinding
//import android.opengl.GLSurfaceView
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//import java.util.concurrent.atomic.AtomicBoolean
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private val CAMERA_REQUEST_CODE = 1001
//
//    private lateinit var modelHelper: TFLiteModelHelper
//    private lateinit var glSurfaceView: GLSurfaceView
//    private lateinit var renderer: Model3DRenderer
//
//    private var isCameraOpened = false
//
//    // Background executor for image processing
//    private lateinit var analysisExecutor: ExecutorService
//    private val isProcessing = AtomicBoolean(false)
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Initialize background executor
//        analysisExecutor = Executors.newSingleThreadExecutor()
//
//        binding.btnUseCamera.setOnClickListener {
//            if (hasCameraPermission()) {
//                initializeGLSurfaceView()
//                showCameraUI()
//            } else {
//                requestCameraPermission()
//            }
//        }
//
//        binding.btnTestImage.setOnClickListener {
//            showTestImageUI()
//            testModelWithImage()
//        }
//    }
//
//    /** --- Permissions --- **/
//    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
//        this, android.Manifest.permission.CAMERA
//    ) == PackageManager.PERMISSION_GRANTED
//
//    private fun requestCameraPermission() {
//        ActivityCompat.requestPermissions(
//            this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE
//        )
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == CAMERA_REQUEST_CODE) {
//            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                initializeGLSurfaceView()
//                showCameraUI()
//            } else {
//                Toast.makeText(
//                    this,
//                    "Camera permission is required",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
//
//    /** --- UI Handling --- **/
//    private fun showCameraUI() {
//        binding.btnUseCamera.visibility = View.GONE
//        binding.btnTestImage.visibility = View.GONE
//        binding.previewView.visibility = View.VISIBLE
//        binding.overlayView.visibility = View.VISIBLE
//        glSurfaceView.visibility = View.VISIBLE
//
//        binding.previewView.post {
//            openCamera()
//        }
//    }
//
//    private fun testModelWithImage() {
//        val bitmap: Bitmap = assets.open("banana.jpeg").use {
//            BitmapFactory.decodeStream(it)
//        }
//
//        if (!::modelHelper.isInitialized) {
//            modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//        }
//
//        val inputTensor = modelHelper.interpreter.getInputTensor(0)
//        val inputShape = inputTensor.shape()
//        val isNHWC = inputShape.size == 4 && inputShape[3] == 3
//        val height = if (isNHWC) inputShape[1] else inputShape[2]
//        val width = if (isNHWC) inputShape[2] else inputShape[3]
//        val channels = if (isNHWC) inputShape[3] else inputShape[1]
//
//        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//
//        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(channels) } } }
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = inputBitmap.getPixel(x, y)
//                inputArray[0][y][x][0] = Color.red(pixel) / 255f
//                inputArray[0][y][x][1] = Color.green(pixel) / 255f
//                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
//            }
//        }
//
//        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
//        val outputShape = outputTensor.shape()
//        val numAttributes = outputShape[1]
//        val numPredictions = outputShape[2]
//        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }
//
//        modelHelper.interpreter.run(inputArray, outputArray)
//
//        val classNames = arrayOf(
//            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
//            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
//            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
//            "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
//            "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
//            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
//            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
//            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
//            "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
//            "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
//        )
//
//        val canvas = Canvas(inputBitmap)
//        val boxPaint = Paint().apply {
//            color = Color.GREEN
//            style = Paint.Style.STROKE
//            strokeWidth = 6f
//            isAntiAlias = true
//        }
//        val textPaint = Paint().apply {
//            color = Color.WHITE
//            textSize = 40f
//            style = Paint.Style.FILL
//            isAntiAlias = true
//        }
//        val textBgPaint = Paint().apply {
//            color = Color.argb(180, 0, 0, 0)
//            style = Paint.Style.FILL
//        }
//
//        var detectionCount = 0
//
//        for (i in 0 until numPredictions) {
//            val x = outputArray[0][0][i]
//            val y = outputArray[0][1][i]
//            val w = outputArray[0][2][i]
//            val h = outputArray[0][3][i]
//
//            val classScores = FloatArray(80) { classIdx -> outputArray[0][4 + classIdx][i] }
//            val (classIndex, maxScore) = classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value } ?: (0 to 0f)
//
//            if (maxScore > 0.5f) {
//                detectionCount++
//
//                val xPixel = x * inputBitmap.width
//                val yPixel = y * inputBitmap.height
//                val wPixel = w * inputBitmap.width
//                val hPixel = h * inputBitmap.height
//
//                val left = xPixel - wPixel / 2
//                val top = yPixel - hPixel / 2
//                val right = xPixel + wPixel / 2
//                val bottom = yPixel + hPixel / 2
//
//                canvas.drawRect(left, top, right, bottom, boxPaint)
//
//                val label = "${classNames[classIndex]} ${(maxScore * 100).toInt()}%"
//                val textWidth = textPaint.measureText(label)
//                canvas.drawRect(left, top - 50, left + textWidth + 20, top, textBgPaint)
//                canvas.drawText(label, left + 10, top - 10, textPaint)
//            }
//        }
//
//        runOnUiThread {
//            binding.testImageView.setImageBitmap(inputBitmap)
//            binding.debugTextView.text = "Detections: $detectionCount"
//
//            if (detectionCount == 0) {
//                Toast.makeText(this, "No objects detected above 50% confidence", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    private fun showTestImageUI() {
//        binding.btnUseCamera.visibility = View.GONE
//        binding.btnTestImage.visibility = View.GONE
//        binding.testImageView.visibility = View.VISIBLE
//    }
//
//    /** --- GLSurfaceView setup --- **/
//    private fun initializeGLSurfaceView() {
//        if (!::glSurfaceView.isInitialized) {
//            glSurfaceView = GLSurfaceView(this)
//            glSurfaceView.setEGLContextClientVersion(2)
//            glSurfaceView.setZOrderOnTop(true)
//            glSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
//
//            renderer = Model3DRenderer(this)
//            glSurfaceView.setRenderer(renderer)
//            // CHANGED: Only render when explicitly requested
//            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
//
//            val params = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT
//            )
//            binding.root.addView(glSurfaceView, params)
//            glSurfaceView.visibility = View.GONE
//        }
//    }
//
//    /** --- CameraX --- **/
//    private fun openCamera() {
//        if (!isCameraOpened) {
//            startCamera()
//            isCameraOpened = true
//        }
//    }
//
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//            val preview = Preview.Builder()
//                .build()
//                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
//
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            val imageAnalyzer = ImageAnalysis.Builder()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                // CHANGED: Use background executor instead of main executor
//                .setTargetResolution(android.util.Size(640, 480)) // Lower resolution for faster processing
//                .build()
//                .also {
//                    it.setAnalyzer(analysisExecutor) { imageProxy ->
//                        analyzeImage(imageProxy)
//                    }
//                }
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
//            } catch (exc: Exception) {
//                exc.printStackTrace()
//                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
//            }
//
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    /** --- Image Analysis --- **/
//    private fun analyzeImage(imageProxy: ImageProxy) {
//        // Skip if already processing a frame
//        if (!isProcessing.compareAndSet(false, true)) {
//            imageProxy.close()
//            return
//        }
//
//        try {
//            if (!::modelHelper.isInitialized) {
//                modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//            }
//
//            val bitmap = imageProxy.toBitmap()
//            val (boxes, labels) = runDetection(bitmap)
//
//            // Update UI on main thread
//            runOnUiThread {
//                binding.overlayView.setResults(boxes, labels)
//
//                if (::renderer.isInitialized && boxes.isNotEmpty()) {
//                    renderer.detectedBoxes = boxes
//                    renderer.surfaceWidth = binding.previewView.width
//                    renderer.surfaceHeight = binding.previewView.height
//                    glSurfaceView.requestRender() // Request render instead of continuous
//                }
//            }
//
//            // Recycle bitmap to free memory
//            bitmap.recycle()
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//        } finally {
//            isProcessing.set(false)
//            imageProxy.close()
//        }
//    }
//
//    /** --- Safe YUV to Bitmap conversion --- **/
//    private fun ImageProxy.toBitmap(): Bitmap {
//        val yBuffer = planes[0].buffer
//        val uBuffer = planes[1].buffer
//        val vBuffer = planes[2].buffer
//
//        val ySize = yBuffer.remaining()
//        val uSize = uBuffer.remaining()
//        val vSize = vBuffer.remaining()
//
//        val nv21 = ByteArray(ySize + uSize + vSize)
//        yBuffer.get(nv21, 0, ySize)
//        vBuffer.get(nv21, ySize, vSize)
//        uBuffer.get(nv21, ySize + vSize, uSize)
//
//        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
//        val out = java.io.ByteArrayOutputStream()
//        yuvImage.compressToJpeg(Rect(0, 0, width, height), 80, out) // Reduced quality for speed
//        val imageBytes = out.toByteArray()
//        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//    }
//
//    /** --- Model detection --- **/
//    private fun runDetection(bitmap: Bitmap): Pair<List<RectF>, List<String>> {
//        val inputShape = modelHelper.interpreter.getInputTensor(0).shape()
//        val height = inputShape[1]
//        val width = inputShape[2]
//
//        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
//        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(3) } } }
//
//        for (y in 0 until height) {
//            for (x in 0 until width) {
//                val pixel = inputBitmap.getPixel(x, y)
//                inputArray[0][y][x][0] = Color.red(pixel) / 255f
//                inputArray[0][y][x][1] = Color.green(pixel) / 255f
//                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
//            }
//        }
//
//        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
//        val outputShape = outputTensor.shape()
//        val numAttributes = outputShape[1]
//        val numPredictions = outputShape[2]
//        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }
//
//        modelHelper.interpreter.run(inputArray, outputArray)
//
//        val classNames = arrayOf(
//            "person","bicycle","car","motorcycle","airplane","bus","train","truck","boat",
//            "traffic light","fire hydrant","stop sign","parking meter","bench","bird","cat","dog",
//            "horse","sheep","cow","elephant","bear","zebra","giraffe","backpack","umbrella",
//            "handbag","tie","suitcase","frisbee","skis","snowboard","sports ball","kite",
//            "baseball bat","baseball glove","skateboard","surfboard","tennis racket","bottle",
//            "wine glass","cup","fork","knife","spoon","bowl","banana","apple","sandwich",
//            "orange","broccoli","carrot","hot dog","pizza","donut","cake","chair","couch",
//            "potted plant","bed","dining table","toilet","tv","laptop","mouse","remote",
//            "keyboard","cell phone","microwave","oven","toaster","sink","refrigerator","book",
//            "clock","vase","scissors","teddy bear","hair drier","toothbrush"
//        )
//
//        val boxes = mutableListOf<RectF>()
//        val labels = mutableListOf<String>()
//
//        for (i in 0 until numPredictions) {
//            val x = outputArray[0][0][i]
//            val y = outputArray[0][1][i]
//            val w = outputArray[0][2][i]
//            val h = outputArray[0][3][i]
//
//            val classScores = FloatArray(80) { classIdx -> outputArray[0][4 + classIdx][i] }
//            val (classIndex, maxScore) = classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value } ?: (0 to 0f)
//
//            if (maxScore > 0.5f) {
//                val xPixel = x * binding.previewView.width
//                val yPixel = y * binding.previewView.height
//                val wPixel = w * binding.previewView.width
//                val hPixel = h * binding.previewView.height
//
//                boxes.add(RectF(
//                    xPixel - wPixel / 2,
//                    yPixel - hPixel / 2,
//                    xPixel + wPixel / 2,
//                    yPixel + hPixel / 2
//                ))
//
//                labels.add("${classNames[classIndex]} ${(maxScore * 100).toInt()}%")
//            }
//        }
//
//        // Recycle scaled bitmap
//        inputBitmap.recycle()
//
//        return boxes to labels
//    }
//
//    override fun onPause() {
//        super.onPause()
//        if (::glSurfaceView.isInitialized) glSurfaceView.onPause()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (::glSurfaceView.isInitialized) glSurfaceView.onResume()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        analysisExecutor.shutdown()
//    }
//}














package com.example.hackathon

import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hackathon.databinding.ActivityMainBinding
import android.opengl.GLSurfaceView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val CAMERA_REQUEST_CODE = 1001

    private lateinit var modelHelper: TFLiteModelHelper
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: Model3DRenderer

    private var isCameraOpened = false

    // Background executor for image processing
    private lateinit var analysisExecutor: ExecutorService
    private val isProcessing = AtomicBoolean(false)
    private var frameCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize background executor
        analysisExecutor = Executors.newSingleThreadExecutor()

        binding.btnUseCamera.setOnClickListener {
            if (hasCameraPermission()) {
                initializeGLSurfaceView()
                showCameraUI()
            } else {
                requestCameraPermission()
            }
        }

        binding.btnTestImage.setOnClickListener {
            showTestImageUI()
            testModelWithImage()
        }
    }

    /** --- Permissions --- **/
    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, android.Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                initializeGLSurfaceView()
                showCameraUI()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** --- UI Handling --- **/
    private fun showCameraUI() {
        binding.btnUseCamera.visibility = View.GONE
        binding.btnTestImage.visibility = View.GONE
        binding.previewView.visibility = View.VISIBLE
        binding.overlayView.visibility = View.VISIBLE
        binding.debugTextView.visibility = View.VISIBLE
        // Disable 3D rendering until model file is added
        // glSurfaceView.visibility = View.VISIBLE

        binding.previewView.post {
            android.util.Log.d("MainActivity", "PreviewView size: ${binding.previewView.width}x${binding.previewView.height}")
            openCamera()
        }
    }

    private fun testModelWithImage() {
        val bitmap: Bitmap = assets.open("banana.jpeg").use {
            BitmapFactory.decodeStream(it)
        }

        if (!::modelHelper.isInitialized) {
            modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
        }

        val inputTensor = modelHelper.interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape()
        val isNHWC = inputShape.size == 4 && inputShape[3] == 3
        val height = if (isNHWC) inputShape[1] else inputShape[2]
        val width = if (isNHWC) inputShape[2] else inputShape[3]
        val channels = if (isNHWC) inputShape[3] else inputShape[1]

        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(channels) } } }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = inputBitmap.getPixel(x, y)
                inputArray[0][y][x][0] = Color.red(pixel) / 255f
                inputArray[0][y][x][1] = Color.green(pixel) / 255f
                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
            }
        }

        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        val numAttributes = outputShape[1]
        val numPredictions = outputShape[2]
        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }

        modelHelper.interpreter.run(inputArray, outputArray)

        val classNames = arrayOf(
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
            "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
            "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
            "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
            "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
        )

        val canvas = Canvas(inputBitmap)
        val boxPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val textBgPaint = Paint().apply {
            color = Color.argb(180, 0, 0, 0)
            style = Paint.Style.FILL
        }

        var detectionCount = 0

        for (i in 0 until numPredictions) {
            val x = outputArray[0][0][i]
            val y = outputArray[0][1][i]
            val w = outputArray[0][2][i]
            val h = outputArray[0][3][i]

            val classScores = FloatArray(80) { classIdx -> outputArray[0][4 + classIdx][i] }
            val (classIndex, maxScore) = classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value } ?: (0 to 0f)

            if (maxScore > 0.5f) {
                detectionCount++

                val xPixel = x * inputBitmap.width
                val yPixel = y * inputBitmap.height
                val wPixel = w * inputBitmap.width
                val hPixel = h * inputBitmap.height

                val left = xPixel - wPixel / 2
                val top = yPixel - hPixel / 2
                val right = xPixel + wPixel / 2
                val bottom = yPixel + hPixel / 2

                canvas.drawRect(left, top, right, bottom, boxPaint)

                val label = "${classNames[classIndex]} ${(maxScore * 100).toInt()}%"
                val textWidth = textPaint.measureText(label)
                canvas.drawRect(left, top - 50, left + textWidth + 20, top, textBgPaint)
                canvas.drawText(label, left + 10, top - 10, textPaint)
            }
        }

        runOnUiThread {
            binding.testImageView.setImageBitmap(inputBitmap)
            binding.debugTextView.text = "Detections: $detectionCount"

            if (detectionCount == 0) {
                Toast.makeText(this, "No objects detected above 50% confidence", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showTestImageUI() {
        binding.btnUseCamera.visibility = View.GONE
        binding.btnTestImage.visibility = View.GONE
        binding.testImageView.visibility = View.VISIBLE
    }

    /** --- GLSurfaceView setup --- **/
//    private fun initializeGLSurfaceView() {
//        if (!::glSurfaceView.isInitialized) {
//            glSurfaceView = GLSurfaceView(this)
//            glSurfaceView.setEGLContextClientVersion(2)
//            glSurfaceView.setZOrderMediaOverlay(true)
//            glSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
//
//            renderer = Model3DRenderer(this)
//            glSurfaceView.setRenderer(renderer)
//            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
//
//            val params = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT
//            )
//            binding.root.addView(glSurfaceView, params)
//            glSurfaceView.visibility = View.GONE  // ← Change this to GONE
//        }
//    }
    /** --- GLSurfaceView setup --- **/
    /** --- GLSurfaceView setup --- **/
    private fun initializeGLSurfaceView() {
        if (!::glSurfaceView.isInitialized) {
            // Create a new GLSurfaceView
            glSurfaceView = GLSurfaceView(this)

            // CRITICAL: Set EGLConfigChooser BEFORE setRenderer
            // 8 bits for RGBA channels, 16 for depth, 0 for stencil
            glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

            // Set EGLContext client version (OpenGL ES 2.0)
            glSurfaceView.setEGLContextClientVersion(2)

            // Make the surface holder format transparent
            glSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)

            // CRITICAL: This makes the GLSurfaceView render on top of the camera
            // but allows the camera preview to show through transparent areas
            glSurfaceView.setZOrderOnTop(true)  // Place on top

            // Initialize the renderer for 3D model drawing
            renderer = Model3DRenderer(this)
            glSurfaceView.setRenderer(renderer)
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

            // Add GLSurfaceView to the layout
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            binding.root.addView(glSurfaceView, params)

            // Initially hide the GLSurfaceView until the model is detected
            glSurfaceView.visibility = View.GONE
            android.util.Log.d("MainActivity", "GLSurfaceView initialized with transparency")
        }
    }

    /** --- CameraX --- **/
    private fun openCamera() {
        if (!isCameraOpened) {
            startCamera()
            isCameraOpened = true
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Set scale type for preview
            binding.previewView.implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
            binding.previewView.scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                // CHANGED: Use background executor instead of main executor
                .setTargetResolution(android.util.Size(640, 480)) // Lower resolution for faster processing
                .build()
                .also {
                    it.setAnalyzer(analysisExecutor) { imageProxy ->
                        analyzeImage(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                exc.printStackTrace()
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /** --- Image Analysis --- **/
//    private fun analyzeImage(imageProxy: ImageProxy) {
//        // Skip if already processing a frame
//        if (!isProcessing.compareAndSet(false, true)) {
//            imageProxy.close()
//            return
//        }
//
//        try {
//            if (!::modelHelper.isInitialized) {
//                modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
//            }
//
//            val bitmap = imageProxy.toBitmap()
//            val (boxes, labels) = runDetection(bitmap)
//
//            // Update UI on main thread
//            runOnUiThread {
//                binding.overlayView.setResults(boxes, labels)
//
//                // Disable 3D rendering until model file is added
//
//                if (::renderer.isInitialized && boxes.isNotEmpty()) {
//                    renderer.detectedBoxes = boxes
//                    renderer.surfaceWidth = binding.previewView.width
//                    renderer.surfaceHeight = binding.previewView.height
//                    glSurfaceView.requestRender() // Request render instead of continuous
//                }
//
//            }
//
//            // Recycle bitmap to free memory
//            bitmap.recycle()
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//        } finally {
//            isProcessing.set(false)
//            imageProxy.close()
//        }
//    }

        /** --- Image Analysis --- **/
        private fun analyzeImage(imageProxy: ImageProxy) {
            // Skip if already processing a frame
            if (!isProcessing.compareAndSet(false, true)) {
                imageProxy.close()
                return
            }

            try {
                if (!::modelHelper.isInitialized) {
                    modelHelper = TFLiteModelHelper(this, "yolov8s-seg_float32.tflite")
                }

                val bitmap = imageProxy.toBitmap()
                val (boxes, labels) = runDetection(bitmap)

                // Update UI on main thread
                runOnUiThread {
                    binding.overlayView.setResults(boxes, labels)

                    // Check if apple is detected
                    // Check if apple is detected
                    val hasApple = labels.any { it.lowercase().contains("apple") }

                    if (::glSurfaceView.isInitialized && ::renderer.isInitialized) {
                        if (hasApple && boxes.isNotEmpty()) {
                            android.util.Log.d("MainActivity", "Apple detected! Showing 3D model")
                            glSurfaceView.visibility = View.VISIBLE
                            renderer.detectedBoxes = boxes
                            renderer.surfaceWidth = binding.previewView.width
                            renderer.surfaceHeight = binding.previewView.height
                            glSurfaceView.requestRender()  // Request render
                        } else {
                            android.util.Log.d("MainActivity", "No apple detected, hiding GLSurfaceView")
                            glSurfaceView.visibility = View.GONE  // Hide if no apple is detected
                        }
                    }

                }

                // Recycle bitmap to free memory
                bitmap.recycle()

            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in analyzeImage: ${e.message}", e)
                e.printStackTrace()
            } finally {
                isProcessing.set(false)
                imageProxy.close()
            }
        }

    /** --- Safe YUV to Bitmap conversion --- **/
    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 80, out) // Reduced quality for speed
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    /** --- Model detection --- **/
    private fun runDetection(bitmap: Bitmap): Pair<List<RectF>, List<String>> {
        val inputShape = modelHelper.interpreter.getInputTensor(0).shape()
        val height = inputShape[1]
        val width = inputShape[2]

        val inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val inputArray = Array(1) { Array(height) { Array(width) { FloatArray(3) } } }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = inputBitmap.getPixel(x, y)
                inputArray[0][y][x][0] = Color.red(pixel) / 255f
                inputArray[0][y][x][1] = Color.green(pixel) / 255f
                inputArray[0][y][x][2] = Color.blue(pixel) / 255f
            }
        }

        val outputTensor = modelHelper.interpreter.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        val numAttributes = outputShape[1]
        val numPredictions = outputShape[2]
        val outputArray = Array(1) { Array(numAttributes) { FloatArray(numPredictions) } }

        modelHelper.interpreter.run(inputArray, outputArray)

        val classNames = arrayOf(
            "person","bicycle","car","motorcycle","airplane","bus","train","truck","boat",
            "traffic light","fire hydrant","stop sign","parking meter","bench","bird","cat","dog",
            "horse","sheep","cow","elephant","bear","zebra","giraffe","backpack","umbrella",
            "handbag","tie","suitcase","frisbee","skis","snowboard","sports ball","kite",
            "baseball bat","baseball glove","skateboard","surfboard","tennis racket","bottle",
            "wine glass","cup","fork","knife","spoon","bowl","banana","apple","sandwich",
            "orange","broccoli","carrot","hot dog","pizza","donut","cake","chair","couch",
            "potted plant","bed","dining table","toilet","tv","laptop","mouse","remote",
            "keyboard","cell phone","microwave","oven","toaster","sink","refrigerator","book",
            "clock","vase","scissors","teddy bear","hair drier","toothbrush"
        )

        val boxes = mutableListOf<RectF>()
        val labels = mutableListOf<String>()

        for (i in 0 until numPredictions) {
            val x = outputArray[0][0][i]
            val y = outputArray[0][1][i]
            val w = outputArray[0][2][i]
            val h = outputArray[0][3][i]

            val classScores = FloatArray(80) { classIdx -> outputArray[0][4 + classIdx][i] }
            val (classIndex, maxScore) = classScores.withIndex().maxByOrNull { it.value }?.let { it.index to it.value } ?: (0 to 0f)

            if (maxScore > 0.5f) {
                val xPixel = x * binding.previewView.width
                val yPixel = y * binding.previewView.height
                val wPixel = w * binding.previewView.width
                val hPixel = h * binding.previewView.height

                boxes.add(RectF(
                    xPixel - wPixel / 2,
                    yPixel - hPixel / 2,
                    xPixel + wPixel / 2,
                    yPixel + hPixel / 2
                ))

                labels.add("${classNames[classIndex]} ${(maxScore * 100).toInt()}%")
            }
        }

        // Recycle scaled bitmap
        inputBitmap.recycle()

        return boxes to labels
    }

    override fun onPause() {
        super.onPause()
        if (::glSurfaceView.isInitialized) glSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::glSurfaceView.isInitialized) glSurfaceView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        analysisExecutor.shutdown()
    }
}