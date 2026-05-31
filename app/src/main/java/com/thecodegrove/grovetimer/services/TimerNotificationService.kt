package com.thecodegrove.grovetimer.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.thecodegrove.grovetimer.MainActivity
import com.thecodegrove.grovetimer.utils.DebugUtils
import com.thecodegrove.grovetimer.R

/**
 * Servicio dedicado para manejar notificaciones del temporizador.
 * Proporciona controles interactivos para la notificación del temporizador.
 */
class TimerNotificationService : Service() {

    // Mantener referencia al último intent recibido para obtener estado actual
    private var lastReceivedIntent: Intent? = null
    
    // Helper function for conditional debug logging
    private fun logd(message: String) {
        if (DebugUtils.isDebug(this)) {
            Log.d(TAG, message)
        }
    }

    override fun onCreate() {
        super.onCreate()
        logd("TimerNotificationService onCreate")
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logd("🔔 TimerNotificationService onStartCommand with action: ${intent?.action}")
        
        // Guardar referencia al intent para obtener estado actual
        if (intent != null) {
            lastReceivedIntent = intent
        }
        
        when (intent?.action) {
            ACTION_PAUSE_TIMER -> {
                logd("🔔 Pause timer requested from notification")
                sendControlBroadcast(ACTION_PAUSE_TIMER)
                // NO actualizar aquí - esperar a que SleepTimerService envíe el estado pausado
                logd("🔔 Waiting for SleepTimerService to send paused state...")
            }
            ACTION_RESUME_TIMER -> {
                logd("🔔 Resume timer requested from notification")
                sendControlBroadcast(ACTION_RESUME_TIMER)
                // NO actualizar aquí - esperar a que SleepTimerService envíe el estado reanudado
                logd("🔔 Waiting for SleepTimerService to send resumed state...")
            }
            ACTION_STOP_TIMER -> {
                logd("🔔 Stop timer requested from notification")
                sendControlBroadcast(ACTION_STOP_TIMER)
                // No actualizar notificación aquí ya que se detendrá el servicio
            }
            ACTION_UPDATE_NOTIFICATION -> {
                logd("🔔 Updating notification - remainingTime: ${intent.getLongExtra(EXTRA_REMAINING_TIME, 0L)} ms, isPaused: ${intent.getBooleanExtra(EXTRA_IS_PAUSED, false)}")
                updateNotificationDisplay()
                logd("🔔 Notification updated successfully")
            }
            ACTION_START_NOTIFICATION -> {
                // NO ejecutar como foreground service, solo iniciar el servicio
                logd("Started as background service with initial time: ${intent.getLongExtra(EXTRA_REMAINING_TIME, 0L)} ms")
            }
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        logd("TimerNotificationService onDestroy")
        super.onDestroy()
    }

    /**
     * Envía un broadcast para comunicar acciones de control al SleepTimerService
     */
    private fun sendControlBroadcast(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
        logd("🔔 Sent control broadcast: $action")
    }

    /**
     * Actualiza la notificación con el tiempo restante actual
     */
    private fun updateNotificationDisplay() {
        val currentTime = getCurrentRemainingTime()
        val currentPausedState = getCurrentPausedState()
        logd("🔔 updateNotificationDisplay() called - time: $currentTime ms, paused: $currentPausedState")
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = buildNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
        logd("🔔 Notification updated successfully - ID: $NOTIFICATION_ID")
    }
    
    /**
     * Obtiene el tiempo restante actual desde el último intent recibido
     */
    private fun getCurrentRemainingTime(): Long {
        // Buscar en el intent más reciente o usar valor por defecto
        return lastReceivedIntent?.getLongExtra(EXTRA_REMAINING_TIME, 0L) ?: 0L
    }
    
    /**
     * Obtiene el estado de pausa actual desde el último intent recibido
     */
    private fun getCurrentPausedState(): Boolean {
        // Buscar en el intent más reciente o usar valor por defecto
        return lastReceivedIntent?.getBooleanExtra(EXTRA_IS_PAUSED, false) ?: false
    }

    /**
     * Construye la notificación con controles interactivos
     */
    private fun buildNotification(): android.app.Notification {
        // Obtener tiempo actual desde el intent más reciente o usar valores por defecto
        val currentTime = getCurrentRemainingTime()
        val currentPausedState = getCurrentPausedState()
        
        val formatter = android.text.format.DateUtils.formatElapsedTime(currentTime / 1000)
        logd("🔔 buildNotification() - time: $currentTime ms, paused: $currentPausedState, formatted: $formatter")
        
        // Intent para abrir la aplicación
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_timer_title))
            .setContentText(getString(R.string.notification_remaining_time, formatter))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Agregar controles de pausar/reanudar
        if (currentPausedState) {
            logd("🔔 Adding RESUME button to notification")
            builder.addAction(
                android.R.drawable.ic_media_play,
                getString(R.string.notification_resume_button),
                createActionPendingIntent(ACTION_RESUME_TIMER)
            )
        } else {
            logd("🔔 Adding PAUSE button to notification")
            builder.addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_pause_button),
                createActionPendingIntent(ACTION_PAUSE_TIMER)
            )
        }

        // Agregar control de detener
        logd("🔔 Adding STOP button to notification")
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.notification_stop_button),
            createActionPendingIntent(ACTION_STOP_TIMER)
        )

        logd("🔔 Notification built successfully")
        return builder.build()
    }

    /**
     * Crea PendingIntent para acciones de control
     */
    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, TimerNotificationService::class.java).apply {
            this.action = action
        }
        
        val requestCode = when (action) {
            ACTION_PAUSE_TIMER -> REQUEST_CODE_PAUSE
            ACTION_RESUME_TIMER -> REQUEST_CODE_RESUME
            ACTION_STOP_TIMER -> REQUEST_CODE_STOP
            else -> 0
        }
        
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Crea el canal de notificaciones requerido para Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            logd("Notification channel created")
        }
    }

    companion object {
        private const val TAG = "TimerNotificationService"
        
        // Notification
        const val NOTIFICATION_ID = 124
        const val NOTIFICATION_CHANNEL_ID = "timer_notification_channel"
        
        // Actions para controles
        const val ACTION_PAUSE_TIMER = "com.thecodegrove.grovetimer.ACTION_PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "com.thecodegrove.grovetimer.ACTION_RESUME_TIMER"
        const val ACTION_STOP_TIMER = "com.thecodegrove.grovetimer.ACTION_STOP_TIMER"
        const val ACTION_UPDATE_NOTIFICATION = "com.thecodegrove.grovetimer.ACTION_UPDATE_NOTIFICATION"
        const val ACTION_START_NOTIFICATION = "com.thecodegrove.grovetimer.ACTION_START_NOTIFICATION"
        
        // Extras para datos
        const val EXTRA_REMAINING_TIME = "extra_remaining_time"
        const val EXTRA_IS_PAUSED = "extra_is_paused"
        
        // Request codes para PendingIntents
        private const val REQUEST_CODE_PAUSE = 100
        private const val REQUEST_CODE_RESUME = 101
        private const val REQUEST_CODE_STOP = 102
        
        /**
         * Inicia el servicio de notificaciones
         */
        fun startNotificationService(context: android.content.Context, initialTime: Long) {
            val intent = Intent(context, TimerNotificationService::class.java).apply {
                action = ACTION_START_NOTIFICATION
                putExtra(EXTRA_REMAINING_TIME, initialTime)
            }
            context.startService(intent)
        }
        
        /**
         * Actualiza la notificación con nuevo tiempo y estado
         */
        fun updateNotification(context: android.content.Context, remainingTime: Long, isPaused: Boolean) {
            val intent = Intent(context, TimerNotificationService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
                putExtra(EXTRA_REMAINING_TIME, remainingTime)
                putExtra(EXTRA_IS_PAUSED, isPaused)
            }
            context.startService(intent)
        }
        
        /**
         * Detiene el servicio de notificaciones
         */
        fun stopNotificationService(context: android.content.Context) {
            val intent = Intent(context, TimerNotificationService::class.java)
            context.stopService(intent)
        }
    }
}
