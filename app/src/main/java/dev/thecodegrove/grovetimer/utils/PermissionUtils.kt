package dev.thecodegrove.grovetimer.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Utilidad para manejar permisos de GroveTimer
 * Proporciona métodos para verificar y solicitar permisos necesarios
 */
object PermissionUtils {
    
    /**
     * Verifica si el permiso de vibración está concedido
     * @param context Contexto de la aplicación
     * @return true si el permiso está concedido, false en caso contrario
     */
    fun hasVibratePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.VIBRATE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Verifica si el permiso de notificaciones está concedido
     * @param context Contexto de la aplicación
     * @return true si el permiso está concedido, false en caso contrario
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Verifica si el permiso de control de medios está concedido
     * @param context Contexto de la aplicación
     * @return true si el permiso está concedido, false en caso contrario
     */
    fun hasMediaControlPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.MEDIA_CONTENT_CONTROL
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Verifica si todos los permisos necesarios están concedidos
     * Nota: MEDIA_CONTENT_CONTROL no se considera requerido porque no se puede solicitar
     * en tiempo de ejecución. La app puede funcionar sin él usando métodos alternativos.
     * @param context Contexto de la aplicación
     * @return true si todos los permisos solicitables están concedidos, false en caso contrario
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        // Solo verificamos permisos que se pueden solicitar en tiempo de ejecución
        return hasVibratePermission(context) &&
                hasNotificationPermission(context)
        // MEDIA_CONTENT_CONTROL se omite porque es un permiso de nivel "signature"
        // que se otorga automáticamente o requiere firma del fabricante
    }
    
    /**
     * Obtiene la lista de permisos que faltan
     * @param context Contexto de la aplicación
     * @return Lista de permisos que no están concedidos
     */
    fun getMissingPermissions(context: Context): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        if (!hasVibratePermission(context)) {
            missingPermissions.add(Manifest.permission.VIBRATE)
        }
        
        if (!hasNotificationPermission(context)) {
            missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Nota: MEDIA_CONTENT_CONTROL es un permiso de nivel "signature" o "privileged"
        // que no se puede solicitar en tiempo de ejecución. Se otorga automáticamente
        // por el sistema o requiere firma del fabricante. No lo incluimos en la lista
        // de permisos solicitables.
        
        return missingPermissions
    }
    
    /**
     * Obtiene la lista de permisos que faltan y que se pueden solicitar en tiempo de ejecución
     * Excluye permisos de nivel "signature" o "privileged" que no se pueden solicitar
     * @param context Contexto de la aplicación
     * @return Lista de permisos solicitables que no están concedidos
     */
    fun getRequestableMissingPermissions(context: Context): List<String> {
        return getMissingPermissions(context)
    }
    
    /**
     * Verifica si faltan permisos críticos que se pueden solicitar
     * MEDIA_CONTENT_CONTROL no se considera crítico porque no se puede solicitar
     * y la app puede funcionar sin él usando métodos alternativos
     * @param context Contexto de la aplicación
     * @return true si faltan permisos críticos solicitables
     */
    fun hasCriticalMissingPermissions(context: Context): Boolean {
        return getRequestableMissingPermissions(context).isNotEmpty()
    }
}
