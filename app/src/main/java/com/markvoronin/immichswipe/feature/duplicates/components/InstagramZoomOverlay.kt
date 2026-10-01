package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision
import kotlinx.coroutines.launch

@Composable
fun InstagramZoomOverlay(
    zoomData: ZoomData?,
    decision: DuplicateDecision,
    rootWindowOffset: Offset,
    baseUrl: String = "",
    apiKey: String = "",
    onDismiss: () -> Unit
) {
    if (zoomData == null) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val baseUrlClean = baseUrl.removeSuffix("/")

    val scaleAnim = remember { Animatable(zoomData.scale) }
    val offsetXAnim = remember { Animatable(zoomData.offset.x) }
    val offsetYAnim = remember { Animatable(zoomData.offset.y) }

    LaunchedEffect(zoomData.scale, zoomData.offset, zoomData.isGestureActive) {
        if (zoomData.isGestureActive) {
            scaleAnim.snapTo(zoomData.scale)
            offsetXAnim.snapTo(zoomData.offset.x)
            offsetYAnim.snapTo(zoomData.offset.y)
        } else {
            launch {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                offsetXAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                offsetYAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                onDismiss()
            }
        }
    }

    val currentScale = scaleAnim.value.coerceAtLeast(1f)
    val inverseScale = 1f / currentScale
    val badgeAlpha = (1f - (currentScale - 1f) * 1.5f).coerceIn(0f, 1f)
    val backdropAlpha = ((currentScale - 1f) / 1.5f).coerceIn(0f, 0.7f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black.copy(alpha = backdropAlpha))
    ) {
        val bounds = zoomData.initialBounds
        val leftPx = bounds.left - rootWindowOffset.x
        val topPx = bounds.top - rootWindowOffset.y

        val widthDp = with(density) { bounds.width.toDp() }
        val heightDp = with(density) { bounds.height.toDp() }
        val leftDp = with(density) { leftPx.toDp() }
        val topDp = with(density) { topPx.toDp() }

        val borderColor = when (decision) {
            DuplicateDecision.DELETE -> MaterialTheme.colorScheme.error
            DuplicateDecision.KEEP -> Color(0xFF4CAF50)
            DuplicateDecision.NONE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        }

        Box(
            modifier = Modifier
                .offset(x = leftDp, y = topDp)
                .size(width = widthDp, height = heightDp)
                .graphicsLayer {
                    scaleX = currentScale
                    scaleY = currentScale
                    translationX = offsetXAnim.value
                    translationY = offsetYAnim.value
                }
                .clip(RoundedCornerShape(8.dp))
                .border(3.dp, borderColor, RoundedCornerShape(8.dp))
        ) {
            val imageRequest = remember(zoomData.asset.id, baseUrlClean, apiKey) {
                ImageRequest.Builder(context)
                    .data("$baseUrlClean/api/assets/${zoomData.asset.id}/thumbnail?format=WEBP&size=preview")
                    .addHeader("x-api-key", apiKey)
                    .crossfade(true)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (decision != DuplicateDecision.NONE) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (decision == DuplicateDecision.DELETE) Icons.Default.Delete else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (decision == DuplicateDecision.DELETE) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .graphicsLayer {
                                scaleX = inverseScale
                                scaleY = inverseScale
                                transformOrigin = TransformOrigin(1f, 0f)
                                alpha = badgeAlpha
                            }
                            .size(32.dp)
                            .background(Color.White, shape = CircleShape)
                    )
                }
            }
        }
    }
}
