package com.thecodegrove.grovetimer.data.repository

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.thecodegrove.grovetimer.domain.model.MediaInfo
import com.thecodegrove.grovetimer.utils.DebugUtils
import com.thecodegrove.grovetimer.domain.model.TimerState
import com.thecodegrove.grovetimer.domain.usecase.GetTimerStateFromServiceUseCase
import com.thecodegrove.grovetimer.services.SleepTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositorio que maneja el estado del temporizador y la comunicación con el servicio
 * 
 * Responsabilidades:
 * - Mantener el estado actual del temporizador
 * - Proporcionar métodos para actualizar el estado
 * - Gestionar la comunicación entre UI y servicio
 * - Manejar información de sesiones multimedia activas
 * - Detectar si SleepTimerService está ejecutándose
 * - Obtener el estado del temporizador desde el servicio
 */
class TimerRepository(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "TimerRepository"
    }
    
    // Helper function for conditional debug logging
    private fun logd(message: String) {
        if (DebugUtils.isDebug(context)) {
            Log.d(TAG, message)
        }
    }
    
    private val _timerState = MutableStateFlow(
        TimerState(
            selectedTimeMillis = 600000L, // 10 minutos por defecto
            remainingTimeMillis = 600000L,
            selectedTimeFormatted = "10:00",
            remainingTimeFormatted = "10:00"
        )
    )
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    // StateFlow separado para información de media para forzar recomposición
    private val _mediaInfo = MutableStateFlow(MediaInfo())
    val mediaInfo: StateFlow<MediaInfo> = _mediaInfo.asStateFlow()
    
    /**
     * Actualiza el estado del temporizador con nuevos valores
     * 
     * @param newState Nuevo estado del temporizador
     */
    suspend fun updateTimerState(newState: TimerState) {
        logd("Updating timer state: $newState")
        _timerState.value = newState
    }
    
    /**
     * Actualiza solo el tiempo seleccionado sin iniciar el timer
     * 
     * @param timeMillis Tiempo en milisegundos
     * @param timeFormatted Tiempo formateado para mostrar
     */
    suspend fun updateSelectedTime(timeMillis: Long, timeFormatted: String) {
        logd("Updating selected time in repository: $timeMillis ms ($timeFormatted)")
        val currentState = _timerState.value
        val newState = currentState.copy(
            selectedTimeMillis = timeMillis,
            remainingTimeMillis = timeMillis,
            selectedTimeFormatted = timeFormatted,
            remainingTimeFormatted = timeFormatted
        )
        _timerState.value = newState
        logd("Selected time updated: ${_timerState.value}")
    }
    
    /**
     * Inicia el temporizador con el tiempo especificado
     * 
     * @param timeMillis Tiempo en milisegundos
     * @param timeFormatted Tiempo formateado para mostrar
     */
    suspend fun startTimer(timeMillis: Long, timeFormatted: String) {
        logd("Starting timer in repository: $timeMillis ms ($timeFormatted)")
        val currentState = _timerState.value
        val newState = TimerState(
            isActive = true,
            selectedTimeMillis = timeMillis,
            remainingTimeMillis = timeMillis,
            selectedTimeFormatted = timeFormatted,
            remainingTimeFormatted = timeFormatted,
            mediaInfo = currentState.mediaInfo // Preservar información de media existente
        )
        _timerState.value = newState
        logd("Timer state updated: ${_timerState.value}")
    }
    
    /**
     * Actualiza el tiempo restante del temporizador
     * 
     * @param remainingMillis Tiempo restante en milisegundos
     * @param remainingFormatted Tiempo restante formateado
     */
    suspend fun updateRemainingTime(remainingMillis: Long, remainingFormatted: String) {
        val currentState = _timerState.value
        logd("Updating remaining time: $remainingMillis ms ($remainingFormatted)")
        logd("Current state before update: $currentState")
        
        val newState = currentState.copy(
            remainingTimeMillis = remainingMillis,
            remainingTimeFormatted = remainingFormatted
        )
        _timerState.value = newState
        
        logd("Timer state after update: ${_timerState.value}")
    }
    
    /**
     * Actualiza la información de media en el estado del temporizador
     * 
     * @param mediaInfo Nueva información de media
     */
    suspend fun updateMediaInfo(mediaInfo: MediaInfo) {
        val currentState = _timerState.value
        logd("Updating media info: $mediaInfo")
        logd("Current media info before update: ${currentState.mediaInfo}")
        
        // Actualizar el StateFlow separado de media info
        _mediaInfo.value = mediaInfo
        
        // También actualizar el estado completo del timer
        val newState = currentState.copy(
            mediaInfo = mediaInfo
        )
        _timerState.value = newState
        
        logd("Media info updated: ${_timerState.value.mediaInfo}")
        logd("MediaInfo StateFlow updated: ${_mediaInfo.value}")
        logd("StateFlow value after update: ${_timerState.value}")
    }
    
    /**
     * Actualiza solo el estado de reproducción de media
     * 
     * @param isPlaying Indica si el contenido multimedia está reproduciéndose
     */
    suspend fun updateMediaPlaybackState(isPlaying: Boolean) {
        val currentState = _timerState.value
        val currentMediaInfo = currentState.mediaInfo
        
        val updatedMediaInfo = currentMediaInfo.copy(
            isPlaying = isPlaying
        )
        
        updateMediaInfo(updatedMediaInfo)
    }
    
    /**
     * Pausa el temporizador manteniendo el tiempo restante
     * 
     * @param remainingMillis Tiempo restante en milisegundos
     * @param remainingFormatted Tiempo restante formateado
     */
    suspend fun pauseTimer(remainingMillis: Long, remainingFormatted: String) {
        val currentState = _timerState.value
        logd("Pausing timer in repository: $remainingMillis ms ($remainingFormatted)")
        
        val newState = currentState.copy(
            isActive = true,
            isPaused = true,
            remainingTimeMillis = remainingMillis,
            remainingTimeFormatted = remainingFormatted
        )
        _timerState.value = newState
        logd("Timer state paused: ${_timerState.value}")
    }
    
    /**
     * Reanuda el temporizador desde el estado pausado
     * 
     * @param remainingMillis Tiempo restante en milisegundos
     * @param remainingFormatted Tiempo restante formateado
     */
    suspend fun resumeTimer(remainingMillis: Long, remainingFormatted: String) {
        val currentState = _timerState.value
        logd("Resuming timer in repository: $remainingMillis ms ($remainingFormatted)")
        
        val newState = currentState.copy(
            isActive = true,
            isPaused = false,
            remainingTimeMillis = remainingMillis,
            remainingTimeFormatted = remainingFormatted
        )
        _timerState.value = newState
        logd("Timer state resumed: ${_timerState.value}")
    }
    
    /**
     * Detiene el temporizador y resetea el estado a 10:00 por defecto
     */
    suspend fun stopTimer() {
        logd("Stopping timer in repository")
        _timerState.value = TimerState(
            selectedTimeMillis = 600000L, // 10 minutos por defecto
            remainingTimeMillis = 600000L,
            selectedTimeFormatted = "10:00",
            remainingTimeFormatted = "10:00"
        )
        logd("Timer state reset to 10:00: ${_timerState.value}")
    }
    
    /**
     * Obtiene el estado actual del temporizador
     * 
     * @return Estado actual del temporizador
     */
    fun getCurrentState(): TimerState = _timerState.value
    
    /**
     * Verifica si SleepTimerService está ejecutándose
     * 
     * @return true si el servicio está ejecutándose, false en caso contrario
     */
    suspend fun isSleepTimerServiceRunning(): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
            
            val serviceName = SleepTimerService::class.java.name
            val packageName = context.packageName
            
            logd("Checking for running services. Looking for: $serviceName")
            logd("Package name: $packageName")
            
            val isRunning = runningServices.any { serviceInfo ->
                val matchesPackage = serviceInfo.service.packageName == packageName
                val matchesService = serviceInfo.service.className == serviceName
                val isActive = serviceInfo.pid > 0
                
                logd("Service: ${serviceInfo.service.className}, Package: ${serviceInfo.service.packageName}, PID: ${serviceInfo.pid}")
                
                matchesPackage && matchesService && isActive
            }
            
            logd("SleepTimerService is running: $isRunning")
            isRunning
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if SleepTimerService is running", e)
            false
        }
    }
    
    /**
     * Obtiene el estado del temporizador desde el servicio ejecutándose
     * 
     * @return Estado del temporizador o estado por defecto si no se puede obtener
     */
    suspend fun getTimerStateFromService(): TimerState {
        return try {
            logd("Requesting timer state from SleepTimerService")
            
            // Usar el Use Case para obtener el estado real del servicio
            val getTimerStateUseCase = GetTimerStateFromServiceUseCase(context)
            val serviceState = getTimerStateUseCase()
            
            logd("Received state from service: $serviceState")
            serviceState
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting timer state from service", e)
            TimerState(
                selectedTimeMillis = 600000L,
                remainingTimeMillis = 600000L,
                selectedTimeFormatted = "10:00",
                remainingTimeFormatted = "10:00"
            )
        }
    }
}
