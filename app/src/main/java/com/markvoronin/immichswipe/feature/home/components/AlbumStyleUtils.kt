package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
fun ErrorView(
    error: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    val isNetworkError = error.contains("Unable to resolve host", ignoreCase = true) ||
            error.contains("UnknownHostException", ignoreCase = true) ||
            error.contains("No address associated with hostname", ignoreCase = true) ||
            error.contains("Failed to connect", ignoreCase = true) ||
            error.contains("NetworkError", ignoreCase = true) ||
            error.contains("timeout", ignoreCase = true)

    val icon = if (isNetworkError) Icons.Outlined.WifiOff else Icons.Outlined.ErrorOutline

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 360.dp)
        ) {
            // Circle Icon Container
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Error Title
            Text(
                text = stringResource(R.string.home_error_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // Error Details Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Retry Button
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(100.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_retry_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
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

@Preview(showBackground = true)
@Composable
fun ErrorViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ErrorView(
                error = "Unable to resolve host \"photos.marksoft.net\": No address associated with hostname",
                onRetry = {}
            )
        }
    }
}
