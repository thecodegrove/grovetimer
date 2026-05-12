package com.thecodegrove.grovetimer.domain.usecase

import com.thecodegrove.grovetimer.data.repository.TimerRepository
import com.thecodegrove.grovetimer.domain.model.TimerState

/**
 * Caso de uso para iniciar el temporizador
 * 
 * Responsabilidades:
 * - Validar los parámetros de entrada
 * - Formatear el tiempo para mostrar en la UI
 * - Coordinar con el repositorio para iniciar el temporizador
 */
class StartTimerUseCase(
    private val repository: TimerRepository
) {
    
    /**
     * Ejecuta el caso de uso para iniciar el temporizador
     * 
     * @param timeMillis Tiempo en milisegundos
     * @param timeUnit Unidad de tiempo (segundos, minutos, horas)
     * @return Resultado de la operación
     */
    suspend operator fun invoke(timeMillis: Long, timeUnit: String): Result<TimerState> {
        return try {
            // Validar parámetros de entrada
            if (timeMillis <= 0) {
                return Result.failure(IllegalArgumentException("El tiempo debe ser mayor a 0"))
            }
            
            // Formatear el tiempo para mostrar en la UI
            val formattedTime = formatTimeForDisplay(timeMillis, timeUnit)
            
            // Iniciar el temporizador a través del repositorio
            repository.startTimer(timeMillis, formattedTime)
            
            // Obtener el estado actualizado
            val currentState = repository.getCurrentState()
            Result.success(currentState)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Formatea el tiempo para mostrar en la UI de manera legible
     * 
     * @param timeMillis Tiempo en milisegundos
     * @param timeUnit Unidad de tiempo
     * @return Tiempo formateado como string
     */
    private fun formatTimeForDisplay(timeMillis: Long, timeUnit: String): String {
        val seconds = timeMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when (timeUnit) {
            "Segundos" -> "${seconds}s"
            "Minutos" -> "${minutes}m"
            "Horas" -> "${hours}h"
            else -> "${seconds}s"
        }
    }
}
