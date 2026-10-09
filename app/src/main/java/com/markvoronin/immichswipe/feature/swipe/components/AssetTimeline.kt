package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialGreen
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialRed
import com.markvoronin.immichswipe.feature.swipe.SwipeDecision
import kotlin.math.abs

private fun Modifier.rotateLayout(rotation: Int) = layout { measurable, constraints ->
    val isRotated = rotation % 180 != 0
    val newConstraints = if (isRotated) {
        Constraints(
            minWidth = constraints.minHeight,
            maxWidth = constraints.maxHeight,
            minHeight = constraints.minWidth,
            maxHeight = constraints.maxWidth
        )
    } else constraints
    val placeable = measurable.measure(newConstraints)
    if (isRotated) {
        layout(placeable.height, placeable.width) {
            placeable.place((placeable.height - placeable.width) / 2, (placeable.width - placeable.height) / 2)
        }
    } else {
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

@Composable
fun AssetTimeline(
    assets: List<Asset>,
    decisions: Map<String, SwipeDecision>,
    currentIndex: Int,
    isFavorite: (String) -> Boolean,
    isArchived: (String) -> Boolean,
    isLocked: (String) -> Boolean,
    onAssetClick: (Int) -> Unit,
    baseUrl: String = "",
    apiKey: String = "",
    isBulkMode: Boolean = false,
    bulkSelection: Set<String> = emptySet(),
    isBulkDelete: Boolean = false,
    getRotation: (String) -> Int = { 0 },
    swipeOffset: Float = 0f,
    nextIndex: Int = -1
) {
    val listState = rememberLazyListState()
    val baseUrlClean = baseUrl.removeSuffix("/")
    val density = LocalDensity.current
    val itemSizePx = with(density) { 64.dp.toPx() }

    var previousAssets by remember { mutableStateOf<List<Asset>?>(null) }

    val swipeProgress = (abs(swipeOffset) / 250f).coerceIn(0f, 1f)
    val hasValidNext = nextIndex in assets.indices && nextIndex != currentIndex
    val isActivelySwiping = swipeProgress > 0f && hasValidNext && !isBulkMode

    val targetIndex = when {
        isBulkMode && bulkSelection.isNotEmpty() -> {
            assets.indices.lastOrNull { i -> bulkSelection.contains(assets[i].id) } ?: currentIndex
        }
        else -> currentIndex
    }

    LaunchedEffect(targetIndex, nextIndex, swipeOffset, isBulkMode, bulkSelection, assets) {
        if (assets.isEmpty()) return@LaunchedEffect

        val assetsChanged = assets !== previousAssets
        previousAssets = assets

        if (isActivelySwiping) {
            val indexDelta = nextIndex - currentIndex
            val basePx = currentIndex * itemSizePx + (indexDelta * swipeProgress * itemSizePx)
            val scrollItemIndex = (basePx / itemSizePx).toInt().coerceIn(0, assets.lastIndex)
            val scrollOffsetPx = (basePx - (scrollItemIndex * itemSizePx)).toInt()

            listState.scrollToItem(scrollItemIndex, scrollOffsetPx)
        } else if (targetIndex in assets.indices) {
            val currentVisible = listState.firstVisibleItemIndex
            val distance = abs(targetIndex - currentVisible)
            if (assetsChanged || distance > 3) {
                listState.scrollToItem(targetIndex, scrollOffset = 0)
            } else {
                listState.animateScrollToItem(targetIndex, scrollOffset = 0)
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp)
            .padding(vertical = 5.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(assets, key = { _, asset -> asset.id }) { index, asset ->
            AssetTimelineItem(
                asset = asset,
                index = index,
                currentIndex = currentIndex,
                nextIndex = nextIndex,
                swipeProgress = if (isActivelySwiping) swipeProgress else 0f,
                isSelected = bulkSelection.contains(asset.id),
                decision = decisions[asset.id],
                hasHeart = isFavorite(asset.id),
                hasArchive = isArchived(asset.id),
                hasLock = isLocked(asset.id),
                isBulkDelete = isBulkDelete,
                rotation = getRotation(asset.id),
                baseUrl = baseUrlClean,
                apiKey = apiKey,
                onAssetClick = onAssetClick
            )
        }
    }
}

@Composable
private fun AssetTimelineItem(
    asset: Asset,
    index: Int,
    currentIndex: Int,
    nextIndex: Int,
    swipeProgress: Float,
    isSelected: Boolean,
    decision: SwipeDecision?,
    hasHeart: Boolean,
    hasArchive: Boolean,
    hasLock: Boolean,
    isBulkDelete: Boolean,
    rotation: Int,
    baseUrl: String,
    apiKey: String,
    onAssetClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val animatedRotation by animateFloatAsState(
        targetValue = rotation.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "TimelineRotation"
    )

    val isCurrent = index == currentIndex
    val isNext = index == nextIndex

    val borderProgress = when {
        isCurrent -> 1f - swipeProgress
        isNext -> swipeProgress
        else -> 0f
    }

    val itemAlpha = when {
        isCurrent -> 1f - (swipeProgress * 0.4f)
        isNext -> 0.6f + (swipeProgress * 0.4f)
        else -> 0.6f
    }

    val borderColor = when {
        isSelected -> if (isBulkDelete) MaterialRed else MaterialGreen
        borderProgress > 0.05f -> MaterialTheme.colorScheme.primary.copy(alpha = borderProgress)
        else -> Color.Transparent
    }

    val borderWidth = when {
        isSelected -> 3.dp
        borderProgress > 0.05f -> (1.dp + (1.dp * borderProgress))
        else -> 0.dp
    }

    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onAssetClick(index) }
    ) {
        if (baseUrl.isNotEmpty()) {
            val thumbnailRequest = remember(asset.id, baseUrl, apiKey, rotation) {
                ImageRequest.Builder(context)
                    .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=thumbnail&edited=true")
                    .addHeader("x-api-key", apiKey)
                    .crossfade(true)
                    .precision(Precision.INEXACT)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            SubcomposeAsyncImage(
                model = thumbnailRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = animatedRotation }
                    .rotateLayout(rotation)
                    .alpha(itemAlpha),
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }

        if (asset.type == "VIDEO") {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(2.dp)
                    .size(14.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            )
        } else if (asset.isGif) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(3.dp)
                    .background(Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(3.dp))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "GIF",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 9.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            decision?.let { d ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            when (d) {
                                SwipeDecision.KEEP -> MaterialGreen
                                SwipeDecision.DELETE -> MaterialRed
                                SwipeDecision.ARCHIVE -> MaterialTheme.colorScheme.primary
                                SwipeDecision.LOCK -> MaterialTheme.colorScheme.outline
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (d) {
                            SwipeDecision.KEEP -> Icons.Default.Check
                            SwipeDecision.DELETE -> Icons.Default.Delete
                            SwipeDecision.ARCHIVE -> Icons.Default.Archive
                            SwipeDecision.LOCK -> Icons.Default.Lock
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            if (hasHeart) {
                TimelineMiniBadge(Icons.Default.Favorite, Color.Red)
            }
            if (hasArchive && decision != SwipeDecision.ARCHIVE) {
                TimelineMiniBadge(Icons.Default.Archive, Color.Black)
            }
            if (hasLock && decision != SwipeDecision.LOCK) {
                TimelineMiniBadge(Icons.Default.Lock, Color.Black)
            }
        }
    }
}

@Composable
fun TimelineMiniBadge(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(10.dp)
        )
    }
}
