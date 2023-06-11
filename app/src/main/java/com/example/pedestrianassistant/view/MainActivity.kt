package com.example.pedestrianassistant.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pedestrianassistant.viewmodel.DetectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val viewModel: DetectionViewModel by viewModels()
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.objectDetectorHelper = ObjectDetectorHelper(settings = DetectorSetting(), context = applicationContext, objectDetectorListener = viewModel)
        viewModel.objectDetectorHelper.setupObjectDetector()

        cameraExecutor = Executors.newSingleThreadExecutor()
        if (allPermissionsGranted()) {
            setContent { DetectionScreen(viewModel, cameraExecutor) }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    private fun allPermissionsGranted() = arrayOf(Manifest.permission.CAMERA).all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && allPermissionsGranted()) { setContent { DetectionScreen(viewModel, cameraExecutor) } }
    }
}
