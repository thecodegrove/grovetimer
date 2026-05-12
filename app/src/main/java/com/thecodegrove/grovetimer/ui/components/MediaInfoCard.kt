package com.thecodegrove.grovetimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.thecodegrove.grovetimer.R
import com.thecodegrove.grovetimer.utils.DebugUtils
import com.thecodegrove.grovetimer.ui.common.MaterialCard
import com.thecodegrove.grovetimer.ui.theme.groveColors

/**
 * Componente de tarjeta informativa de media
 * Muestra información sobre la aplicación y contenido que se está reproduciendo
 */
@Composable
fun MediaInfoCard(
    appName: String? = null,
    contentTitle: String? = null,
    modifier: Modifier = Modifier
) {
    // Logging para debugging
    val context = LocalContext.current
    if (DebugUtils.isDebug(context)) {
        android.util.Log.d("MediaInfoCard", "MediaInfoCard recomposed - appName: $appName, contentTitle: $contentTitle")
    }
    MaterialCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de música Material Design
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = stringResource(R.string.media_info_music_icon),
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.groveColors.progressBackground,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(9.dp),
                tint = MaterialTheme.groveColors.progressActive
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Información de media
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (appName != null) stringResource(R.string.media_info_playing_currently) else stringResource(R.string.media_info_playback_status),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.groveColors.mutedText
                )
                
                Text(
                    text = if (appName != null && contentTitle != null) {
                        "$appName - $contentTitle"
                    } else {
                        stringResource(R.string.media_info_nothing_playing)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MediaInfoCardPreview() {
    MediaInfoCard(
        appName = "Spotify",
        contentTitle = "Bohemian Rhapsody - Queen"
    )
}

@Preview(showBackground = true)
@Composable
fun MediaInfoCardEmptyPreview() {
    MediaInfoCard()
}
