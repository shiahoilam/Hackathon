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
        "candy" to FoodInfo(394, 1.20f, 40, 0.0f, 0.2f, 99.0f),            // density approx (processed confectionery)
        "egg tart" to FoodInfo(350, 0.90f, 80, 6.5f, 18.0f, 40.0f),        // approx (bakery product)
        "french fries" to FoodInfo(312, 0.45f, 120, 3.4f, 15.0f, 41.0f),   // fries (USDA composite)
        "chocolate" to FoodInfo(546, 1.30f, 40, 4.9f, 31.0f, 61.0f),       // solid milk/dark chocolate (USDA)
        "biscuit" to FoodInfo(488, 0.45f, 30, 6.5f, 21.0f, 68.0f),         // cookie/biscuit (USDA)
        "popcorn" to FoodInfo(387, 0.10f, 30, 12.6f, 4.5f, 78.0f),         // popped popcorn bulk density (approx)
        "pudding" to FoodInfo(131, 1.02f, 150, 2.8f, 2.7f, 23.0f),        // dairy pudding (USDA)
        "ice cream" to FoodInfo(207, 0.61f, 100, 3.5f, 11.0f, 24.0f),      // vanilla ice cream (FAO/USDA)
        "cheese butter" to FoodInfo(717, 0.91f, 14, 0.9f, 81.0f, 0.1f),    // butter (USDA/FAO)
        "cake" to FoodInfo(337, 0.42f, 100, 4.9f, 13.0f, 51.0f),           // sponge cake (FAO/USDA)
        "wine" to FoodInfo(85, 0.99f, 150, 0.1f, 0.0f, 2.6f),              // table wine (USDA)
        "milkshake" to FoodInfo(119, 1.02f, 250, 3.3f, 3.4f, 19.0f),      // milkshake (USDA composite; approx)
        "coffee" to FoodInfo(2, 0.99f, 240, 0.3f, 0.0f, 0.0f),            // brewed coffee (USDA)
        "juice" to FoodInfo(45, 1.04f, 250, 0.7f, 0.2f, 10.4f),           // fruit juice (USDA)
        "milk" to FoodInfo(61, 1.03f, 250, 3.2f, 3.3f, 4.8f),             // whole milk (USDA)
        "tea" to FoodInfo(1, 0.99f, 240, 0.0f, 0.0f, 0.3f),               // brewed tea (USDA)
        "almond" to FoodInfo(579, 0.55f, 28, 21.0f, 50.0f, 22.0f),        // almonds (FAO/USDA)
        "red beans" to FoodInfo(333, 0.75f, 100, 24.0f, 1.0f, 60.0f),     // red kidney / adzuki (USDA)
        "cashew" to FoodInfo(553, 0.50f, 28, 18.0f, 44.0f, 30.0f),        // cashews (FAO/USDA)
        "dried cranberries" to FoodInfo(308, 0.60f, 40, 0.2f, 1.4f, 82.0f),// sweetened dried cranberries (USDA)
        "soy" to FoodInfo(173, 1.00f, 100, 17.0f, 9.0f, 10.0f),           // soybeans (cooked / USDA)
        "walnut" to FoodInfo(654, 0.60f, 28, 15.0f, 65.0f, 14.0f),        // walnuts (USDA)
        "peanut" to FoodInfo(567, 0.53f, 28, 26.0f, 49.0f, 16.0f),        // peanuts (USDA)
        "egg" to FoodInfo(155, 1.03f, 50, 13.0f, 11.0f, 1.1f),            // whole egg (USDA)
        "apple" to FoodInfo(52, 0.53f, 150, 0.3f, 0.2f, 14.0f),           // raw apple (USDA/FAO)
        "date" to FoodInfo(282, 1.30f, 24, 2.5f, 0.4f, 75.0f),           // dried dates (USDA)
        "apricot" to FoodInfo(48, 0.50f, 35, 1.4f, 0.4f, 11.0f),         // fresh apricot (USDA)
        "avocado" to FoodInfo(160, 0.95f, 100, 2.0f, 15.0f, 8.5f),       // avocado (USDA)
        "banana" to FoodInfo(89, 0.94f, 118, 1.1f, 0.3f, 23.0f),         // raw banana (USDA)
        "strawberry" to FoodInfo(32, 0.60f, 100, 0.7f, 0.3f, 7.7f),      // raw strawberries (USDA)
        "cherry" to FoodInfo(63, 0.60f, 150, 1.1f, 0.2f, 16.0f),         // sweet cherries (USDA)
        "blueberry" to FoodInfo(57, 0.57f, 100, 0.7f, 0.3f, 14.0f),      // raw blueberries (USDA)
        "raspberry" to FoodInfo(52, 0.60f, 100, 1.2f, 0.7f, 12.0f),      // raw raspberries (USDA)
        "mango" to FoodInfo(60, 0.55f, 165, 0.8f, 0.4f, 15.0f),          // raw mango (USDA)
        "olives" to FoodInfo(115, 0.92f, 15, 0.8f, 11.0f, 6.3f),         // table olives (USDA)
        "peach" to FoodInfo(39, 0.60f, 150, 0.9f, 0.3f, 9.5f),           // raw peach (USDA)
        "lemon" to FoodInfo(29, 0.53f, 58, 1.1f, 0.3f, 9.3f),            // raw lemon (USDA)
        "pear" to FoodInfo(57, 0.56f, 150, 0.4f, 0.1f, 15.0f),           // raw pear (USDA)
        "fig" to FoodInfo(74, 0.75f, 50, 0.8f, 0.3f, 19.0f),             // fresh fig (USDA)
        "pineapple" to FoodInfo(50, 0.52f, 165, 0.5f, 0.1f, 13.0f),      // fresh pineapple (USDA)
        "grape" to FoodInfo(69, 0.65f, 151, 0.7f, 0.2f, 18.0f),          // table grapes (USDA)
        "kiwi" to FoodInfo(61, 0.62f, 76, 1.1f, 0.5f, 15.0f),            // raw kiwi (USDA)
        "melon" to FoodInfo(34, 0.45f, 150, 0.8f, 0.2f, 8.2f),           // cantaloupe / melon (USDA)
        "orange" to FoodInfo(47, 0.55f, 131, 0.9f, 0.1f, 12.0f),         // raw orange (USDA)
        "watermelon" to FoodInfo(30, 0.45f, 150, 0.6f, 0.2f, 7.6f),      // raw watermelon (USDA)
        "steak" to FoodInfo(271, 1.05f, 150, 25.0f, 19.0f, 0.0f),        // beef steak (cooked average, USDA)
        "pork" to FoodInfo(242, 1.04f, 100, 27.0f, 14.0f, 0.0f),         // pork (cooked, USDA)
        "chicken duck" to FoodInfo(337, 1.02f, 150, 19.0f, 28.0f, 0.0f), // mixed/roasted (approx; composition varies)
        "sausage" to FoodInfo(301, 0.95f, 80, 13.0f, 27.0f, 3.0f),       // pork sausage (USDA)
        "fried meat" to FoodInfo(290, 1.00f, 100, 26.0f, 20.0f, 1.0f),   // fried battered meat (approx)
        "lamb" to FoodInfo(294, 1.05f, 100, 25.0f, 21.0f, 0.0f),        // lamb (cooked, USDA)
        "sauce" to FoodInfo(120, 0.98f, 30, 2.0f, 8.0f, 12.0f),         // generic sauce (approx)
        "crab" to FoodInfo(97, 1.02f, 100, 19.0f, 1.3f, 0.7f),          // crab meat (USDA)
        "fish" to FoodInfo(208, 1.03f, 150, 20.0f, 13.0f, 0.0f),        // oily fish average (USDA)
        "shellfish" to FoodInfo(85, 1.02f, 100, 15.0f, 1.5f, 4.0f),     // mixed shellfish (approx)
        "shrimp" to FoodInfo(99, 1.02f, 100, 24.0f, 0.3f, 0.2f),        // shrimp (USDA)
        "soup" to FoodInfo(38, 1.01f, 250, 2.5f, 1.2f, 5.0f),          // clear soup (USDA / approx)
        "bread" to FoodInfo(265, 0.25f, 50, 9.0f, 3.2f, 49.0f),         // white bread (USDA)
        "corn" to FoodInfo(86, 0.60f, 90, 3.3f, 1.4f, 19.0f),           // sweet corn (cooked, USDA)
        "hamburg" to FoodInfo(295, 0.85f, 150, 17.0f, 14.0f, 27.0f),    // burger (typical cooked avg)
        "pizza" to FoodInfo(266, 0.68f, 150, 11.0f, 10.0f, 33.0f),      // pizza (average slice per 100 g values)
        "hanamaki baozi" to FoodInfo(221, 0.55f, 100, 7.0f, 0.9f, 47.0f),// steamed bun (approx)
        "wonton dumplings" to FoodInfo(206, 0.60f, 150, 8.0f, 7.0f, 28.0f),// wonton (boiled, approx)
        "pasta" to FoodInfo(131, 0.60f, 180, 5.0f, 1.1f, 25.0f),        // cooked pasta (USDA)
        "noodles" to FoodInfo(138, 0.60f, 180, 4.5f, 0.5f, 28.0f),      // cooked noodles (approx)
        "rice" to FoodInfo(130, 0.85f, 150, 2.7f, 0.3f, 28.0f),         // cooked white rice (USDA)
        "pie" to FoodInfo(295, 0.50f, 150, 3.8f, 15.0f, 37.0f),         // savory/pastry pie (approx)
        "tofu" to FoodInfo(76, 1.06f, 100, 8.0f, 4.8f, 1.9f),           // firm tofu (USDA)
        "eggplant" to FoodInfo(25, 0.92f, 100, 1.0f, 0.2f, 5.9f),       // raw eggplant (USDA)
        "potato" to FoodInfo(77, 0.71f, 150, 2.0f, 0.1f, 17.0f),        // boiled potato (USDA)
        "garlic" to FoodInfo(149, 0.60f, 3, 6.4f, 0.5f, 33.0f),         // raw garlic (USDA)
        "cauliflower" to FoodInfo(25, 0.22f, 100, 1.9f, 0.3f, 5.0f),    // raw cauliflower (FAO/USDA)
        "tomato" to FoodInfo(18, 0.95f, 100, 0.9f, 0.2f, 3.9f),         // raw tomato (USDA)
        "kelp" to FoodInfo(43, 0.12f, 50, 1.7f, 0.6f, 9.6f),            // kelp/sea vegetable (FAO/approx)
        "seaweed" to FoodInfo(45, 0.35f, 5, 6.0f, 0.6f, 9.0f),         // dried seaweed (approx)
        "spring onion" to FoodInfo(32, 0.20f, 10, 1.8f, 0.2f, 7.3f),    // scallion (USDA)
        "rape" to FoodInfo(13, 0.20f, 85, 1.5f, 0.2f, 2.2f),            // rapeseed/leaf veg (approx)
        "ginger" to FoodInfo(80, 0.75f, 5, 1.8f, 0.8f, 18.0f),         // raw ginger (USDA)
        "okra" to FoodInfo(33, 0.60f, 100, 1.9f, 0.2f, 7.5f),           // okra (USDA)
        "lettuce" to FoodInfo(15, 0.14f, 50, 1.4f, 0.2f, 2.9f),         // lettuce (USDA)
        "pumpkin" to FoodInfo(26, 0.44f, 100, 1.0f, 0.1f, 6.5f),        // pumpkin (USDA)
        "cucumber" to FoodInfo(16, 0.96f, 100, 0.7f, 0.1f, 3.6f),       // cucumber (USDA)
        "white radish" to FoodInfo(18, 0.90f, 100, 0.7f, 0.1f, 4.2f),   // daikon / radish (USDA)
        "carrot" to FoodInfo(41, 0.64f, 61, 0.9f, 0.2f, 9.6f),          // raw carrot (USDA)
        "asparagus" to FoodInfo(20, 0.24f, 90, 2.2f, 0.2f, 3.9f),       // asparagus (FAO/USDA)
        "bamboo shoots" to FoodInfo(27, 0.70f, 100, 2.6f, 0.3f, 5.2f),  // bamboo shoots (approx)
        "broccoli" to FoodInfo(34, 0.31f, 91, 2.8f, 0.4f, 6.6f),        // raw broccoli (USDA)
        "celery stick" to FoodInfo(16, 0.14f, 40, 0.7f, 0.2f, 3.0f),    // celery (USDA)
        "cilantro mint" to FoodInfo(23, 0.20f, 10, 2.1f, 0.5f, 3.7f),   // fresh herbs (approx)
        "snow peas" to FoodInfo(42, 0.70f, 100, 2.8f, 0.2f, 7.6f),     // snow peas (USDA)
        "cabbage" to FoodInfo(25, 0.25f, 100, 1.3f, 0.1f, 5.8f),        // raw cabbage (USDA)
        "bean sprouts" to FoodInfo(30, 0.34f, 100, 3.1f, 0.2f, 5.9f),   // mung bean sprouts (USDA)
        "onion" to FoodInfo(40, 0.60f, 110, 1.1f, 0.1f, 9.3f),          // raw onion (USDA)
        "pepper" to FoodInfo(31, 0.20f, 100, 1.0f, 0.3f, 6.0f),         // bell pepper (USDA)
        "green beans" to FoodInfo(31, 0.70f, 100, 1.8f, 0.2f, 7.0f),    // green beans (USDA)
        "French beans" to FoodInfo(31, 0.70f, 100, 1.8f, 0.1f, 7.0f),   // same as green beans (naming)
        "king oyster mushroom" to FoodInfo(43, 0.40f, 100, 3.3f, 0.4f, 7.6f), // king oyster (approx)
        "shiitake" to FoodInfo(34, 0.40f, 100, 2.2f, 0.5f, 6.8f),       // shiitake mushroom (USDA/approx)
        "enoki mushroom" to FoodInfo(37, 0.45f, 100, 2.7f, 0.3f, 7.8f), // enoki (approx)
        "oyster mushroom" to FoodInfo(33, 0.35f, 100, 3.3f, 0.4f, 6.1f),// oyster mushroom (approx)
        "white button mushroom" to FoodInfo(22, 0.30f, 100, 3.1f, 0.3f, 3.3f), // white button (USDA)
        "salad" to FoodInfo(15, 0.20f, 85, 1.2f, 0.2f, 3.0f),          // mixed salad (approx)
        "other ingredients" to FoodInfo(0, 0.00f, 0, 0.0f, 0.0f, 0.0f)
    
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