package com.example.pedestrianassistant.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.example.pedestrianassistant.view.ObjectDetectorHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tensorflow.lite.task.gms.vision.detector.Detection
import javax.inject.Inject

@HiltViewModel
class DetectionViewModel @Inject constructor() : ViewModel(), ObjectDetectorHelper.DetectorListener {

    private var _detectionResult = MutableStateFlow("Nothing")
    val detectionResult = _detectionResult.asStateFlow()

    lateinit var objectDetectorHelper: ObjectDetectorHelper
    var bitmapBuffer: Bitmap? = null

    override fun onError(error: String) {
        Log.d("RESULTS-ERROR",error)
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        _detectionResult.value = if (results?.isNotEmpty() == true) results.first().categories.first().label.toString() else "Nothing"
    }

    fun detectObjects(image: ImageProxy) {
        image.use { bitmapBuffer!!.copyPixelsFromBuffer(image.planes[0].buffer) }
        val imageRotation = image.imageInfo.rotationDegrees
        objectDetectorHelper.detect(bitmapBuffer!!, imageRotation)
    }
}
