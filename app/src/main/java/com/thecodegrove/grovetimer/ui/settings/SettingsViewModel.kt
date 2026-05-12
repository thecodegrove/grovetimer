package com.thecodegrove.grovetimer.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecodegrove.grovetimer.domain.model.UserSettings
import com.thecodegrove.grovetimer.utils.DebugUtils
import com.thecodegrove.grovetimer.domain.repository.SettingsRepository
import com.thecodegrove.grovetimer.domain.usecase.GetUserSettingsUseCase
import com.thecodegrove.grovetimer.domain.usecase.UpdateUserSettingsUseCase
import com.thecodegrove.grovetimer.services.SleepTimerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * ViewModel para manejar las configuraciones del usuario en GroveTimer
 * Proporciona métodos para leer y actualizar configuraciones persistentes
 */
class SettingsViewModel(
    private val context: Context? = null,
    private val getUserSettingsUseCase: GetUserSettingsUseCase? = null,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase? = null
) : ViewModel() {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    // Helper function for conditional debug logging
    private fun logd(message: String) {
        if (context != null && DebugUtils.isDebug(context!!)) {
            Log.d(TAG, message)
        } else if (DebugUtils.isDebug()) {
            Log.d(TAG, message)
        }
    }
    
    // Implementación funcional de SettingsRepository usando SharedPreferences
    private val settingsRepository = object : SettingsRepository {
        private val prefs: SharedPreferences? by lazy {
            context?.getSharedPreferences("grovetimer_settings", Context.MODE_PRIVATE)
        }
        
        override fun getUserSettings(): Flow<UserSettings> {
            return flowOf(loadSettings())
        }
        
        override suspend fun updateUserSettings(settings: UserSettings) {
            prefs?.edit()?.apply {
                putBoolean("fadeout_enabled", settings.fadeoutEnabled)
                putBoolean("vibrate_on_finish", settings.vibrateOnFinish)
                putBoolean("dark_mode_enabled", settings.darkModeEnabled)
                putLong("default_timer_duration", settings.defaultTimerDuration)
                putInt("fadeout_duration", settings.fadeoutDuration)
                putBoolean("sound_enabled", settings.soundEnabled)
                putBoolean("haptic_feedback_enabled", settings.hapticFeedbackEnabled)
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
            return prefs?.getString(key, null)
        }
        
        override suspend fun setSetting(key: String, value: String) {
            prefs?.edit()?.putString(key, value)?.apply()
        }
        
        private fun loadSettings(): UserSettings {
            return UserSettings(
                fadeoutEnabled = prefs?.getBoolean("fadeout_enabled", UserSettings.DEFAULT.fadeoutEnabled) ?: UserSettings.DEFAULT.fadeoutEnabled,
                vibrateOnFinish = prefs?.getBoolean("vibrate_on_finish", UserSettings.DEFAULT.vibrateOnFinish) ?: UserSettings.DEFAULT.vibrateOnFinish,
                darkModeEnabled = prefs?.getBoolean("dark_mode_enabled", UserSettings.DEFAULT.darkModeEnabled) ?: UserSettings.DEFAULT.darkModeEnabled,
                defaultTimerDuration = prefs?.getLong("default_timer_duration", UserSettings.DEFAULT.defaultTimerDuration) ?: UserSettings.DEFAULT.defaultTimerDuration,
                fadeoutDuration = prefs?.getInt("fadeout_duration", UserSettings.DEFAULT.fadeoutDuration) ?: UserSettings.DEFAULT.fadeoutDuration,
                soundEnabled = prefs?.getBoolean("sound_enabled", UserSettings.DEFAULT.soundEnabled) ?: UserSettings.DEFAULT.soundEnabled,
                hapticFeedbackEnabled = prefs?.getBoolean("haptic_feedback_enabled", UserSettings.DEFAULT.hapticFeedbackEnabled) ?: UserSettings.DEFAULT.hapticFeedbackEnabled
            )
        }
    }
    
    private val getUserSettings = getUserSettingsUseCase ?: GetUserSettingsUseCase(settingsRepository)
    private val updateUserSettings = updateUserSettingsUseCase ?: UpdateUserSettingsUseCase(settingsRepository)
    
    private val _settingsState = MutableStateFlow(UserSettings.DEFAULT)
    val settingsState: StateFlow<UserSettings> = _settingsState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Carga las configuraciones actuales del usuario
     */
    private fun loadSettings() {
        viewModelScope.launch {
            getUserSettings().collect { settings ->
                _settingsState.value = settings
            }
        }
    }
    
    /**
     * Envía un broadcast al servicio para notificar cambios en la configuración
     */
    private fun notifySettingsChanged() {
        context?.let { ctx ->
            try {
                val intent = Intent(SleepTimerService.ACTION_UPDATE_SETTINGS).apply {
                    setPackage(ctx.packageName)
                }
                ctx.sendBroadcast(intent)
                logd("📢 Settings change broadcast sent to SleepTimerService")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending settings change broadcast", e)
            }
        }
    }
    
    /**
     * Actualiza la configuración de fadeout progresivo
     */
    fun updateFadeoutEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateUserSettings { currentSettings ->
                currentSettings.copy(fadeoutEnabled = enabled)
            }
            // Recargar configuraciones después del cambio
            loadSettings()
            // Notificar al servicio del cambio
            notifySettingsChanged()
        }
    }
    
    /**
     * Actualiza la configuración de vibración al finalizar
     */
    fun updateVibrateOnFinish(enabled: Boolean) {
        viewModelScope.launch {
            updateUserSettings { currentSettings ->
                currentSettings.copy(vibrateOnFinish = enabled)
            }
            // Recargar configuraciones después del cambio
            loadSettings()
            // Notificar al servicio del cambio
            notifySettingsChanged()
        }
    }
    
    /**
     * Actualiza la configuración de modo oscuro
     */
    fun updateDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateUserSettings { currentSettings ->
                currentSettings.copy(darkModeEnabled = enabled)
            }
            // Recargar configuraciones después del cambio
            loadSettings()
        }
    }
    
    /**
     * Actualiza la duración por defecto del timer
     */
    fun updateDefaultTimerDuration(durationMillis: Long) {
        viewModelScope.launch {
            updateUserSettings { currentSettings ->
                currentSettings.copy(defaultTimerDuration = durationMillis)
            }
        }
    }
    
    /**
     * Actualiza la duración del fadeout
     */
    fun updateFadeoutDuration(durationSeconds: Int) {
        viewModelScope.launch {
            updateUserSettings { currentSettings ->
                currentSettings.copy(fadeoutDuration = durationSeconds)
            }
            // Notificar al servicio del cambio
            notifySettingsChanged()
        }
    }
    
    /**
     * Actualiza la configuración de sonido
     */
    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateUserSettings { currentSettings ->
                currentSettings.copy(soundEnabled = enabled)
            }
            // Notificar al servicio del cambio
            notifySettingsChanged()
        }
    }
    
    /**
     * Actualiza la configuración de retroalimentación háptica
     */
    fun updateHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateUserSettings { currentSettings ->
                currentSettings.copy(hapticFeedbackEnabled = enabled)
            }
        }
    }
    
    /**
     * Restablece todas las configuraciones a los valores por defecto
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            updateUserSettings.resetToDefaults()
        }
    }
}
