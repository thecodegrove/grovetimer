package com.thecodegrove.grovetimer.ui.theme

import androidx.compose.ui.graphics.Color

// ===============================================
// GroveTimer - Colores Base
// Verde Base + Marrón Pausa
// ===============================================

// === COLORES VERDES (PRIMARIOS) ===
val GroveGreen50 = Color(0xFFE8F5E8)      // Fondo muy claro
val GroveGreen100 = Color(0xFFC8E6C9)     // Claro
val GroveGreen200 = Color(0xFFA5D6A7)     // Claro medio
val GroveGreen300 = Color(0xFF81C784)     // Medio
val GroveGreen400 = Color(0xFF66BB6A)     // Medio oscuro
val GroveGreen500 = Color(0xFF4CAF50)     // Principal
val GroveGreen600 = Color(0xFF388E3C)     // Oscuro
val GroveGreen700 = Color(0xFF2E7D32)     // Muy oscuro
val GroveGreen800 = Color(0xFF1B5E20)     // Extremo oscuro
val GroveGreen900 = Color(0xFF0D2818)     // Negro verdoso

// === COLORES MARRONES (SECUNDARIOS) ===
val GroveBrown50 = Color(0xFFEFEBE9)      // Fondo muy claro
val GroveBrown100 = Color(0xFFD7CCC8)     // Claro
val GroveBrown200 = Color(0xFFBCAA94)     // Claro medio
val GroveBrown300 = Color(0xFFA1887F)     // Medio
val GroveBrown400 = Color(0xFF8D6E63)     // Medio oscuro
val GroveBrown500 = Color(0xFF795548)     // Principal
val GroveBrown600 = Color(0xFF6D4C41)     // Oscuro
val GroveBrown700 = Color(0xFF5D4037)     // Muy oscuro (pausa)
val GroveBrown800 = Color(0xFF4E342E)     // Extremo oscuro
val GroveBrown900 = Color(0xFF3E2723)     // Negro marrón

// === COLORES STONE (NEUTROS CÁLIDOS) ===
val Stone0 = Color(0xFFFAFAF8)
val Stone50 = Color(0xFFF4F4F0)
val Stone100 = Color(0xFFE8E8E2)
val Stone200 = Color(0xFFD0D0C8)
val Stone300 = Color(0xFFA8A89E)
val Stone400 = Color(0xFF808078)
val Stone500 = Color(0xFF585852)
val Stone600 = Color(0xFF38382E)
val Stone900 = Color(0xFF181810)

// === COLORES DE ACENTO ===
val GroveAccentWarm = Color(0xFFFFB74D)   // Naranja cálido
val GroveAccentGold = Color(0xFFFFCC02)   // Dorado
val GroveAccentCool = Color(0xFF81C784)   // Verde acento

// === COLORES DE SISTEMA ===
val GroveError = Color(0xFFD32F2F)
val GroveWarning = Color(0xFFFF9800)
val GroveSuccess = Color(0xFF4CAF50)
val GroveInfo = Color(0xFF1976D2)

// ===============================================
// COLORES PERSONALIZADOS DE GROVETIMER
// ===============================================

data class GroveTimerColors(
    val progressBackground: Color,
    val progressActive: Color,
    val pauseSymbol: Color,
    val timerDisplay: Color,
    val cardBackground: Color,
    val appBackground: Color,
    val borderSubtle: Color,
    val mutedText: Color,
    val accentWarm: Color,
    val accentGold: Color,
    val successGreen: Color,
    val warningOrange: Color,
    val surfaceElevated: Color
)

val LightGroveTimerColors = GroveTimerColors(
    progressBackground = GroveGreen100,
    progressActive = GroveGreen500,
    pauseSymbol = GroveBrown500,
    timerDisplay = GroveBrown500,
    cardBackground = Color.White,
    appBackground = Stone0,
    borderSubtle = Stone100,
    mutedText = Stone400,
    accentWarm = GroveAccentWarm,
    accentGold = GroveAccentGold,
    successGreen = GroveSuccess,
    warningOrange = GroveBrown500,
    surfaceElevated = Stone50
)

val DarkGroveTimerColors = GroveTimerColors(
    progressBackground = GroveGreen800,
    progressActive = GroveGreen400,
    pauseSymbol = GroveBrown300,
    timerDisplay = GroveBrown200,
    cardBackground = Color(0xFF1F2A1F),
    appBackground = Color(0xFF0F1419),
    borderSubtle = GroveGreen800,
    mutedText = GroveGreen300,
    accentWarm = GroveAccentWarm,
    accentGold = GroveAccentGold,
    successGreen = GroveGreen400,
    warningOrange = GroveBrown300,
    surfaceElevated = Color(0xFF1F2A1F)
)

// ===============================================
// COLORES DE COMPATIBILIDAD (MANTENER TEMPORALMENTE)
// ===============================================

// Colores originales de Material Design - mantener para compatibilidad
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
