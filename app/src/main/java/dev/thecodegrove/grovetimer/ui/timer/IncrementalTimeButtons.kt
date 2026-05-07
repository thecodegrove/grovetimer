package dev.thecodegrove.grovetimer.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.thecodegrove.grovetimer.R
import dev.thecodegrove.grovetimer.ui.common.MaterialIncrementButton

/**
 * Componente que muestra botones incrementales para sumar tiempo al timer actual
 * 
 * Los botones permiten incrementar el tiempo en valores específicos:
 * - +5 minutos
 * - +10 minutos  
 * - +30 minutos
 * - +1 hora (60 minutos)
 * 
 * Cada botón suma su valor al tiempo actual del slider, respetando los límites
 * de 0-240 minutos establecidos por el slider.
 * 
 * @param onIncrement Callback que se ejecuta cuando se presiona un botón de incremento
 * @param modifier Modificador de Compose para personalizar el layout
 */
@Composable
fun IncrementalTimeButtons(
    onIncrement: (incrementMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón +5 minutos
        MaterialIncrementButton(
            text = stringResource(R.string.increment_5_minutes),
            onClick = { onIncrement(5) },
            modifier = Modifier.weight(1f)
        )
        
        // Botón +10 minutos
        MaterialIncrementButton(
            text = stringResource(R.string.increment_10_minutes),
            onClick = { onIncrement(10) },
            modifier = Modifier.weight(1f)
        )
        
        // Botón +30 minutos
        MaterialIncrementButton(
            text = stringResource(R.string.increment_30_minutes),
            onClick = { onIncrement(30) },
            modifier = Modifier.weight(1f)
        )
        
        // Botón +1 hora (60 minutos)
        MaterialIncrementButton(
            text = stringResource(R.string.increment_1_hour),
            onClick = { onIncrement(60) },
            modifier = Modifier.weight(1f)
        )
    }
}
