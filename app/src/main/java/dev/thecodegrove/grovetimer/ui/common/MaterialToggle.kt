package dev.thecodegrove.grovetimer.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import dev.thecodegrove.grovetimer.R
import dev.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Componente reutilizable para toggle switches con estilo Material Design 3
 * Proporciona consistencia visual y mejor accesibilidad
 */
@Composable
fun MaterialToggle(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.groveColors.timerDisplay
            )
            
            if (description != null) {
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.groveColors.timerDisplay.copy(alpha = 0.7f)
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.groveColors.accentWarm,
                checkedTrackColor = MaterialTheme.groveColors.progressActive,
                uncheckedThumbColor = MaterialTheme.groveColors.pauseSymbol,
                uncheckedTrackColor = MaterialTheme.groveColors.progressBackground
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MaterialTogglePreview() {
    MaterialToggle(
        title = stringResource(R.string.fadeout_progressive_title),
        description = stringResource(R.string.fadeout_progressive_description),
        checked = true,
        onCheckedChange = { /* Preview only */ }
    )
}

@Preview(showBackground = true)
@Composable
fun MaterialToggleLongTextPreview() {
    MaterialToggle(
        title = stringResource(R.string.advanced_notification_setting_title),
        description = stringResource(R.string.advanced_notification_setting_description),
        checked = false,
        onCheckedChange = { /* Preview only */ }
    )
}
