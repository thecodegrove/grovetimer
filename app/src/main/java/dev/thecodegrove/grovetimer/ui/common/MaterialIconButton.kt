package dev.thecodegrove.grovetimer.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Componente reutilizable para botones con iconos Material Design
 * Proporciona tinting consistente y accesibilidad mejorada
 * 
 * Cuando tint es null, usa LocalContentColor para permitir que componentes
 * padre (como TopAppBar) controlen el color del icono
 */
@Composable
fun MaterialIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val iconTint = tint ?: LocalContentColor.current
    
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = iconTint
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MaterialIconButtonPreview() {
    MaterialIconButton(
        onClick = { /* Preview only */ },
        icon = Icons.Default.Settings,
        contentDescription = androidx.compose.ui.res.stringResource(dev.thecodegrove.grovetimer.R.string.settings_content_description),
        tint = MaterialTheme.groveColors.timerDisplay
    )
}
