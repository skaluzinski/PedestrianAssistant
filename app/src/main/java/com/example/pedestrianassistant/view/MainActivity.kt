package com.example.pedestrianassistant.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.pedestrianassistant.ui.theme.PedestrianAssistantTheme
import com.example.pedestrianassistant.view.navigation.NavRoutes
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import io.eyram.iconsax.IconSax

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        super.onDestroy()
    }
    private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.i("perm", "Permission granted")
                shouldShowCamera.value = true //
            } else {
                Log.i("perm", "Permission denied")
            }
        }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                Log.i("perm", "Permission previously granted")
                shouldShowCamera.value = true // 👈🏽
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ->
                Log.i("perm", "Show camera permissions dialog")
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
                        1 -> CameraView({},
                        )
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

@Composable
fun CameraView(
    onInputVinClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = modifier) {
        AndroidView(
            modifier =
                Modifier.fillMaxSize().drawWithContent {
                    drawContent()
                },
            factory = {
                PreviewView(it).apply {
                    controller =
                        getVinRecognizingCamera(
                            context = context,
                            lifecycleOwner = lifecycleOwner
                        )
                }
            },
        )

        Button(
            modifier = Modifier.align(Alignment.BottomCenter),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(),
            onClick = onInputVinClick
        ) {
            Text(modifier = Modifier.padding(16.dp), text = "Input VIN")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VinInputField(
    vinInput: String,
    onScanClick: () -> Unit,
    onInputChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = vinInput,
            onValueChange = onInputChange,
            shape = RoundedCornerShape(8.dp),
            colors =
                TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color(0xFFFF8A65),
                    unfocusedIndicatorColor = Color.LightGray
                )
        )

        IconButton(
            modifier =
                Modifier.background(color = Color(0xFFFF8A65), shape = RoundedCornerShape(8.dp)),
            onClick = onScanClick
        ) {
            Icon(
                painterResource(id = IconSax.TwoTone.Scan),
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

fun getVinRecognizingCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner
): CameraController {
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val vinRegex =
        Regex(pattern = """\b[(A-H|J-N|P|R-Z|0-9)]{17}\b""", options = setOf(RegexOption.MULTILINE))

    return LifecycleCameraController(context).apply {
        setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(context),
            MlKitAnalyzer(
                listOf(textRecognizer),
                COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(context)
            ) { analyzerResult ->
                analyzerResult.getValue(textRecognizer)?.let { text ->


                    val recognizedVins =
                        text.textBlocks
                            .flatMap { textBlock -> textBlock.lines }
                            .mapNotNull { lines ->
                                lines
                                    .takeIf { vinRegex.matches(it.text) }
                                    ?.let { RecognizedVin(it.text, it.boundingBox) }
                            }

                }
            }
        )
        bindToLifecycle(lifecycleOwner)
    }
}

data class RecognizedVin(
    val text: String,
    val boundingBox: android.graphics.Rect?,
)
