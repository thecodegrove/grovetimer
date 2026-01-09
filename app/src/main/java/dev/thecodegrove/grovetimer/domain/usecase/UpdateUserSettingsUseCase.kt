package dev.thecodegrove.grovetimer.domain.usecase

import dev.thecodegrove.grovetimer.domain.model.UserSettings
import dev.thecodegrove.grovetimer.domain.repository.SettingsRepository

/**
 * Caso de uso para actualizar las configuraciones del usuario
 * Encapsula la lógica de negocio para la escritura de configuraciones
 */
class UpdateUserSettingsUseCase constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Ejecuta el caso de uso para actualizar las configuraciones completas
     * @param settings Las nuevas configuraciones a guardar
     */
    suspend operator fun invoke(settings: UserSettings) {
        settingsRepository.updateUserSettings(settings)
    }
    
    /**
     * Ejecuta el caso de uso para actualizar configuraciones específicas
     * @param update Lambda que recibe las configuraciones actuales y retorna las actualizadas
     */
    suspend operator fun invoke(update: (UserSettings) -> UserSettings) {
        settingsRepository.updateSettings(update)
    }
    
    /**
     * Actualiza una configuración específica
     * @param key La clave de la configuración
     * @param value El valor a guardar
     */
    suspend fun updateSetting(key: String, value: String) {
        settingsRepository.setSetting(key, value)
    }
    
    /**
     * Restablece las configuraciones a los valores por defecto
     */
    suspend fun resetToDefaults() {
        settingsRepository.resetToDefaults()
    }
}
