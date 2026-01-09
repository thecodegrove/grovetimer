package dev.thecodegrove.grovetimer.ui.timer

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.thecodegrove.grovetimer.data.repository.TimerRepository
import dev.thecodegrove.grovetimer.utils.DebugUtils
import dev.thecodegrove.grovetimer.domain.model.MediaInfo
import dev.thecodegrove.grovetimer.domain.model.TimerState
import dev.thecodegrove.grovetimer.domain.model.UserSettings
import dev.thecodegrove.grovetimer.domain.usecase.CheckTimerServiceUseCase
import dev.thecodegrove.grovetimer.domain.usecase.GetTimerStateFromServiceUseCase
import dev.thecodegrove.grovetimer.domain.usecase.GetUserSettingsUseCase
import dev.thecodegrove.grovetimer.domain.usecase.UpdateUserSettingsUseCase
import dev.thecodegrove.grovetimer.services.MyNotificationListener
import dev.thecodegrove.grovetimer.services.SleepTimerService
import dev.thecodegrove.grovetimer.utils.PermissionUtils
import dev.thecodegrove.grovetimer.ui.permissions.PermissionRequestActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel que maneja la lógica del temporizador y el estado de la UI
 * 
 * Responsabilidades:
 * - Gestionar el estado del temporizador a través del repositorio
 * - Proporcionar métodos para iniciar, detener y actualizar el temporizador
 * - Exponer el estado del temporizador a la UI
 * - Escuchar actualizaciones del servicio del temporizador
 * - Obtener y gestionar información de sesiones multimedia activas
 */
