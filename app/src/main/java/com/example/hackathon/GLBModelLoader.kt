package com.example.hackathon

import android.content.Context
import android.opengl.GLES20
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import org.json.JSONObject

data class ModelData(
    val vertices: FloatBuffer,
    val indices: ShortBuffer,
    val indexCount: Int
)

class GLBModelLoader(private val context: Context) {

    fun loadModel(assetPath: String): ModelData {
        val inputStream = context.assets.open(assetPath)
        val glbBytes = inputStream.readBytes()
        inputStream.close()

        // Parse GLB header
        val buffer = ByteBuffer.wrap(glbBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Skip magic (4 bytes), version (4 bytes), length (4 bytes)
        buffer.position(12)

        // Read JSON chunk
        val jsonChunkLength = buffer.int
        val jsonChunkType = buffer.int // Should be 0x4E4F534A (JSON)

        val jsonBytes = ByteArray(jsonChunkLength)
        buffer.get(jsonBytes)
        val jsonString = String(jsonBytes)
        val json = JSONObject(jsonString)

        // Read BIN chunk
        val binChunkLength = buffer.int
        val binChunkType = buffer.int // Should be 0x004E4942 (BIN)

        val binData = ByteArray(binChunkLength)
        buffer.get(binData)
        val binBuffer = ByteBuffer.wrap(binData).order(ByteOrder.LITTLE_ENDIAN)

        // Extract vertices and indices from GLTF structure
        val accessors = json.getJSONArray("accessors")
        val bufferViews = json.getJSONArray("bufferViews")

        // Find position accessor (usually first one)
        var vertices: FloatBuffer? = null
        var indices: ShortBuffer? = null
        var indexCount = 0

        for (i in 0 until accessors.length()) {
            val accessor = accessors.getJSONObject(i)
            val bufferView = bufferViews.getJSONObject(accessor.getInt("bufferView"))
            val byteOffset = bufferView.optInt("byteOffset", 0)
            val byteLength = bufferView.getInt("byteLength")
            val count = accessor.getInt("count")
            val type = accessor.getString("type")

            if (type == "VEC3") {
                // Position data
                binBuffer.position(byteOffset)
                val floatArray = FloatArray(count * 3)
                for (j in 0 until count * 3) {
                    floatArray[j] = binBuffer.float
                }
                vertices = ByteBuffer.allocateDirect(floatArray.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(floatArray)
                vertices.position(0)
            } else if (type == "SCALAR" && accessor.optString("componentType") == "5123") {
                // Index data (unsigned short)
                binBuffer.position(byteOffset)
                val shortArray = ShortArray(count)
                for (j in 0 until count) {
                    shortArray[j] = binBuffer.short
                }
                indices = ByteBuffer.allocateDirect(shortArray.size * 2)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer()
                    .put(shortArray)
                indices.position(0)
                indexCount = count
            }
        }

        // Fallback: simple cube if parsing fails
        if (vertices == null || indices == null) {
            vertices = createCubeVertices()
            indices = createCubeIndices()
            indexCount = 36
        }

        return ModelData(vertices, indices, indexCount)
    }

    private fun createCubeVertices(): FloatBuffer {
        val vertices = floatArrayOf(
            // Front face
            -0.5f, -0.5f, 0.5f,
            0.5f, -0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f,
            // Back face
            -0.5f, -0.5f, -0.5f,
            -0.5f, 0.5f, -0.5f,
            0.5f, 0.5f, -0.5f,
            0.5f, -0.5f, -0.5f
        )
        return ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
            .position(0) as FloatBuffer
    }

    private fun createCubeIndices(): ShortBuffer {
        val indices = shortArrayOf(
            0, 1, 2, 0, 2, 3,    // front
            4, 5, 6, 4, 6, 7,    // back
            3, 2, 6, 3, 6, 5,    // top
            0, 4, 7, 0, 7, 1,    // bottom
            1, 7, 6, 1, 6, 2,    // right
            0, 3, 5, 0, 5, 4     // left
        )
        return ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(indices)
            .position(0) as ShortBuffer
    }
}