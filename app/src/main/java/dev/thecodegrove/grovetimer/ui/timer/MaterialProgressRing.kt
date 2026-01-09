package dev.thecodegrove.grovetimer.ui.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Componente de anillo de progreso con Material Design 3
 * Proporciona mejor elevación y animaciones suaves
 */
@Composable
fun MaterialProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 200.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 8.dp,
    backgroundColor: Color = MaterialTheme.groveColors.progressBackground,
    progressColor: Color = MaterialTheme.groveColors.progressActive
) {
    Canvas(
        modifier = modifier.size(size)
    ) {
        val center = this.size.center
        val radius = (this.size.minDimension - strokeWidth.toPx()) / 2f
        
        // Dibujar círculo de fondo
        drawCircle(
            color = backgroundColor,
            radius = radius,
            center = center,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
        )
        
        // Dibujar arco de progreso
        if (progress > 0f) {
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MaterialProgressRingPreview() {
    MaterialProgressRing(
        progress = 0.3f
    )
}
