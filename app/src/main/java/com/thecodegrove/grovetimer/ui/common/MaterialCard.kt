package com.thecodegrove.grovetimer.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.thecodegrove.grovetimer.R
import com.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Componente reutilizable para tarjetas con elevación Material Design 3
 * Proporciona consistencia visual y mejor jerarquía
 */
@Composable
fun MaterialCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.groveColors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = BorderStroke(1.dp, MaterialTheme.groveColors.borderSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MaterialCardPreview() {
    MaterialCard {
        androidx.compose.material3.Text(
            text = stringResource(R.string.material_card_content),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
