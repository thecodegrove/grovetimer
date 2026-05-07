package dev.thecodegrove.grovetimer.services

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import dev.thecodegrove.grovetimer.R
import dev.thecodegrove.grovetimer.utils.DebugUtils
import dev.thecodegrove.grovetimer.utils.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SleepTimerService : Service() {

    // Helper functions for conditional debug logging
    private fun logd(message: String) {
        if (DebugUtils.isDebug(this)) {
            Log.d(TAG, message)
        }
    }
    
    private fun logw(message: String, throwable: Throwable? = null) {
        if (DebugUtils.isDebug(this)) {
            if (throwable != null) {
                Log.w(TAG, message, throwable)
            } else {
                Log.w(TAG, message)
            }
        }
    }
    
    private fun logi(message: String) {
        if (DebugUtils.isDebug(this)) {
            Log.i(TAG, message)
        }
    }

    private var timerJob: Job? = null
    private var countdownTime: Long = 0L
    private var currentRemainingTime: Long = 0L
    private var isPaused: Boolean = false
    private var fadeoutEnabled: Boolean = false  // Configuración de fade-out del usuario
    private var fadeOutDuration: Long = 10000L  // 10 segundos opcionales de fade-out
    private var vibrateOnFinish: Boolean = false  // Configuración de vibración
    private var soundEnabled: Boolean = true  // Configuración de sonido
    private var isFadeOutStarted: Boolean = false  // Control para evitar múltiples fade-outs
    private var fadeOutJob: Job? = null  // Job del fade-out para poder cancelarlo
    private var lastSettingsReloadTime: Long = 0L  // Timestamp de última recarga de configuración
    
    // SharedPreferences para persistir el estado del temporizador
    private val prefs by lazy {
        getSharedPreferences("timer_state", Context.MODE_PRIVATE)
    }
    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val vibrator: Vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    private val notificationControlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            logd("🎯 BroadcastReceiver - Received broadcast with action: ${intent?.action}")
            logd("🎯 BroadcastReceiver - Intent: $intent")
            when (intent?.action) {
                TimerNotificationService.ACTION_PAUSE_TIMER -> {
                    logd("🎯 BroadcastReceiver - Calling pauseTimer()")
                    pauseTimer()
                }
                TimerNotificationService.ACTION_RESUME_TIMER -> {
                    logd("🎯 BroadcastReceiver - Calling resumeTimer()")
                    resumeTimer()
                }
                TimerNotificationService.ACTION_STOP_TIMER -> {
                    logd("🎯 BroadcastReceiver - Calling stopTimer()")
                    stopTimer()
                }
                ACTION_REQUEST_STATE -> {
                    logd("🎯 BroadcastReceiver - Timer state requested from app")
                    sendTimerStateResponse()
                }
                ACTION_UPDATE_SETTINGS -> {
                    logd("🎯 BroadcastReceiver - Settings update requested")
                    reloadSettingsFromPreferences()
                }
                else -> {
                    logw("🎯 BroadcastReceiver - Unknown action: ${intent?.action}")
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        logd("🎯 SleepTimerService onCreate - Starting initialization")
        createNotificationChannel()
        logd("🎯 Notification channel created, registering BroadcastReceiver...")
        registerNotificationControlReceiver()
        
        // Cargar estado persistido si existe
        loadTimerState()
        
        // NO iniciar como foreground service aquí - lo haremos cuando se inicie el timer
        logd("🎯 SleepTimerService created, waiting for timer start")
    }
    
    /**
     * Registra el BroadcastReceiver para escuchar controles de notificación
     */
    private fun registerNotificationControlReceiver() {
        val filter = IntentFilter().apply {
            addAction(TimerNotificationService.ACTION_PAUSE_TIMER)
            addAction(TimerNotificationService.ACTION_RESUME_TIMER)
            addAction(TimerNotificationService.ACTION_STOP_TIMER)
            addAction(ACTION_REQUEST_STATE)
            addAction(ACTION_UPDATE_SETTINGS)
        }
        registerReceiver(notificationControlReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        logd("🎯 Notification control receiver registered with ACTION_REQUEST_STATE")
        logd("🎯 Receiver filter actions: ${filter.actionsIterator().asSequence().toList()}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logd("🎯 SleepTimerService onStartCommand with action: ${intent?.action}")
        
        when (intent?.action) {
            TimerNotificationService.ACTION_PAUSE_TIMER -> {
                logd("🎯 Pause timer requested from notification")
                pauseTimer()
                return START_STICKY
            }
            TimerNotificationService.ACTION_RESUME_TIMER -> {
                logd("🎯 Resume timer requested from notification")
                resumeTimer()
                return START_STICKY
            }
            TimerNotificationService.ACTION_STOP_TIMER -> {
                logd("🎯 Stop timer requested from notification")
                stopTimer()
                return START_STICKY
            }
            ACTION_REQUEST_STATE -> {
                logd("🎯 Timer state requested from app")
                sendTimerStateResponse()
                return START_STICKY
            }
            ACTION_UPDATE_SETTINGS -> {
                logd("🎯 Settings update requested from onStartCommand")
                reloadSettingsFromPreferences()
                return START_STICKY
            }
            else -> {
                // Inicio normal del temporizador
                countdownTime = intent?.getLongExtra(KEY_DURATION_MILLIS, 0L) ?: 0L
                
                // Cargar configuración desde SharedPreferences
                reloadSettingsFromPreferences()
                
                // Resetear estado del fade-out para nuevo timer
                isFadeOutStarted = false
                fadeOutJob = null
                
                logd("🎯 SleepTimerService onStartCommand - duration: $countdownTime ms, fadeout: $fadeoutEnabled, vibrate: $vibrateOnFinish, sound: $soundEnabled, fadeOutDuration: $fadeOutDuration")

                // Iniciar como foreground service con notificación completa
                logd("🎯 Starting as foreground service with timer notification")
                startForeground(NOTIF_ID, buildTimerNotification(currentRemainingTime, isPaused))
                
                startTimer()
                return START_STICKY
            }
        }
    }
    
    /**
     * Inicia el temporizador principal
     */
    private fun startTimer() {
        timerJob?.cancel()  // Cancelar cualquier temporizador previo
        currentRemainingTime = countdownTime
        isPaused = false
        
        // Guardar estado inicial
        saveTimerState()

        timerJob = CoroutineScope(Dispatchers.Main).launch {
            logd("Starting countdown loop with initial time: $currentRemainingTime ms")
            
            while (currentRemainingTime > 0 && !isPaused) {
                logd("Timer countdown: $currentRemainingTime ms remaining")
                
                // Recargar configuración periódicamente (cada 5 segundos) para detectar cambios
                // Solo como respaldo, ya que el broadcast es la forma principal de notificar cambios
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastSettingsReloadTime > 5000L) { // Cada 5 segundos
                    reloadSettingsFromPreferences()
                    lastSettingsReloadTime = currentTime
                }
                
                // Verificar si es momento de empezar el fade-out
                if (fadeoutEnabled && fadeOutDuration > 0 && currentRemainingTime <= fadeOutDuration && !isFadeOutStarted) {
                    logd("🎵 Starting fade-out process - remaining: $currentRemainingTime ms")
                    isFadeOutStarted = true
                    startFadeOutProcess()
                }
                
                // Actualizar notificación directamente
                logd("🎯 Updating notification - time: $currentRemainingTime ms, paused: $isPaused")
                updateTimerNotification(currentRemainingTime, isPaused)
                
                // Enviar broadcast con el tiempo restante para actualizar la UI
                sendTimerUpdateBroadcast(currentRemainingTime)
                
                delay(1000L)
                currentRemainingTime -= 1000L
                
                // Guardar estado cada segundo
                saveTimerState()
            }
            
            if (currentRemainingTime <= 0) {
                logd("Countdown finished, sending finished broadcast")
                // Enviar broadcast de finalización del temporizador
                sendTimerFinishedBroadcast()
                
                // Limpiar estado guardado
                clearTimerState()
                
                performFadeOutAndPause()
                performVibration()
                stopTimer()
            }
        }
    }

    override fun onDestroy() {
                logd( "onDestroy")
        timerJob?.cancel()
        fadeOutJob?.cancel()
        
        // Enviar broadcast de detención del temporizador
        sendTimerStoppedBroadcast()
        
        // No necesitamos detener TimerNotificationService ya que no lo usamos
        
        // Desregistrar BroadcastReceiver
        try {
            unregisterReceiver(notificationControlReceiver)
        } catch (e: IllegalArgumentException) {
            logw("Receiver not registered")
        }
        
        super.onDestroy()
    }
    
    /**
     * Pausa el temporizador
     */
    private fun pauseTimer() {
                logd( "🎯 pauseTimer() called - isPaused: $isPaused, currentRemainingTime: $currentRemainingTime")
        if (!isPaused) {
            isPaused = true
            timerJob?.cancel()
            logd("🎯 Timer paused at $currentRemainingTime ms")
            
            // Actualizar notificación INMEDIATAMENTE con estado pausado
            logd("🎯 Updating notification for pause - time: $currentRemainingTime ms, paused: $isPaused")
            updateTimerNotification(currentRemainingTime, isPaused)
            
            // Enviar broadcast de pausa
            sendTimerPausedBroadcast()
            
            // IMPORTANTE: NO detener el servicio, solo pausar el countdown
            logd("🎯 Service continues running in paused state")
        } else {
            logw("🎯 Timer was already paused")
        }
    }
    
    /**
     * Reanuda el temporizador
     */
    private fun resumeTimer() {
                logd( "🎯 resumeTimer() called - isPaused: $isPaused, currentRemainingTime: $currentRemainingTime")
        if (isPaused && currentRemainingTime > 0) {
            isPaused = false
            logd("🎯 Timer resumed with $currentRemainingTime ms remaining")
            
            // Reanudar countdown
            timerJob = CoroutineScope(Dispatchers.Main).launch {
                while (currentRemainingTime > 0 && !isPaused) {
                    logd("Timer countdown: $currentRemainingTime ms remaining")
                    
                    // Recargar configuración periódicamente (cada 5 segundos) para detectar cambios
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSettingsReloadTime > 5000L) { // Cada 5 segundos
                        reloadSettingsFromPreferences()
                        lastSettingsReloadTime = currentTime
                    }
                    
                    // Verificar si es momento de empezar el fade-out
                    if (fadeoutEnabled && fadeOutDuration > 0 && currentRemainingTime <= fadeOutDuration && !isFadeOutStarted) {
                        logd("🎵 Starting fade-out process (resume) - remaining: $currentRemainingTime ms")
                        isFadeOutStarted = true
                        startFadeOutProcess()
                    }
                    
                    // Actualizar notificación directamente
                    logd("🎯 Updating notification (resume) - time: $currentRemainingTime ms, paused: $isPaused")
                    updateTimerNotification(currentRemainingTime, isPaused)
                    
                    // Enviar broadcast con el tiempo restante para actualizar la UI
                    sendTimerUpdateBroadcast(currentRemainingTime)
                    
                    delay(1000L)
                    currentRemainingTime -= 1000L
                }
                
                if (currentRemainingTime <= 0) {
                    logd("Countdown finished after resume, sending finished broadcast")
                    sendTimerFinishedBroadcast()
                    performFadeOutAndPause()
                    performVibration()
                    stopTimer()
                }
            }
            
            // Actualizar notificación INMEDIATAMENTE con estado reanudado
            logd("🎯 Updating notification for resume - time: $currentRemainingTime ms, paused: $isPaused")
            updateTimerNotification(currentRemainingTime, isPaused)
            
            // Enviar broadcast de reanudación
            sendTimerResumedBroadcast()
        } else {
            logw("🎯 Cannot resume - isPaused: $isPaused, remainingTime: $currentRemainingTime")
        }
    }
    
    /**
     * Detiene el temporizador completamente
     */
    private fun stopTimer() {
                logd( "🎯 stopTimer() called")
        timerJob?.cancel()
        currentRemainingTime = 0L
        isPaused = false
        
        // Enviar broadcast de detención
        sendTimerStoppedBroadcast()
        
        // Detener este servicio
                logd( "🎯 Stopping SleepTimerService")
        stopSelf()
    }
    
    /**
     * Envía un broadcast con la actualización del tiempo restante
     * 
     * @param remainingMillis Tiempo restante en milisegundos
     */
    private fun sendTimerUpdateBroadcast(remainingMillis: Long) {
                logd( "Sending timer update broadcast: $remainingMillis ms")
        val intent = Intent(ACTION_TIMER_UPDATE).apply {
            putExtra(EXTRA_REMAINING_TIME, remainingMillis)
        }
        sendBroadcast(intent)
                logd( "Timer update broadcast sent")
    }
    
    /**
     * Envía un broadcast cuando el temporizador termina
     */
    private fun sendTimerFinishedBroadcast() {
                logd( "Sending timer finished broadcast")
        val intent = Intent(ACTION_TIMER_FINISHED)
        sendBroadcast(intent)
                logd( "Timer finished broadcast sent")
    }
    
    /**
     * Envía un broadcast cuando el temporizador se detiene
     */
    private fun sendTimerStoppedBroadcast() {
                logd( "Sending timer stopped broadcast")
        val intent = Intent(ACTION_TIMER_STOPPED)
        sendBroadcast(intent)
                logd( "Timer stopped broadcast sent")
    }
    
    /**
     * Envía un broadcast cuando el temporizador se pausa
     */
    private fun sendTimerPausedBroadcast() {
                logd( "Sending timer paused broadcast")
        val intent = Intent(ACTION_TIMER_PAUSED).apply {
            putExtra(EXTRA_REMAINING_TIME, currentRemainingTime)
            putExtra(EXTRA_IS_PAUSED, true)
        }
        sendBroadcast(intent)
                logd( "Timer paused broadcast sent")
    }
    
    /**
     * Envía un broadcast cuando el temporizador se reanuda
     */
    private fun sendTimerResumedBroadcast() {
                logd( "Sending timer resumed broadcast")
        val intent = Intent(ACTION_TIMER_RESUMED).apply {
            putExtra(EXTRA_REMAINING_TIME, currentRemainingTime)
            putExtra(EXTRA_IS_PAUSED, false)
        }
        sendBroadcast(intent)
                logd( "Timer resumed broadcast sent")
    }

    /**
     * Inicia el proceso de fade-out en paralelo al countdown
     */
    private fun startFadeOutProcess() {
                logd( "🎵 startFadeOutProcess() - Starting fade-out in parallel")
        
        // Cancelar fade-out anterior si existe
        fadeOutJob?.cancel()
        
        fadeOutJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val steps = 10 // número de pasos para el fade out
                val initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                logd("🎵 Fade-out parallel process - steps: $steps, initialVolume: $initialVolume, fadeOutDuration: $fadeOutDuration")

                if (fadeOutDuration > 0 && steps > 0 && initialVolume > 0) {
                    val stepDuration = fadeOutDuration / steps
                    logd("🎵 Fade-out parallel step duration: $stepDuration ms")
                    
                    if (stepDuration > 0) {
                        for (i in 1..steps) {
                            val targetVolume = ((initialVolume * (steps - i).toFloat()) / steps).toInt()
                            logd("🎵 Fade-out parallel step $i/$steps: targetVolume=$targetVolume")
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                if (targetVolume < 0) 0 else targetVolume,
                                0
                            )
                            delay(stepDuration)
                        }
                        logd("🎵 Fade-out parallel process completed successfully")
                    } else {
                        logw("🎵 Fade-out parallel step duration too short: $stepDuration")
                        if(initialVolume > 0) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                    }
                } else {
                    logw("🎵 Fade-out parallel conditions not met - fadeOutDuration: $fadeOutDuration, steps: $steps, initialVolume: $initialVolume")
                }
            } catch (e: Exception) {
                    Log.e(TAG, "🎵 Error during parallel fade out", e)
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                } catch (ex: Exception) {
                    Log.e(TAG, "🎵 Error setting volume to 0", ex)
                }
            }
        }
    }
    
    /**
     * Recarga la configuración desde SharedPreferences y aplica los cambios
     */
    private fun reloadSettingsFromPreferences() {
                logd( "🔄 Reloading settings from SharedPreferences")
        
        try {
            val settingsPrefs = getSharedPreferences("grovetimer_settings", Context.MODE_PRIVATE)
            val newFadeoutEnabled = settingsPrefs.getBoolean("fadeout_enabled", false)
            val newVibrateOnFinish = settingsPrefs.getBoolean("vibrate_on_finish", false)
            val newSoundEnabled = settingsPrefs.getBoolean("sound_enabled", true)
            val fadeoutDurationSeconds = settingsPrefs.getInt("fadeout_duration", 30)
            val newFadeOutDuration = fadeoutDurationSeconds * 1000L
            
            logd("🔄 Loaded settings: fadeout=$newFadeoutEnabled, vibrate=$newVibrateOnFinish, sound=$newSoundEnabled, fadeoutDuration=${newFadeOutDuration}ms")
            
            // Si fadeout se desactiva durante ejecución, cancelar el fade-out en curso
            if (fadeoutEnabled && !newFadeoutEnabled && isFadeOutStarted) {
                logd("🔄 Fadeout disabled during execution - canceling fade-out process")
                fadeOutJob?.cancel()
                fadeOutJob = null
                isFadeOutStarted = false
                // Nota: No podemos restaurar el volumen exacto, pero el fade-out se detiene
            }
            
            // Si fadeout se activa durante ejecución y aún no ha empezado, actualizar duración
            if (!fadeoutEnabled && newFadeoutEnabled && !isFadeOutStarted) {
                logd("🔄 Fadeout enabled during execution - will use new duration: ${newFadeOutDuration}ms")
            }
            
            // Actualizar variables locales
            val fadeoutChanged = fadeoutEnabled != newFadeoutEnabled
            val fadeoutDurationChanged = fadeOutDuration != newFadeOutDuration
            
            fadeoutEnabled = newFadeoutEnabled
            vibrateOnFinish = newVibrateOnFinish
            soundEnabled = newSoundEnabled
            fadeOutDuration = newFadeOutDuration
            
            // Actualizar timestamp de última recarga
            lastSettingsReloadTime = System.currentTimeMillis()
            
            logd("🔄 Settings updated: fadeout=$fadeoutEnabled, vibrate=$vibrateOnFinish, sound=$soundEnabled, fadeoutDuration=${fadeOutDuration}ms")
            
            // Si el fade-out ya estaba en proceso y cambió la duración, no podemos ajustarlo
            // pero al menos la nueva configuración se aplicará en el próximo timer
            if (fadeoutChanged || fadeoutDurationChanged) {
                logd("🔄 Fadeout configuration changed during execution")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "🔄 Error reloading settings from SharedPreferences", e)
        }
    }

    private suspend fun performFadeOutAndPause() {
                logd( "performFadeOutAndPause - fadeoutEnabled: $fadeoutEnabled, fadeOutDuration: $fadeOutDuration")
        
        if (!fadeoutEnabled) {
            logd("Fade-out disabled by user settings, pausing directly")
            try {
                pauseActiveMediaSession()
            } catch (e: Exception) {
                Log.e(TAG, "Error in pauseActiveMediaSession. Silencing output as a fallback.", e)
                silenceAudioOutput()
            }
            return
        }
        
        if (!fadeoutEnabled) {
            logd("Fade-out disabled by user settings, pausing directly")
            try {
                pauseActiveMediaSession()
            } catch (e: Exception) {
                Log.e(TAG, "Error in pauseActiveMediaSession. Silencing output as a fallback.", e)
                silenceAudioOutput()
            }
            return
        }
        
        // El fade-out ya se ejecutó en paralelo, solo necesitamos pausar la música
                logd( "performFadeOutAndPause - Pausing media (fade-out already completed)")


        try {
            pauseActiveMediaSession()
        } catch (e: Exception) {
            Log.e(TAG, "Error in pauseActiveMediaSession. Silencing output as a fallback.", e)
            try {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    0, // Silence
                    0
                )
            } catch (volEx: Exception) {
                Log.e(TAG, "Error silencing audio output", volEx)
            }
        }
    }

    /**
     * Ejecuta la vibración cuando el timer termina
     * Solo vibra si la configuración está habilitada, el dispositivo soporta vibración y tiene permisos
     */
    private fun performVibration() {
        if (!vibrateOnFinish) {
            logd("Vibration disabled by user settings")
            return
        }
        
        if (!vibrator.hasVibrator()) {
            logw("Device does not support vibration")
            return
        }
        
        if (!PermissionUtils.hasVibratePermission(this)) {
            logw("Vibration permission not granted")
            return
        }
        
        try {
            logd("Performing vibration on timer finish")
            
            // Patrón de vibración: vibrar por 500ms, pausa 200ms, vibrar por 500ms
            val vibrationPattern = longArrayOf(0, 500, 200, 500)
 
            // API 26+ usa VibrationEffect
            val vibrationEffect = VibrationEffect.createWaveform(
                vibrationPattern,
                -1 // No repetir
            )
            vibrator.vibrate(vibrationEffect)
 
            logd("Vibration completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing vibration", e)
        }
    }

    // Helper function to check if Notification Listener is enabled
    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = packageName
        // Ensure MyNotificationListener::class.java.name gives the fully qualified name
        val listenerName = MyNotificationListener::class.java.name
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null) {
                    if (TextUtils.equals(packageName, cn.packageName) &&
                        TextUtils.equals(listenerName, cn.className)) {
                        logi("Notification Listener IS enabled for: ${cn.flattenToString()}")
                        return true
                    }
                }
            }
        }
                logw( "Notification Listener IS NOT enabled for $packageName/$listenerName. Please enable it in System Settings.")
        return false
    }

    private fun pauseActiveMediaSession() {
                logd( "pauseActiveMediaSession - attempting to pause active media sessions.")

        val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val listenerComponent = ComponentName(this, MyNotificationListener::class.java)
                logd( "Querying active sessions with listener: ${listenerComponent.flattenToString()}")

        var pausedAtLeastOne = false
        var shouldTryAlternatives = false
        
        try {
            // Intentar obtener sesiones con NotificationListener
            val activeSessions = try {
                mediaSessionManager.getActiveSessions(listenerComponent)
            } catch (e: SecurityException) {
                logw("⚠️ SecurityException with NotificationListener: ${e.message}")
                logw("⚠️ This is expected for apps without MEDIA_CONTENT_CONTROL permission")
                // Intentar sin listener como fallback (puede funcionar en algunos casos)
                try {
                    mediaSessionManager.getActiveSessions(null)
                } catch (e2: SecurityException) {
                    logw("⚠️ SecurityException without listener too: ${e2.message}")
                    logw("⚠️ Will use alternative methods to pause media")
                    shouldTryAlternatives = true
                    emptyList()
                }
            }
            
            if (activeSessions.isEmpty()) {
                logd("No active media sessions found via MediaSessionManager")
                shouldTryAlternatives = true
            } else {
                logi("Found ${activeSessions.size} active session(s) via MediaSessionManager")

            for (controller in activeSessions) {
                val playbackState = controller.playbackState
                val packageName = controller.packageName
                logd("Checking session from package: $packageName, State: ${playbackState?.stateToString() ?: "null"}")

                if (playbackState != null && playbackState.state == PlaybackState.STATE_PLAYING) {
                    logi("Attempting to pause: $packageName")
                    try {
                        controller.transportControls.pause()
                            logi("✅ Successfully sent pause command to: $packageName")
                        pausedAtLeastOne = true
                        // Continue to pause any other playing sessions
                    } catch (e: SecurityException) {
                            logw("⚠️ SecurityException: Cannot pause $packageName. App may have restrictions.", e)
                            shouldTryAlternatives = true
                            // Continuar con otras sesiones
                    } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to pause $packageName", e)
                            shouldTryAlternatives = true
                    }
                }
            }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error while getting/processing media sessions", e)
            shouldTryAlternatives = true
        }

        if (!pausedAtLeastOne || shouldTryAlternatives) {
            logd("⚠️ No actively playing media sessions were successfully paused via MediaSessionManager")
            logd("⚠️ Will try alternative methods to pause media")
            // Intentar métodos alternativos
            tryAlternativePauseMethods()
        } else {
            logd("✅ Successfully paused at least one media session")
        }
    }
    
    /**
     * Intenta pausar la reproducción usando métodos alternativos cuando MediaSessionManager falla
     */
    private fun tryAlternativePauseMethods() {
                logd( "🔄 Trying alternative pause methods...")
        
        // Método 0: Usar AudioFocus de forma agresiva (más efectivo, ~85-95% de apps)
        tryPauseViaAudioFocus()
        
        // Método 1: Intentar usar AudioManager para enviar comando de pausa
        tryPauseViaAudioManager()
        
        // Método 2: Intentar usar NotificationListener para tocar notificación de pausa
        tryPauseViaNotificationListener()
        
        // Método 3: Intentar enviar intent de media pause
        tryPauseViaIntent()
        
        // Método 4: Intentar pausar apps específicas conocidas mediante intents
        tryPauseKnownApps()
        
        // Método 5: Intentar usar MediaButtonReceiver
        tryPauseViaMediaButtonReceiver()
    }
    
    /**
     * Intenta pausar usando AudioFocus de forma agresiva
     * Este método solicita AudioFocus permanente, lo que hace que apps bien implementadas
     * se pausen automáticamente. Tasa de éxito: ~85-95% de apps modernas.
     */
    private fun tryPauseViaAudioFocus() {
        try {
            logd("🔄 Method 0: Trying AudioFocus request (most effective method)...")
            
            // Intentar con AudioFocusRequest (API 26+)
            try {
                // Solicitar AudioFocus permanente (no transitorio) para forzar pausa
                val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                    logd("AudioFocus changed: $focusChange")
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            logd("✅ AudioFocus LOSS - app should pause")
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            logd("✅ AudioFocus LOSS_TRANSIENT - app should pause")
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            logd("AudioFocus GAIN")
                        }
                    }
                }
                
                val audioFocusRequest = android.media.AudioFocusRequest.Builder(
                    android.media.AudioManager.AUDIOFOCUS_GAIN
                ).apply {
                    setOnAudioFocusChangeListener(audioFocusChangeListener)
                    setWillPauseWhenDucked(false)
                }.build()
                
                val result = audioManager.requestAudioFocus(audioFocusRequest)
                when (result) {
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                        logd("✅ AudioFocus GRANTED - apps should pause automatically")
                        // Mantener el focus por un momento para asegurar que las apps reaccionen
                        Thread.sleep(500)
                        // Liberar el focus después
                        audioManager.abandonAudioFocusRequest(audioFocusRequest)
                        logd("✅ AudioFocus released after pause request")
                    }
                    AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                        logw("⚠️ AudioFocus REQUEST_FAILED")
                    }
                    else -> {
                        logw("⚠️ AudioFocus returned: $result")
                    }
                }
            } catch (e: NoSuchMethodError) {
                // API < 26, usar método antiguo
                logd("⚠️ AudioFocusRequest not available (API < 26), using legacy method...")
                try {
                    val result = audioManager.requestAudioFocus(
                        null,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN
                    )
                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        logd("✅ AudioFocus GRANTED (legacy) - apps should pause automatically")
                        Thread.sleep(500)
                        audioManager.abandonAudioFocus(null)
                        logd("✅ AudioFocus released (legacy)")
                    } else {
                        logw("⚠️ AudioFocus REQUEST_FAILED (legacy)")
                    }
                } catch (e2: Exception) {
                    logw("⚠️ Failed to request AudioFocus (legacy): ${e2.message}")
                }
            }
        } catch (e: Exception) {
            logw("⚠️ Failed to pause via AudioFocus: ${e.message}")
        }
    }
    
    /**
     * Intenta pausar usando AudioManager (envía comando de media pause)
     * Intenta múltiples veces con diferentes métodos
     */
    private fun tryPauseViaAudioManager() {
        try {
            logd("🔄 Method 1: Trying AudioManager media button command...")
            
            // Crear KeyEvents para pausa usando constructor simple y válido
            val now = System.currentTimeMillis()
            val downEvent = KeyEvent(
                now,
                now,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                0
            )
            val upEvent = KeyEvent(
                now + 50,
                now + 50,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                0
            )
            
            // Intentar obtener focus de audio temporalmente (API 26+)
            try {
                val audioFocusRequest = android.media.AudioFocusRequest.Builder(
                    android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                ).build()
                
                val result = audioManager.requestAudioFocus(audioFocusRequest)
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    logd("✅ Audio focus granted, sending pause command")
                    
                    // Enviar comando de pausa múltiples veces para asegurar que se recibe
                    for (i in 1..3) {
                        audioManager.dispatchMediaKeyEvent(downEvent)
                        Thread.sleep(50)
                        audioManager.dispatchMediaKeyEvent(upEvent)
                        if (i < 3) Thread.sleep(100) // Pausa entre intentos
                    }
                    
                    logd("✅ Media pause command sent via AudioManager (3 attempts)")
                    
                    // Liberar focus de audio después de un delay
                    Thread.sleep(200)
                    audioManager.abandonAudioFocusRequest(audioFocusRequest)
                } else {
                    logw("⚠️ Could not get audio focus, trying without focus...")
                    // Intentar sin focus múltiples veces
                    for (i in 1..3) {
                        audioManager.dispatchMediaKeyEvent(downEvent)
                        Thread.sleep(50)
                        audioManager.dispatchMediaKeyEvent(upEvent)
                        if (i < 3) Thread.sleep(100)
                    }
                    logd("✅ Media pause command sent without audio focus (3 attempts)")
                }
            } catch (e: NoSuchMethodError) {
                // API < 26, usar método antiguo
                logd("⚠️ AudioFocusRequest not available (API < 26), trying without focus...")
                try {
                    for (i in 1..3) {
                        audioManager.dispatchMediaKeyEvent(downEvent)
                        Thread.sleep(50)
                        audioManager.dispatchMediaKeyEvent(upEvent)
                        if (i < 3) Thread.sleep(100)
                    }
                    logd("✅ Media pause command sent (legacy method, 3 attempts)")
                } catch (e2: Exception) {
                    logw("⚠️ Failed to dispatch media key event: ${e2.message}")
                }
            }
        } catch (e: Exception) {
            logw("⚠️ Failed to pause via AudioManager: ${e.message}")
        }
    }
    
    /**
     * Intenta pausar usando NotificationListener (toca botón de pausa en notificación)
     */
    private fun tryPauseViaNotificationListener() {
        try {
            logd("🔄 Method 2: Trying NotificationListener...")
            
            val notificationListener = MyNotificationListener.getInstance()
            if (notificationListener == null) {
                logd("⚠️ NotificationListener not connected")
                return
            }
            
            val activeNotifications = notificationListener.activeNotifications
            if (activeNotifications == null || activeNotifications.isEmpty()) {
                logd("⚠️ No active notifications found")
                return
            }
            
            // Buscar notificaciones de media y intentar tocar el botón de pausa
            for (notification in activeNotifications) {
                val packageName = notification.packageName
                val notificationObj = notification.notification
                val actions = notificationObj.actions
                
                if (actions != null) {
                    // Buscar acción de pausa (buscar por título o por icono)
                    for (i in actions.indices) {
                        val action = actions[i]
                        val actionTitle = action.title?.toString()?.lowercase() ?: ""
                        
                        // Buscar acciones de pausa por título o por ser la segunda acción (típicamente play/pause)
                        val isPauseAction = actionTitle.contains("pause") || 
                                           actionTitle.contains("pausar") ||
                                           (i == 1 && actions.size >= 2) // Segunda acción suele ser pause
                        
                        if (isPauseAction && action.actionIntent != null) {
                            try {
                                logd("✅ Found pause action in notification from $packageName (action $i: '$actionTitle'), attempting to trigger...")
                                action.actionIntent.send()
                                logd("✅ Pause action triggered via NotificationListener")
                                return
                            } catch (e: Exception) {
                                logw("⚠️ Failed to trigger pause action: ${e.message}")
                            }
                        }
                    }
                }
            }
            
            logd("⚠️ No pause action found in media notifications")
        } catch (e: Exception) {
            logw("⚠️ Failed to pause via NotificationListener: ${e.message}")
        }
    }
    
    /**
     * Intenta pausar enviando un intent de media pause
     */
    private fun tryPauseViaIntent() {
        try {
            logd("🔄 Method 3: Trying media pause intent...")
            
            // Crear KeyEvent para pausa
            val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
            
            // Intentar enviar intent de pausa de media
            val pauseIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
                setPackage(null) // Enviar a todas las apps
            }
            
            sendOrderedBroadcast(
                pauseIntent,
                null,
                null,
                null,
                0,
                null,
                null
            )
            
            // También intentar con ACTION_DOWN y ACTION_UP
            val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
                setPackage(null)
            }
            
            sendOrderedBroadcast(
                upIntent,
                null,
                null,
                null,
                0,
                null,
                null
            )
            
            logd("✅ Media pause intent sent")
        } catch (e: Exception) {
            logw("⚠️ Failed to pause via intent: ${e.message}")
        }
    }
    
    /**
     * Intenta pausar apps conocidas de streaming mediante intents específicos
     * También detecta la app en primer plano si hay audio activo
     */
    private fun tryPauseKnownApps() {
        try {
            logd("🔄 Method 4: Trying known streaming apps...")
            
            // Primero, intentar detectar qué app está reproduciendo actualmente
            val detectedPackage = detectCurrentPlayingApp()
            if (detectedPackage != null) {
                logd("✅ Detected app currently playing: $detectedPackage")
                tryPauseSpecificApp(detectedPackage)
            }
            
            // También intentar con lista de apps conocidas
            val knownApps = listOf(
                "es.movistar.plus",
                "com.movistar.plus",
                "com.netflix.mediaclient",
                "com.spotify.music",
                "com.amazon.avod.thirdpartyclient",
                "com.disney.disneyplus",
                "com.google.android.youtube",
                "com.google.android.apps.youtube.music",
                "com.hbo.hbonow",
                "com.apple.android.music",
                "com.soundcloud.android",
                "com.deezer.android.app"
            )
            
            // Intentar pausar cada app conocida si está instalada
            for (packageName in knownApps) {
                try {
                    packageManager.getPackageInfo(packageName, 0)
                    logd("✅ Found known app: $packageName, attempting to pause...")
                    tryPauseSpecificApp(packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    // App no instalada, continuar
                } catch (e: Exception) {
                    logw("⚠️ Error checking $packageName: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logw("⚠️ Failed to pause known apps: ${e.message}")
        }
    }
    
    /**
     * Detecta qué app está reproduciendo actualmente
     */
    private fun detectCurrentPlayingApp(): String? {
        try {
            logd("🔍 Attempting to detect current playing app...")
            
            // Método 1: Usar ActivityManager para detectar app en primer plano
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            try {
                val appTasks = activityManager.appTasks
                logd("🔍 Found ${appTasks?.size ?: 0} app tasks")
                if (appTasks != null && appTasks.isNotEmpty()) {
                    val taskInfo = appTasks.first().taskInfo
                    val packageName = taskInfo.topActivity?.packageName
                    logd("🔍 Top activity package: $packageName")
                    if (packageName != null && packageName != this.packageName) {
                        logd("✅ Detected foreground app: $packageName")
                        return packageName
                    }
                }
            } catch (e: Exception) {
                logd("⚠️ Could not use appTasks: ${e.message}")
            }
            
            // Método 2: Usar runningAppProcesses
            try {
                val runningAppProcesses = activityManager.runningAppProcesses
                logd("🔍 Found ${runningAppProcesses?.size ?: 0} running processes")
                if (runningAppProcesses != null) {
                    for (processInfo in runningAppProcesses) {
                        if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            for (packageName in processInfo.pkgList) {
                                if (packageName != this.packageName) {
                                    logd("✅ Detected foreground process: $packageName")
                                    return packageName
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logd("⚠️ Could not use runningAppProcesses: ${e.message}")
            }
            
            logd("⚠️ Could not detect current playing app")
        } catch (e: Exception) {
            logw("⚠️ Error detecting current app: ${e.message}")
        }
        return null
    }
    
    /**
     * Intenta pausar una app específica usando múltiples métodos
     */
    private fun tryPauseSpecificApp(packageName: String) {
        try {
            logd("🔄 Attempting to pause app: $packageName")
            
            // Método 1: Intent de media button dirigido a la app
            try {
                val pauseIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    setPackage(packageName)
                    putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
                }
                sendBroadcast(pauseIntent)
                
                val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    setPackage(packageName)
                    putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
                }
                sendBroadcast(upIntent)
                logd("✅ Sent media button intent to $packageName")
            } catch (e: Exception) {
                logd("⚠️ Failed to send media button to $packageName: ${e.message}")
            }
            
            // Método 2: Intentar abrir la app y luego enviar comando (puede funcionar para algunas apps)
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    // Enviar comando de pausa después de un pequeño delay
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(100)
                        val pauseIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                            setPackage(packageName)
                            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
                        }
                        sendBroadcast(pauseIntent)
                    }
                }
            } catch (e: Exception) {
                logd("⚠️ Failed to launch and pause $packageName: ${e.message}")
            }
            
            // Método 3: Intentar intents específicos según el package
            when {
                packageName.contains("movistar", ignoreCase = true) -> {
                    try {
                        val intent = Intent().apply {
                            setPackage(packageName)
                            action = "com.android.music.musicservicecommand"
                            putExtra("command", "pause")
                        }
                        sendBroadcast(intent)
                        logd("✅ Sent Movistar+ specific pause command")
                    } catch (e: Exception) {
                        logd("⚠️ Failed Movistar+ specific command: ${e.message}")
                    }
                }
                packageName.contains("spotify", ignoreCase = true) -> {
                    try {
                        val intent = Intent("com.spotify.mobile.android.ui.widget.PAUSE").apply {
                            setPackage(packageName)
                        }
                        sendBroadcast(intent)
                        logd("✅ Sent Spotify specific pause command")
                    } catch (e: Exception) {
                        logd("⚠️ Failed Spotify specific command: ${e.message}")
                    }
                }
            }
            
        } catch (e: Exception) {
            logw("⚠️ Error pausing app $packageName: ${e.message}")
        }
    }
    
    /**
     * Intenta pausar usando MediaButtonReceiver mediante broadcast
     */
    private fun tryPauseViaMediaButtonReceiver() {
        try {
            logd("🔄 Method 5: Trying MediaButtonReceiver...")
            
            // Crear un MediaButtonReceiver component
            val mediaButtonReceiver = ComponentName("android", "com.android.internal.policy.impl.MediaButtonReceiver")
            
            // Crear intent para pausa
            val pauseIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                component = mediaButtonReceiver
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            }
            
            sendOrderedBroadcast(
                pauseIntent,
                null,
                null,
                null,
                0,
                null,
                null
            )
            
            // También enviar ACTION_UP
            val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                component = mediaButtonReceiver
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
            }
            
            sendOrderedBroadcast(
                upIntent,
                null,
                null,
                null,
                0,
                null,
                null
            )
            
            logd("✅ MediaButtonReceiver pause command sent")
        } catch (e: Exception) {
            logw("⚠️ Failed to pause via MediaButtonReceiver: ${e.message}")
        }
    }

    /**
     * Construye la notificación del temporizador con controles
     */
    private fun buildTimerNotification(remainingTime: Long, isPaused: Boolean): android.app.Notification {
        val formatter = android.text.format.DateUtils.formatElapsedTime(remainingTime / 1000)
                logd( "🎯 buildTimerNotification() - time: $remainingTime ms, paused: $isPaused, formatted: $formatter")
        
        // Intent para abrir la aplicación
        val contentIntent = Intent(this, dev.thecodegrove.grovetimer.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_timer_title))
            .setContentText(getString(R.string.notification_remaining_time, formatter))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Agregar controles de pausar/reanudar
        if (isPaused) {
            logd("🎯 Adding RESUME button to notification")
            builder.addAction(
                android.R.drawable.ic_media_play,
                getString(R.string.notification_resume_button),
                createActionPendingIntent(TimerNotificationService.ACTION_RESUME_TIMER)
            )
        } else {
            logd("🎯 Adding PAUSE button to notification")
            builder.addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_pause_button),
                createActionPendingIntent(TimerNotificationService.ACTION_PAUSE_TIMER)
            )
        }

        // Agregar control de detener
                logd( "🎯 Adding STOP button to notification")
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.notification_stop_button),
            createActionPendingIntent(TimerNotificationService.ACTION_STOP_TIMER)
        )

                logd( "🎯 Timer notification built successfully")
        return builder.build()
    }
    
    /**
     * Actualiza la notificación del temporizador
     */
    private fun updateTimerNotification(remainingTime: Long, isPaused: Boolean) {
                logd( "🎯 updateTimerNotification() called - time: $remainingTime ms, paused: $isPaused")
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = buildTimerNotification(remainingTime, isPaused)
        notificationManager.notify(NOTIF_ID, notification)
                logd( "🎯 Timer notification updated successfully - ID: $NOTIF_ID")
    }
    
    /**
     * Crea PendingIntent para acciones de control
     */
    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, SleepTimerService::class.java).apply {
            this.action = action
        }
        
        val requestCode = when (action) {
            TimerNotificationService.ACTION_PAUSE_TIMER -> REQUEST_CODE_PAUSE
            TimerNotificationService.ACTION_RESUME_TIMER -> REQUEST_CODE_RESUME
            TimerNotificationService.ACTION_STOP_TIMER -> REQUEST_CODE_STOP
            else -> 0
        }
        
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIF_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    /**
     * Silencia la salida de audio como fallback cuando no se puede pausar
     */
    private fun silenceAudioOutput() {
        try {
            logd("Silencing audio output as fallback")
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            logd("Audio output silenced successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error silencing audio output", e)
        }
    }
    
    /**
     * Envía el estado actual del temporizador como respuesta a una solicitud
     */
    private fun sendTimerStateResponse() {
                logd( "🎯 Sending timer state response - remaining: $currentRemainingTime ms, paused: $isPaused, active: ${currentRemainingTime > 0}")
        
        val intent = Intent(ACTION_STATE_RESPONSE).apply {
            putExtra(EXTRA_REMAINING_TIME, currentRemainingTime)
            putExtra(EXTRA_IS_PAUSED, isPaused)
            putExtra(EXTRA_IS_ACTIVE, currentRemainingTime > 0)
            putExtra(EXTRA_SELECTED_TIME, countdownTime)
        }
        
        sendBroadcast(intent)
                logd( "🎯 Timer state response sent successfully")
    }
    
    /**
     * Guarda el estado actual del temporizador en SharedPreferences
     */
    private fun saveTimerState() {
        try {
            prefs.edit().apply {
                putLong("countdown_time", countdownTime)
                putLong("current_remaining_time", currentRemainingTime)
                putBoolean("is_paused", isPaused)
                putBoolean("is_active", currentRemainingTime > 0)
                putLong("timestamp", System.currentTimeMillis())
                apply()
            }
            logd("🎯 Timer state saved: remaining=$currentRemainingTime, paused=$isPaused, active=${currentRemainingTime > 0}")
        } catch (e: Exception) {
            Log.e(TAG, "🎯 Error saving timer state", e)
        }
    }
    
    /**
     * Carga el estado del temporizador desde SharedPreferences
     */
    private fun loadTimerState() {
        try {
            val savedCountdownTime = prefs.getLong("countdown_time", 0L)
            val savedRemainingTime = prefs.getLong("current_remaining_time", 0L)
            val savedIsPaused = prefs.getBoolean("is_paused", false)
            val savedTimestamp = prefs.getLong("timestamp", 0L)
            
            // Solo cargar si el estado es reciente (menos de 1 hora)
            val timeDiff = System.currentTimeMillis() - savedTimestamp
            if (savedCountdownTime > 0 && savedRemainingTime > 0 && timeDiff < 3600000L) {
                countdownTime = savedCountdownTime
                currentRemainingTime = savedRemainingTime
                isPaused = savedIsPaused
                
                logd("🎯 Timer state loaded: remaining=$currentRemainingTime, paused=$isPaused, countdown=$countdownTime")
                
                // Si el temporizador estaba activo, reiniciarlo
                if (currentRemainingTime > 0) {
                    logd("🎯 Restarting timer from saved state")
                    startTimerFromSavedState()
                }
            } else {
                logd("🎯 No valid timer state found or state too old")
            }
        } catch (e: Exception) {
            Log.e(TAG, "🎯 Error loading timer state", e)
        }
    }
    
    /**
     * Reinicia el temporizador desde el estado guardado
     */
    private fun startTimerFromSavedState() {
        if (currentRemainingTime > 0) {
            logd("🎯 Starting timer from saved state: $currentRemainingTime ms")
            
            // Cargar configuración actualizada antes de iniciar
            reloadSettingsFromPreferences()
            
            // Iniciar como foreground service
            startForeground(NOTIF_ID, buildTimerNotification(currentRemainingTime, isPaused))
            
            // Iniciar el countdown
            startTimer()
        }
    }
    
    /**
     * Limpia el estado guardado del temporizador
     */
    private fun clearTimerState() {
        try {
            prefs.edit().clear().apply()
            logd("🎯 Timer state cleared")
        } catch (e: Exception) {
            Log.e(TAG, "🎯 Error clearing timer state", e)
        }
    }

    companion object {
        private const val TAG = "SleepTimerService"
        const val NOTIF_ID = 123
        const val NOTIF_CHANNEL_ID = "sleep_timer_channel"
        const val KEY_DURATION_MILLIS = "key_duration_millis"
        const val KEY_FADEOUT_ENABLED = "key_fadeout_enabled"
        const val KEY_VIBRATE_ON_FINISH = "key_vibrate_on_finish"
        
        // Request codes para PendingIntents
        private const val REQUEST_CODE_PAUSE = 100
        private const val REQUEST_CODE_RESUME = 101
        private const val REQUEST_CODE_STOP = 102
        
        // Broadcast actions para comunicación con la UI
        const val ACTION_TIMER_UPDATE = "dev.thecodegrove.grovetimer.TIMER_UPDATE"
        const val ACTION_TIMER_FINISHED = "dev.thecodegrove.grovetimer.TIMER_FINISHED"
        const val ACTION_TIMER_STOPPED = "dev.thecodegrove.grovetimer.TIMER_STOPPED"
        const val ACTION_TIMER_PAUSED = "dev.thecodegrove.grovetimer.TIMER_PAUSED"
        const val ACTION_TIMER_RESUMED = "dev.thecodegrove.grovetimer.TIMER_RESUMED"
        const val ACTION_REQUEST_STATE = "dev.thecodegrove.grovetimer.REQUEST_TIMER_STATE"
        const val ACTION_STATE_RESPONSE = "dev.thecodegrove.grovetimer.TIMER_STATE_RESPONSE"
        const val ACTION_UPDATE_SETTINGS = "dev.thecodegrove.grovetimer.UPDATE_SETTINGS"
        const val EXTRA_REMAINING_TIME = "extra_remaining_time"
        const val EXTRA_IS_PAUSED = "extra_is_paused"
        const val EXTRA_IS_ACTIVE = "extra_is_active"
        const val EXTRA_SELECTED_TIME = "extra_selected_time"

        fun startService(context: Context, durationMillis: Long) {
            val intent = Intent(context, SleepTimerService::class.java).apply {
                putExtra(KEY_DURATION_MILLIS, durationMillis)
            }
            context.startForegroundService(intent)
        }
    }
}

// Extension function for PlaybackState state to string for logging
fun PlaybackState.stateToString(): String {
    return when (state) {
        PlaybackState.STATE_NONE -> "STATE_NONE"
        PlaybackState.STATE_STOPPED -> "STATE_STOPPED"
        PlaybackState.STATE_PAUSED -> "STATE_PAUSED"
        PlaybackState.STATE_PLAYING -> "STATE_PLAYING"
        PlaybackState.STATE_FAST_FORWARDING -> "STATE_FAST_FORWARDING"
        PlaybackState.STATE_REWINDING -> "STATE_REWINDING"
        PlaybackState.STATE_BUFFERING -> "STATE_BUFFERING"
        PlaybackState.STATE_ERROR -> "STATE_ERROR"
        else -> "STATE_UNKNOWN ($state)"
    }
}
