package dev.thecodegrove.grovetimer.domain.model

/**
 * Modelo de datos para las configuraciones del usuario
 * Define todas las opciones configurables de GroveTimer
 */
data class UserSettings(
    val fadeoutEnabled: Boolean = false,
    val vibrateOnFinish: Boolean = false,
    val autoStartTimer: Boolean = false,
    val darkModeEnabled: Boolean = false,
    val defaultTimerDuration: Long = 600000L, // 10 minutos en milisegundos
    val fadeoutDuration: Int = 30, // segundos
    val vibrationPattern: LongArray = longArrayOf(0, 500, 200, 500), // patrón de vibración
    val soundEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserSettings

        if (fadeoutEnabled != other.fadeoutEnabled) return false
        if (vibrateOnFinish != other.vibrateOnFinish) return false
        if (autoStartTimer != other.autoStartTimer) return false
        if (darkModeEnabled != other.darkModeEnabled) return false
        if (defaultTimerDuration != other.defaultTimerDuration) return false
        if (fadeoutDuration != other.fadeoutDuration) return false
        if (soundEnabled != other.soundEnabled) return false
        if (hapticFeedbackEnabled != other.hapticFeedbackEnabled) return false
        if (!vibrationPattern.contentEquals(other.vibrationPattern)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fadeoutEnabled.hashCode()
        result = 31 * result + vibrateOnFinish.hashCode()
        result = 31 * result + autoStartTimer.hashCode()
        result = 31 * result + darkModeEnabled.hashCode()
        result = 31 * result + defaultTimerDuration.hashCode()
        result = 31 * result + fadeoutDuration
        result = 31 * result + soundEnabled.hashCode()
        result = 31 * result + hapticFeedbackEnabled.hashCode()
        result = 31 * result + vibrationPattern.contentHashCode()
        return result
    }

    /**
     * Crea una copia de las configuraciones con valores actualizados
     */
    fun copyWith(
        fadeoutEnabled: Boolean? = null,
        vibrateOnFinish: Boolean? = null,
        autoStartTimer: Boolean? = null,
        darkModeEnabled: Boolean? = null,
        defaultTimerDuration: Long? = null,
        fadeoutDuration: Int? = null,
        vibrationPattern: LongArray? = null,
        soundEnabled: Boolean? = null,
        hapticFeedbackEnabled: Boolean? = null
    ): UserSettings {
        return copy(
            fadeoutEnabled = fadeoutEnabled ?: this.fadeoutEnabled,
            vibrateOnFinish = vibrateOnFinish ?: this.vibrateOnFinish,
            autoStartTimer = autoStartTimer ?: this.autoStartTimer,
            darkModeEnabled = darkModeEnabled ?: this.darkModeEnabled,
            defaultTimerDuration = defaultTimerDuration ?: this.defaultTimerDuration,
            fadeoutDuration = fadeoutDuration ?: this.fadeoutDuration,
            vibrationPattern = vibrationPattern ?: this.vibrationPattern,
            soundEnabled = soundEnabled ?: this.soundEnabled,
            hapticFeedbackEnabled = hapticFeedbackEnabled ?: this.hapticFeedbackEnabled
        )
    }

    companion object {
        /**
         * Configuraciones por defecto
         */
        val DEFAULT = UserSettings()
    }
}
