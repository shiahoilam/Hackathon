//package com.example.hackathon
//
//import android.content.Context
//import android.graphics.RectF
//import android.opengl.GLES20
//import android.opengl.GLSurfaceView
//import android.opengl.Matrix
//import javax.microedition.khronos.egl.EGLConfig
//import javax.microedition.khronos.opengles.GL10
//
//class Model3DRenderer(private val context: Context) : GLSurfaceView.Renderer {
//
//    private lateinit var modelData: ModelData
//    private var program = 0
//    private val mvpMatrix = FloatArray(16)
//    private val projectionMatrix = FloatArray(16)
//    private val viewMatrix = FloatArray(16)
//    private val modelMatrix = FloatArray(16)
//
//    var detectedBoxes = listOf<RectF>()
//    var surfaceWidth = 1
//    var surfaceHeight = 1
//
//    private val vertexShaderCode = """
//        uniform mat4 uMVPMatrix;
//        attribute vec4 vPosition;
//        void main() {
//            gl_Position = uMVPMatrix * vPosition;
//        }
//    """.trimIndent()
//
//    private val fragmentShaderCode = """
//        precision mediump float;
//        uniform vec4 vColor;
//        void main() {
//            gl_FragColor = vColor;
//        }
//    """.trimIndent()
//
//    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//        GLES20.glClearColor(0f, 0f, 0f, 0f)
//        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
//
//        // Load 3D model
//        val loader = GLBModelLoader(context)
//        modelData = loader.loadModel("apple.glb")
//
//        // Create shader program
//        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
//        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
//
//        program = GLES20.glCreateProgram()
//        GLES20.glAttachShader(program, vertexShader)
//        GLES20.glAttachShader(program, fragmentShader)
//        GLES20.glLinkProgram(program)
//    }
//
//    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
//        GLES20.glViewport(0, 0, width, height)
//        surfaceWidth = width
//        surfaceHeight = height
//
//        val ratio = width.toFloat() / height.toFloat()
//        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
//    }
//
//    override fun onDrawFrame(gl: GL10?) {
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
//
//        if (detectedBoxes.isEmpty()) return
//
//        GLES20.glUseProgram(program)
//
//        // Set camera position
//        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
//
//        // Draw model for each detected object
//        for (box in detectedBoxes) {
//            drawModelAtPosition(box)
//        }
//    }
//
//    private fun drawModelAtPosition(box: RectF) {
//        Matrix.setIdentityM(modelMatrix, 0)
//
//        // Convert screen coordinates to OpenGL coordinates
//        val centerX = (box.centerX() / surfaceWidth) * 2f - 1f
//        val centerY = 1f - (box.centerY() / surfaceHeight) * 2f
//
//        // Position model
//        Matrix.translateM(modelMatrix, 0, centerX * 2f, centerY * 2f, 0f)
//
//        // Scale model based on box size
//        val scale = (box.width() / surfaceWidth) * 2f
//        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
//
//        // Rotate for visual effect
//        val time = System.currentTimeMillis() % 10000L / 10000f
//        Matrix.rotateM(modelMatrix, 0, time * 360f, 0f, 1f, 0f)
//
//        // Calculate MVP matrix
//        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
//        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
//
//        // Get shader handles
//        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
//        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
//        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
//
//        // Enable vertex array
//        GLES20.glEnableVertexAttribArray(positionHandle)
//
//        // Pass vertex data
//        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, modelData.vertices)
//
//        // Set color (red apple color)
//        GLES20.glUniform4fv(colorHandle, 1, floatArrayOf(0.8f, 0.2f, 0.2f, 1f), 0)
//
//        // Pass MVP matrix
//        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
//
//        // Draw
//        GLES20.glDrawElements(GLES20.GL_TRIANGLES, modelData.indexCount, GLES20.GL_UNSIGNED_SHORT, modelData.indices)
//
//        // Disable vertex array
//        GLES20.glDisableVertexAttribArray(positionHandle)
//    }
//
//    private fun loadShader(type: Int, shaderCode: String): Int {
//        val shader = GLES20.glCreateShader(type)
//        GLES20.glShaderSource(shader, shaderCode)
//        GLES20.glCompileShader(shader)
//        return shader
//    }
//}









