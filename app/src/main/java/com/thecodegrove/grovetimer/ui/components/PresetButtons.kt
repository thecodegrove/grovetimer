package com.thecodegrove.grovetimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Componente de botones de tiempo predefinido
 * Muestra botones circulares para tiempos rápidos (5, 10, 15, 30 minutos)
 */
@Composable
fun PresetButtons(
    onPresetSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetTimes = listOf(5L, 10L, 15L, 30L)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        presetTimes.forEach { minutes ->
            PresetButton(
                minutes = minutes,
                onClick = { onPresetSelected(minutes * 60 * 1000) }
            )
        }
    }
}

@Composable
private fun PresetButton(
    minutes: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(70.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.groveColors.progressActive,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = minutes.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PresetButtonsPreview() {
    PresetButtons(
        onPresetSelected = { /* Preview only */ }
    )
}
