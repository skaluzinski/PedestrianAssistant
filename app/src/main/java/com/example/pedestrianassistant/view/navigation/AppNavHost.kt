package com.example.pedestrianassistant.view.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    setScaffoldTitle: (String) -> Unit
) {

    NavHost(
        navController = navController,
        startDestination = NavRoutes.MainScreen.route
    ) {
        composable(NavRoutes.TrainingScreen.route){}
        composable(NavRoutes.DetectingScreen.route){}
        composable(NavRoutes.MainScreen.route){}
    }
}