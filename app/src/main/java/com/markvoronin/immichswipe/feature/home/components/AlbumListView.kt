package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.feature.home.AlbumStatus
import com.markvoronin.immichswipe.ui.theme.VirtualGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumList(
    groupedAlbums: Map<AlbumStatus, List<Album>>,
    treatedCounts: Map<String, Int>,
    unsyncedChanges: Map<String, Int>,
    collapsedCategories: Set<AlbumStatus>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onToggleCategory: (AlbumStatus) -> Unit
) {
    val state = rememberLazyListState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusOrder = listOf(AlbumStatus.IN_PROGRESS, AlbumStatus.NOT_STARTED, AlbumStatus.COMPLETED, AlbumStatus.VIRTUAL)

                statusOrder.forEach { status ->
                    val albumsInStatus = groupedAlbums[status]
                    if (!albumsInStatus.isNullOrEmpty()) {
                        val isCollapsed = collapsedCategories.contains(status)
                        item(key = "group_${status.name}") {
                            val statusLabel = when(status) {
                                AlbumStatus.IN_PROGRESS -> stringResource(R.string.home_status_in_progress)
                                AlbumStatus.NOT_STARTED -> stringResource(R.string.home_status_not_started)
                                AlbumStatus.COMPLETED -> stringResource(R.string.home_status_completed)
                                AlbumStatus.VIRTUAL -> stringResource(R.string.home_status_virtual)
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleCategory(status) }
                                        .padding(top = 8.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = statusLabel,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (status == AlbumStatus.VIRTUAL)
                                            VirtualGold
                                        else
                                            MaterialTheme.colorScheme.primary
                                    )
                                    
                                    val rotation by animateFloatAsState(
                                        targetValue = if (isCollapsed) -90f else 0f,
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                        label = "chevronRotation"
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.graphicsLayer { rotationZ = rotation }
                                    )
                                }

                                AnimatedVisibility(
                                    visible = !isCollapsed,
                                    enter = expandVertically(
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    ) + fadeIn(
                                        animationSpec = tween(durationMillis = 300)
                                    ),
                                    exit = shrinkVertically(
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    ) + fadeOut(
                                        animationSpec = tween(durationMillis = 200)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        albumsInStatus.forEach { album ->
                                            AlbumItem(
                                                album = album,
                                                treatedCount = treatedCounts[album.id] ?: 0,
                                                unsyncedCount = unsyncedChanges[album.id] ?: 0,
                                                onClick = { onAlbumClick(album) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val scrollFraction by remember {
            derivedStateOf {
                val layoutInfo = state.layoutInfo
                if (layoutInfo.totalItemsCount > 0 && layoutInfo.visibleItemsInfo.isNotEmpty()) {
                    val firstItem = layoutInfo.visibleItemsInfo.first()
                    val totalItems = layoutInfo.totalItemsCount
                    (firstItem.index + (-firstItem.offset.toFloat() / firstItem.size.coerceAtLeast(1).toFloat())) / totalItems.toFloat()
                } else 0f
            }
        }
        val visibleFraction by remember {
            derivedStateOf {
                val layoutInfo = state.layoutInfo
                if (layoutInfo.totalItemsCount > 0 && layoutInfo.visibleItemsInfo.isNotEmpty()) {
                    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                    val averageItemSize = layoutInfo.visibleItemsInfo.sumOf { it.size }.toFloat() / layoutInfo.visibleItemsInfo.size
                    (viewportHeight / averageItemSize) / layoutInfo.totalItemsCount
                } else 1.0f
            }
        }
        
        val animatedOffset by animateFloatAsState(targetValue = scrollFraction, label = "scrollbarOffset")
        val animatedHeight by animateFloatAsState(targetValue = visibleFraction.coerceIn(0.05f, 1.0f), label = "scrollbarHeight")

        if (visibleFraction < 1.0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, top = 16.dp, bottom = 16.dp)
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val height = maxHeight * animatedHeight
                    val offset = maxHeight * animatedOffset
                    Box(
                        modifier = Modifier
                            .offset(y = offset.coerceAtMost(maxHeight - height))
                            .fillMaxWidth()
                            .height(height)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumItem(
    album: Album,
    treatedCount: Int,
    unsyncedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val baseUrl = remember { SessionManager.getBaseUrl()?.removeSuffix("/") }
    val apiKey = remember { SessionManager.getApiKey() ?: "" }
    val progress = if (album.assetCount > 0) treatedCount.toFloat() / album.assetCount else 0f
    val isCompleted = album.assetCount in 1..treatedCount
    val isNotStarted = treatedCount == 0
    val hasUnsyncedChanges = unsyncedCount > 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(60.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        if (album.albumThumbnailAssetId != null && baseUrl != null) {
                            val imageRequest = remember(album.albumThumbnailAssetId, baseUrl, apiKey) {
                                ImageRequest.Builder(context)
                                    .data("$baseUrl/api/assets/${album.albumThumbnailAssetId}/thumbnail?format=WEBP&size=thumbnail")
                                    .addHeader("x-api-key", apiKey)
                                    .crossfade(true)
                                    .precision(Precision.INEXACT)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build()
                            }

                            AsyncImage(
                                model = imageRequest,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                placeholder = rememberVectorPainter(Icons.Default.PhotoLibrary),
                                error = rememberVectorPainter(Icons.Default.PhotoLibrary)
                            )
                        } else {
                            val (icon, brush, tint) = getVirtualCollectionStyle(album.id)
                            Box(
                                modifier = Modifier.fillMaxSize().background(brush),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = tint
                                )
                            }
                        }
                    }
                    
                    if (isCompleted) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp),
                            color = Color(0xFF388E3C),
                            shape = CircleShape,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp).padding(2.dp))
                        }
                    } else if (isNotStarted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(14.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = album.albumName, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        if (isCompleted) {
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.home_album_completed), fontSize = 10.sp, color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (hasUnsyncedChanges) {
                        Text(
                            text = stringResource(R.string.home_unsynced_changes, unsyncedCount),
                            fontSize = 11.sp,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!album.description.isNullOrBlank()) {
                        Text(text = album.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 3, lineHeight = 14.sp)
                    }
                    Text(
                        text = stringResource(R.string.home_sorted_count, treatedCount, album.assetCount),
                        fontSize = 12.sp,
                        color = if (isCompleted) Color(0xFF388E3C) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
            }

            if (progress > 0 && !isCompleted) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            }
        }
    }
}
