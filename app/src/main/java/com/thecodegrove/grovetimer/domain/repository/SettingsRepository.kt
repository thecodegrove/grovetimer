package com.thecodegrove.grovetimer.domain.repository

import com.thecodegrove.grovetimer.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para manejar las configuraciones del usuario
 * Define las operaciones de lectura y escritura de configuraciones
 */
interface SettingsRepository {
    /**
     * Obtiene las configuraciones actuales del usuario
     * @return Flow que emite las configuraciones actuales
     */
    fun getUserSettings(): Flow<UserSettings>
    
    /**
     * Actualiza las configuraciones del usuario
     * @param settings Las nuevas configuraciones a guardar
     */
    suspend fun updateUserSettings(settings: UserSettings)
    
    /**
     * Actualiza una configuración específica
     * @param update Lambda que recibe las configuraciones actuales y retorna las actualizadas
     */
    suspend fun updateSettings(update: (UserSettings) -> UserSettings)
    
    /**
     * Restablece las configuraciones a los valores por defecto
     */
    suspend fun resetToDefaults()
    
    /**
     * Obtiene una configuración específica
     * @param key La clave de la configuración
     * @return El valor de la configuración o null si no existe
     */
    suspend fun getSetting(key: String): String?
    
    /**
     * Establece una configuración específica
     * @param key La clave de la configuración
     * @param value El valor a guardar
     */
    suspend fun setSetting(key: String, value: String)
}
