package com.thecodegrove.grovetimer.domain.usecase

import com.thecodegrove.grovetimer.data.repository.TimerRepository
import com.thecodegrove.grovetimer.domain.model.TimerState

/**
 * Caso de uso para actualizar el estado del temporizador
 * 
 * Responsabilidades:
 * - Validar el tiempo restante
 * - Formatear el tiempo restante para mostrar en la UI
 * - Coordinar con el repositorio para actualizar el estado
 */
class UpdateTimerStateUseCase(
    private val repository: TimerRepository
) {
    
    /**
     * Ejecuta el caso de uso para actualizar el estado del temporizador
     * 
     * @param remainingMillis Tiempo restante en milisegundos
     * @return Resultado de la operación
     */
    suspend operator fun invoke(remainingMillis: Long): Result<TimerState> {
        return try {
            // Validar parámetros de entrada
            if (remainingMillis < 0) {
                return Result.failure(IllegalArgumentException("El tiempo restante no puede ser negativo"))
            }
            
            // Formatear el tiempo restante para mostrar en la UI
            val formattedTime = formatRemainingTime(remainingMillis)
            
            // Actualizar el tiempo restante a través del repositorio
            repository.updateRemainingTime(remainingMillis, formattedTime)
            
            // Obtener el estado actualizado
            val currentState = repository.getCurrentState()
            Result.success(currentState)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Formatea el tiempo restante para mostrar en la UI de manera legible
     * 
     * @param remainingMillis Tiempo restante en milisegundos
     * @return Tiempo restante formateado como string
     */
    private fun formatRemainingTime(remainingMillis: Long): String {
        val totalSeconds = remainingMillis / 1000
        
        if (totalSeconds <= 0) {
            return "0s"
        }
        
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%dh %02dm %02ds", hours, minutes, seconds)
            minutes > 0 -> String.format("%dm %02ds", minutes, seconds)
            else -> "${seconds}s"
        }
    }
}
