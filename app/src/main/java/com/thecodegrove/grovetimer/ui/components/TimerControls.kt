package com.thecodegrove.grovetimer.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thecodegrove.grovetimer.R
import com.thecodegrove.grovetimer.ui.common.MaterialButton
import com.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Componente de controles de timer dinámicos
 * Muestra botón de iniciar cuando está inactivo o detener cuando está activo
 */
@Composable
fun TimerControls(
    isActive: Boolean,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    MaterialButton(
        text = if (isActive) stringResource(R.string.timer_stop_button) else stringResource(R.string.start_timer_button),
        onClick = if (isActive) onStopTimer else onStartTimer,
        modifier = modifier.padding(horizontal = 20.dp),
        containerColor = if (isActive) {
            MaterialTheme.groveColors.warningOrange
        } else {
            MaterialTheme.groveColors.progressActive
        }
    )
}

@Preview(showBackground = true)
@Composable
fun TimerControlsPreview() {
    TimerControls(
        isActive = false,
        onStartTimer = { /* Preview only */ },
        onStopTimer = { /* Preview only */ }
    )
}
