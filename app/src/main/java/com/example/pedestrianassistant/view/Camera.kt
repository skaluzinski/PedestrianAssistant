package com.example.pedestrianassistant.view

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.pedestrianassistant.viewmodel.DetectionViewModel
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun CameraView(viewModel: DetectionViewModel, cameraExecutor: Executor) {

    val localContext = LocalContext.current
    val previewView = remember { PreviewView(localContext) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(localContext) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val observer = remember(previewView, cameraProviderFuture) { CameraXObserver(previewView, cameraProviderFuture, cameraExecutor, viewModel) }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize(),
        update = { lifecycleOwner.lifecycle.addObserver(observer) }
    )
}

@RequiresApi(Build.VERSION_CODES.P)
private fun initializeCameraProvider(
    previewView: PreviewView,
    cameraExecutor: Executor,
    cameraProvider: ProcessCameraProvider,
    viewModel: DetectionViewModel
) {

    val cameraSelector =
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
    cameraProvider.unbindAll()
    val preview =
        Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
    val imageAnalyzer =
        ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

    imageAnalyzer.setAnalyzer(cameraExecutor) { image ->
        if (viewModel.bitmapBuffer == null) { viewModel.bitmapBuffer = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888) }
        viewModel.detectObjects(image)
    }
    cameraProvider.bindToLifecycle(
        previewView.context as ComponentActivity,
        cameraSelector,
        preview,
        imageAnalyzer
    )
}

 class CameraXObserver(
    private val previewView: PreviewView,
    private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    private val cameraExecutor: Executor,
    private val viewModel: DetectionViewModel
) : LifecycleEventObserver {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME ->
                cameraProviderFuture.addListener(
                    {
                        initializeCameraProvider(
                            previewView = previewView,
                            cameraExecutor = cameraExecutor,
                            cameraProvider = cameraProviderFuture.get(),
                            viewModel = viewModel
                        )
                    },
                    ContextCompat.getMainExecutor(previewView.context)
                )
            Lifecycle.Event.ON_PAUSE -> cameraProviderFuture.get().unbindAll()
            else -> Unit
        }
    }
}