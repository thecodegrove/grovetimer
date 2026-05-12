package com.thecodegrove.grovetimer.services

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import com.thecodegrove.grovetimer.utils.DebugUtils
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Servicio que escucha las notificaciones. Es necesario para que MediaSessionManager 
 * nos devuelva las sesiones activas, y también puede detectar reproducción de medios
 * mediante notificaciones de media.
 *
 * Recuerda que el usuario debe habilitar el acceso a notificaciones para la app en los ajustes del sistema.
 */
class MyNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "MyNotificationListener"
        
        /** Devuelve el ComponentName de este servicio, para pasarlo a MediaSessionManager */
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, MyNotificationListener::class.java)
        }
        
        /**
         * Obtiene una instancia activa del NotificationListener para consultar notificaciones
         * Nota: Esto requiere que el servicio esté activo y habilitado
         */
        @Volatile
        private var instance: MyNotificationListener? = null
        
        fun setInstance(listener: MyNotificationListener?) {
            instance = listener
        }
        
        fun getInstance(): MyNotificationListener? = instance
    }
    
    // Helper function for conditional debug logging
    private fun logd(message: String) {
        if (DebugUtils.isDebug(this)) {
            Log.d(TAG, message)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        logd("NotificationListener connected")
        setInstance(this)
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        logd("NotificationListener disconnected")
        setInstance(null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // No es necesario hacer nada aquí para esta prueba de concepto.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No es necesario hacer nada aquí para esta prueba de concepto.
    }
    
    /**
     * Obtiene notificaciones activas de media que puedan indicar reproducción
     * 
     * @return Lista de paquetes de apps que tienen notificaciones de media activas
     */
    fun getActiveMediaNotifications(): List<String> {
        return try {
            val activeNotifications = activeNotifications
            val mediaPackages = mutableListOf<String>()
            
            for (notification in activeNotifications) {
                val packageName = notification.packageName
                val notificationObj = notification.notification
                
                // Detectar notificaciones de media:
                // 1. Tienen acciones de media (play/pause)
                // 2. Usan MediaStyle o BigMediaStyle
                // 3. Tienen categoría CATEGORY_TRANSPORT
                val hasMediaActions = notificationObj.actions?.any { action ->
                    action.title.toString().lowercase().contains("play") ||
                    action.title.toString().lowercase().contains("pause") ||
                    action.title.toString().lowercase().contains("reproducir") ||
                    action.title.toString().lowercase().contains("pausar")
                } ?: false
                
                val isMediaStyle = notificationObj.extras?.getString("android.template")?.contains("Media") ?: false
                val isTransportCategory = notificationObj.category == android.app.Notification.CATEGORY_TRANSPORT
                
                if (hasMediaActions || isMediaStyle || isTransportCategory) {
                    if (!mediaPackages.contains(packageName)) {
                        mediaPackages.add(packageName)
                        logd("Found media notification from: $packageName")
                    }
                }
            }
            
            mediaPackages
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active media notifications", e)
            emptyList()
        }
    }
    
    /**
     * Obtiene información detallada de una notificación de media activa
     * 
     * @return Triple con (packageName, title, artist) o null si no hay notificaciones de media
     */
    fun getActiveMediaNotificationInfo(): Triple<String, String, String>? {
        return try {
            val activeNotifications = activeNotifications
            
            for (notification in activeNotifications) {
                val packageName = notification.packageName
                val notificationObj = notification.notification
                val extras = notificationObj.extras
                
                // Detectar notificaciones de media
                val hasMediaActions = notificationObj.actions?.any { action ->
                    action.title.toString().lowercase().contains("play") ||
                    action.title.toString().lowercase().contains("pause") ||
                    action.title.toString().lowercase().contains("reproducir") ||
                    action.title.toString().lowercase().contains("pausar")
                } ?: false
                
                val isMediaStyle = extras?.getString("android.template")?.contains("Media") ?: false
                val isTransportCategory = notificationObj.category == android.app.Notification.CATEGORY_TRANSPORT
                
                if (hasMediaActions || isMediaStyle || isTransportCategory) {
                    // Intentar obtener de MediaMetadata primero (más confiable)
                    var title = ""
                    var artist = ""
                    
                    try {
                        val mediaMetadata = extras?.getParcelable<android.media.MediaMetadata>("android.media.metadata")
                        if (mediaMetadata != null) {
                            title = mediaMetadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: ""
                            artist = mediaMetadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                            logd("Extracted from MediaMetadata: $title by $artist")
                        }
                    } catch (e: Exception) {
                        logd("Could not extract MediaMetadata: ${e.message}")
                    }
                    
                    // Si no hay MediaMetadata, intentar de los extras de la notificación
                    if (title.isEmpty()) {
                        title = extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: 
                               extras?.getCharSequence("android.title")?.toString() ?: 
                               extras?.getCharSequence("android.media.metadata.TITLE")?.toString() ?: ""
                    }
                    
                    if (artist.isEmpty()) {
                        artist = extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?:
                                extras?.getCharSequence("android.text")?.toString() ?:
                                extras?.getCharSequence("android.media.metadata.ARTIST")?.toString() ?: ""
                        
                        // También intentar obtener de EXTRA_SUB_TEXT o EXTRA_INFO_TEXT
                        if (artist.isEmpty()) {
                            artist = extras?.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
                        }
                        if (artist.isEmpty()) {
                            artist = extras?.getCharSequence(android.app.Notification.EXTRA_INFO_TEXT)?.toString() ?: ""
                        }
                    }
                    
                    // También intentar obtener de bigText si está disponible
                    if (title.isEmpty() && artist.isEmpty()) {
                        val bigText = extras?.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()
                        if (bigText != null && bigText.isNotEmpty()) {
                            // Intentar parsear título y artista del bigText
                            val lines = bigText.split("\n")
                            if (lines.isNotEmpty()) {
                                title = lines[0]
                                if (lines.size > 1) {
                                    artist = lines[1]
                                }
                            }
                        }
                    }
                    
                    if (title.isNotEmpty() || packageName.isNotEmpty()) {
                        logd("Found media notification info: $packageName - '$title' by '$artist'")
                        return Triple(packageName, title, artist)
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active media notification info", e)
            null
        }
    }
}