package com.markvoronin.immichswipe.feature.swipe

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.domain.model.Asset
import kotlin.math.abs

@Composable
fun AssetTimeline(
    assets: List<Asset>,
    decisions: Map<String, SwipeDecision>,
    currentIndex: Int,
    isFavorite: (String) -> Boolean,
    isArchived: (String) -> Boolean,
    isLocked: (String) -> Boolean,
    onAssetClick: (Int) -> Unit,
    isBulkMode: Boolean = false,
    bulkSelection: Set<String> = emptySet(),
    isBulkDelete: Boolean = false
) {
    val listState = rememberLazyListState()
    val baseUrl = remember { SessionManager.getBaseUrl()?.removeSuffix("/") }
    val apiKey = remember { SessionManager.getApiKey() ?: "" }

    val targetIndex = if (isBulkMode && bulkSelection.isNotEmpty()) {
        assets.indices.lastOrNull { i -> bulkSelection.contains(assets[i].id) } ?: currentIndex
    } else {
        currentIndex
    }
    val currentAssetId = assets.getOrNull(targetIndex)?.id

    LaunchedEffect(targetIndex, currentAssetId, isBulkMode, bulkSelection) {
        if (assets.isNotEmpty() && targetIndex in assets.indices) {
            val scrollIndex = (targetIndex - 1).coerceAtLeast(0)
            val currentVisible = listState.firstVisibleItemIndex
            val distance = abs(scrollIndex - currentVisible)
            if (distance > 3) {
                listState.scrollToItem(scrollIndex, scrollOffset = 0)
            } else {
                listState.animateScrollToItem(scrollIndex, scrollOffset = 0)
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(vertical = 2.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(assets, key = { _, asset -> asset.id }) { index, asset ->
            AssetTimelineItem(
                asset = asset,
                index = index,
                isCurrent = index == currentIndex,
                isSelected = bulkSelection.contains(asset.id),
                decision = decisions[asset.id],
                hasHeart = isFavorite(asset.id),
                hasArchive = isArchived(asset.id),
                hasLock = isLocked(asset.id),
                isBulkDelete = isBulkDelete,
                baseUrl = baseUrl,
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
    isCurrent: Boolean,
    isSelected: Boolean,
    decision: SwipeDecision?,
    hasHeart: Boolean,
    hasArchive: Boolean,
    hasLock: Boolean,
    isBulkDelete: Boolean,
    baseUrl: String?,
    apiKey: String,
    onAssetClick: (Int) -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 3.dp else if (isCurrent) 2.dp else 0.dp,
                color = if (isSelected) {
                    if (isBulkDelete) MaterialRed else MaterialGreen
                } else if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onAssetClick(index) }
    ) {
        if (baseUrl != null) {
            val thumbnailRequest = remember(asset.id, baseUrl, apiKey) {
                ImageRequest.Builder(context)
                    .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=thumbnail")
                    .addHeader("x-api-key", apiKey)
                    .crossfade(true)
                    .precision(Precision.INEXACT)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            AsyncImage(
                model = thumbnailRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(if (isCurrent) 1f else 0.6f)
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
