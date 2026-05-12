package com.thecodegrove.grovetimer.utils

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * Utilidad para verificar si la aplicación está en modo debug
 * Funciona sin depender de BuildConfig, que puede no estar disponible hasta que se compile
 */
object DebugUtils {
    /**
     * Verifica si la aplicación está en modo debug usando ApplicationInfo
     * @param context Contexto de la aplicación
     * @return true si está en modo debug, false en caso contrario
     */
    fun isDebug(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    /**
     * Verifica si la aplicación está en modo debug sin necesidad de Context
     * Nota: Sin Context, no podemos verificar de forma confiable si es debug.
     * Esta función retorna false por defecto (asumiendo producción).
     * Para verificación confiable, use isDebug(context) cuando tenga acceso a Context.
     * @return false por defecto (producción). Use isDebug(context) para verificación confiable.
     */
    fun isDebug(): Boolean {
        // Sin Context, no podemos verificar de forma confiable.
        // Retornar false por defecto (asumiendo producción).
        // Los archivos con Context deberían usar isDebug(context) en su lugar.
        return false
    }
}