//package com.example.hackathon
//
//import android.content.Context
//import android.graphics.RectF
//import android.opengl.GLES20
//import android.opengl.GLSurfaceView
//import android.opengl.Matrix
//import android.util.Log
//import javax.microedition.khronos.egl.EGLConfig
//import javax.microedition.khronos.opengles.GL10

//class Model3DRenderer(private val context: Context) : GLSurfaceView.Renderer {
//
//    private var modelData: ModelData? = null
//    private var program = 0
//    private val mvpMatrix = FloatArray(16)
//    private val projectionMatrix = FloatArray(16)
//    private val viewMatrix = FloatArray(16)
//    private val modelMatrix = FloatArray(16)
//
//    var detectedBoxes = listOf<RectF>()
//    var surfaceWidth = 1
//    var surfaceHeight = 1
//
//    private val vertexShaderCode = """
//        uniform mat4 uMVPMatrix;
//        attribute vec4 vPosition;
//        void main() {
//            gl_Position = uMVPMatrix * vPosition;
//        }
//    """.trimIndent()
//
//    private val fragmentShaderCode = """
//        precision mediump float;
//        uniform vec4 vColor;
//        void main() {
//            gl_FragColor = vColor;
//        }
//    """.trimIndent()
//
//    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//        Log.d("Model3DRenderer", "onSurfaceCreated - Loading model")
//        // CRITICAL: Set clear color to fully transparent (alpha = 0)
//        GLES20.glClearColor(0f, 0f, 0f, 0f)
//
//        // Disable depth test initially or it might block transparency
//        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
//
//        // Enable blending for transparency
//        GLES20.glEnable(GLES20.GL_BLEND)
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
//
//        try {
//            // Try to load 3D model
//            val loader = GLBModelLoader(context)
////            modelData = loader.loadModel("apple.glb")
//            try {
//                val modelStream = context.assets.open("apple.glb")
//                // Proceed with loading the model
//            } catch (e: Exception) {
//                android.util.Log.d("ModelLoading", "Error loading apple.glb: ${e.message}")
//            }
//
//            android.util.Log.d("Model3DRenderer", "Model loaded successfully")
//        } catch (e: Exception) {
//            android.util.Log.e("Model3DRenderer", "Error loading model, will use fallback sphere: ${e.message}")
//            e.printStackTrace()
//            // Create a simple sphere as fallback
//            modelData = createSimpleSphere()
//        }
//
//        // Create shader program
//        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
//        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
//
//        program = GLES20.glCreateProgram()
//        GLES20.glAttachShader(program, vertexShader)
//        GLES20.glAttachShader(program, fragmentShader)
//        GLES20.glLinkProgram(program)
//
//        // Check if program linked successfully
//        val linkStatus = IntArray(1)
//        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
//        if (linkStatus[0] == 0) {
//            android.util.Log.e("Model3DRenderer", "Program linking failed")
//        }
//    }
//
//    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
//        GLES20.glViewport(0, 0, width, height)
//        surfaceWidth = width
//        surfaceHeight = height
//
//        val ratio = width.toFloat() / height.toFloat()
//        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
//    }
//
//    override fun onDrawFrame(gl: GL10?) {
//        // Clear with TRANSPARENT background - this is crucial!
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//
//        if (modelData == null) {
//            Log.e("Model3DRenderer", "Model data is null, skipping rendering")
//            return  // Skip rendering if model is null
//        }
//
//
//        if (detectedBoxes.isEmpty()) {
//            android.util.Log.d("Model3DRenderer", "No boxes to draw")
//            return
//        }
//
//        val model = modelData
//        if (model == null) {
//            android.util.Log.e("Model3DRenderer", "Model data is null")
//            return
//        }
//
//        GLES20.glUseProgram(program)
//
//        android.util.Log.d("Model3DRenderer", "Detected Boxes: $detectedBoxes")
//
//        // Set camera position
//        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
//
//        // Draw model for each detected object (usually just the first apple)
//        for ((index, box) in detectedBoxes.withIndex()) {
//            android.util.Log.d("Model3DRenderer", "Drawing model at box $index: $box")
//            drawModelAtPosition(box, model)
//        }
//    }
//
//    private fun drawModelAtPosition(box: RectF, model: ModelData) {
//        Matrix.setIdentityM(modelMatrix, 0)
//
//        // Convert screen coordinates to normalized device coordinates
//        val centerX = (box.centerX() / surfaceWidth) * 2f - 1f
//        val centerY = 1f - (box.centerY() / surfaceHeight) * 2f
//
//
//
//        // Position model
//        Matrix.translateM(modelMatrix, 0, centerX, centerY, 0f)
//
//        // Scale model based on box size
//        val scale = (box.width() / surfaceWidth) * 1.5f
//        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
//
//        android.util.Log.e("Model3DRenderer", "Model scaling factor: $scale, Position: $centerX, $centerY")
//
//
//        // Rotate for visual effect
//        val time = System.currentTimeMillis() % 4000L / 4000f
//        Matrix.rotateM(modelMatrix, 0, time * 360f, 0f, 1f, 0f)
//
//        // Calculate MVP matrix
//        val tempMatrix = FloatArray(16)
//        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
//        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
//
//        // Get shader handles
//        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
//        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
//        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
//
//        // Enable vertex array
//        GLES20.glEnableVertexAttribArray(positionHandle)
//
//        // Pass vertex data
//        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, model.vertices)
//
//        // Set color (bright red apple color)
//        GLES20.glUniform4fv(colorHandle, 1, floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f), 0)
//
//        // Pass MVP matrix
//        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
//
//        // Draw
//        GLES20.glDrawElements(GLES20.GL_TRIANGLES, model.indexCount, GLES20.GL_UNSIGNED_SHORT, model.indices)
//
//        // Disable vertex array
//        GLES20.glDisableVertexAttribArray(positionHandle)
//    }
//
//    private fun loadShader(type: Int, shaderCode: String): Int {
//        val shader = GLES20.glCreateShader(type)
//        GLES20.glShaderSource(shader, shaderCode)
//        GLES20.glCompileShader(shader)
//
//        // Check compilation status
//        val compiled = IntArray(1)
//        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
//        if (compiled[0] == 0) {
//            android.util.Log.e("Model3DRenderer", "Shader compilation failed: " + GLES20.glGetShaderInfoLog(shader))
//        }
//
//        return shader
//    }
//
//    // Fallback: Create a simple sphere if GLB loading fails
//    private fun createSimpleSphere(): ModelData {
//        val vertices = mutableListOf<Float>()
//        val indices = mutableListOf<Short>()
//
//        val radius = 0.5f
//        val stacks = 10
//        val slices = 10
//
//        // Generate sphere vertices
//        for (i in 0..stacks) {
//            val phi = Math.PI * i / stacks
//            for (j in 0..slices) {
//                val theta = 2 * Math.PI * j / slices
//                val x = (radius * Math.sin(phi) * Math.cos(theta)).toFloat()
//                val y = (radius * Math.cos(phi)).toFloat()
//                val z = (radius * Math.sin(phi) * Math.sin(theta)).toFloat()
//                vertices.add(x)
//                vertices.add(y)
//                vertices.add(z)
//            }
//        }
//
//        // Generate indices
//        for (i in 0 until stacks) {
//            for (j in 0 until slices) {
//                val first = (i * (slices + 1) + j).toShort()
//                val second = (first + slices + 1).toShort()
//
//                indices.add(first)
//                indices.add(second)
//                indices.add((first + 1).toShort())
//
//                indices.add(second)
//                indices.add((second + 1).toShort())
//                indices.add((first + 1).toShort())
//            }
//        }
//
//        val vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertices.size * 4)
//            .order(java.nio.ByteOrder.nativeOrder())
//            .asFloatBuffer()
//            .put(vertices.toFloatArray())
//        vertexBuffer.position(0)
//
//        val indexBuffer = java.nio.ByteBuffer.allocateDirect(indices.size * 2)
//            .order(java.nio.ByteOrder.nativeOrder())
//            .asShortBuffer()
//            .put(indices.toShortArray())
//        indexBuffer.position(0)
//
//        return ModelData(vertexBuffer, indexBuffer, indices.size)
//    }
//}





