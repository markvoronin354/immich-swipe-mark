package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Album

@Composable
fun SwipePlaceholder(selectedAlbum: Album?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Swipe, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            if (selectedAlbum != null) {
                Text(stringResource(R.string.home_session_title, selectedAlbum.albumName), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.home_photos_to_discover, selectedAlbum.assetCount), fontSize = 14.sp)
            } else {
                Text(stringResource(R.string.home_select_album))
            }
        }
    }
}

@Composable
fun ErrorView(error: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.home_error_title), color = MaterialTheme.colorScheme.error)
            Text(error, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.home_retry_button)) }
        }
    }
}

/**
 * Retourne le style visuel pour une collection virtuelle.
 */
@Composable
fun getVirtualCollectionStyle(albumId: String): Triple<ImageVector, Brush, Color> {
    return when (albumId) {
        Album.VIRTUAL_ALL_ID -> Triple(
            Icons.Default.AutoAwesomeMotion,
            Brush.linearGradient(listOf(Color(0xFFf6d365), Color(0xFFfda085))),
            Color.White
        )
        Album.VIRTUAL_ORPHANS_ID -> Triple(
            Icons.Default.Extension,
            Brush.linearGradient(listOf(Color(0xFF84fab0), Color(0xFF8fd3f4))),
            Color.White
        )
        Album.VIRTUAL_DUPLICATES_ID -> Triple(
            Icons.Default.ContentCopy,
            Brush.linearGradient(listOf(Color(0xFFff758c), Color(0xFFff7eb3))),
            Color.White
        )
        else -> Triple(
            Icons.Default.PhotoLibrary, 
            Brush.linearGradient(listOf(Color.Gray, Color.DarkGray)), 
            Color.White
        )
    }
}

/**
 * Retourne la couleur Compose correspondant au nom de couleur Immich.
 */
fun getAvatarColor(colorName: String?): Color {
    return when (colorName?.lowercase()) {
        "primary" -> Color(0xFFadcbfa)
        "pink" -> Color(0xFFE91E63)
        "red" -> Color(0xFFF44336)
        "yellow" -> Color(0xFFFFEB3B)
        "blue" -> Color(0xFF2196F3)
        "green" -> Color(0xFF4CAF50)
        "purple" -> Color(0xFF9C27B0)
        "orange" -> Color(0xFFFF9800)
        "gray", "grey" -> Color(0xFF9E9E9E)
        "amber" -> Color(0xFFFFC107)
        "cyan" -> Color(0xFF00BCD4)
        "indigo" -> Color(0xFF3F51B5)
        "lime" -> Color(0xFFCDDC39)
        "teal" -> Color(0xFF009688)
        else -> Color(0xFF9C27B0) // Valeur par défaut (violet)
    }
}
