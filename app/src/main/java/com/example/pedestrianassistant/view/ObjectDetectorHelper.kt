package com.example.pedestrianassistant.view

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.gms.vision.detector.Detection
import org.tensorflow.lite.task.gms.vision.detector.ObjectDetector

class ObjectDetectorHelper(
    private val context: Context,
    private var threshold: Float = 0.5f,
    private var numThreads: Int = 2,
    private var maxResults: Int = 3,
    private var currentModel: String
) {

    private val GPU_DELEGATE_ERROR = "GPU is not supported on this device"
    private val MODEL_LOADING_ERROR = "TFLite failed to load model with error: "

    private lateinit var objectDetector: ObjectDetector

    init {
        setupObjectDetector()
    }

    private fun setupObjectDetector() {

        val optionsBuilder = ObjectDetector
            .ObjectDetectorOptions
            .builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())
        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(context, currentModel, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            Log.e("Test", MODEL_LOADING_ERROR + e.message)
        }
    }

    fun detect(image: Bitmap, imageRotation: Int): DetectionResults {
        if (!::objectDetector.isInitialized) setupObjectDetector()
        val imageProcessor = ImageProcessor.Builder().add(Rot90Op(-imageRotation / 90)).build()
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))
        val results = objectDetector.detect(tensorImage)

        return DetectionResults(results, tensorImage.height, tensorImage.width)
    }

    data class DetectionResults(val results: MutableList<Detection>?, val height: Int, val width: Int)
}