package com.example.pedestrianassistant.view.navigation

import androidx.annotation.Keep

@Keep
sealed class NavRoutes(val route: String) {
    object TrainingScreen : NavRoutes("train")
    object MainScreen : NavRoutes("main")
    object DetectingScreen : NavRoutes("detect")
}
