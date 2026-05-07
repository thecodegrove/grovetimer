package dev.thecodegrove.grovetimer.domain.usecase

import dev.thecodegrove.grovetimer.domain.model.UserSettings
import dev.thecodegrove.grovetimer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para obtener las configuraciones del usuario
 * Encapsula la lógica de negocio para la lectura de configuraciones
 */
class GetUserSettingsUseCase constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Ejecuta el caso de uso para obtener las configuraciones
     * @return Flow que emite las configuraciones actuales del usuario
     */
    operator fun invoke(): Flow<UserSettings> {
        return settingsRepository.getUserSettings()
    }
}
