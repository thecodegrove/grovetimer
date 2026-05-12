package com.thecodegrove.grovetimer.domain.model

/**
 * Representa el estado actual del temporizador de la aplicación
 * 
 * @property isActive Indica si el temporizador está actualmente ejecutándose
 * @property isPaused Indica si el temporizador está pausado
 * @property selectedTimeMillis Tiempo seleccionado por el usuario en milisegundos
 * @property remainingTimeMillis Tiempo restante del temporizador en milisegundos
 * @property selectedTimeFormatted Tiempo seleccionado formateado para mostrar en UI
 * @property remainingTimeFormatted Tiempo restante formateado para mostrar en UI
 * @property mediaInfo Información de la sesión multimedia activa
 */
data class TimerState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val selectedTimeMillis: Long = 0L,
    val remainingTimeMillis: Long = 0L,
    val selectedTimeFormatted: String = "",
    val remainingTimeFormatted: String = "",
    val mediaInfo: MediaInfo = MediaInfo()
)
