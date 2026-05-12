package com.thecodegrove.grovetimer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ===============================================
// ESQUEMAS DE COLOR MATERIAL 3 - GROVETIMER
// ===============================================

val GroveTimerLightColorScheme = lightColorScheme(
    // === PRIMARIOS (VERDE) ===
    primary = GroveGreen500,                    // Verde principal
    onPrimary = Color.White,                    // Texto sobre verde
    primaryContainer = GroveGreen50,            // Contenedor verde claro
    onPrimaryContainer = GroveGreen800,         // Texto sobre contenedor verde
    
    // === SECUNDARIOS (MARRÓN) ===
    secondary = GroveBrown700,                  // Marrón pausa
    onSecondary = Color.White,                  // Texto sobre marrón
    secondaryContainer = GroveBrown50,          // Contenedor marrón claro
    onSecondaryContainer = GroveBrown900,       // Texto sobre contenedor marrón
    
    // === TERCIARIOS (ACENTO) ===
    tertiary = GroveAccentWarm,                 // Naranja cálido
    onTertiary = Color.White,                   // Texto sobre naranja
    tertiaryContainer = Color(0xFFFFE0B2),      // Contenedor naranja claro
    onTertiaryContainer = Color(0xFFE65100),    // Texto sobre contenedor naranja
    
    // === FONDOS ===
    background = Stone0,                        // Fondo general cálido
    onBackground = Stone900,                    // Texto sobre fondo
    
    // === SUPERFICIES ===
    surface = Color.White,                      // Superficie de tarjetas
    onSurface = Stone900,                       // Texto sobre superficie
    surfaceVariant = Stone50,                   // Superficie variante cálida
    onSurfaceVariant = Stone600,                // Texto sobre superficie variante
    
    // === CONTORNOS ===
    outline = Stone200,                         // Líneas de contorno
    outlineVariant = Stone100,                  // Contorno sutil
    
    // === COLORES ESPECIALES ===
    error = GroveError,
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFD32F2F)
)

val GroveTimerDarkColorScheme = darkColorScheme(
    // === PRIMARIOS (VERDE) ===
    primary = GroveGreen300,                    // Verde más claro para contraste
    onPrimary = GroveGreen900,                  // Texto oscuro sobre verde
    primaryContainer = GroveGreen800,           // Contenedor verde oscuro
    onPrimaryContainer = GroveGreen100,         // Texto claro sobre contenedor
    
    // === SECUNDARIOS (MARRÓN) ===
    secondary = GroveBrown300,                  // Marrón más claro
    onSecondary = GroveBrown900,                // Texto oscuro sobre marrón
    secondaryContainer = GroveBrown800,         // Contenedor marrón oscuro
    onSecondaryContainer = GroveBrown100,       // Texto claro sobre contenedor
    
    // === TERCIARIOS (ACENTO) ===
    tertiary = GroveAccentWarm,                 // Mantener naranja cálido
    onTertiary = Color(0xFF1A0E00),            // Texto muy oscuro
    tertiaryContainer = Color(0xFFBF360C),      // Contenedor naranja oscuro
    onTertiaryContainer = Color(0xFFFFE0B2),    // Texto claro sobre contenedor
    
    // === FONDOS ===
    background = Color(0xFF0F1419),             // Fondo oscuro con toque verdoso
    onBackground = Color(0xFFE6E1E5),           // Texto claro sobre fondo
    
    // === SUPERFICIES ===
    surface = Color(0xFF141218),                // Superficie oscura
    onSurface = Color(0xFFE6E1E5),             // Texto claro sobre superficie
    surfaceVariant = Color(0xFF1F2A1F),         // Superficie variante verdosa
    onSurfaceVariant = GroveGreen200,           // Texto verde sobre superficie
    
    // === CONTORNOS ===
    outline = GroveGreen600,                    // Líneas verdosas
    outlineVariant = GroveGreen800,             // Contorno sutil oscuro
    
    // === COLORES ESPECIALES ===
    error = Color(0xFFEF5350),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// ===============================================
// COLORES DE COMPATIBILIDAD (MANTENER TEMPORALMENTE)
// ===============================================

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// ===============================================
// LOCAL COMPOSITION PARA COLORES PERSONALIZADOS
// ===============================================

val LocalGroveTimerColors = staticCompositionLocalOf { LightGroveTimerColors }

// ===============================================
// TEMA PRINCIPAL
// ===============================================

@Composable
fun GroveTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    followSystemTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Determine actual theme based on user preference
    val actualDarkTheme = if (followSystemTheme) {
        isSystemInDarkTheme()
    } else {
        darkTheme
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (actualDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        actualDarkTheme -> GroveTimerDarkColorScheme
        else -> GroveTimerLightColorScheme
    }
    
    val customColors = when {
        actualDarkTheme -> DarkGroveTimerColors
        else -> LightGroveTimerColors
    }

    // Configurar iconos del sistema (status bar) y windowBackground según el tema
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? android.app.Activity
            val window = activity?.window
            window?.let {
                // Configurar iconos del sistema (status bar)
                WindowCompat.getInsetsController(it, view).apply {
                    // En modo claro: iconos oscuros (isAppearanceLightStatusBars = true)
                    // En modo oscuro: iconos claros (isAppearanceLightStatusBars = false)
                    isAppearanceLightStatusBars = !actualDarkTheme
                }
                
                // Configurar windowBackground para evitar flashes blancos durante transiciones
                // Esto es especialmente importante en modo oscuro
                val backgroundColor = if (actualDarkTheme) {
                    android.graphics.Color.parseColor("#0F1419") // Fondo oscuro
                } else {
                    android.graphics.Color.parseColor("#FAFAF8") // Fondo claro
                }
                it.setBackgroundDrawable(
                    android.graphics.drawable.ColorDrawable(backgroundColor)
                )
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalGroveTimerColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GroveTimerTypography,
            shapes = GroveTimerShapes,
            content = content
        )
    }
}

private val GroveTimerShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ===============================================
// EXTENSION PARA ACCEDER A COLORES PERSONALIZADOS
// ===============================================

val MaterialTheme.groveColors: GroveTimerColors
    @Composable
    get() = LocalGroveTimerColors.current
