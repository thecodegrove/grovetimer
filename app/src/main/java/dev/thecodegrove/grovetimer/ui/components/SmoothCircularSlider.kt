package dev.thecodegrove.grovetimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import dev.thecodegrove.grovetimer.ui.theme.groveColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Slider circular suave y preciso basado en las mejores prácticas
 * Implementación basada en Shift2Dev: https://shift2dev.com/create-custom-android-circular-slider-in-compose/
 */
@Composable
fun SmoothCircularSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float> = 0f..240f,
    strokeWidth: Float = 28f,
    enabled: Boolean = true
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Obtener colores del tema
    val progressBackground = MaterialTheme.groveColors.progressBackground
    val progressActive = MaterialTheme.groveColors.progressActive
    
    // Convertir valor a ángulo para mostrar
    val currentAngle = valueToAngle(value, range)
    
    Canvas(
        modifier = modifier
            .size(280.dp)
            .onGloballyPositioned { layoutCoordinates ->
                canvasSize = layoutCoordinates.size
            }
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            dragOffset = change.position
                            val newAngle = calculateNewAngle(canvasSize.center.toOffset(), dragOffset)
                            val newValue = angleToValue(newAngle, range)
                            onValueChange(newValue)
                        }
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                dragOffset = offset
                                val newAngle = calculateNewAngle(canvasSize.center.toOffset(), dragOffset)
                                val newValue = angleToValue(newAngle, range)
                                onValueChange(newValue)
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        drawSmoothSlider(
            center = canvasSize.center.toOffset(),
            radius = min(canvasSize.width, canvasSize.height) / 2f - strokeWidth / 2f,
            angle = currentAngle,
            strokeWidth = strokeWidth,
            progressBackground = progressBackground,
            progressActive = progressActive
        )
    }
}

/**
 * Calcula el nuevo ángulo basado en la posición del drag
 * Modificado para que el valor 0 esté en las 12 en punto (arriba)
 */
private fun calculateNewAngle(center: Offset, dragOffset: Offset): Float {
    val rad = atan2((dragOffset.y - center.y).toDouble(), (dragOffset.x - center.x).toDouble())
    val angle = Math.toDegrees(rad)
    // Convertir para que 0 esté en las 12 en punto (arriba)
    // +90 grados para empezar desde arriba, +360 para normalizar, %360 para mantener en rango 0-360
    return ((angle + 90f + 360f) % 360f).roundToInt().toFloat()
}

/**
 * Convierte ángulo a valor dentro del rango especificado
 */
private fun angleToValue(angle: Float, range: ClosedFloatingPointRange<Float>): Float {
    val normalizedAngle = angle / 360f
    val rangeSize = range.endInclusive - range.start
    val value = range.start + (normalizedAngle * rangeSize)
    return value.coerceIn(range.start, range.endInclusive)
}

/**
 * Convierte valor a ángulo para renderizado
 */
private fun valueToAngle(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    val normalizedValue = (value - range.start) / (range.endInclusive - range.start)
    return normalizedValue * 360f
}

/**
 * Calcula la posición del handle en el círculo
 * Modificado para que el valor 0 esté en las 12 en punto (arriba)
 */
private fun calculateHandleOffset(size: IntSize, angle: Float): Offset {
    // Convertir ángulo para que coincida con el sistema de coordenadas del progreso
    // El progreso usa startAngle = -90f, así que necesitamos ajustar el ángulo del thumb
    val adjustedAngle = angle - 90f
    val angleRadians = Math.toRadians(adjustedAngle.toDouble())
    return Offset(
        (size.center.x + cos(angleRadians) * (size.width / 2)).toFloat(),
        (size.center.y + sin(angleRadians) * (size.height / 2)).toFloat()
    )
}

/**
 * Renderiza el slider circular con progreso y thumb
 * Implementación basada en Shift2Dev
 */
private fun DrawScope.drawSmoothSlider(
    center: Offset,
    radius: Float,
    angle: Float,
    strokeWidth: Float,
    progressBackground: Color,
    progressActive: Color
) {
    // Dibujar fondo del círculo (track inactivo) - estilo icono de app
    drawCircle(
        color = progressBackground,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round)
    )
    
    // Dibujar progreso activo - ligeramente más ancho que el fondo
    if (angle > 0f) {
        drawArc(
            color = progressActive,
            startAngle = -90f, // Empezar desde las 12:00 (arriba)
            sweepAngle = angle,
            useCenter = false,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth * 1.3f, cap = StrokeCap.Round)
        )
    }
    
    // Sin thumb/handle - solo track circular como el icono
}