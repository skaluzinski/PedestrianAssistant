package com.example.pedestrianassistant.view

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pedestrianassistant.viewmodel.DetectionViewModel
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun DetectionScreen(viewModel: DetectionViewModel, cameraExecutor: Executor) {
    val detectionResult = viewModel.detectionResult.collectAsState()
    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = detectionResult.value,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CameraView(viewModel, cameraExecutor)
    }
}