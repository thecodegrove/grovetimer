package dev.thecodegrove.grovetimer.data.repository

import android.content.Context
import dev.thecodegrove.grovetimer.domain.model.UserSettings
import dev.thecodegrove.grovetimer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación concreta del SettingsRepository usando SharedPreferences
 * Maneja la persistencia de configuraciones del usuario de GroveTimer
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val context: Context
) : SettingsRepository {
    
    companion object {
        private const val PREFS_NAME = "grovetimer_settings"
        private const val KEY_FADEOUT_ENABLED = "fadeout_enabled"
        private const val KEY_VIBRATE_ON_FINISH = "vibrate_on_finish"
        private const val KEY_DARK_MODE_ENABLED = "dark_mode_enabled"
        private const val KEY_DEFAULT_TIMER_DURATION = "default_timer_duration"
        private const val KEY_FADEOUT_DURATION = "fadeout_duration"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    override fun getUserSettings(): Flow<UserSettings> {
        return flow {
            emit(loadSettings())
        }
    }
    
    override suspend fun updateUserSettings(settings: UserSettings) {
        prefs.edit().apply {
            putBoolean(KEY_FADEOUT_ENABLED, settings.fadeoutEnabled)
            putBoolean(KEY_VIBRATE_ON_FINISH, settings.vibrateOnFinish)
            putBoolean(KEY_DARK_MODE_ENABLED, settings.darkModeEnabled)
            putLong(KEY_DEFAULT_TIMER_DURATION, settings.defaultTimerDuration)
            putInt(KEY_FADEOUT_DURATION, settings.fadeoutDuration)
            putBoolean(KEY_SOUND_ENABLED, settings.soundEnabled)
            putBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, settings.hapticFeedbackEnabled)
            apply()
        }
    }
    
    override suspend fun updateSettings(update: (UserSettings) -> UserSettings) {
        val currentSettings = loadSettings()
        val updatedSettings = update(currentSettings)
        updateUserSettings(updatedSettings)
    }
    
    override suspend fun resetToDefaults() {
        updateUserSettings(UserSettings.DEFAULT)
    }
    
    override suspend fun getSetting(key: String): String? {
        return prefs.getString(key, null)
    }
    
    override suspend fun setSetting(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    
    /**
     * Carga las configuraciones actuales desde SharedPreferences
     * Usa valores por defecto si no existen configuraciones guardadas
     */
    private fun loadSettings(): UserSettings {
        return UserSettings(
            fadeoutEnabled = prefs.getBoolean(KEY_FADEOUT_ENABLED, false),
            vibrateOnFinish = prefs.getBoolean(KEY_VIBRATE_ON_FINISH, false),
            darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE_ENABLED, false),
            defaultTimerDuration = prefs.getLong(KEY_DEFAULT_TIMER_DURATION, 600000L),
            fadeoutDuration = prefs.getInt(KEY_FADEOUT_DURATION, 30),
            soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true),
            hapticFeedbackEnabled = prefs.getBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, true)
        )
    }
}
