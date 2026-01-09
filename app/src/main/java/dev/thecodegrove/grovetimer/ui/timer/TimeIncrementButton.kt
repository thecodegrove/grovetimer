package dev.thecodegrove.grovetimer.ui.timer

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Botón individual para incrementar tiempo en el timer
 * 
 * @param incrementMinutes Cantidad de minutos a incrementar
 * @param onClick Callback que se ejecuta cuando se presiona el botón
 * @param modifier Modificador de Compose para personalizar el layout
 */
@Composable
fun TimeIncrementButton(
    incrementMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.groveColors.progressActive,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp,
            hoveredElevation = 8.dp
        )
    ) {
        Text(
            text = when (incrementMinutes) {
                60 -> "+1h"
                else -> "+${incrementMinutes}"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
