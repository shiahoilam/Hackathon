//package com.example.hackathon
//
//import android.content.Context
//import org.tensorflow.lite.Interpreter
//import java.io.FileInputStream
//import java.nio.MappedByteBuffer
//import java.nio.channels.FileChannel
//
//class TFLiteModelHelper(context: Context, modelFileName: String) {
//    val interpreter: Interpreter = Interpreter(loadModelFile(context, modelFileName))
//
//    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
//        val fileDescriptor = context.assets.openFd(filename)
//        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
//        val fileChannel = inputStream.channel
//        val startOffset = fileDescriptor.startOffset
//        val declaredLength = fileDescriptor.declaredLength
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
//    }
//}









package com.example.hackathon

import android.content.Context
import org.tensorflow.lite.Interpreter

// Minimal wrapper to open TFLite file from assets and create Interpreter
class TFLiteModelHelper(context: Context, modelAssetPath: String) {
    val interpreter: Interpreter

    init {
        val fileDescriptor = context.assets.openFd(modelAssetPath)
        val inputStream = fileDescriptor.createInputStream()
        val bytes = inputStream.readBytes()
        val buffer = java.nio.ByteBuffer.allocateDirect(bytes.size)
        buffer.put(bytes)
        buffer.rewind()
        interpreter = Interpreter(buffer)
    }
}

