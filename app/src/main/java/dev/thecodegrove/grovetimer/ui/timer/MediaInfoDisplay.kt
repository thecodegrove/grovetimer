package dev.thecodegrove.grovetimer.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.thecodegrove.grovetimer.R
import dev.thecodegrove.grovetimer.domain.model.MediaInfo
import dev.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Componente que muestra información de la sesión multimedia activa
 * 
 * @param mediaInfo Información de la sesión multimedia
 * @param modifier Modificador para personalizar el layout
 */
@Composable
fun MediaInfoDisplay(
    mediaInfo: MediaInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.groveColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título de la sección
            Text(
                text = stringResource(R.string.media_info_display_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.groveColors.timerDisplay
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (mediaInfo.hasActiveSession) {
                // Mostrar información de media activa
                MediaInfoContent(mediaInfo = mediaInfo)
            } else {
                // Mostrar mensaje cuando no hay sesión activa
                NoMediaSessionContent()
            }
        }
    }
}

/**
 * Contenido cuando hay una sesión multimedia activa
 */
@Composable
private fun MediaInfoContent(mediaInfo: MediaInfo) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icono de estado de reproducción - usando iconos básicos verificados
        val icon = if (mediaInfo.isPlaying) {
            Icons.Filled.Info  // Icono básico para "información activa"
        } else {
            Icons.Filled.Settings  // Icono básico para "configuración/pausado"
        }
        
        val iconTint = if (mediaInfo.isPlaying) {
            MaterialTheme.groveColors.successGreen
        } else {
            MaterialTheme.groveColors.pauseSymbol
        }
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.groveColors.progressBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (mediaInfo.isPlaying) stringResource(R.string.media_info_playing) else stringResource(R.string.media_info_paused),
                modifier = Modifier.size(24.dp),
                tint = iconTint
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Nombre de la aplicación
        if (mediaInfo.appName.isNotEmpty()) {
            Text(
                text = mediaInfo.appName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.groveColors.timerDisplay,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Título del contenido multimedia
        if (mediaInfo.mediaTitle.isNotEmpty()) {
            Text(
                text = mediaInfo.mediaTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Información adicional del artista y álbum
        if (mediaInfo.mediaArtist.isNotEmpty() || mediaInfo.mediaAlbum.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mediaInfo.mediaArtist.isNotEmpty()) {
                    Text(
                        text = mediaInfo.mediaArtist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (mediaInfo.mediaArtist.isNotEmpty() && mediaInfo.mediaAlbum.isNotEmpty()) {
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (mediaInfo.mediaAlbum.isNotEmpty()) {
                    Text(
                        text = mediaInfo.mediaAlbum,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        // Estado de reproducción
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (mediaInfo.isPlaying) stringResource(R.string.media_info_playing) else stringResource(R.string.media_info_paused),
            style = MaterialTheme.typography.labelLarge,
            color = if (mediaInfo.isPlaying) {
                MaterialTheme.groveColors.successGreen
            } else {
                MaterialTheme.groveColors.pauseSymbol
            },
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Contenido cuando no hay sesión multimedia activa
 */
@Composable
private fun NoMediaSessionContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icono de información - usando icono básico verificado
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.groveColors.progressBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Info,  // Icono básico para información
                contentDescription = stringResource(R.string.media_info_no_playback),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.groveColors.pauseSymbol
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.media_info_nothing_playing_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.media_info_start_timer_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Preview del componente MediaInfoDisplay con información de media
 */
@Preview(showBackground = true)
@Composable
private fun MediaInfoDisplayPreview() {
    val sampleMediaInfo = MediaInfo(
        isPlaying = true,
        appPackage = "com.spotify.music",
        appName = "Spotify",
        mediaTitle = "Bohemian Rhapsody",
        mediaArtist = "Queen",
        mediaAlbum = "A Night at the Opera",
        hasActiveSession = true
    )
    
    MediaInfoDisplay(
        mediaInfo = sampleMediaInfo,
        modifier = Modifier.padding(16.dp)
    )
}

/**
 * Preview del componente MediaInfoDisplay sin sesión activa
 */
@Preview(showBackground = true)
@Composable
private fun MediaInfoDisplayNoMediaPreview() {
    val emptyMediaInfo = MediaInfo(
        hasActiveSession = false
    )
    
    MediaInfoDisplay(
        mediaInfo = emptyMediaInfo,
        modifier = Modifier.padding(16.dp)
    )
}
