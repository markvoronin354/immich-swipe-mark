package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision

@Composable
fun DuplicateAssetItem(
    asset: Asset,
    decision: DuplicateDecision,
    isBeingZoomed: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onZoomStateUpdate: (ZoomData?) -> Unit
) {
    val context = LocalContext.current
    val baseUrl = remember { SessionManager.getBaseUrl()?.removeSuffix("/") }
    val apiKey = remember { SessionManager.getApiKey() ?: "" }

    val currentDecision by rememberUpdatedState(decision)
    val currentOnToggle by rememberUpdatedState(onToggle)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnZoomStateUpdate by rememberUpdatedState(onZoomStateUpdate)

    val borderColor = when (decision) {
        DuplicateDecision.DELETE -> MaterialTheme.colorScheme.error
        DuplicateDecision.KEEP -> Color(0xFF4CAF50)
        DuplicateDecision.NONE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    var itemBounds by remember { mutableStateOf<Rect?>(null) }
    val currentItemBounds by rememberUpdatedState(itemBounds)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.75f)
                .onGloballyPositioned { coords ->
                    itemBounds = coords.boundsInWindow()
                }
                .graphicsLayer {
                    alpha = if (isBeingZoomed) 0f else 1f
                }
                .clip(RoundedCornerShape(8.dp))
                .border(3.dp, borderColor, RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { currentOnToggle() },
                        onLongPress = { currentOnLongPress() }
                    )
                }
                .pointerInput(asset.id) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var currentScale = 1f
                        var currentOffset = Offset.Zero
                        var activeZooming = false

                        do {
                            val event = awaitPointerEvent()
                            val pressedPointers = event.changes.filter { it.pressed }
                            val pressedCount = pressedPointers.size

                            if (pressedCount >= 2) {
                                activeZooming = true
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()

                                currentScale = (currentScale * zoom).coerceIn(1f, 4f)
                                val maxOffset = 250f * currentScale
                                currentOffset = Offset(
                                    (currentOffset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                                    (currentOffset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                                )

                                currentItemBounds?.let { bounds ->
                                    currentOnZoomStateUpdate(
                                        ZoomData(
                                            asset = asset,
                                            decision = currentDecision,
                                            initialBounds = bounds,
                                            scale = currentScale,
                                            offset = currentOffset,
                                            isGestureActive = true
                                        )
                                    )
                                }
                                event.changes.forEach {
                                    if (it.positionChange() != Offset.Zero) {
                                        it.consume()
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (activeZooming) {
                            currentItemBounds?.let { bounds ->
                                currentOnZoomStateUpdate(
                                    ZoomData(
                                        asset = asset,
                                        decision = currentDecision,
                                        initialBounds = bounds,
                                        scale = currentScale,
                                        offset = currentOffset,
                                        isGestureActive = false
                                    )
                                )
                            }
                        }
                    }
                }
        ) {
            val imageRequest = remember(asset.id, baseUrl, apiKey) {
                ImageRequest.Builder(context)
                    .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview")
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

            if (asset.type == "VIDEO") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

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
                            .size(32.dp)
                            .background(Color.White, shape = CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val sizeStr = asset.exifInfo?.fileSizeInBytes?.let { formatSizeStr(it) } ?: "Unknown size"
        Text(text = sizeStr, style = MaterialTheme.typography.bodySmall)

        val decisionText = when (decision) {
            DuplicateDecision.DELETE -> "DELETE"
            DuplicateDecision.KEEP -> "KEEP"
            DuplicateDecision.NONE -> "-"
        }

        Text(
            text = decisionText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (decision == DuplicateDecision.NONE) MaterialTheme.colorScheme.outline else borderColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
