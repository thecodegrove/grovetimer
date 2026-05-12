package com.thecodegrove.grovetimer.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.thecodegrove.grovetimer.R
import com.thecodegrove.grovetimer.ui.common.MaterialCard
import com.thecodegrove.grovetimer.ui.common.MaterialToggle
import com.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Componente de tarjeta de configuraciones
 * Muestra opciones de fadeout progresivo y vibrar al finalizar
 */
@Composable
fun SettingsCard(
    fadeoutEnabled: Boolean,
    onFadeoutChanged: (Boolean) -> Unit,
    vibrateOnFinish: Boolean,
    onVibrateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    MaterialCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.timer_options_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.groveColors.timerDisplay
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Opción de fadeout
        MaterialToggle(
            title = stringResource(R.string.fadeout_progressive_title),
            description = stringResource(R.string.fadeout_progressive_description),
            checked = fadeoutEnabled,
            onCheckedChange = onFadeoutChanged
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Opción de vibrar
        MaterialToggle(
            title = stringResource(R.string.vibrate_on_finish_title),
            description = stringResource(R.string.vibrate_on_finish_description),
            checked = vibrateOnFinish,
            onCheckedChange = onVibrateChanged
        )
    }
}


@Preview(showBackground = true)
@Composable
fun SettingsCardPreview() {
    SettingsCard(
        fadeoutEnabled = false,
        onFadeoutChanged = { /* Preview only */ },
        vibrateOnFinish = false,
        onVibrateChanged = { /* Preview only */ }
    )
}
