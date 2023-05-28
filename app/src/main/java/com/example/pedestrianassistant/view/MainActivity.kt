package com.example.pedestrianassistant.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.ProcessCameraProvider.getInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import org.tensorflow.lite.task.gms.vision.detector.Detection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class MainActivity : ComponentActivity(),ObjectDetectorHelper.DetectorListener {

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var detectedObject = mutableStateOf("Nothing")
    /** Blocking camera operations are performed using this executor */
    private var cameraExecutor: ExecutorService  = Executors.newSingleThreadExecutor()

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
        private const val TAG = "CameraX"
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("TFLITE", "TensorFlow Lite initialization successful")
        objectDetectorHelper =
            ObjectDetectorHelper(context = applicationContext, objectDetectorListener = this)
        if (allPermissionsGranted()) {
            setContent { MainContent() }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    private fun allPermissionsGranted() =
        arrayOf(Manifest.permission.CAMERA).all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION && allPermissionsGranted()) {
            setContent { MainContent() }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Composable
    private fun MainContent() {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = detectedObject.value,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            CameraView()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Composable
    private fun CameraView() {

        val localContext = LocalContext.current
        val previewView = remember { PreviewView(localContext) }
        val cameraProviderFuture = remember { getInstance(localContext) }
        val lifecycleOwner = LocalLifecycleOwner.current

        val observer =
            remember(previewView, cameraProviderFuture) {
                CameraXObserver(previewView, cameraProviderFuture)
            }

        AndroidView({ previewView }, Modifier.fillMaxSize()) {
            lifecycleOwner.lifecycle.addObserver(observer)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun initializeCameraProvider(
        previewView: PreviewView,
        cameraProvider: ProcessCameraProvider
    ) {

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        cameraProvider.unbindAll()

        val preview =
            Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                //
                // .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            bitmapBuffer =
                                Bitmap.createBitmap(
                                    image.width,
                                    image.height,
                                    Bitmap.Config.ARGB_8888
                                )
                        }

                        detectObjects(image)
                    }
                }
        cameraProvider.bindToLifecycle(
            previewView.context as ComponentActivity,
            cameraSelector,
            preview,
            imageAnalyzer
        )
    }

    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    inner class CameraXObserver(
        private val previewView: PreviewView,
        private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    ) : LifecycleEventObserver {

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    cameraProviderFuture.addListener(
                        { initializeCameraProvider(previewView, cameraProviderFuture.get()) },
                        ContextCompat.getMainExecutor(previewView.context)
                    )
                Lifecycle.Event.ON_PAUSE -> cameraProviderFuture.get().unbindAll()
                else -> Unit
            }
        }
    }

    override fun onInitialized() {
        objectDetectorHelper.setupObjectDetector()
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onError(error: String) {
        Log.d("RESULTS-ERROR",error)
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        detectedObject.value = if (results?.isNotEmpty() == true) results.first().categories.first().label.toString() else "Notghing"
        Log.d("RESULTS", results.toString())
    }
}