//
//package com.example.hackathon
//
//
//import android.content.Context
//import android.graphics.RectF
//import android.opengl.GLES20
//import android.opengl.GLSurfaceView
//import android.opengl.Matrix
//import android.util.Log
//import java.io.FileNotFoundException  // <-- Add this import
//import java.io.InputStream
//import javax.microedition.khronos.opengles.GL10
//import javax.microedition.khronos.egl.EGLConfig
//
//
//class Model3DRenderer(private val context: Context) : GLSurfaceView.Renderer {
//
//    private var modelData: ModelData? = null
//    private var program = 0
//    private val mvpMatrix = FloatArray(16)
//    private val projectionMatrix = FloatArray(16)
//    private val viewMatrix = FloatArray(16)
//    private val modelMatrix = FloatArray(16)
//
//    var detectedBoxes = listOf<RectF>()
//    var surfaceWidth = 1
//    var surfaceHeight = 1
//
//    private val vertexShaderCode = """
//        uniform mat4 uMVPMatrix;
//        attribute vec4 vPosition;
//        void main() {
//            gl_Position = uMVPMatrix * vPosition;
//        }
//    """.trimIndent()
//
//    private val fragmentShaderCode = """
//        precision mediump float;
//        uniform vec4 vColor;
//        void main() {
//            gl_FragColor = vColor;
//        }
//    """.trimIndent()
//
//    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//        Log.d("Model3DRenderer", "onSurfaceCreated - Loading model")
//
//        // Set clear color to fully transparent (alpha = 0)
//        GLES20.glClearColor(0f, 0f, 0f, 0f)
//
//        // Disable depth test initially or it might block transparency
//        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
//
//        // Enable blending for transparency
//        GLES20.glEnable(GLES20.GL_BLEND)
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
//
//        // Load model from assets
//        try {
//            val modelStream = context.assets.open("apple.glb")
//            Log.d("Model3DRenderer", "Model file found, attempting to load.")
//            modelData = loadModel(modelStream)  // Ensure this method returns a valid ModelData object
//            if (modelData != null) {
//                Log.d("Model3DRenderer", "Model loaded successfully")
//            } else {
//                Log.e("Model3DRenderer", "Model data is null after loading.")
//            }
//        } catch (e: FileNotFoundException) {
//            Log.e("Model3DRenderer", "apple.glb not found in assets folder: ${e.message}")
//            // If model file is not found, fall back to a simple sphere
//            modelData = createSimpleSphere()
//        } catch (e: Exception) {
//            Log.e("Model3DRenderer", "Error loading model: ${e.message}")
//            // Fall back to a simple sphere on any other error
//            modelData = createSimpleSphere()
//        }
//
//        // Create shader program
//        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
//        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
//
//        program = GLES20.glCreateProgram()
//        GLES20.glAttachShader(program, vertexShader)
//        GLES20.glAttachShader(program, fragmentShader)
//        GLES20.glLinkProgram(program)
//
//        // Check if program linked successfully
//        val linkStatus = IntArray(1)
//        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
//        if (linkStatus[0] == 0) {
//            Log.e("Model3DRenderer", "Program linking failed")
//        }
//    }
//
//    // Add this method to load the model
//    private fun loadModel(modelStream: InputStream): ModelData? {
//        try {
//            // For now, let's just log the byte length of the file to check if it's being read
//            val modelBytes = modelStream.readBytes()
//            Log.d("Model3DRenderer", "Loaded model with ${modelBytes.size} bytes")
//
//            // Now, try parsing the .glb file (this is a placeholder)
//            // You would normally parse the .glb content and convert it into ModelData
//            // In this case, we'll return null to simulate the model not being loaded successfully
//
//            // TODO: Replace this with actual GLB model parsing logic
//            return null  // In actual code, you would parse the GLB file here and return the parsed ModelData
//        } catch (e: Exception) {
//            Log.e("Model3DRenderer", "Error loading model from InputStream: ${e.message}")
//            return null
//        }
//    }
//
//
//    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
//        GLES20.glViewport(0, 0, width, height)
//        surfaceWidth = width
//        surfaceHeight = height
//
//        val ratio = width.toFloat() / height.toFloat()
//        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
//    }
//
//    override fun onDrawFrame(gl: GL10?) {
//        // Clear with TRANSPARENT background - this is crucial!
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
//
//        if (modelData == null) {
//            Log.e("Model3DRenderer", "Model data is null, skipping rendering")
//            return  // Skip rendering if model is null
//        }
//
//        if (detectedBoxes.isEmpty()) {
//            Log.d("Model3DRenderer", "No boxes to draw")
//            return
//        }
//
//        val model = modelData
//        if (model == null) {
//            Log.e("Model3DRenderer", "Model data is null")
//            return
//        }
//
//        GLES20.glUseProgram(program)
//
//        Log.d("Model3DRenderer", "Detected Boxes: $detectedBoxes")
//
//        // Set camera position
//        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
//
//        // Draw model for each detected object (usually just the first apple)
//        for ((index, box) in detectedBoxes.withIndex()) {
//            Log.d("Model3DRenderer", "Drawing model at box $index: $box")
//            drawModelAtPosition(box, model)
//        }
//    }
//
//    private fun drawModelAtPosition(box: RectF, model: ModelData) {
//        Matrix.setIdentityM(modelMatrix, 0)
//
//        // Convert screen coordinates to normalized device coordinates
//        val centerX = (box.centerX() / surfaceWidth) * 2f - 1f
//        val centerY = 1f - (box.centerY() / surfaceHeight) * 2f
//
//        // Position model
//        Matrix.translateM(modelMatrix, 0, centerX, centerY, 0f)
//
//        // Scale model based on box size
//        val scale = (box.width() / surfaceWidth) * 1.5f
//        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
//
//        Log.e("Model3DRenderer", "Model scaling factor: $scale, Position: $centerX, $centerY")
//
//        // Rotate for visual effect
//        val time = System.currentTimeMillis() % 4000L / 4000f
//        Matrix.rotateM(modelMatrix, 0, time * 360f, 0f, 1f, 0f)
//
//        // Calculate MVP matrix
//        val tempMatrix = FloatArray(16)
//        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
//        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
//
//        // Get shader handles
//        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
//        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
//        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
//
//        // Enable vertex array
//        GLES20.glEnableVertexAttribArray(positionHandle)
//
//        // Pass vertex data
//        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, model.vertices)
//
//        // Set color (bright red apple color)
//        GLES20.glUniform4fv(colorHandle, 1, floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f), 0)
//
//        // Pass MVP matrix
//        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
//
//        // Draw
//        GLES20.glDrawElements(GLES20.GL_TRIANGLES, model.indexCount, GLES20.GL_UNSIGNED_SHORT, model.indices)
//
//        // Disable vertex array
//        GLES20.glDisableVertexAttribArray(positionHandle)
//    }
//
//    private fun loadShader(type: Int, shaderCode: String): Int {
//        val shader = GLES20.glCreateShader(type)
//        GLES20.glShaderSource(shader, shaderCode)
//        GLES20.glCompileShader(shader)
//
//        // Check compilation status
//        val compiled = IntArray(1)
//        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
//        if (compiled[0] == 0) {
//            Log.e("Model3DRenderer", "Shader compilation failed: " + GLES20.glGetShaderInfoLog(shader))
//        }
//
//        return shader
//    }
//
//    // Fallback: Create a simple sphere if GLB loading fails
//    private fun createSimpleSphere(): ModelData {
//        val vertices = mutableListOf<Float>()
//        val indices = mutableListOf<Short>()
//
//        val radius = 0.5f
//        val stacks = 10
//        val slices = 10
//
//        // Generate sphere vertices
//        for (i in 0..stacks) {
//            val phi = Math.PI * i / stacks
//            for (j in 0..slices) {
//                val theta = 2 * Math.PI * j / slices
//                val x = (radius * Math.sin(phi) * Math.cos(theta)).toFloat()
//                val y = (radius * Math.cos(phi)).toFloat()
//                val z = (radius * Math.sin(phi) * Math.sin(theta)).toFloat()
//                vertices.add(x)
//                vertices.add(y)
//                vertices.add(z)
//            }
//        }
//
//        // Generate indices
//        for (i in 0 until stacks) {
//            for (j in 0 until slices) {
//                val first = (i * (slices + 1) + j).toShort()
//                val second = (first + slices + 1).toShort()
//
//                indices.add(first)
//                indices.add(second)
//                indices.add((first + 1).toShort())
//
//                indices.add(second)
//                indices.add((second + 1).toShort())
//                indices.add((first + 1).toShort())
//            }
//        }
//
//        val vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertices.size * 4)
//            .order(java.nio.ByteOrder.nativeOrder())
//            .asFloatBuffer()
//            .put(vertices.toFloatArray())
//        vertexBuffer.position(0)
//
//        val indexBuffer = java.nio.ByteBuffer.allocateDirect(indices.size * 2)
//            .order(java.nio.ByteOrder.nativeOrder())
//            .asShortBuffer()
//            .put(indices.toShortArray())
//        indexBuffer.position(0)
//
//        return ModelData(vertexBuffer, indexBuffer, indices.size)
//    }
//}




