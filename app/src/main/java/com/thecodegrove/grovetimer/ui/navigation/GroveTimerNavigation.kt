package com.thecodegrove.grovetimer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thecodegrove.grovetimer.domain.model.GroveTimerScreen
import com.thecodegrove.grovetimer.ui.screens.HomeScreen
import com.thecodegrove.grovetimer.ui.screens.SettingsScreen
import com.thecodegrove.grovetimer.ui.timer.TimerViewModel

/**
 * Componente de navegación principal de GroveTimer
 * Maneja la navegación entre las pantallas principales de la aplicación
 */
@Composable
fun GroveTimerNavigation(viewModel: TimerViewModel) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = GroveTimerScreen.Home.route
    ) {
        composable(GroveTimerScreen.Home.route) {
            HomeScreen(navController = navController, viewModel = viewModel)
        }
        composable(GroveTimerScreen.Settings.route) {
            SettingsScreen(navController = navController, timerViewModel = viewModel)
        }
    }
}
