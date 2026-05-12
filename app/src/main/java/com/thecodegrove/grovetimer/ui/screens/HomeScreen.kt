package com.thecodegrove.grovetimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.thecodegrove.grovetimer.R
import com.thecodegrove.grovetimer.utils.DebugUtils
import com.thecodegrove.grovetimer.ui.common.MaterialIconButton
import com.thecodegrove.grovetimer.ui.components.CircularTimer
import com.thecodegrove.grovetimer.ui.components.MediaInfoCard
import com.thecodegrove.grovetimer.ui.components.TimerControls
import com.thecodegrove.grovetimer.ui.theme.BrandTitleTextStyle
import com.thecodegrove.grovetimer.ui.theme.groveColors
import com.thecodegrove.grovetimer.ui.timer.IncrementalTimeButtons
import com.thecodegrove.grovetimer.ui.timer.TimerViewModel

/**
 * Pantalla principal de GroveTimer con estado dinámico
 * Cambia entre estado inactivo y activo según el timer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    // Estado del timer desde ViewModel - usando collectAsState() en lugar de collectAsStateWithLifecycle()
    val timerState by viewModel.uiState.collectAsState()
    val isTimerActive = timerState.isActive
    val timeRemaining = if (isTimerActive) timerState.remainingTimeMillis else timerState.selectedTimeMillis
    
    // Estado separado de media info para forzar recomposición
    val mediaInfo by viewModel.mediaInfoState.collectAsState()
    
    // Inicializar BroadcastReceiver y actualizar media info inmediatamente
    val context = LocalContext.current
    
    // Logging para debugging de UI
    if (DebugUtils.isDebug(context)) {
        android.util.Log.d("HomeScreen", "UI State updated - isActive: $isTimerActive, mediaInfo: $mediaInfo")
    }
    
    // Forzar recomposición cuando cambie mediaInfo usando LaunchedEffect
    LaunchedEffect(mediaInfo) {
        if (DebugUtils.isDebug(context)) {
            android.util.Log.d("HomeScreen", "LaunchedEffect triggered - mediaInfo changed: $mediaInfo")
        }
    }
    DisposableEffect(context) {
        viewModel.initializeBroadcastReceiver(context)
        // Actualizar media info inmediatamente cuando se inicializa HomeScreen
        viewModel.updateMediaInfo()
        onDispose {
            // Cleanup se maneja en el ViewModel
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.groveColors.appBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = BrandTitleTextStyle,
                        color = MaterialTheme.groveColors.timerDisplay
                    )
                },
                actions = {
                    MaterialIconButton(
                        onClick = { navController.navigate("settings") },
                        icon = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_title)
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Timer circular principal
            CircularTimer(
                timeRemaining = timeRemaining,
                totalTime = if (isTimerActive) timerState.selectedTimeMillis else timeRemaining,
                isActive = isTimerActive,
                onTimeChange = { newTime ->
                    if (!isTimerActive) {
                        // Solo actualizar el tiempo seleccionado, no iniciar el timer
                        viewModel.updateSelectedTime(newTime, formatTime(newTime))
                    }
                }
            )
            
            // Botones incrementales (solo visibles cuando timer inactivo)
            if (!isTimerActive) {
                IncrementalTimeButtons(
                    onIncrement = { incrementMinutes ->
                        // Incrementar el tiempo actual del slider
                        viewModel.incrementSelectedTime(timeRemaining, incrementMinutes)
                    }
                )
            }
            
            // Controles de timer
            TimerControls(
                isActive = isTimerActive,
                onStartTimer = { 
                    viewModel.startTimer(timeRemaining, formatTime(timeRemaining))
                },
                onStopTimer = { 
                    viewModel.stopTimer()
                }
            )
            
            // Tarjeta de información de media
            MediaInfoCard(
                appName = if (mediaInfo.hasActiveSession) mediaInfo.appName else null,
                contentTitle = if (mediaInfo.hasActiveSession) mediaInfo.mediaTitle else null
            )
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Formatea el tiempo en milisegundos a formato legible
 */
private fun formatTime(timeMillis: Long): String {
    val minutes = (timeMillis / 60000).toInt()
    val seconds = ((timeMillis % 60000) / 1000).toInt()
    return String.format("%02d:%02d", minutes, seconds)
}
