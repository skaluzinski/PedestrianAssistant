package com.example.pedestrianassistant.view

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import kotlin.reflect.KFunction1

class ImageAnalyzer(private val detectImage: KFunction1<ImageProxy, Task<MutableList<DetectedObject>>>) : ImageAnalysis.Analyzer {
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image

        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            // Pass image to an ML Kit Vision API
            // ...
            detectImage(image)
            Log.d("aaa", inputImage.mediaImage.toString())
        }
        image.close()
    }
}