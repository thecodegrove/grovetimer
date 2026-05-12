package com.thecodegrove.grovetimer.ui.permissions

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.thecodegrove.grovetimer.services.MyNotificationListener
import com.thecodegrove.grovetimer.utils.DebugUtils
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.thecodegrove.grovetimer.R
import com.thecodegrove.grovetimer.ui.theme.GroveTimerTheme
import com.thecodegrove.grovetimer.utils.PermissionUtils

/**
 * Activity para solicitar permisos necesarios para GroveTimer
 * Se muestra cuando faltan permisos críticos para el funcionamiento de la app
 * Incluye verificación y solicitud de NotificationListener
 */
class PermissionRequestActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "PermissionRequestActivity"
    }
    
    // Helper function for conditional debug logging
    private fun logd(message: String) {
        if (DebugUtils.isDebug(this)) {
            Log.d(TAG, message)
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Esperar un momento para que el sistema actualice el estado de los permisos
        // antes de verificar. Usar Handler para ejecutar después de que la actividad
        // vuelva a estar en primer plano.
        Handler(Looper.getMainLooper()).postDelayed({
            checkAllPermissionsAndFinish()
        }, 300) // 300ms de delay para asegurar que los permisos estén actualizados
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Obtener el tema del usuario desde SharedPreferences
        val prefs = getSharedPreferences("grovetimer_settings", Context.MODE_PRIVATE)
        val darkModeEnabled = prefs.getBoolean("dark_mode_enabled", false)
        
        setContent {
            GroveTimerTheme(
                darkTheme = darkModeEnabled,
                followSystemTheme = false
            ) {
                PermissionRequestScreen(
                    onRequestPermissions = { permissions ->
                        if (permissions.isNotEmpty()) {
                            requestPermissionLauncher.launch(permissions.toTypedArray())
                        } else {
                            // Si no hay permisos que solicitar, verificar NotificationListener
                            checkAllPermissionsAndFinish()
                        }
                    },
                    onOpenSettings = {
                        openAppSettings()
                    },
                    onOpenNotificationListenerSettings = {
                        openNotificationListenerSettings()
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Cuando vuelve a la app, verificar si todos los permisos están concedidos
        // Usar delays múltiples para asegurar que el sistema haya actualizado el estado
        // Esto es especialmente importante cuando se vuelve de los settings del sistema
        Handler(Looper.getMainLooper()).postDelayed({
            checkAllPermissionsAndFinish()
        }, 200) // Primer intento después de 200ms
        
        // Verificación adicional después de un delay más largo por si el sistema tarda más
        Handler(Looper.getMainLooper()).postDelayed({
            checkAllPermissionsAndFinish()
        }, 800) // Segundo intento después de 800ms
    }
    
    private fun checkAllPermissionsAndFinish() {
        // Verificar solo si la actividad aún está activa
        if (isFinishing) {
            logd("Activity is finishing, skipping check")
            return
        }
        
        // Usar hasAllRequiredPermissions que excluye MEDIA_CONTENT_CONTROL
        // (permiso de nivel "signature" que no se puede solicitar en tiempo de ejecución)
        val allRequiredPermissionsGranted = PermissionUtils.hasAllRequiredPermissions(this)
        val notificationListenerEnabled = isNotificationListenerEnabled()
        
        val missingPermissions = PermissionUtils.getMissingPermissions(this)
        logd("Checking permissions - Missing: $missingPermissions, AllRequired: $allRequiredPermissionsGranted, NotificationListener: $notificationListenerEnabled")
        
        // Si todos los permisos solicitables están concedidos y el NotificationListener está habilitado,
        // cerrar la actividad y volver a MainActivity
        if (allRequiredPermissionsGranted && notificationListenerEnabled) {
            logd("✅ All required permissions granted, finishing activity")
            // Ejecutar finish() directamente ya que estamos en el hilo principal
            finish()
        } else {
            logd("⚠️ Still missing permissions or NotificationListener not enabled")
            if (!allRequiredPermissionsGranted) {
                logd("Missing requestable permissions: $missingPermissions")
            }
            if (!notificationListenerEnabled) {
                logd("NotificationListener not enabled - user needs to enable it in system settings")
            }
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
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
    
    private fun openNotificationListenerSettings() {
        try {
            logd("Opening NotificationListener settings...")
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            logd("✅ NotificationListener settings opened")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening NotificationListener settings", e)
            // Intentar método alternativo
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, 
                        MyNotificationListener.getComponentName(this@PermissionRequestActivity).flattenToString())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                logd("✅ NotificationListener detail settings opened")
            } catch (e2: Exception) {
                Log.e(TAG, "Error opening NotificationListener detail settings", e2)
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(
    onRequestPermissions: (List<String>) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    var showSettings by remember { mutableStateOf(false) }
    
    // Usar mutableStateOf para que los valores se puedan actualizar
    var missingPermissions by remember { mutableStateOf(PermissionUtils.getMissingPermissions(context)) }
    var notificationListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    
    // Actualizar los valores cuando la composición se active o se recomponga
    // Esto asegura que los valores se actualicen cuando el usuario vuelve de los settings
    LaunchedEffect(Unit) {
        // Actualizar valores inicialmente
        missingPermissions = PermissionUtils.getMissingPermissions(context)
        notificationListenerEnabled = isNotificationListenerEnabled(context)
        
        if (missingPermissions.isNotEmpty()) {
            onRequestPermissions(missingPermissions)
        }
    }
    
    // Actualizar los valores periódicamente para detectar cambios cuando el usuario vuelve de los settings
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500) // Verificar cada 500ms
            val newMissingPermissions = PermissionUtils.getMissingPermissions(context)
            val newNotificationListenerEnabled = isNotificationListenerEnabled(context)
            
            // Solo actualizar si hay cambios para evitar recomposiciones innecesarias
            if (newMissingPermissions != missingPermissions || newNotificationListenerEnabled != notificationListenerEnabled) {
                missingPermissions = newMissingPermissions
                notificationListenerEnabled = newNotificationListenerEnabled
                
                // Si todos los permisos están concedidos y el NotificationListener está habilitado,
                // la actividad se cerrará automáticamente en checkAllPermissionsAndFinish()
            }
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Text(
                text = stringResource(R.string.permissions_required_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.permissions_required_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Lista de permisos
            missingPermissions.forEach { permission ->
                PermissionItem(
                    permission = permission,
                    description = getPermissionDescription(permission)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // NotificationListener (si no está habilitado)
            if (!notificationListenerEnabled) {
                NotificationListenerItem(
                    onOpenSettings = onOpenNotificationListenerSettings
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (showSettings) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.permissions_denied_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.permissions_denied_description),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onOpenSettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.open_settings_button))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Botón para volver a la aplicación
            Button(
                onClick = { 
                    activity?.finish()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(stringResource(R.string.permissions_back_to_app_button))
            }
        }
    }
}

@Composable
private fun PermissionItem(
    permission: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = getPermissionIcon(permission),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getPermissionTitle(permission),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                )
            }
        }
    }
}

@Composable
private fun getPermissionDescription(permission: String): String {
    val context = LocalContext.current
    return when (permission) {
        Manifest.permission.VIBRATE -> context.getString(R.string.permission_vibrate_description)
        Manifest.permission.POST_NOTIFICATIONS -> context.getString(R.string.permission_notifications_description)
        Manifest.permission.MEDIA_CONTENT_CONTROL -> context.getString(R.string.permission_notifications_description) // No se usa, pero por si acaso
        else -> context.getString(R.string.permission_generic_description)
    }
}

@Composable
private fun getPermissionTitle(permission: String): String {
    val context = LocalContext.current
    return when (permission) {
        Manifest.permission.VIBRATE -> context.getString(R.string.permission_vibrate_title)
        Manifest.permission.POST_NOTIFICATIONS -> context.getString(R.string.permission_notifications_title)
        Manifest.permission.MEDIA_CONTENT_CONTROL -> context.getString(R.string.permission_media_control_title) // No se usa, pero por si acaso
        else -> context.getString(R.string.permission_generic_title)
    }
}

@Composable
private fun getPermissionIcon(permission: String) = when (permission) {
    Manifest.permission.VIBRATE -> Icons.Default.Vibration
    Manifest.permission.POST_NOTIFICATIONS -> Icons.Default.Notifications
    Manifest.permission.MEDIA_CONTENT_CONTROL -> Icons.Default.MusicNote
    else -> Icons.Default.Security
}

@Composable
private fun NotificationListenerItem(
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = stringResource(R.string.notification_listener_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.notification_listener_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    stringResource(R.string.notification_listener_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
    val packageName = context.packageName
    val listenerName = ComponentName(context, MyNotificationListener::class.java).flattenToString()
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
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
