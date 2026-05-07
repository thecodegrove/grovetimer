package dev.thecodegrove.grovetimer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import dev.thecodegrove.grovetimer.R
import dev.thecodegrove.grovetimer.ui.theme.groveColors
import dev.thecodegrove.grovetimer.ui.theme.TimerDisplayTextStyle
import dev.thecodegrove.grovetimer.ui.theme.TimerLabelTextStyle
import dev.thecodegrove.grovetimer.ui.timer.MaterialProgressRing

/**
 * Componente de timer circular con progreso animado
 * Muestra el tiempo restante y el progreso del timer
 * 
 * NUEVA IMPLEMENTACIÓN: Usa SmoothCircularSlider para selección precisa
 */
@Composable
fun CircularTimer(
    timeRemaining: Long, // en milisegundos
    totalTime: Long, // en milisegundos
    isActive: Boolean,
    onTimeChange: ((Long) -> Unit)? = null, // Callback para cambiar tiempo
    modifier: Modifier = Modifier
) {
    // Estado local para mostrar el tiempo mientras se arrastra
    var localTimeRemaining by remember { mutableStateOf(timeRemaining) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Actualizar estado local cuando cambia timeRemaining (botones incrementales)
    LaunchedEffect(timeRemaining) {
        localTimeRemaining = timeRemaining
    }
    
    // Actualizar estado local cuando cambian los props
    if (!isActive && !isDragging) {
        localTimeRemaining = timeRemaining
    } else if (isActive) {
        // Cuando el timer está activo, usar los valores reales
        localTimeRemaining = timeRemaining
    }
    
    // Convertir tiempo a minutos para el slider (rango 0-240 minutos)
    val currentMinutes = localTimeRemaining / (60 * 1000f)
    val maxMinutes = 240f
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Usar el nuevo SmoothCircularSlider cuando no está activo y se puede cambiar el tiempo
        if (!isActive && onTimeChange != null) {
            SmoothCircularSlider(
                value = currentMinutes,
                onValueChange = { newMinutes ->
                    isDragging = true
                    val newTimeMillis = (newMinutes * 60 * 1000).toLong()
                    localTimeRemaining = newTimeMillis
                    onTimeChange(newTimeMillis)
                },
                range = 0f..maxMinutes,
                strokeWidth = 30f,
                enabled = true
            )
        } else {
            // Mostrar progreso del timer cuando está activo o no se puede cambiar
            val progress by animateFloatAsState(
                targetValue = if (isActive && totalTime > 0) {
                    // Cuando está activo, mostrar progreso basado en tiempo restante vs tiempo total
                    (timeRemaining.toFloat() / totalTime.toFloat()).coerceIn(0f, 1f)
                } else {
                    // Cuando no está activo, mostrar progreso basado en tiempo seleccionado vs máximo
                    (localTimeRemaining.toFloat() / (maxMinutes * 60 * 1000f)).coerceIn(0f, 1f)
                },
                animationSpec = tween(durationMillis = 1000),
                label = "timer_progress"
            )
            
            // Usar MaterialProgressRing para mejor Material Design
            MaterialProgressRing(
                progress = progress,
                modifier = Modifier.size(220.dp),
                strokeWidth = 10.dp
            )
        }
        
        // Tiempo restante centrado dentro del círculo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val minutes = (localTimeRemaining / 60000).toInt()
            val seconds = ((localTimeRemaining % 60000) / 1000).toInt()
            val timeText = String.format("%02d:%02d", minutes, seconds)
            
            Text(
                text = timeText,
                style = TimerDisplayTextStyle,
                color = MaterialTheme.groveColors.timerDisplay
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Etiqueta del estado
            Text(
                text = if (isActive) stringResource(R.string.timer_remaining_label) else stringResource(R.string.timer_minutes_label),
                style = TimerLabelTextStyle,
                color = if (isActive) {
                    MaterialTheme.groveColors.mutedText
                } else {
                    MaterialTheme.groveColors.mutedText
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CircularTimerPreview() {
    CircularTimer(
        timeRemaining = 300000L, // 5 minutos
        totalTime = 600000L, // 10 minutos
        isActive = true
    )
}

@Preview(showBackground = true)
@Composable
fun CircularTimerEditablePreview() {
    CircularTimer(
        timeRemaining = 300000L, // 5 minutos
        totalTime = 0L, // No hay tiempo total cuando es editable
        isActive = false,
        onTimeChange = { /* Preview no necesita callback */ }
    )
}
