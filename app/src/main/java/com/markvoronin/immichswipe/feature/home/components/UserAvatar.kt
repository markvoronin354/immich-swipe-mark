package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@Composable
fun UserAvatar(
    userId: String?,
    baseUrl: String,
    apiKey: String,
    name: String?,
    avatarColorName: String?,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 1.dp,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val avatarColor = getAvatarColor(avatarColorName)
    val baseUrlClean = baseUrl.removeSuffix("/")
    val imageUrl = if (!userId.isNullOrEmpty() && baseUrlClean.isNotEmpty()) {
        "$baseUrlClean/api/users/$userId/profile-image"
    } else {
        null
    }

    val baseModifier = modifier
        .border(borderWidth, avatarColor, CircleShape)
        .padding(borderWidth)
        .clip(CircleShape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    if (imageUrl != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .addHeader("x-api-key", apiKey)
                .crossfade(enable = true)
                .build(),
            contentDescription = contentDescription,
            modifier = baseModifier,
            contentScale = ContentScale.Crop,
            loading = {
                AvatarPlaceholder(
                    name = name,
                    avatarColor = avatarColor,
                )
            },
            error = {
                AvatarPlaceholder(
                    name = name,
                    avatarColor = avatarColor,
                )
            },
        )
    } else {
        Box(modifier = baseModifier) {
            AvatarPlaceholder(
                name = name,
                avatarColor = avatarColor,
            )
        }
    }
}

@Composable
fun AvatarPlaceholder(
    name: String?,
    avatarColor: Color,
    modifier: Modifier = Modifier,
) {
    val initial = name?.trim()?.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()
    val isLightColor = avatarColor.luminance() > 0.6f
    val contentColor = if (isLightColor) Color.Black.copy(alpha = 0.87f) else Color.White

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(avatarColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val minDim = minOf(maxWidth, maxHeight)
        if (minDim > 0.dp) {
            val fontSize = (minDim.value * 0.45f).sp
            val iconSize = minDim * 0.55f

            if (initial != null) {
                Text(
                    text = initial.toString(),
                    color = contentColor,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}
