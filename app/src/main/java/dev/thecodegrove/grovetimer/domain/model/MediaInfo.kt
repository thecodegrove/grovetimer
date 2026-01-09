package dev.thecodegrove.grovetimer.domain.model

/**
 * Representa la información de una sesión multimedia activa
 * 
 * @property isPlaying Indica si el contenido multimedia está reproduciéndose actualmente
 * @property appPackage Nombre del paquete de la aplicación que reproduce el contenido
 * @property appName Nombre legible de la aplicación
 * @property mediaTitle Título del contenido multimedia (canción, video, podcast, etc.)
 * @property mediaArtist Artista o creador del contenido multimedia
 * @property mediaAlbum Álbum o colección del contenido multimedia
 * @property hasActiveSession Indica si hay una sesión multimedia activa en el sistema
 */
data class MediaInfo(
    val isPlaying: Boolean = false,
    val appPackage: String = "",
    val appName: String = "",
    val mediaTitle: String = "",
    val mediaArtist: String = "",
    val mediaAlbum: String = "",
    val hasActiveSession: Boolean = false
)
