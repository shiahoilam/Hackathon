package com.example.hackathon

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.media.Image
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    private lateinit var surfaceView: GLSurfaceView
    private lateinit var detectionOverlay: DetectionOverlayView

    private var session: Session? = null
    private val backgroundRenderer = BackgroundRenderer()
    private var installRequested = false

    // TFLite
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private val modelInputWidth = 640
    private val modelInputHeight = 640
    private val outputBuffer = Array(1) { Array(139) { FloatArray(8400) } }
    private val detectionExecutor = Executors.newSingleThreadExecutor()
    private val isDetecting = AtomicBoolean(false)

    // Data
    private val labels = mutableListOf<String>()

    // UPDATED DATABASE: Added Standard Portion Sizes (in grams)
    private val foodDatabase = mapOf(
        "apple" to FoodInfo(52, 0.80f, standardPortion = 150),   // 1 medium apple
        "banana" to FoodInfo(89, 0.94f, standardPortion = 120), // 1 medium banana
        "orange" to FoodInfo(47, 0.85f, standardPortion = 130),
        "bread" to FoodInfo(265, 0.25f, standardPortion = 60),  // 2 slices
        "steak" to FoodInfo(271, 1.05f, standardPortion = 225), // 8 oz steak
        "rice" to FoodInfo(130, 0.75f, standardPortion = 150),  // 1 cup cooked
        "carrot" to FoodInfo(41, 0.65f, standardPortion = 80),
        "egg" to FoodInfo(155, 1.03f, standardPortion = 50),    // 1 large egg
        "default" to FoodInfo(100, 0.85f, standardPortion = 150)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        surfaceView = findViewById(R.id.previewView)
        detectionOverlay = findViewById(R.id.detectionOverlay)

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        setupSurfaceView()
        loadLabels()
        initializeModel()
    }

    private fun setupSurfaceView() {
        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        surfaceView.setRenderer(this)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    override fun onResume() {
        super.onResume()

        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        if (session == null) {
            try {
                when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        return
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> { }
                }

                session = Session(this)
                val config = session!!.config
                if (session!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    config.depthMode = Config.DepthMode.AUTOMATIC
                }
                config.focusMode = Config.FocusMode.AUTO
                session!!.configure(config)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to create AR Session", e)
                return
            }
        }

        if (session != null && backgroundRenderer.getTextureId() != -1) {
            session!!.setCameraTextureName(backgroundRenderer.getTextureId())
        }

        try {
            session!!.resume()
        } catch (e: CameraNotAvailableException) {
            session = null
            return
        }
        surfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (session != null) {
            surfaceView.onPause()
            session!!.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.close()
        session = null
        interpreter?.close()
        gpuDelegate?.close()
        detectionExecutor.shutdown()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundRenderer.createOnGlThread(this)
        session?.setCameraTextureName(backgroundRenderer.getTextureId())
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        session?.setDisplayGeometry(0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val session = session ?: return
        try {
            val frame = session.update()
            backgroundRenderer.draw(frame)

            if (!isDetecting.get()) {
                try {
                    val cameraImage = frame.acquireCameraImage()
                    val depthImage = try { frame.acquireDepthImage16Bits() } catch (e: Exception) { null }

                    if (cameraImage != null) {
                        isDetecting.set(true)
                        val intrinsics = frame.camera.imageIntrinsics
                        detectionExecutor.execute { processImage(cameraImage, depthImage, intrinsics) }
                    }
                } catch (e: Exception) {
                    isDetecting.set(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in draw frame", e)
        }
    }

    private fun processImage(cameraImage: Image, depthImage: Image?, intrinsics: com.google.ar.core.CameraIntrinsics) {
        try {
            val bitmap = yuvToBitmap(cameraImage)
            cameraImage.close()

            val tensorImage = TensorImage.fromBitmap(bitmap)
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(modelInputHeight, modelInputWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()
            val processedImage = imageProcessor.process(tensorImage)

            val inputBuffer = convertTensorImageToByteBuffer(processedImage)
            val rawResults = runInference(inputBuffer)

            val finalResults = rawResults.map { result ->
                if (depthImage != null) {
                    val info = foodDatabase[result.foodName.lowercase()] ?: foodDatabase["default"]!!

                    val weight = VolumeEstimator.calculateWeight(
                        result.boundingBox,
                        depthImage,
                        intrinsics,
                        info.density,
                        surfaceView.width,
                        surfaceView.height
                    )

                    val realCalories = (info.calories * (weight / 100f)).toInt()

                    result.copy(
                        weightGrams = weight,
                        calories = realCalories,
                        unit = "($weight g)",
                        standardPortion = info.standardPortion // Pass the target portion
                    )
                } else {
                    result
                }
            }

            depthImage?.close()
            runOnUiThread { detectionOverlay.setDetectionResults(finalResults) }

        } catch (e: Exception) {
            Log.e(TAG, "Detection failed", e)
            cameraImage.close()
            depthImage?.close()
        } finally {
            isDetecting.set(false)
        }
    }

    private fun yuvToBitmap(image: Image): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 80, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val matrix = Matrix()
        matrix.postRotate(90f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private fun requestCameraPermission() = ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)

    // Standard boiler plate...
    private fun loadLabels() {
        try { assets.open("labels.txt").bufferedReader().useLines { lines -> labels.addAll(lines) } } catch (e: Exception) {}
    }
    private fun initializeModel() {
        try {
            val modelBuffer = loadModelFile("food_seg16.tflite")
            val compatList = CompatibilityList()
            val options = Interpreter.Options().apply {
                if(compatList.isDelegateSupportedOnThisDevice) addDelegate(GpuDelegate(compatList.bestOptionsForThisDevice))
                else setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {}
    }
    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }
    private fun convertTensorImageToByteBuffer(tensorImage: TensorImage): ByteBuffer {
        val buffer = tensorImage.buffer
        val byteBuffer = ByteBuffer.allocateDirect(buffer.capacity())
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.put(buffer)
        byteBuffer.rewind()
        return byteBuffer
    }
    private fun runInference(inputBuffer: ByteBuffer): List<DetectionResult> {
        val interp = interpreter ?: return emptyList()
        val outputs = mutableMapOf<Int, Any>()
        outputs[0] = outputBuffer
        interp.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
        return parseRawOutput(outputBuffer)
    }
    private fun parseRawOutput(outputBuffer: Array<Array<FloatArray>>): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        val transposedOutput = Array(1) { Array(8400) { FloatArray(139) } }
        for (i in 0 until 8400) {
            for (j in 0 until 139) { transposedOutput[0][i][j] = outputBuffer[0][j][i] }
        }
        val detections = transposedOutput[0]
        val numClasses = 139 - 4
        for (detection in detections) {
            val cx = detection[0]
            val cy = detection[1]
            val w = detection[2]
            val h = detection[3]
            var maxScore = 0f
            var classId = -1
            val maxClassIndex = minOf(numClasses, labels.size)
            for (i in 4 until (4 + maxClassIndex)) {
                if (detection[i] > maxScore) { maxScore = detection[i]; classId = i - 4 }
            }
            if (maxScore > 0.45f && classId >= 0 && classId < labels.size) {
                val label = labels[classId].trim()
                if (label.isNotEmpty()) {
                    val previewWidth = surfaceView.width.toFloat()
                    val previewHeight = surfaceView.height.toFloat()
                    if (previewWidth > 0f && previewHeight > 0f) {
                        val xmin = maxOf(0f, (cx - w / 2) * previewWidth)
                        val ymin = maxOf(0f, (cy - h / 2) * previewHeight)
                        val xmax = minOf(previewWidth, (cx + w / 2) * previewWidth)
                        val ymax = minOf(previewHeight, (cy + h / 2) * previewHeight)
                        if (xmax > xmin && ymax > ymin) {
                            val info = foodDatabase[label.lowercase()] ?: foodDatabase["default"]!!
                            results.add(DetectionResult(label, maxScore, RectF(xmin, ymin, xmax, ymax), info.calories))
                        }
                    }
                }
            }
        }
        return applyNms(results)
    }
    private fun applyNms(results: List<DetectionResult>): List<DetectionResult> {
        val sortedResults = results.sortedByDescending { it.confidence }
        val finalResults = mutableListOf<DetectionResult>()
        for (result in sortedResults) {
            var shouldAdd = true
            for (finalResult in finalResults) {
                if (calculateIoU(result.boundingBox, finalResult.boundingBox) > 0.45f) { shouldAdd = false; break }
            }
            if (shouldAdd) finalResults.add(result)
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
    companion object { private const val TAG = "CameraActivity" }
}