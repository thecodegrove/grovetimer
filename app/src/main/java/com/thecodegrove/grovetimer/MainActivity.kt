package com.thecodegrove.grovetimer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thecodegrove.grovetimer.data.repository.TimerRepository
import com.thecodegrove.grovetimer.domain.model.UserSettings
import com.thecodegrove.grovetimer.utils.DebugUtils
import com.thecodegrove.grovetimer.domain.repository.SettingsRepository
import com.thecodegrove.grovetimer.domain.usecase.CheckTimerServiceUseCase
import com.thecodegrove.grovetimer.domain.usecase.GetTimerStateFromServiceUseCase
import com.thecodegrove.grovetimer.domain.usecase.GetUserSettingsUseCase
import com.thecodegrove.grovetimer.domain.usecase.UpdateUserSettingsUseCase
import com.thecodegrove.grovetimer.services.MyNotificationListener
import com.thecodegrove.grovetimer.services.SleepTimerService
import com.thecodegrove.grovetimer.ui.navigation.GroveTimerNavigation
import com.thecodegrove.grovetimer.ui.theme.GroveTimerTheme
import com.thecodegrove.grovetimer.ui.timer.TimerViewModel
import com.thecodegrove.grovetimer.utils.PermissionUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }
    
    // Helper functions for conditional debug logging
    private fun logd(message: String) {
        if (DebugUtils.isDebug(this)) {
            Log.d(TAG, message)
        }
    }
    
    private fun logw(message: String) {
        if (DebugUtils.isDebug(this)) {
            Log.w(TAG, message)
        }
    }

    private val viewModel: TimerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = TimerRepository(this@MainActivity)
                
                // Implementación funcional de SettingsRepository usando SharedPreferences
                val settingsRepository = object : SettingsRepository {
                    private val prefs: SharedPreferences by lazy {
                        this@MainActivity.getSharedPreferences("grovetimer_settings", Context.MODE_PRIVATE)
                    }
                    
                    override fun getUserSettings(): Flow<UserSettings> {
                        return flowOf(loadSettings())
                    }
                    
                    override suspend fun updateUserSettings(settings: UserSettings) {
                        prefs.edit().apply {
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
                        return prefs.getString(key, null)
                    }
                    
                    override suspend fun setSetting(key: String, value: String) {
                        prefs.edit().putString(key, value).apply()
                    }
                    
                    private fun loadSettings(): UserSettings {
                        return UserSettings(
                            fadeoutEnabled = prefs.getBoolean("fadeout_enabled", UserSettings.DEFAULT.fadeoutEnabled),
                            vibrateOnFinish = prefs.getBoolean("vibrate_on_finish", UserSettings.DEFAULT.vibrateOnFinish),
                            darkModeEnabled = prefs.getBoolean("dark_mode_enabled", UserSettings.DEFAULT.darkModeEnabled),
                            defaultTimerDuration = prefs.getLong("default_timer_duration", UserSettings.DEFAULT.defaultTimerDuration),
                            fadeoutDuration = prefs.getInt("fadeout_duration", UserSettings.DEFAULT.fadeoutDuration),
                            soundEnabled = prefs.getBoolean("sound_enabled", UserSettings.DEFAULT.soundEnabled),
                            hapticFeedbackEnabled = prefs.getBoolean("haptic_feedback_enabled", UserSettings.DEFAULT.hapticFeedbackEnabled)
                        )
                    }
                }
                
                val getUserSettingsUseCase = GetUserSettingsUseCase(settingsRepository)
                val updateUserSettingsUseCase = UpdateUserSettingsUseCase(settingsRepository)
                val checkTimerServiceUseCase = CheckTimerServiceUseCase(this@MainActivity)
                val getTimerStateFromServiceUseCase = GetTimerStateFromServiceUseCase(this@MainActivity)
                return TimerViewModel(repository, getUserSettingsUseCase, updateUserSettingsUseCase, checkTimerServiceUseCase, getTimerStateFromServiceUseCase) as T
            }
        }
    }

    private var showDialog by mutableStateOf(false)
    private var selectedMinutes by mutableStateOf(10)
    private var fadeoutEnabled by mutableStateOf(false)
    
    // Launcher para solicitar permiso de notificaciones
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        logd("Notification permission granted: $isGranted")
        if (!isGranted) {
            logw("Notification permission denied by user")
            // Opcionalmente mostrar un mensaje explicativo al usuario
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Verificar permisos necesarios al inicio
        checkRequiredPermissions()
        
        // Inicializar el BroadcastReceiver del ViewModel
        viewModel.initializeBroadcastReceiver(this)
        
        // Detectar si hay un temporizador activo y recuperar su estado
        logd("🚀 MainActivity: Calling checkAndRecoverTimerState()")
        viewModel.checkAndRecoverTimerState()
        logd("🚀 MainActivity: checkAndRecoverTimerState() called successfully")
        
        setContent {
            val settingsState by viewModel.settingsState.collectAsState(initial = UserSettings.DEFAULT)
            GroveTimerTheme(
                darkTheme = settingsState.darkModeEnabled, // true = oscuro, false = claro
                followSystemTheme = false // Nunca seguir sistema, siempre usar configuración del usuario
            ) {
                GroveTimerNavigation(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        logd("onStart called - updating media info")
        // Actualizar información de media cuando la actividad se inicia
        viewModel.updateMediaInfo()
    }
    
    override fun onResume() {
        super.onResume()
        logd("onResume called - updating media info")
        // Actualizar información de media cuando la actividad vuelve a primer plano
        viewModel.updateMediaInfo()
    }

    /**
     * Inicia el servicio del timer con el tiempo especificado
     * 
     * @param timeMillis Tiempo en milisegundos para el timer
     */
    private fun startSleepTimerServiceWithTime(timeMillis: Long) {
        // Verificar que tenemos permisos necesarios antes de iniciar el servicio
        if (!PermissionUtils.hasNotificationPermission(this)) {
            logw("Cannot start timer service without notification permission")
            checkRequiredPermissions()
            return
        }
        
        val intent = Intent(this@MainActivity, SleepTimerService::class.java).apply {
            putExtra(SleepTimerService.KEY_DURATION_MILLIS, timeMillis)
            putExtra(SleepTimerService.KEY_FADEOUT_ENABLED, fadeoutEnabled)
        }
        ContextCompat.startForegroundService(this@MainActivity, intent)
        logd("Started SleepTimerService with ${timeMillis}ms")
    }

    private fun startSleepTimerService() {
        // Verificar que tenemos permisos necesarios antes de iniciar el servicio
        if (!PermissionUtils.hasNotificationPermission(this)) {
            logw("Cannot start timer service without notification permission")
            checkRequiredPermissions()
            return
        }
        
        val tiempoRestante: Long = convertMinutesToMillis(selectedMinutes)
        
        // Iniciar el temporizador en el ViewModel
        viewModel.startTimer(tiempoRestante, "${selectedMinutes}m")
        
        val intent = Intent(this@MainActivity, SleepTimerService::class.java).apply {
            putExtra(SleepTimerService.KEY_DURATION_MILLIS, tiempoRestante)
            putExtra(SleepTimerService.KEY_FADEOUT_ENABLED, fadeoutEnabled)
        }
        ContextCompat.startForegroundService(this@MainActivity, intent)
        logd("Started SleepTimerService with ${tiempoRestante}ms")
    }

    // Función para convertir minutos a milisegundos
    private fun convertMinutesToMillis(minutes: Int): Long {
        return minutes * 60 * 1000L
    }
    
    /**
     * Verifica todos los permisos necesarios y solicita los que faltan
     * Incluye verificación de NotificationListener
     * Nota: MEDIA_CONTENT_CONTROL no se considera crítico porque no se puede solicitar
     * en tiempo de ejecución (es un permiso de nivel "signature")
     */
    private fun checkRequiredPermissions() {
        val allRequiredPermissionsGranted = PermissionUtils.hasAllRequiredPermissions(this)
        val notificationListenerEnabled = isNotificationListenerEnabled()
        
        logd("All required permissions granted: $allRequiredPermissionsGranted")
        logd("NotificationListener enabled: $notificationListenerEnabled")
        
        // Si faltan permisos críticos solicitables O NotificationListener no está habilitado, mostrar la actividad de permisos
        if (!allRequiredPermissionsGranted || !notificationListenerEnabled) {
            val intent = Intent(this, com.thecodegrove.grovetimer.ui.permissions.PermissionRequestActivity::class.java)
            startActivity(intent)
        } else {
            logd("All required permissions are granted and NotificationListener is enabled")
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = packageName
        val listenerName = ComponentName(this, MyNotificationListener::class.java).flattenToString()
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (name in names) {
                if (TextUtils.equals(name, listenerName)) {
                    return true
                }
            }
        }
        return false
    }
}

@Composable
fun NotificationPermissionDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_permission_title)) },
        text = { Text(stringResource(R.string.notification_permission_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.open_settings_dialog_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}