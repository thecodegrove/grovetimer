package com.thecodegrove.grovetimer.domain.usecase

import com.thecodegrove.grovetimer.data.repository.TimerRepository
import com.thecodegrove.grovetimer.domain.model.TimerState

/**
 * Caso de uso para resetear el temporizador
 * 
 * Responsabilidades:
 * - Coordinar con el repositorio para detener el temporizador
 * - Resetear el estado a los valores iniciales
 * - Proporcionar confirmación de la operación
 */
class ResetTimerUseCase(
    private val repository: TimerRepository
) {
    
    /**
     * Ejecuta el caso de uso para resetear el temporizador
     * 
     * @return Resultado de la operación con el estado reseteado
     */
    suspend operator fun invoke(): Result<TimerState> {
        return try {
            // Detener el temporizador a través del repositorio
            repository.stopTimer()
            
            // Obtener el estado reseteado
            val resetState = repository.getCurrentState()
            Result.success(resetState)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
