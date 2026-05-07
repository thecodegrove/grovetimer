package dev.thecodegrove.grovetimer.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Componente que muestra botones de tiempos predefinidos para agilizar la configuración del timer
 * 
 * @param onPresetSelected Callback que se ejecuta cuando se selecciona un tiempo predefinido
 * @param modifier Modificador de Compose para personalizar el layout
 */
@Composable
fun PresetTimerButtons(
    onPresetSelected: (timeMillis: Long, timeFormatted: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón 5 minutos
        Button(
            onClick = {
                val timeMillis = 5 * 60 * 1000L // 5 minutos en milisegundos
                onPresetSelected(timeMillis, "5m")
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.groveColors.progressActive,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "5m",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Botón 10 minutos
        Button(
            onClick = {
                val timeMillis = 10 * 60 * 1000L // 10 minutos en milisegundos
                onPresetSelected(timeMillis, "10m")
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.groveColors.progressActive,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "10m",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Botón 15 minutos
        Button(
            onClick = {
                val timeMillis = 15 * 60 * 1000L // 15 minutos en milisegundos
                onPresetSelected(timeMillis, "15m")
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.groveColors.progressActive,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "15m",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Botón 30 minutos
        Button(
            onClick = {
                val timeMillis = 30 * 60 * 1000L // 30 minutos en milisegundos
                onPresetSelected(timeMillis, "30m")
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.groveColors.progressActive,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "30m",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Botón 1 hora
        Button(
            onClick = {
                val timeMillis = 60 * 60 * 1000L // 1 hora en milisegundos
                onPresetSelected(timeMillis, "1h")
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.groveColors.progressActive,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "1h",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