package com.example.hackathon

import android.content.Context
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import java.io.FileNotFoundException
import java.io.InputStream
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.egl.EGLConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder


class Model3DRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private var modelData: ModelData? = null
    private var program = 0
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    var detectedBoxes = listOf<RectF>()
    var surfaceWidth = 1
    var surfaceHeight = 1

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """.trimIndent()

//    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//        Log.d("Model3DRenderer", "onSurfaceCreated - Loading model")
//
//        // CRITICAL: Set clear color to fully transparent
//        GLES20.glClearColor(0f, 0f, 0f, 0f)
//
//        // Enable blending for transparency - MUST be before depth test
//        GLES20.glEnable(GLES20.GL_BLEND)
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
//
//        // Enable depth test for proper 3D rendering
//        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
//        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
//
//        // Load model from assets
//        try {
//            val modelStream = context.assets.open("apple.obj")
//            Log.d("Model3DRenderer", "Model file found, attempting to load.")
//            modelData = loadModel(modelStream)
//            Log.d("Model3DRenderer", "Model initialized: ${modelData != null}")
//        } catch (e: FileNotFoundException) {
//            Log.w("Model3DRenderer", "apple.glb not found, using fallback sphere")
//            modelData = createSimpleSphere()
//        } catch (e: Exception) {
//            Log.e("Model3DRenderer", "Error loading model: ${e.message}")
//            modelData = createSimpleSphere()
//        }
//
//        // Ensure we always have a model
//        if (modelData == null) {
//            Log.w("Model3DRenderer", "Model is null, creating fallback sphere")
//            modelData = createSimpleSphere()
//        }
//
//        // Create shader program
//        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
//        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
//
//        program = GLES20.glCreateProgram()
//        GLES20.glAttachShader(program, vertexShader)
//        GLES20.glAttachShader(program, fragmentShader)
//        GLES20.glLinkProgram(program)
//
//        val linkStatus = IntArray(1)
//        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
//        if (linkStatus[0] == 0) {
//            Log.e("Model3DRenderer", "Program linking failed: ${GLES20.glGetProgramInfoLog(program)}")
//        } else {
//            Log.d("Model3DRenderer", "Shader program linked successfully")
//        }
//    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d("Model3DRenderer", "onSurfaceCreated - Loading model")

        // CRITICAL: Set clear color to fully transparent
        GLES20.glClearColor(0f, 0f, 0f, 0f)

        // Enable blending for transparency - MUST be before depth test
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Enable depth test for proper 3D rendering
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        // Load model from assets
        try {
            val modelStream = context.assets.open("apple.obj") // Path to your OBJ file
            Log.d("Model3DRenderer", "Model file found, attempting to load.")
            modelData = loadObjModel(modelStream)
            Log.d("Model3DRenderer", "Model initialized: ${modelData != null}")
        } catch (e: FileNotFoundException) {
            Log.w("Model3DRenderer", "apple.obj not found, using fallback sphere")
            modelData = createSimpleSphere()
        } catch (e: Exception) {
            Log.e("Model3DRenderer", "Error loading model: ${e.message}")
            modelData = createSimpleSphere()
        }

        // Ensure we always have a model
        if (modelData == null) {
            Log.w("Model3DRenderer", "Model is null, creating fallback sphere")
            modelData = createSimpleSphere()
        }

        // Create shader program
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e("Model3DRenderer", "Program linking failed: ${GLES20.glGetProgramInfoLog(program)}")
        } else {
            Log.d("Model3DRenderer", "Shader program linked successfully")
        }
    }


    private fun loadModel(modelStream: InputStream): ModelData? {
        try {
            val modelBytes = modelStream.readBytes()
            Log.d("Model3DRenderer", "Loaded GLB model with ${modelBytes.size} bytes")

            // TODO: Implement GLB parsing library
            // For now, we'll use the fallback sphere
            Log.w("Model3DRenderer", "GLB parsing not implemented, using fallback sphere")
            return createSimpleSphere()
        } catch (e: Exception) {
            Log.e("Model3DRenderer", "Error loading model from InputStream: ${e.message}")
            return createSimpleSphere()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        surfaceWidth = width
        surfaceHeight = height

        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

        Log.d("Model3DRenderer", "Surface changed: ${width}x${height}, ratio: $ratio")
    }

    override fun onDrawFrame(gl: GL10?) {
        // CRITICAL: Clear both color AND depth buffer with transparent background
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (modelData == null) {
            Log.e("Model3DRenderer", "Model data is null, skipping rendering")
            return
        }

        if (detectedBoxes.isEmpty()) {
            // Don't log every frame - just clear and return
            return
        }

        val model = modelData ?: return

        GLES20.glUseProgram(program)

        // Set camera position
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)

        // Draw model for the first detected apple only (to avoid multiple overlapping models)
        if (detectedBoxes.isNotEmpty()) {
            drawModelAtPosition(detectedBoxes[0], model)
        }
    }

    private fun drawModelAtPosition(box: RectF, model: ModelData) {
        Matrix.setIdentityM(modelMatrix, 0)

        // Convert screen coordinates to normalized device coordinates
        val centerX = (box.centerX() / surfaceWidth) * 2f - 1f
        val centerY = 1f - (box.centerY() / surfaceHeight) * 2f

        // Position model
        Matrix.translateM(modelMatrix, 0, centerX, centerY, 0f)

        // Scale model based on box size - adjusted for better visibility
        val scale = (box.width() / surfaceWidth) * 2.0f
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)

        // Rotate for visual effect
        val time = System.currentTimeMillis() % 4000L / 4000f
        Matrix.rotateM(modelMatrix, 0, time * 360f, 0f, 1f, 0f)

        // Calculate MVP matrix
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        // Get shader handles
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")

        // Enable vertex array
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Pass vertex data
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, model.vertices)

        // Set color (bright red apple with full opacity)
        GLES20.glUniform4fv(colorHandle, 1, floatArrayOf(0.9f, 0.1f, 0.1f, 1.0f), 0)

        // Pass MVP matrix
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Draw
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, model.indexCount, GLES20.GL_UNSIGNED_SHORT, model.indices)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("Model3DRenderer", "Shader compilation failed: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }

        return shader
    }

    private fun createSimpleSphere(): ModelData {
        val vertices = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        val radius = 0.5f
        val stacks = 16  // Increased for smoother sphere
        val slices = 16

        // Generate sphere vertices
        for (i in 0..stacks) {
            val phi = Math.PI * i / stacks
            for (j in 0..slices) {
                val theta = 2 * Math.PI * j / slices
                val x = (radius * Math.sin(phi) * Math.cos(theta)).toFloat()
                val y = (radius * Math.cos(phi)).toFloat()
                val z = (radius * Math.sin(phi) * Math.sin(theta)).toFloat()
                vertices.add(x)
                vertices.add(y)
                vertices.add(z)
            }
        }

        // Generate indices
        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val first = (i * (slices + 1) + j).toShort()
                val second = (first + slices + 1).toShort()

                indices.add(first)
                indices.add(second)
                indices.add((first + 1).toShort())

                indices.add(second)
                indices.add((second + 1).toShort())
                indices.add((first + 1).toShort())
            }
        }

        val vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertices.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices.toFloatArray())
        vertexBuffer.position(0)

        val indexBuffer = java.nio.ByteBuffer.allocateDirect(indices.size * 2)
            .order(java.nio.ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(indices.toShortArray())
        indexBuffer.position(0)

        Log.d("Model3DRenderer", "Created sphere with ${vertices.size / 3} vertices and ${indices.size} indices")

        return ModelData(vertexBuffer, indexBuffer, indices.size)
    }

    private fun loadObjModel(modelStream: InputStream): ModelData? {
        try {
            val reader = BufferedReader(InputStreamReader(modelStream))

            val positions = mutableListOf<Float>()
            val texcoords = mutableListOf<Float>()
            val normals = mutableListOf<Float>()

            data class VertexIndex(val p: Int, val t: Int?, val n: Int?)
            val faceIndices = mutableListOf<VertexIndex>()

            reader.forEachLine { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEachLine

                val parts = line.split("\\s+".toRegex())

                when (parts[0]) {
                    "v" -> {
                        positions.add(parts[1].toFloat())
                        positions.add(parts[2].toFloat())
                        positions.add(parts[3].toFloat())
                    }
                    "vt" -> {
                        if (parts.size >= 3) {
                            texcoords.add(parts[1].toFloat())
                            texcoords.add(parts[2].toFloat())
                        }
                    }
                    "vn" -> {
                        if (parts.size >= 4) {
                            normals.add(parts[1].toFloat())
                            normals.add(parts[2].toFloat())
                            normals.add(parts[3].toFloat())
                        }
                    }
                    "f" -> {
                        // Handle both triangles and quads
                        val faceVerts = mutableListOf<VertexIndex>()
                        for (i in 1 until parts.size) {
                            val seg = parts[i].split("/")
                            val p = seg[0].toInt() - 1
                            val t = seg.getOrNull(1)?.takeIf { it.isNotEmpty() }?.toIntOrNull()?.minus(1)
                            val n = seg.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toIntOrNull()?.minus(1)
                            faceVerts.add(VertexIndex(p, t, n))
                        }

                        // Triangulate if quad (convert quad to 2 triangles)
                        if (faceVerts.size == 3) {
                            faceIndices.addAll(faceVerts)
                        } else if (faceVerts.size == 4) {
                            // Triangle 1: 0,1,2
                            faceIndices.add(faceVerts[0])
                            faceIndices.add(faceVerts[1])
                            faceIndices.add(faceVerts[2])
                            // Triangle 2: 0,2,3
                            faceIndices.add(faceVerts[0])
                            faceIndices.add(faceVerts[2])
                            faceIndices.add(faceVerts[3])
                        }
                    }
                }
            }

            Log.d("Model3DRenderer", "OBJ parsed: ${positions.size/3} positions, ${faceIndices.size} face indices")

            // Build unique vertex list and index mapping
            val uniqueVertices = mutableMapOf<VertexIndex, Short>()
            val vertexList = mutableListOf<Float>()
            val indexList = mutableListOf<Short>()
            var nextIndex: Short = 0

            for (vertIndex in faceIndices) {
                val existingIndex = uniqueVertices[vertIndex]
                if (existingIndex != null) {
                    // Reuse existing vertex
                    indexList.add(existingIndex)
                } else {
                    // Add new vertex
                    if (nextIndex >= Short.MAX_VALUE) {
                        Log.e("Model3DRenderer", "Model too large: exceeded Short.MAX_VALUE vertices")
                        return createSimpleSphere()
                    }

                    // Add position (required)
                    val px = positions[vertIndex.p * 3]
                    val py = positions[vertIndex.p * 3 + 1]
                    val pz = positions[vertIndex.p * 3 + 2]

                    vertexList.add(px)
                    vertexList.add(py)
                    vertexList.add(pz)

                    uniqueVertices[vertIndex] = nextIndex
                    indexList.add(nextIndex)
                    nextIndex++
                }
            }

            Log.d("Model3DRenderer", "OBJ optimized: ${vertexList.size/3} unique vertices, ${indexList.size} indices")

            val vertexBuffer = ByteBuffer.allocateDirect(vertexList.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            vertexBuffer.put(vertexList.toFloatArray()).position(0)

            val indexBuffer = ByteBuffer.allocateDirect(indexList.size * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
            indexBuffer.put(indexList.toShortArray()).position(0)

            return ModelData(vertexBuffer, indexBuffer, indexList.size)

        } catch (e: Exception) {
            Log.e("Model3DRenderer", "OBJ parse error: ${e.message}", e)
            return createSimpleSphere()
        }
    }


}