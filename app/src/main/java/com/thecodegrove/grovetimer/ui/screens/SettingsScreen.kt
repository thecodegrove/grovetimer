package com.thecodegrove.grovetimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import com.thecodegrove.grovetimer.R
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import com.thecodegrove.grovetimer.ui.common.MaterialCard
import com.thecodegrove.grovetimer.ui.common.MaterialIconButton
import com.thecodegrove.grovetimer.ui.common.MaterialToggle
import com.thecodegrove.grovetimer.ui.settings.SettingsViewModel
import com.thecodegrove.grovetimer.ui.timer.TimerViewModel
import com.thecodegrove.grovetimer.ui.theme.BrandTitleTextStyle
import com.thecodegrove.grovetimer.ui.theme.MonoValueTextStyle
import com.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Pantalla de configuraciones de GroveTimer
 * Muestra opciones avanzadas y configuraciones del usuario
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    timerViewModel: TimerViewModel?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Conectar con SettingsViewModel para obtener configuraciones persistentes
    val settingsViewModel: SettingsViewModel = viewModel {
        SettingsViewModel(context = context)
    }
    val settingsState by settingsViewModel.settingsState.collectAsState()
    
    // Para el modo oscuro, usar TimerViewModel si está disponible
    val darkModeState by (timerViewModel?.settingsState?.collectAsState(initial = com.thecodegrove.grovetimer.domain.model.UserSettings.DEFAULT) 
        ?: androidx.compose.runtime.mutableStateOf(com.thecodegrove.grovetimer.domain.model.UserSettings.DEFAULT))
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.groveColors.appBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = BrandTitleTextStyle,
                        color = MaterialTheme.groveColors.timerDisplay
                    )
                },
                navigationIcon = {
                    MaterialIconButton(
                        onClick = { navController.navigateUp() },
                        icon = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back_content_description)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.groveColors.appBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sección de Timer
            SettingsSection(
                title = stringResource(R.string.timer_settings_title)
            ) {
                MaterialToggle(
                    title = stringResource(R.string.fadeout_progressive_title),
                    description = stringResource(R.string.fadeout_progressive_description),
                    checked = settingsState.fadeoutEnabled,
                    onCheckedChange = { settingsViewModel.updateFadeoutEnabled(it) }
                )
                
                MaterialToggle(
                    title = stringResource(R.string.vibrate_on_finish_title),
                    description = stringResource(R.string.vibrate_on_finish_description),
                    checked = settingsState.vibrateOnFinish,
                    onCheckedChange = { settingsViewModel.updateVibrateOnFinish(it) }
                )
            }
            
            // Sección de Apariencia
            SettingsSection(
                title = stringResource(R.string.appearance_title)
            ) {
                MaterialToggle(
                    title = stringResource(R.string.dark_mode_title),
                    description = stringResource(R.string.dark_mode_description),
                    checked = darkModeState.darkModeEnabled,
                    onCheckedChange = { timerViewModel?.updateDarkModeEnabled(it) }
                )
            }
            
            // Sección de Información
            SettingsSection(
                title = stringResource(R.string.information_title)
            ) {
                InfoRow(
                    title = stringResource(R.string.version_title),
                    value = getAppVersion(context)
                )
                
                InfoRow(
                    title = stringResource(R.string.developer_title),
                    value = stringResource(R.string.developer_value)
                )
                
                LinkRow(
                    title = stringResource(R.string.about_github_title),
                    onClick = {
                        openUrl(context, "https://github.com/thecodegrove/grovetimer")
                    }
                )
                
                LinkRow(
                    title = stringResource(R.string.about_buymeacoffee_title),
                    onClick = {
                        openUrl(context, "https://buymeacoffee.com/thecodegrove")
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    MaterialCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.groveColors.timerDisplay
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        content()
    }
}


@Composable
private fun InfoRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MonoValueTextStyle,
            color = MaterialTheme.groveColors.mutedText
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun LinkRow(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.groveColors.mutedText,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
}

/**
 * Abre una URL en el navegador del dispositivo
 */
private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        // Si no hay aplicación para abrir la URL, simplemente no hacer nada
        // En producción podrías mostrar un Toast o un mensaje de error
    }
}

/**
 * Obtiene la versión de la aplicación desde el PackageManager
 */
@Composable
private fun getAppVersion(context: android.content.Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "Unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val navController = rememberNavController()
    SettingsScreen(navController = navController, timerViewModel = null)
}
