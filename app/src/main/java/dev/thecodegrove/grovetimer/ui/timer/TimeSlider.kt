package dev.thecodegrove.grovetimer.ui.timer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.thecodegrove.grovetimer.R
import dev.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Componente TimeSlider que permite seleccionar tiempo entre 0-240 minutos
 * con un slider visual y un label que muestra el valor seleccionado
 * 
 * @param value Valor actual del slider en minutos (0-240)
 * @param onValueChange Callback cuando el valor del slider cambia
 * @param modifier Modifier para personalizar el layout
 */
@Composable
fun TimeSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Label que muestra el valor seleccionado
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.time_slider_selected_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.time_slider_minutes_format, value),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.groveColors.timerDisplay
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Slider para seleccionar el tiempo
        Slider(
            value = value.toFloat(),
            onValueChange = { newValue ->
                onValueChange(newValue.toInt())
            },
            valueRange = 0f..240f,
            steps = 239, // 240 valores posibles (0-240)
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.groveColors.progressActive,
                activeTrackColor = MaterialTheme.groveColors.progressActive,
                inactiveTrackColor = MaterialTheme.groveColors.progressBackground
            )
        )
        
        // Labels para mostrar el rango del slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.time_slider_min_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.time_slider_max_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
