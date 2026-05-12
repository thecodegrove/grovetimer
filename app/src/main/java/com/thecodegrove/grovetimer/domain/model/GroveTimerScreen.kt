package com.thecodegrove.grovetimer.domain.model

/**
 * Modelo de navegación para las pantallas principales de GroveTimer
 * Define las rutas de navegación de la aplicación
 */
sealed class GroveTimerScreen(val route: String) {
    object Home : GroveTimerScreen("home")
    object Settings : GroveTimerScreen("settings")
}
