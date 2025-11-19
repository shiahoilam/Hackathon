package com.example.hackathon

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min

class CameraActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var detectionOverlay: DetectionOverlayView
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // TensorFlow Lite Interpreter
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private val modelInputWidth = 640
    private val modelInputHeight = 640

    // Model output buffer
    private val outputBuffer = Array(1) { Array(139) { FloatArray(8400) } } // Shape [1, 139, 8400]


    // Calorie mapping
    private val foodDatabase = mapOf(
        "candy" to FoodInfo(394, 0.0f, 0.2f, 99.0f),
        "egg tart" to FoodInfo(350, 6.5f, 18.0f, 40.0f),
        "french fries" to FoodInfo(312, 3.4f, 15.0f, 41.0f),
        "chocolate" to FoodInfo(546, 4.9f, 31.0f, 61.0f),
        "biscuit" to FoodInfo(488, 6.5f, 21.0f, 68.0f),
        "popcorn" to FoodInfo(387, 12.6f, 4.5f, 78.0f),
        "pudding" to FoodInfo(131, 2.8f, 2.7f, 23.0f),
        "ice cream" to FoodInfo(207, 3.5f, 11.0f, 24.0f),
        "cheese butter" to FoodInfo(717, 0.9f, 81.0f, 0.1f),
        "cake" to FoodInfo(337, 4.9f, 13.0f, 51.0f),
        "wine" to FoodInfo(85, 0.1f, 0.0f, 2.6f),
        "milkshake" to FoodInfo(119, 3.3f, 3.4f, 19.0f),
        "coffee" to FoodInfo(2, 0.3f, 0.0f, 0.0f),
        "juice" to FoodInfo(45, 0.7f, 0.2f, 10.4f),
        "milk" to FoodInfo(61, 3.2f, 3.3f, 4.8f),
        "tea" to FoodInfo(1, 0.0f, 0.0f, 0.3f),
        "almond" to FoodInfo(579, 21.0f, 50.0f, 22.0f),
        "red beans" to FoodInfo(333, 24.0f, 1.0f, 60.0f),
        "cashew" to FoodInfo(553, 18.0f, 44.0f, 30.0f),
        "dried cranberries" to FoodInfo(308, 0.2f, 1.4f, 82.0f),
        "soy" to FoodInfo(173, 17.0f, 9.0f, 10.0f),
        "walnut" to FoodInfo(654, 15.0f, 65.0f, 14.0f),
        "peanut" to FoodInfo(567, 26.0f, 49.0f, 16.0f),
        "egg" to FoodInfo(155, 13.0f, 11.0f, 1.1f),
        "apple" to FoodInfo(52, 0.3f, 0.2f, 14.0f),
        "date" to FoodInfo(282, 2.5f, 0.4f, 75.0f),
        "apricot" to FoodInfo(48, 1.4f, 0.4f, 11.0f),
        "avocado" to FoodInfo(160, 2.0f, 15.0f, 8.5f),
        "banana" to FoodInfo(89, 1.1f, 0.3f, 23.0f),
        "strawberry" to FoodInfo(32, 0.7f, 0.3f, 7.7f),
        "cherry" to FoodInfo(63, 1.1f, 0.2f, 16.0f),
        "blueberry" to FoodInfo(57, 0.7f, 0.3f, 14.0f),
        "raspberry" to FoodInfo(52, 1.2f, 0.7f, 12.0f),
        "mango" to FoodInfo(60, 0.8f, 0.4f, 15.0f),
        "olives" to FoodInfo(115, 0.8f, 11.0f, 6.3f),
        "peach" to FoodInfo(39, 0.9f, 0.3f, 9.5f),
        "lemon" to FoodInfo(29, 1.1f, 0.3f, 9.3f),
        "pear" to FoodInfo(57, 0.4f, 0.1f, 15.0f),
        "fig" to FoodInfo(74, 0.8f, 0.3f, 19.0f),
        "pineapple" to FoodInfo(50, 0.5f, 0.1f, 13.0f),
        "grape" to FoodInfo(69, 0.7f, 0.2f, 18.0f),
        "kiwi" to FoodInfo(61, 1.1f, 0.5f, 15.0f),
        "melon" to FoodInfo(34, 0.8f, 0.2f, 8.2f),
        "orange" to FoodInfo(47, 0.9f, 0.1f, 12.0f),
        "watermelon" to FoodInfo(30, 0.6f, 0.2f, 7.6f),
        "steak" to FoodInfo(271, 25.0f, 19.0f, 0.0f),
        "pork" to FoodInfo(242, 27.0f, 14.0f, 0.0f),
        "chicken duck" to FoodInfo(337, 19.0f, 28.0f, 0.0f),
        "sausage" to FoodInfo(301, 13.0f, 27.0f, 3.0f),
        "fried meat" to FoodInfo(290, 26.0f, 20.0f, 1.0f),
        "lamb" to FoodInfo(294, 25.0f, 21.0f, 0.0f),
        "sauce" to FoodInfo(120, 2.0f, 8.0f, 12.0f),
        "crab" to FoodInfo(97, 19.0f, 1.3f, 0.7f),
        "fish" to FoodInfo(208, 20.0f, 13.0f, 0.0f),
        "shellfish" to FoodInfo(85, 15.0f, 1.5f, 4.0f),
        "shrimp" to FoodInfo(99, 24.0f, 0.3f, 0.2f),
        "soup" to FoodInfo(38, 2.5f, 1.2f, 5.0f),
        "bread" to FoodInfo(265, 9.0f, 3.2f, 49.0f),
        "corn" to FoodInfo(86, 3.3f, 1.4f, 19.0f),
        "hamburg" to FoodInfo(295, 17.0f, 14.0f, 27.0f),
        "pizza" to FoodInfo(266, 11.0f, 10.0f, 33.0f),
        "hanamaki baozi" to FoodInfo(221, 7.0f, 0.9f, 47.0f),
        "wonton dumplings" to FoodInfo(206, 8.0f, 7.0f, 28.0f),
        "pasta" to FoodInfo(131, 5.0f, 1.1f, 25.0f),
        "noodles" to FoodInfo(138, 4.5f, 0.5f, 28.0f),
        "rice" to FoodInfo(130, 2.7f, 0.3f, 28.0f),
        "pie" to FoodInfo(295, 3.8f, 15.0f, 37.0f),
        "tofu" to FoodInfo(76, 8.0f, 4.8f, 1.9f),
        "eggplant" to FoodInfo(25, 1.0f, 0.2f, 5.9f),
        "potato" to FoodInfo(77, 2.0f, 0.1f, 17.0f),
        "garlic" to FoodInfo(149, 6.4f, 0.5f, 33.0f),
        "cauliflower" to FoodInfo(25, 1.9f, 0.3f, 5.0f),
        "tomato" to FoodInfo(18, 0.9f, 0.2f, 3.9f),
        "kelp" to FoodInfo(43, 1.7f, 0.6f, 9.6f),
        "seaweed" to FoodInfo(45, 6.0f, 0.6f, 9.0f),
        "spring onion" to FoodInfo(32, 1.8f, 0.2f, 7.3f),
        "rape" to FoodInfo(13, 1.5f, 0.2f, 2.2f),
        "ginger" to FoodInfo(80, 1.8f, 0.8f, 18.0f),
        "okra" to FoodInfo(33, 1.9f, 0.2f, 7.5f),
        "lettuce" to FoodInfo(15, 1.4f, 0.2f, 2.9f),
        "pumpkin" to FoodInfo(26, 1.0f, 0.1f, 6.5f),
        "cucumber" to FoodInfo(16, 0.7f, 0.1f, 3.6f),
        "white radish" to FoodInfo(18, 0.7f, 0.1f, 4.2f),
        "carrot" to FoodInfo(41, 0.9f, 0.2f, 9.6f),
        "asparagus" to FoodInfo(20, 2.2f, 0.2f, 3.9f),
        "bamboo shoots" to FoodInfo(27, 2.6f, 0.3f, 5.2f),
        "broccoli" to FoodInfo(34, 2.8f, 0.4f, 6.6f),
        "celery stick" to FoodInfo(16, 0.7f, 0.2f, 3.0f),
        "cilantro mint" to FoodInfo(23, 2.1f, 0.5f, 3.7f),
        "snow peas" to FoodInfo(42, 2.8f, 0.2f, 7.6f),
        "cabbage" to FoodInfo(25, 1.3f, 0.1f, 5.8f),
        "bean sprouts" to FoodInfo(30, 3.1f, 0.2f, 5.9f),
        "onion" to FoodInfo(40, 1.1f, 0.1f, 9.3f),
        "pepper" to FoodInfo(31, 1.0f, 0.3f, 6.0f),
        "green beans" to FoodInfo(31, 1.8f, 0.2f, 7.0f),
        "french beans" to FoodInfo(31, 1.8f, 0.1f, 7.0f),
        "king oyster mushroom" to FoodInfo(43, 3.3f, 0.4f, 7.6f),
        "shiitake" to FoodInfo(34, 2.2f, 0.5f, 6.8f),
        "enoki mushroom" to FoodInfo(37, 2.7f, 0.3f, 7.8f),
        "oyster mushroom" to FoodInfo(33, 3.3f, 0.4f, 6.1f),
        "white button mushroom" to FoodInfo(22, 3.1f, 0.3f, 3.3f),
        "salad" to FoodInfo(15, 1.2f, 0.2f, 3.0f),
        "other ingredients" to FoodInfo(0, 0.0f, 0.0f, 0.0f)
    )

    // Class labels will be loaded from assets
    private val labels = mutableListOf<String>()

    // Add this variable to store the history
    private var previousDetections: List<DetectionResult> = emptyList()

    // Add this function to smooth the coordinates
    private fun smoothResults(newResults: List<DetectionResult>): List<DetectionResult> {
        // 1. If we have no previous history, just save and return the new ones
        if (previousDetections.isEmpty()) {
            previousDetections = newResults
            return newResults
        }

        val smoothedList = mutableListOf<DetectionResult>()

        for (newResult in newResults) {
            // 2. Find if this object was detected in the last frame (check overlapping area)
            val bestMatch = previousDetections.firstOrNull { prev ->
                // Check if it's the same food and overlaps significantly (> 50%)
                prev.foodName == newResult.foodName &&
                        calculateIoU(prev.boundingBox, newResult.boundingBox) > 0.5f
            }

            if (bestMatch != null) {
                // 3. FOUND IT! Blend the new coordinates with the old ones
                // 'alpha' determines smoothness. 0.7f means "Keep 70% of old position, use 30% of new"
                // Higher alpha = smoother but more "laggy". Lower alpha = faster but more jittery.
                val alpha = 0.6f

                val smoothedBox = RectF(
                    (bestMatch.boundingBox.left * alpha) + (newResult.boundingBox.left * (1 - alpha)),
                    (bestMatch.boundingBox.top * alpha) + (newResult.boundingBox.top * (1 - alpha)),
                    (bestMatch.boundingBox.right * alpha) + (newResult.boundingBox.right * (1 - alpha)),
                    (bestMatch.boundingBox.bottom * alpha) + (newResult.boundingBox.bottom * (1 - alpha))
                )

                smoothedList.add(newResult.copy(boundingBox = smoothedBox))
            } else {
                // 4. NEW OBJECT? Just add it as is.
                smoothedList.add(newResult)
            }
        }

        // Update history for the next frame
        previousDetections = smoothedList
        return smoothedList
    }

    private fun getCaloriesForLabel(label: String): Int {
        val cleanLabel = label.lowercase().trim()

        val foodInfo = foodDatabase[cleanLabel]

        return foodInfo?.calories ?: 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        previewView = findViewById(R.id.previewView)
        detectionOverlay = findViewById(R.id.detectionOverlay)

        val backButton = findViewById<View>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Load labels and then initialize model
        loadLabels()
        initializeModel()


        // Check and request permissions
        if (isCameraPermissionGranted()) {
            Log.d(TAG, "Camera permission already granted")
            startCamera()
        } else {
            Log.d(TAG, "Requesting camera permission")
            requestCameraPermission()
        }
    }

    private fun loadLabels() {
        try {
            assets.open("labels.txt").bufferedReader().useLines { lines ->
                labels.addAll(lines)
            }
            Log.d(TAG, "Loaded ${labels.size} labels.")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading labels from assets", e)
            Toast.makeText(this, "Could not load labels.txt from assets", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeModel() {
        try {
            // Load model file from assets
            val modelBuffer = loadModelFile("food_seg16.tflite")

            val compatList = CompatibilityList()

            // Configure interpreter options
            val options = Interpreter.Options().apply{
                if(compatList.isDelegateSupportedOnThisDevice){
                    // if the device has a supported GPU, add the GPU delegate
                    val delegateOptions = compatList.bestOptionsForThisDevice
                    this.addDelegate(GpuDelegate(delegateOptions))
                } else {
                    // if the GPU is not supported, run on 4 threads
                    this.setNumThreads(4)
                }
            }

            Log.d(TAG, "GPU delegate enabled")
            Log.d(TAG, "Using CPU inference with 4 threads")
            // Create interpreter
            interpreter = Interpreter(modelBuffer, options)

            // Log input/output tensor info
            logTensorInfo()

            Log.d(TAG, "Model loaded successfully")
            runOnUiThread {
                Toast.makeText(this, "Model loaded successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading TFLite model", e)
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Error loading model: ${e.message}\nMake sure food_seg.tflite is in assets folder",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun logTensorInfo() {
        interpreter?.let { interp ->
            // Input tensor info
            val inputIndex = 0
            val inputShape = interp.getInputTensor(inputIndex).shape()
            val inputType = interp.getInputTensor(inputIndex).dataType()
            Log.d(TAG, "Input shape: ${inputShape.joinToString()}, type: $inputType")

            // Output tensor info
            for (i in 0 until interp.outputTensorCount) {
                val shape = interp.getOutputTensor(i).shape()
                val type = interp.getOutputTensor(i).dataType()
                Log.d(TAG, "Output $i shape: ${shape.joinToString()}, type: $type")
            }
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(
                this,
                "Camera permission is needed to detect food items",
                Toast.LENGTH_LONG
            ).show()
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Camera permission granted")
                    startCamera()
                } else {
                    Toast.makeText(
                        this,
                        "Camera permission required",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun startCamera() {
        if (!isCameraPermissionGranted()) {
            Log.e(TAG, "Attempted to start camera without permission!")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(modelInputWidth, modelInputHeight))
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, ObjectDetectorAnalyzer())
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                Log.d(TAG, "Camera started successfully")
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
                Toast.makeText(this, "Camera error: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        interpreter?.close()
        gpuDelegate?.close()
        cameraExecutor.shutdown()
    }

    inner class ObjectDetectorAnalyzer : ImageAnalysis.Analyzer {
        private val analyzing = AtomicBoolean(false)
        private var frameCount = 0
        private val processEveryNthFrame = 5

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            frameCount++
            if (frameCount % processEveryNthFrame != 0) {
                imageProxy.close()
                return
            }

            if (!analyzing.compareAndSet(false, true)) {
                imageProxy.close()
                return
            }

            try {
                val mediaImage = imageProxy.image
                if (mediaImage != null && interpreter != null) {
                    // Create TensorImage
                    val tensorImage = TensorImage.fromBitmap(
                        imageProxyToBitmap(imageProxy)
                    )

                    // Preprocess image
                    val imageProcessor = ImageProcessor.Builder()
                        .add(ResizeOp(modelInputHeight, modelInputWidth, ResizeOp.ResizeMethod.BILINEAR))
                        .add(NormalizeOp(0f, 255f)) // Normalize to [0, 1]
                        .build()

                    val processedImage = imageProcessor.process(tensorImage)

                    // Convert to ByteBuffer for quantized model
                    val inputBuffer = convertTensorImageToByteBuffer(processedImage)

                    // Run inference
                    val results = runInference(inputBuffer, imageProxy.width, imageProxy.height)

                    if (results.isNotEmpty()) {

                        val stableResults = smoothResults(results)

                        runOnUiThread {
                            detectionOverlay.setDetectionResults(stableResults)
                        }
                        Log.d(TAG, "Detected ${results.size} objects")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing image", e)
            } finally {
                analyzing.set(false)
                imageProxy.close()
            }
        }
    }

    private fun convertTensorImageToByteBuffer(tensorImage: TensorImage): ByteBuffer {
        val buffer = tensorImage.buffer
        val byteBuffer = ByteBuffer.allocateDirect(buffer.capacity())
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.put(buffer)
        byteBuffer.rewind()
        return byteBuffer
    }

    @OptIn(ExperimentalGetImage::class)
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        // --- ADD THIS ROTATION LOGIC ---
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    private fun runInference(
        inputBuffer: ByteBuffer,
        imageWidth: Int,
        imageHeight: Int
    ): List<DetectionResult> {
        val interp = interpreter ?: return emptyList()

        return try {
            // Prepare output map
            val outputs = mutableMapOf<Int, Any>()
            outputs[0] = outputBuffer

            // Run inference
            interp.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

            // Post-processing
            val detectionResults = mutableListOf<DetectionResult>()

            // Transpose the output from [1, 139, 8400] to [1, 8400, 139] for easier processing
            val transposedOutput = Array(1) { Array(8400) { FloatArray(139) } }
            for (i in 0 until 8400) {
                for (j in 0 until 139) {
                    transposedOutput[0][i][j] = outputBuffer[0][j][i]
                }
            }

            val detections = transposedOutput[0]

            // Calculate actual number of classes from model output
            val numClasses = 139 - 4  // 135 classes
            Log.d(TAG, "Model has $numClasses classes, labels file has ${labels.size} labels")

            for (detection in detections) {
                // First 4 values are bbox: cx, cy, w, h (normalized 0-1)
                val cx = detection[0]
                val cy = detection[1]
                val w = detection[2]
                val h = detection[3]

                // Find the class with the highest score
                var maxScore = 0f
                var classId = -1
                // Only check classes that we have labels for
                val maxClassIndex = minOf(numClasses, labels.size)
                for (i in 4 until (4 + maxClassIndex)) {
                    if (detection[i] > maxScore) {
                        maxScore = detection[i]
                        classId = i - 4 // Class index is offset by 4
                    }
                }

                if (maxScore > 0.45f) { // Confidence threshold
                    // Check if classId is valid for our labels
                    if (classId >= 0 && classId < labels.size) {
                        val label = labels[classId].trim()

                        // Skip empty labels
                        if (label.isEmpty()) continue

                        val calorie = getCaloriesForLabel(label)

                        val previewWidth = previewView.width.toFloat()
                        val previewHeight = previewView.height.toFloat()

                        if (previewWidth > 0f && previewHeight > 0f) {
                            // Coordinates are normalized (0-1), convert to screen coordinates
                            // Also need to handle aspect ratio properly
                            val xmin = maxOf(0f, (cx - w / 2) * previewWidth)
                            val ymin = maxOf(0f, (cy - h / 2) * previewHeight)
                            val xmax = minOf(previewWidth, (cx + w / 2) * previewWidth)
                            val ymax = minOf(previewHeight, (cy + h / 2) * previewHeight)

                            // Only add valid bounding boxes
                            if (xmax > xmin && ymax > ymin) {
                                detectionResults.add(
                                    DetectionResult(
                                        foodName = label,
                                        confidence = maxScore,
                                        boundingBox = RectF(xmin, ymin, xmax, ymax),
                                        calories = calorie
                                    )
                                )
                                Log.d(TAG, "Added detection: $label at [$xmin, $ymin, $xmax, $ymax]")
                            }
                        }
                    } else {
                        Log.w(TAG, "ClassId $classId out of bounds (labels size: ${labels.size})")
                    }
                }
            }

            val nmsResults = applyNms(detectionResults)
            Log.d(TAG, "Final detections after NMS: ${nmsResults.size}")
            return nmsResults

        } catch (e: Exception) {
            Log.e(TAG, "Error running inference", e)
            emptyList()
        }
    }

    private fun applyNms(results: List<DetectionResult>): List<DetectionResult> {
        val sortedResults = results.sortedByDescending { it.confidence }
        val finalResults = mutableListOf<DetectionResult>()
        val iouThreshold = 0.45f

        for (result in sortedResults) {
            var shouldAdd = true
            for (finalResult in finalResults) {
                if (calculateIoU(result.boundingBox, finalResult.boundingBox) > iouThreshold) {
                    shouldAdd = false
                    break
                }
            }
            if (shouldAdd) {
                finalResults.add(result)
            }
        }
        return finalResults
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val xA = maxOf(box1.left, box2.left)
        val yA = maxOf(box1.top, box2.top)
        val xB = minOf(box1.right, box2.right)
        val yB = minOf(box1.bottom, box2.bottom)

        val intersectionArea = maxOf(0f, xB - xA) * maxOf(0f, yB - yA)

        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)

        val unionArea = box1Area + box2Area - intersectionArea

        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }


    data class DetectionResult(
        val foodName: String,
        val confidence: Float,
        val boundingBox: RectF,
        val calories: Int
    )

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val TAG = "CameraActivity"
    }
}

data class FoodInfo(
    val calories: Int,  // kcal per 100g (standard estimate)
    val protein: Float = 0.0f,
    val fat: Float = 0.0f,
    val carbs: Float = 0.0f
)

// Update your data class
data class DetectionResult(
    val foodName: String,
    val confidence: Float,
    val boundingBox: RectF,
    val calories: Int,
    val unit: String = "per 100g"
)