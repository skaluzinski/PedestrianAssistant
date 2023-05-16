package com.example.pedestrianassistant.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pedestrianassistant.ui.theme.PedestrianAssistantTheme
import com.example.pedestrianassistant.view.navigation.NavRoutes
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("perm", "Permission granted")
            shouldShowCamera.value = true //
        } else {
            Log.i("perm", "Permission denied")
        }
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("perm", "Permission previously granted")
                shouldShowCamera.value = true // 👈🏽
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> Log.i("perm", "Show camera permissions dialog")

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PedestrianAssistantTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting()
                }
            }

        }
        requestCameraPermission()

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting() {
    var tabIndex by remember { mutableStateOf(0) }

    val tabs = listOf(NavRoutes.TrainingScreen, NavRoutes.DetectingScreen)
    Scaffold(
        content = { innerPadding ->
            Column(
                Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding()),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    when (tabIndex) {
                        0 -> TrainingScreen()
                        1 -> DetectingScreen()
                    }
                }
                TabRow(selectedTabIndex = tabIndex) {
                    tabs.forEachIndexed { index, navRoute ->
                        Tab(
                            text = { Text(text = navRoute.route) },
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                            icon = {
                                when (index) {
                                    0 ->
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null
                                        )
                                    1 ->
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null
                                        )
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun TrainingScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Work in progress", modifier = Modifier.align(Alignment.Center))
    }
}






