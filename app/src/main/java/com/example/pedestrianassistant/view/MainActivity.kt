package com.example.pedestrianassistant.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.ProcessCameraProvider.getInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.room.jarjarred.org.stringtemplate.v4.Interpreter
import com.google.android.gms.tasks.Task
import com.google.android.gms.tflite.java.TfLite
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import java.io.FileDescriptor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val initializeTask: Task<Void> by lazy {
        TfLite.initialize(this).addOnFailureListener{
            Log.e("TFLITE", it.message.toString())
        }
    }
//    private val objectDetectorHelper by lazy {
//        ObjectDetectorHelper(applicationContext,0.5f, 2, 3, 0, "mobilenetWithMetadata.tflite")
//    }

    private var objectDetectorHelper: ObjectDetectorHelper? = null
    private lateinit var interpreter: Interpreter

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
        private const val TAG = "CameraX"
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeTask.addOnSuccessListener {
            Log.d("TFLITE", "TensorFlow Lite initialization successful")
            objectDetectorHelper = ObjectDetectorHelper(
                applicationContext,
                0.5f,
                2,
                3,
                "model.tflite"
            )
            if (allPermissionsGranted()) {
                setContent { MainContent() }
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
            }
        }.addOnFailureListener { e ->
            Log.e("TFLITE", "TensorFlow Lite initialization failed: ${e.message}")
        }
    }

    private fun allPermissionsGranted() = arrayOf(Manifest.permission.CAMERA).all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION && allPermissionsGranted()) {
            setContent {
                MainContent()
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Composable
    private fun MainContent() {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "CameraX with Compose", fontSize = 24.sp, modifier = Modifier.padding(bottom = 8.dp))
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

        val observer = remember(previewView, cameraProviderFuture) {
            CameraXObserver(previewView, cameraProviderFuture)
        }

        AndroidView({ previewView }, Modifier.fillMaxSize()) {
            lifecycleOwner.lifecycle.addObserver(observer)
        }

    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun initializeCameraProvider(previewView: PreviewView, cameraProvider: ProcessCameraProvider) {

        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        cameraProvider.unbindAll()

        val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().apply {
            setAnalyzer(ContextCompat.getMainExecutor(previewView.context)) { imageProxy ->
                processImageProxy(imageProxy)
            }
        }

        cameraProvider.bindToLifecycle(previewView.context as ComponentActivity, cameraSelector, preview, imageAnalyzer)
    }

    private fun processImageProxy(image: ImageProxy) {
        image.use {
            val bitmap = image.toBitmap()
            val results = objectDetectorHelper!!.detect(bitmap, image.imageInfo.rotationDegrees)
            Log.i(TAG, results.toString())
        }
    }

    inner class CameraXObserver(private val previewView: PreviewView, private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider>) : LifecycleEventObserver {

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_RESUME -> cameraProviderFuture.addListener({ initializeCameraProvider(previewView, cameraProviderFuture.get()) }, ContextCompat.getMainExecutor(previewView.context))
                Lifecycle.Event.ON_PAUSE -> cameraProviderFuture.get().unbindAll()
                else -> Unit
            }
        }
    }
}