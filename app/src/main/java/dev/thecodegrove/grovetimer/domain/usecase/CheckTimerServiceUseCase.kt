package dev.thecodegrove.grovetimer.domain.usecase

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import dev.thecodegrove.grovetimer.services.SleepTimerService
import dev.thecodegrove.grovetimer.utils.DebugUtils

/**
 * Caso de uso para detectar si SleepTimerService está ejecutándose
 * 
 * Responsabilidades:
 * - Verificar si SleepTimerService está activo en el sistema
 * - Proporcionar información sobre el estado del servicio
 * - Manejar errores de acceso al ActivityManager
 */
class CheckTimerServiceUseCase(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "CheckTimerServiceUseCase"
    }
    
    // Helper functions for conditional debug logging
    private fun logd(message: String) {
        if (DebugUtils.isDebug(context)) {
            Log.d(TAG, message)
        }
    }
    
    /**
     * Ejecuta el caso de uso para verificar si SleepTimerService está ejecutándose
     * 
     * @return true si el servicio está ejecutándose, false en caso contrario
     */
    suspend operator fun invoke(): Boolean {
        return try {
            logd("🔍 CheckTimerServiceUseCase: Starting service detection...")
            
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
            
            val serviceName = SleepTimerService::class.java.name
            val packageName = context.packageName
            
            logd("🔍 Looking for service: $serviceName")
            logd("🔍 In package: $packageName")
            logd("🔍 Total running services found: ${runningServices.size}")
            
            val isRunning = runningServices.any { serviceInfo ->
                val matchesPackage = serviceInfo.service.packageName == packageName
                val matchesService = serviceInfo.service.className == serviceName
                val isActive = serviceInfo.pid > 0
                
                logd("🔍 Service: ${serviceInfo.service.className}, Package: ${serviceInfo.service.packageName}, PID: ${serviceInfo.pid}")
                logd("🔍 Matches package: $matchesPackage, Matches service: $matchesService, Is active: $isActive")
                
                matchesPackage && matchesService && isActive
            }
            
            logd("🔍 FINAL RESULT: SleepTimerService is running: $isRunning")
            isRunning
            
        } catch (e: Exception) {
            Log.e(TAG, "🔍 ERROR: Error checking if SleepTimerService is running", e)
            false
        }
    }
}
