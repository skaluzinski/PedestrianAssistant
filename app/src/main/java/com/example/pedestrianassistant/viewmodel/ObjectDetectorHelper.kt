package com.example.pedestrianassistant.view

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.gms.vision.TfLiteVision
import org.tensorflow.lite.task.gms.vision.detector.Detection
import org.tensorflow.lite.task.gms.vision.detector.ObjectDetector

class ObjectDetectorHelper(
    val settings: DetectorSetting,
    val context: Context,
    val objectDetectorListener: DetectorListener
) {

    private val TAG = "ObjectDetectionHelper"
    private var objectDetector: ObjectDetector? = null
    private var gpuSupported = false

    init {
        TfLiteGpu.isGpuDelegateAvailable(context).onSuccessTask { gpuAvailable: Boolean ->
            val optionsBuilder = TfLiteInitializationOptions.builder()
            if (gpuAvailable) { optionsBuilder.setEnableGpuDelegateSupport(true) }
            TfLiteVision.initialize(context, optionsBuilder.build())
        }
            .addOnFailureListener{ objectDetectorListener.onError("TfLiteVision failed to initialize: ${it.message}")
        }
    }

    fun setupObjectDetector() {
        if (!TfLiteVision.isInitialized()) {
            Log.e(TAG, "setupObjectDetector: TfLiteVision is not initialized yet")
            return
        }

        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(settings.threshold)
                .setMaxResults(settings.maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(settings.numThreads)

        when (settings.currentDelegate) {
            DELEGATE_CPU -> {}
            DELEGATE_GPU -> { if (gpuSupported) { baseOptionsBuilder.useGpu() } else { objectDetectorListener.onError("GPU is not supported on this device") } }
            DELEGATE_NNAPI -> { baseOptionsBuilder.useNnapi() }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        val modelName = "lite-model_ssd_mobilenet_v1_1_metadata_2.tflite"

        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: Exception) {
            objectDetectorListener.onError("Object detector failed to initialize. See error logs for details")
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        if (!TfLiteVision.isInitialized()) {
            Log.e(TAG, "detect: TfLiteVision is not initialized yet")
            return
        }
        if (objectDetector == null) { setupObjectDetector() }
        var inferenceTime = SystemClock.uptimeMillis()
        val imageProcessor = ImageProcessor
            .Builder()
            .add(Rot90Op(-imageRotation / 90))
            .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val results = objectDetector?.detect(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        objectDetectorListener.onResults(results, inferenceTime, tensorImage.height, tensorImage.width)
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            results: MutableList<Detection>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
    }
}

data class DetectorSetting(
    val threshold: Float = 0.5f,
    val numThreads: Int = 2,
    val maxResults: Int = 3,
    val currentDelegate: Int = 0,
)