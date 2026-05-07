package dev.thecodegrove.grovetimer.domain.usecase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import dev.thecodegrove.grovetimer.domain.model.TimerState
import dev.thecodegrove.grovetimer.utils.DebugUtils
import dev.thecodegrove.grovetimer.services.SleepTimerService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Caso de uso para obtener el estado del temporizador desde el servicio ejecutándose
 * 
 * Responsabilidades:
 * - Solicitar el estado actual del temporizador al servicio
 * - Manejar la comunicación con el servicio
 * - Proporcionar fallback si el servicio no responde
 */
class GetTimerStateFromServiceUseCase(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "GetTimerStateFromServiceUseCase"
        private const val TIMEOUT_MS = 5000L // 5 segundos de timeout
    }
    
    // Helper function for conditional debug logging
    private fun logd(message: String) {
        if (DebugUtils.isDebug(context)) {
            Log.d(TAG, message)
        }
    }
    
    /**
     * Ejecuta el caso de uso para obtener el estado del temporizador desde el servicio
     * 
     * @return Estado del temporizador o estado por defecto si no se puede obtener
     */
    suspend operator fun invoke(): TimerState {
        return try {
            logd("🎯 GetTimerStateFromServiceUseCase: Reading timer state from SharedPreferences...")
            
            // Leer directamente de SharedPreferences
            val prefs = context.getSharedPreferences("timer_state", Context.MODE_PRIVATE)
            val savedCountdownTime = prefs.getLong("countdown_time", 0L)
            val savedRemainingTime = prefs.getLong("current_remaining_time", 0L)
            val savedIsPaused = prefs.getBoolean("is_paused", false)
            val savedTimestamp = prefs.getLong("timestamp", 0L)
            
            logd("🎯 SharedPreferences values: countdown=$savedCountdownTime, remaining=$savedRemainingTime, paused=$savedIsPaused, timestamp=$savedTimestamp")
            
            // Verificar si el estado es válido y reciente (menos de 1 hora)
            val timeDiff = System.currentTimeMillis() - savedTimestamp
            if (savedCountdownTime > 0 && savedRemainingTime > 0 && timeDiff < 3600000L) {
                logd("🎯 Valid timer state found, creating TimerState")
                
                val remainingFormatted = formatTime(savedRemainingTime)
                val selectedFormatted = formatTime(savedCountdownTime)
                
                val timerState = TimerState(
                    isActive = savedRemainingTime > 0,
                    isPaused = savedIsPaused,
                    selectedTimeMillis = savedCountdownTime,
                    remainingTimeMillis = savedRemainingTime,
                    selectedTimeFormatted = selectedFormatted,
                    remainingTimeFormatted = remainingFormatted
                )
                
                logd("🎯 Timer state created: $timerState")
                timerState
            } else {
                logd("🎯 No valid timer state found, returning default state")
                getDefaultState()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "🎯 Error reading timer state from SharedPreferences", e)
            getDefaultState()
        }
    }
    
    /**
     * Formatea el tiempo en milisegundos a formato legible
     */
    private fun formatTime(timeMillis: Long): String {
        val totalSeconds = timeMillis / 1000
        
        if (totalSeconds <= 0) {
            return "0:00"
        }
        
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        
        return String.format("%d:%02d", minutes, seconds)
    }
    
    /**
     * Retorna el estado por defecto del temporizador
     */
    private fun getDefaultState(): TimerState {
        return TimerState(
            selectedTimeMillis = 600000L, // 10 minutos por defecto
            remainingTimeMillis = 600000L,
            selectedTimeFormatted = "10:00",
            remainingTimeFormatted = "10:00"
        )
    }
}