class TimerViewModel(
    private val repository: TimerRepository,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase,
    private val checkTimerServiceUseCase: CheckTimerServiceUseCase,
    private val getTimerStateFromServiceUseCase: GetTimerStateFromServiceUseCase
) : ViewModel() {
    
    companion object {
        private const val TAG = "TimerViewModel"
    }
    
    // Helper functions for conditional debug logging
    private fun logd(message: String) {
        if (context != null && DebugUtils.isDebug(context!!)) {
            Log.d(TAG, message)
        } else if (DebugUtils.isDebug()) {
            Log.d(TAG, message)
        }
    }
    
    private fun logw(message: String, throwable: Throwable? = null) {
        if (context != null && DebugUtils.isDebug(context!!)) {
            if (throwable != null) {
                Log.w(TAG, message, throwable)
            } else {
                Log.w(TAG, message)
            }
        } else if (DebugUtils.isDebug()) {
            if (throwable != null) {
                Log.w(TAG, message, throwable)
            } else {
                Log.w(TAG, message)
            }
        }
    }
    
    // Exponer directamente el estado del repositorio como fuente de verdad
    val uiState: StateFlow<TimerState> = repository.timerState
    val mediaInfoState: StateFlow<MediaInfo> = repository.mediaInfo
    
    // Exponer configuraciones del usuario para el tema
    private val _settingsState = MutableStateFlow(UserSettings.DEFAULT)
    val settingsState: StateFlow<UserSettings> = _settingsState.asStateFlow()
    
    private var broadcastReceiver: BroadcastReceiver? = null
    private var context: Context? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var currentSettings: UserSettings = UserSettings.DEFAULT
    
    /**
     * Inicializa el BroadcastReceiver para escuchar actualizaciones del servicio
     * 
     * @param context Contexto de la aplicación
     */
    fun initializeBroadcastReceiver(context: Context) {
        if (broadcastReceiver != null) return // Ya inicializado
        
        logd("Initializing BroadcastReceiver")
        this.context = context.applicationContext
        this.mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        
        // Cargar configuraciones actuales
        loadCurrentSettings()
        
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                logd("Broadcast received: ${intent?.action}")
                when (intent?.action) {
                    SleepTimerService.ACTION_TIMER_UPDATE -> {
                        val remainingTime = intent.getLongExtra(
                            SleepTimerService.EXTRA_REMAINING_TIME, 0L
                        )
                        logd("Timer update received: $remainingTime ms")
                        updateRemainingTime(remainingTime, formatRemainingTime(remainingTime))
                        
                        // Actualizar información de media cuando el timer está activo
                        updateMediaInfo()
                    }
                    SleepTimerService.ACTION_TIMER_FINISHED -> {
                        logd("Timer finished received")
                        stopTimer()
                    }
                    SleepTimerService.ACTION_TIMER_STOPPED -> {
                        logd("Timer stopped received")
                        stopTimer()
                    }
                    SleepTimerService.ACTION_TIMER_PAUSED -> {
                        val remainingTime = intent.getLongExtra(
                            SleepTimerService.EXTRA_REMAINING_TIME, 0L
                        )
                        logd("Timer paused received: $remainingTime ms")
                        pauseTimer(remainingTime, formatRemainingTime(remainingTime))
                    }
                    SleepTimerService.ACTION_TIMER_RESUMED -> {
                        val remainingTime = intent.getLongExtra(
                            SleepTimerService.EXTRA_REMAINING_TIME, 0L
                        )
                        logd("Timer resumed received: $remainingTime ms")
                        resumeTimer(remainingTime, formatRemainingTime(remainingTime))
                    }
                }
            }
        }
        
        // Registrar el receiver para las acciones del servicio
        val filter = IntentFilter().apply {
            addAction(SleepTimerService.ACTION_TIMER_UPDATE)
            addAction(SleepTimerService.ACTION_TIMER_FINISHED)
            addAction(SleepTimerService.ACTION_TIMER_STOPPED)
            addAction(SleepTimerService.ACTION_TIMER_PAUSED)
            addAction(SleepTimerService.ACTION_TIMER_RESUMED)
        }
        
        // Registrar el receiver para recibir broadcasts del servicio
        this.context?.let { context ->
            ContextCompat.registerReceiver(
                context,
                broadcastReceiver!!,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            logd("BroadcastReceiver registered successfully")
        }
    }
    
    /**
     * Carga las configuraciones actuales del usuario
     */
    private fun loadCurrentSettings() {
        viewModelScope.launch {
            getUserSettingsUseCase().collect { settings ->
                currentSettings = settings
                _settingsState.value = settings
                logd("Settings loaded: fadeout=${settings.fadeoutEnabled}, vibrate=${settings.vibrateOnFinish}, darkMode=${settings.darkModeEnabled}")
            }
        }
    }
    
    /**
     * Recarga las configuraciones actuales desde SharedPreferences
     * Se llama antes de iniciar el timer para asegurar configuraciones actualizadas
     */
    private suspend fun reloadCurrentSettings() {
        try {
            getUserSettingsUseCase().collect { settings ->
                currentSettings = settings
                logd("Settings reloaded: fadeout=${settings.fadeoutEnabled}, vibrate=${settings.vibrateOnFinish}")
                return@collect // Solo tomar el primer valor
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading settings", e)
        }
    }
    
    /**
     * Actualiza solo el tiempo seleccionado sin iniciar el timer
     * 
     * @param timeMillis Tiempo en milisegundos
     * @param timeFormatted Tiempo formateado para mostrar en la UI
     */
    fun updateSelectedTime(timeMillis: Long, timeFormatted: String) {
        logd("Updating selected time: $timeMillis ms ($timeFormatted)")
        viewModelScope.launch {
            repository.updateSelectedTime(timeMillis, timeFormatted)
        }
    }
    
    /**
     * Inicia el temporizador con el tiempo especificado
     * 
     * @param timeMillis Tiempo en milisegundos
     * @param timeFormatted Tiempo formateado para mostrar en la UI
     */
    fun startTimer(timeMillis: Long, timeFormatted: String) {
        logd("Starting timer: $timeMillis ms ($timeFormatted)")
        
        // Verificar permisos antes de iniciar el timer
        context?.let { ctx ->
            val allRequiredPermissionsGranted = PermissionUtils.hasAllRequiredPermissions(ctx)
            val notificationListenerEnabled = isNotificationListenerEnabled(ctx)
            
            if (!allRequiredPermissionsGranted || !notificationListenerEnabled) {
                logw("Cannot start timer: missing permissions or NotificationListener not enabled")
                // Lanzar la actividad de permisos
                val intent = Intent(ctx, PermissionRequestActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(intent)
                return
            }
        } ?: run {
            logw("Context not available, cannot verify permissions")
            return
        }
        
        viewModelScope.launch {
            repository.startTimer(timeMillis, timeFormatted)
            // Actualizar información de media al iniciar el timer
            updateMediaInfo()
            
            // Recargar configuraciones actuales antes de iniciar el servicio
            reloadCurrentSettings()
            
            // Iniciar el servicio del timer
            context?.let { ctx ->
                try {
                    val intent = Intent(ctx, SleepTimerService::class.java).apply {
                        putExtra(SleepTimerService.KEY_DURATION_MILLIS, timeMillis)
                        putExtra(SleepTimerService.KEY_FADEOUT_ENABLED, currentSettings.fadeoutEnabled)
                        putExtra(SleepTimerService.KEY_VIBRATE_ON_FINISH, currentSettings.vibrateOnFinish)
                    }
                    ContextCompat.startForegroundService(ctx, intent)
                    logd("Started SleepTimerService with ${timeMillis}ms, fadeout=${currentSettings.fadeoutEnabled}, vibrate=${currentSettings.vibrateOnFinish}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting SleepTimerService", e)
                }
            } ?: run {
                logw("Context not available, cannot start service")
            }
        }
    }
    
    /**
     * Actualiza el tiempo restante del temporizador
     * 
     * @param remainingMillis Tiempo restante en milisegundos
     * @param remainingFormatted Tiempo restante formateado
     */
    fun updateRemainingTime(remainingMillis: Long, remainingFormatted: String) {
        logd("Updating remaining time: $remainingMillis ms ($remainingFormatted)")
        viewModelScope.launch {
            repository.updateRemainingTime(remainingMillis, remainingFormatted)
        }
    }
    
    /**
     * Pausa el temporizador y actualiza el estado
     * 
     * @param remainingMillis Tiempo restante en milisegundos
     * @param remainingFormatted Tiempo restante formateado
     */
    fun pauseTimer(remainingMillis: Long, remainingFormatted: String) {
        logd("Pausing timer: $remainingMillis ms ($remainingFormatted)")
        viewModelScope.launch {
            repository.pauseTimer(remainingMillis, remainingFormatted)
        }
    }
    
    /**
     * Reanuda el temporizador y actualiza el estado
     * 
     * @param remainingMillis Tiempo restante en milisegundos
     * @param remainingFormatted Tiempo restante formateado
     */
    fun resumeTimer(remainingMillis: Long, remainingFormatted: String) {
        logd("Resuming timer: $remainingMillis ms ($remainingFormatted)")
        viewModelScope.launch {
            repository.resumeTimer(remainingMillis, remainingFormatted)
            // Actualizar información de media al reanudar el timer
            updateMediaInfo()
        }
    }
    
    /**
     * Obtiene y actualiza la información de media del sistema
     * Si detecta audio activo pero no puede obtener metadatos porque NotificationListener no está habilitado,
     * abre automáticamente los ajustes para que el usuario lo habilite
     */
    fun updateMediaInfo() {
        logd("updateMediaInfo called")
        logd("Context available: ${context != null}")
        logd("MediaSessionManager available: ${mediaSessionManager != null}")
        
        if (context == null) {
            logw("Context is null, cannot update media info")
            return
        }
        
        if (mediaSessionManager == null) {
            logw("MediaSessionManager is null, cannot update media info")
            return
        }
        
        // Verificar estado del NotificationListener
        val listenerEnabled = isNotificationListenerEnabled()
        logd("NotificationListener enabled: $listenerEnabled")
        
        viewModelScope.launch {
            try {
                logd("Starting media info update...")
                val mediaInfo = getCurrentMediaInfo()
                
                // Ya no abrimos automáticamente los ajustes cuando detectamos audio
                // Los permisos se solicitan al inicio de la app
                
                repository.updateMediaInfo(mediaInfo)
                logd("Media info updated successfully: $mediaInfo")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating media info", e)
                // En caso de error, crear MediaInfo vacío
                val emptyMediaInfo = MediaInfo()
                repository.updateMediaInfo(emptyMediaInfo)
            }
        }
    }
    
    /**
     * Obtiene la información actual de media del sistema
     * Usa múltiples métodos de detección en cascada:
     * 1. MediaSessionManager (método principal)
     * 2. NotificationListener (método alternativo 1)
     * 3. AudioManager (método alternativo 2)
     * 4. ActivityManager (método alternativo 3)
     * 
     * @return MediaInfo con la información de la sesión multimedia activa
     */
    fun getCurrentMediaInfo(): MediaInfo {
        logd("getCurrentMediaInfo called - trying multiple detection methods")
        
        if (context == null) {
            logw("Context not available")
            return MediaInfo()
        }
        
        // Método 1: Intentar con MediaSessionManager usando NotificationListener
        val mediaInfoFromSessions = tryGetMediaInfoFromSessions()
        if (mediaInfoFromSessions.hasActiveSession) {
            logd("✅ Found media via MediaSessionManager: $mediaInfoFromSessions")
            return mediaInfoFromSessions
        }
        
        logd("⚠️ MediaSessionManager didn't find active sessions, trying alternative methods...")
        
        // Método 2: Intentar detectar mediante NotificationListener
        val mediaInfoFromNotifications = tryGetMediaInfoFromNotifications()
        if (mediaInfoFromNotifications.hasActiveSession) {
            logd("✅ Found media via NotificationListener: $mediaInfoFromNotifications")
            return mediaInfoFromNotifications
        }
        
        // Método 3: Intentar detectar mediante AudioManager
        val mediaInfoFromAudio = tryGetMediaInfoFromAudioManager()
        if (mediaInfoFromAudio.hasActiveSession) {
            logd("✅ Found media via AudioManager: $mediaInfoFromAudio")
            return mediaInfoFromAudio
        }
        
        // Método 4: Intentar detectar mediante ActivityManager (apps de streaming conocidas)
        val mediaInfoFromActivity = tryGetMediaInfoFromActivityManager()
        if (mediaInfoFromActivity.hasActiveSession) {
            logd("✅ Found media via ActivityManager: $mediaInfoFromActivity")
            return mediaInfoFromActivity
        }
        
        logd("❌ No active media detected with any method")
        return MediaInfo(hasActiveSession = false)
    }
    
    /**
     * Intenta obtener información de media usando MediaSessionManager
     */
    private fun tryGetMediaInfoFromSessions(): MediaInfo {
        if (mediaSessionManager == null || context == null) {
            return MediaInfo()
        }
        
        try {
            logd("🔍 Method 1: Trying MediaSessionManager...")
            
            // Siempre intentar con NotificationListener primero (requerido para apps normales)
            val listenerComponent = MyNotificationListener.getComponentName(context!!)
            val activeSessions = try {
                mediaSessionManager!!.getActiveSessions(listenerComponent)
            } catch (e: SecurityException) {
                logw("⚠️ SecurityException with NotificationListener: ${e.message}")
                // Si falla con listener, intentar sin él (puede funcionar en algunos casos)
                try {
                    mediaSessionManager!!.getActiveSessions(null)
                } catch (e2: SecurityException) {
                    logw("⚠️ SecurityException without listener too: ${e2.message}")
                    return MediaInfo()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting sessions", e)
                return MediaInfo()
            }
            
            logd("Found ${activeSessions.size} active sessions via MediaSessionManager")
            
            if (activeSessions.isEmpty()) {
                return MediaInfo()
            }
            
            // Buscar la primera sesión que esté reproduciendo o pausada
            for (controller in activeSessions) {
                val playbackState = controller.playbackState
                val packageName = controller.packageName
                
                logd("Checking session: $packageName, state: ${playbackState?.state}")
                
                if (playbackState != null && 
                    (playbackState.state == PlaybackState.STATE_PLAYING || 
                     playbackState.state == PlaybackState.STATE_PAUSED)) {
                    
                    val appName = getAppNameFromPackage(packageName)
                    val mediaMetadata = controller.metadata
                    val isPlaying = playbackState.state == PlaybackState.STATE_PLAYING
                    
                    return MediaInfo(
                        isPlaying = isPlaying,
                        appPackage = packageName,
                        appName = appName,
                        mediaTitle = mediaMetadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "",
                        mediaArtist = mediaMetadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "",
                        mediaAlbum = mediaMetadata?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM) ?: "",
                        hasActiveSession = true
                    )
                }
            }
            
            // Si hay sesiones pero ninguna reproduciendo, devolver la primera
            val firstController = activeSessions.first()
            val packageName = firstController.packageName
            val appName = getAppNameFromPackage(packageName)
            
            return MediaInfo(
                isPlaying = false,
                appPackage = packageName,
                appName = appName,
                hasActiveSession = true
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in tryGetMediaInfoFromSessions", e)
            return MediaInfo()
        }
    }
    
    /**
     * Intenta obtener información de media usando NotificationListener
     */
    private fun tryGetMediaInfoFromNotifications(): MediaInfo {
        if (context == null) {
            return MediaInfo()
        }
        
        try {
            logd("🔍 Method 2: Trying NotificationListener...")
            
            val notificationListener = MyNotificationListener.getInstance()
            if (notificationListener == null) {
                logd("NotificationListener not connected")
                return MediaInfo()
            }
            
            val mediaPackages = notificationListener.getActiveMediaNotifications()
            if (mediaPackages.isEmpty()) {
                logd("No media notifications found")
                return MediaInfo()
            }
            
            // Usar el primer paquete encontrado
            val packageName = mediaPackages.first()
            val appName = getAppNameFromPackage(packageName)
            
            logd("Found media notification from: $packageName")
            return MediaInfo(
                isPlaying = true, // Asumimos que está reproduciendo si hay notificación
                appPackage = packageName,
                appName = appName,
                hasActiveSession = true
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in tryGetMediaInfoFromNotifications", e)
            return MediaInfo()
        }
    }
    
    /**
     * Intenta obtener información de media usando AudioManager
     * Si detecta audio activo, intenta obtener más información de notificaciones
     */
    private fun tryGetMediaInfoFromAudioManager(): MediaInfo {
        if (context == null) {
            return MediaInfo()
        }
        
        try {
            logd("🔍 Method 3: Trying AudioManager...")
            
            val audioManager = context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val isMusicActive = audioManager.isMusicActive
            
            if (!isMusicActive) {
                logd("AudioManager reports no active music")
                return MediaInfo()
            }
            
            logd("AudioManager reports music is active - attempting to get more info...")
            
            // PRIMERO: Intentar detectar la app en primer plano (más rápido y no requiere NotificationListener)
            logd("Step 1: Attempting to detect foreground app...")
            val detectedApp = tryDetectForegroundApp()
            if (detectedApp != null) {
                val appName = getAppNameFromPackage(detectedApp)
                logd("✅ Detected app from foreground: $appName ($detectedApp)")
                
                // Si detectamos una app, intentar obtener metadatos de notificaciones si está disponible
                val notificationListener = MyNotificationListener.getInstance()
                if (notificationListener != null) {
                    logd("Step 2: NotificationListener available, trying to get media info from notifications...")
                    try {
                        val mediaInfo = notificationListener.getActiveMediaNotificationInfo()
                        if (mediaInfo != null) {
                            val (packageName, title, artist) = mediaInfo
                            // Solo usar si es la misma app que detectamos
                            if (packageName == detectedApp || packageName.isEmpty()) {
                                val finalAppName = if (packageName.isNotEmpty()) getAppNameFromPackage(packageName) else appName
                                logd("✅ Found media info from notification: $finalAppName - '$title' by '$artist'")
                                return MediaInfo(
                                    isPlaying = true,
                                    appPackage = packageName.ifEmpty { detectedApp },
                                    appName = finalAppName,
                                    mediaTitle = title,
                                    mediaArtist = artist,
                                    hasActiveSession = true
                                )
                            }
                        }
                    } catch (e: Exception) {
                        logd("⚠️ Could not get media info from notification: ${e.message}")
                    }
                }
                
                // Si no podemos obtener metadatos, al menos devolver el nombre de la app
                return MediaInfo(
                    isPlaying = true,
                    appPackage = detectedApp,
                    appName = appName,
                    hasActiveSession = true
                )
            } else {
                logd("⚠️ Could not detect foreground app")
            }
            
            // SEGUNDO: Si no detectamos app en primer plano, intentar obtener de notificaciones
            val notificationListener = MyNotificationListener.getInstance()
            if (notificationListener != null) {
                logd("Step 2: NotificationListener available, trying to get media info from notifications...")
                try {
                    val mediaInfo = notificationListener.getActiveMediaNotificationInfo()
                    if (mediaInfo != null) {
                        val (packageName, title, artist) = mediaInfo
                        val appName = getAppNameFromPackage(packageName)
                        
                        logd("✅ Found media info from notification: $appName - '$title' by '$artist'")
                        return MediaInfo(
                            isPlaying = true,
                            appPackage = packageName,
                            appName = appName,
                            mediaTitle = title,
                            mediaArtist = artist,
                            hasActiveSession = true
                        )
                    } else {
                        logd("⚠️ No media notification info found")
                    }
                } catch (e: Exception) {
                    logd("⚠️ Could not get media info from notification: ${e.message}")
                }
            } else {
                logd("⚠️ NotificationListener not available, cannot get media info from notifications")
            }
            
            // Si no podemos obtener más información, devolver información genérica
            logd("⚠️ Falling back to generic 'Reproducción activa' message")
            val context = context ?: return MediaInfo()
            return MediaInfo(
                isPlaying = true,
                appPackage = "",
                appName = context.getString(dev.thecodegrove.grovetimer.R.string.media_info_active_playback),
                hasActiveSession = true
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in tryGetMediaInfoFromAudioManager", e)
            return MediaInfo()
        }
    }
    
    /**
     * Intenta detectar la app en primer plano
     */
    private fun tryDetectForegroundApp(): String? {
        if (context == null) {
            logd("⚠️ Context is null, cannot detect foreground app")
            return null
        }
        
        try {
            val activityManager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            // Intentar usar appTasks
            try {
                val appTasks = activityManager.appTasks
                logd("🔍 Found ${appTasks?.size ?: 0} app tasks")
                if (appTasks != null && appTasks.isNotEmpty()) {
                    val taskInfo = appTasks.first().taskInfo
                    val packageName = taskInfo.topActivity?.packageName
                    logd("🔍 Top activity package: $packageName")
                    if (packageName != null && packageName != context!!.packageName) {
                        logd("✅ Detected foreground app via appTasks: $packageName")
                        return packageName
                    } else {
                        logd("⚠️ Top activity is our own app or null")
                    }
                } else {
                    logd("⚠️ No app tasks found")
                }
            } catch (e: Exception) {
                logd("⚠️ Could not use appTasks: ${e.message}")
            }
            
            // Intentar usar runningAppProcesses
            try {
                val runningAppProcesses = activityManager.runningAppProcesses
                logd("🔍 Found ${runningAppProcesses?.size ?: 0} running processes")
                if (runningAppProcesses != null) {
                    for (processInfo in runningAppProcesses) {
                        if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            logd("🔍 Found foreground process with packages: ${processInfo.pkgList.joinToString()}")
                            for (packageName in processInfo.pkgList) {
                                if (packageName != context!!.packageName) {
                                    logd("✅ Detected foreground app via runningAppProcesses: $packageName")
                                    return packageName
                                }
                            }
                        }
                    }
                    logd("⚠️ No foreground processes found (excluding our app)")
                } else {
                    logd("⚠️ runningAppProcesses is null")
                }
            } catch (e: Exception) {
                logd("⚠️ Could not use runningAppProcesses: ${e.message}")
            }
        } catch (e: Exception) {
            logw("⚠️ Error detecting foreground app: ${e.message}")
        }
        
        logd("⚠️ Could not detect any foreground app")
        return null
    }
    
    /**
     * Intenta obtener información de media usando ActivityManager
     * Detecta apps de streaming conocidas que estén en primer plano
     * Nota: Este método es menos preciso pero puede ayudar cuando otros métodos fallan
     */
    private fun tryGetMediaInfoFromActivityManager(): MediaInfo {
        if (context == null) {
            return MediaInfo()
        }
        
        try {
            logd("🔍 Method 4: Trying ActivityManager...")
            
            val activityManager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            // Lista de paquetes conocidos de apps de streaming
            val streamingPackages = listOf(
                "es.movistar.plus", // Movistar+
                "com.netflix.mediaclient", // Netflix
                "com.amazon.avod.thirdpartyclient", // Prime Video
                "com.disney.disneyplus", // Disney+
                "com.hbo.hbonow", // HBO
                "com.spotify.music", // Spotify
                "com.google.android.youtube", // YouTube
                "com.google.android.apps.youtube.music", // YouTube Music
                "com.apple.android.music", // Apple Music
                "com.soundcloud.android", // SoundCloud
                "com.deezer.android.app", // Deezer
            )
            
            // Intentar usar getAppTasks (disponible desde API 21)
            try {
                val appTasks = activityManager.appTasks
                if (appTasks.isNotEmpty()) {
                    val taskInfo = appTasks.first().taskInfo
                    val packageName = taskInfo.topActivity?.packageName
                    
                    if (packageName != null && packageName in streamingPackages) {
                        val appName = getAppNameFromPackage(packageName)
                        logd("Found streaming app via appTasks: $packageName")
                        return MediaInfo(
                            isPlaying = true,
                            appPackage = packageName,
                            appName = appName,
                            hasActiveSession = true
                        )
                    }
                }
            } catch (e: Exception) {
                logd("Could not use appTasks: ${e.message}")
            }
            
            // Método alternativo: verificar procesos en ejecución
            // Nota: Este método es menos preciso pero puede ayudar
            try {
                val runningAppProcesses = activityManager.runningAppProcesses
                if (runningAppProcesses != null) {
                    for (processInfo in runningAppProcesses) {
                        if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            for (packageName in processInfo.pkgList) {
                                if (packageName in streamingPackages) {
                                    val appName = getAppNameFromPackage(packageName)
                                    logd("Found streaming app via runningAppProcesses: $packageName")
                                    return MediaInfo(
                                        isPlaying = true,
                                        appPackage = packageName,
                                        appName = appName,
                                        hasActiveSession = true
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logd("Could not use runningAppProcesses: ${e.message}")
            }
            
            return MediaInfo()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in tryGetMediaInfoFromActivityManager", e)
            return MediaInfo()
        }
    }
    
    /**
     * Obtiene el nombre legible de una aplicación a partir de su package name
     * 
     * @param packageName Package name de la aplicación
     * @return Nombre legible de la aplicación o package name si no se puede obtener
     */
    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val packageManager = context?.packageManager
            val applicationInfo = packageManager?.getApplicationInfo(packageName, 0)
            if (applicationInfo != null) {
                packageManager?.getApplicationLabel(applicationInfo)?.toString() ?: packageName
            } else {
                packageName
            }
        } catch (e: Exception) {
            logw("Could not get app name for package: $packageName", e)
            packageName
        }
    }
    
    /**
     * Detiene el temporizador y resetea el estado
     */
    fun stopTimer() {
        logd("Stopping timer")
        viewModelScope.launch {
            // Detener el temporizador en el repositorio
            repository.stopTimer()
            
            // Detener el servicio del temporizador
            context?.let { context ->
                try {
                    val intent = Intent(context, SleepTimerService::class.java)
                    context.stopService(intent)
                    logd("SleepTimerService stop request sent")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop SleepTimerService", e)
                }
            }
        }
    }
    
    /**
     * Verifica si el NotificationListener está habilitado en el sistema
     * 
     * @return true si está habilitado, false en caso contrario
     */
    fun isNotificationListenerEnabled(): Boolean {
        if (context == null) return false
        return isNotificationListenerEnabled(context!!)
    }
    
    private fun isNotificationListenerEnabled(ctx: Context): Boolean {
        return try {
            val packageName = ctx.packageName
            val listenerName = MyNotificationListener.getComponentName(ctx).flattenToString()
            val flat = android.provider.Settings.Secure.getString(
                ctx.contentResolver, 
                "enabled_notification_listeners"
            )
            
            if (flat.isNullOrEmpty()) {
                logd("No notification listeners enabled")
                false
            } else {
                val names = flat.split(":")
                val isEnabled = names.contains(listenerName)
                logd("NotificationListener enabled: $isEnabled")
                isEnabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification listener status", e)
            false
        }
    }
    
    /**
     * Abre automáticamente la pantalla de ajustes para habilitar el NotificationListener
     * No se puede habilitar automáticamente por seguridad, pero podemos abrir los ajustes
     */
    fun openNotificationListenerSettings() {
        if (context == null) {
            logw("Context is null, cannot open NotificationListener settings")
            return
        }
        
        try {
            logd("Opening NotificationListener settings...")
            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context!!.startActivity(intent)
            logd("✅ NotificationListener settings opened")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening NotificationListener settings", e)
            // Intentar método alternativo
            try {
                val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, 
                        MyNotificationListener.getComponentName(context!!).flattenToString())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context!!.startActivity(intent)
                logd("✅ NotificationListener detail settings opened")
            } catch (e2: Exception) {
                Log.e(TAG, "Error opening NotificationListener detail settings", e2)
            }
        }
    }
    
    /**
     * Actualiza la configuración de modo oscuro
     */
    fun updateDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateUserSettingsUseCase { currentSettings ->
                currentSettings.copy(darkModeEnabled = enabled)
            }
            // Recargar configuraciones después del cambio
            loadCurrentSettings()
            // Notificar al servicio del cambio (aunque el modo oscuro no afecta al timer, lo enviamos por consistencia)
            notifySettingsChanged()
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
     * Obtiene el estado actual del temporizador
     * 
     * @return Estado actual del temporizador
     */
    fun getCurrentState(): TimerState = repository.getCurrentState()
    
    /**
     * Verifica si el temporizador está activo
     * 
     * @return true si el temporizador está ejecutándose, false en caso contrario
     */
    fun isTimerActive(): Boolean = repository.getCurrentState().isActive
    
    /**
     * Incrementa el tiempo seleccionado en la cantidad especificada de minutos
     * Respeta los límites del slider (0-240 minutos)
     * 
     * @param currentTimeMillis Tiempo actual en milisegundos (del slider)
     * @param incrementMinutes Cantidad de minutos a incrementar
     */
    fun incrementSelectedTime(currentTimeMillis: Long, incrementMinutes: Int) {
        val incrementMillis = incrementMinutes * 60 * 1000L
        val newTimeMillis = (currentTimeMillis + incrementMillis).coerceAtMost(240 * 60 * 1000L)
        val newTimeFormatted = formatRemainingTime(newTimeMillis)
        
        val currentMinutes = currentTimeMillis / (60 * 1000)
        val newMinutes = newTimeMillis / (60 * 1000)
        logd("Incrementing time: +${incrementMinutes}min, ${currentMinutes}min -> ${newMinutes}min")
        
        viewModelScope.launch {
            repository.updateSelectedTime(newTimeMillis, newTimeFormatted)
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
    
    /**
     * Verifica si hay un temporizador activo y recupera su estado
     * Se llama al iniciar la aplicación para sincronizar la UI con el estado del servicio
     */
    fun checkAndRecoverTimerState() {
        logd("🚀 STARTING checkAndRecoverTimerState() - App launch detected")
        viewModelScope.launch {
            try {
                logd("🚀 Step 1: Checking if SleepTimerService is running...")
                
                // Verificar si SleepTimerService está ejecutándose
                val isServiceRunning = checkTimerServiceUseCase()
                logd("🚀 Step 1 Result: SleepTimerService is running: $isServiceRunning")
                
                if (isServiceRunning) {
                    logd("🚀 Step 2: Service is running, attempting to recover timer state...")
                    
                    // Obtener el estado del temporizador desde el servicio
                    val serviceState = getTimerStateFromServiceUseCase()
                    logd("🚀 Step 2 Result: Recovered state from service: $serviceState")
                    
                    // Actualizar el estado del repositorio con el estado recuperado
                    repository.updateTimerState(serviceState)
                    logd("🚀 Step 3: Timer state recovered and updated successfully")
                    
                    // Actualizar información de media
                    logd("🚀 Step 4: Updating media info...")
                    updateMediaInfo()
                    
                    logd("🚀 SUCCESS: Timer state recovery completed successfully")
                    
                } else {
                    logd("🚀 INFO: Service is not running, using default state")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "🚀 ERROR: Error checking and recovering timer state", e)
                // En caso de error, continuar con el estado por defecto
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // Limpiar el BroadcastReceiver cuando el ViewModel se destruye
        broadcastReceiver?.let { receiver ->
            context?.let { context ->
                try {
                    context.unregisterReceiver(receiver)
                    logd("BroadcastReceiver unregistered successfully")
                } catch (e: IllegalArgumentException) {
                    // El receiver ya fue desregistrado o no estaba registrado
                    logw("BroadcastReceiver already unregistered")
                }
            }
        }
        broadcastReceiver = null
        context = null
        mediaSessionManager = null
    }
}
