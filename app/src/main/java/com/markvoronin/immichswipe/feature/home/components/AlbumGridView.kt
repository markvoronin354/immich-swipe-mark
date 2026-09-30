package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.markvoronin.immichswipe.feature.settings.components.verticalFadingEdges
import com.markvoronin.immichswipe.ui.theme.VirtualGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumGrid(
    groupedAlbums: Map<AlbumStatus, List<Album>>,
    treatedCounts: Map<String, Int>,
    unsyncedChanges: Map<String, Int>,
    collapsedCategories: Set<AlbumStatus>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onToggleCategory: (AlbumStatus) -> Unit
) {
    val state = rememberLazyGridState()

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                state = state,
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalFadingEdges(state, length = 32.dp),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusOrder = listOf(AlbumStatus.IN_PROGRESS, AlbumStatus.NOT_STARTED, AlbumStatus.COMPLETED, AlbumStatus.VIRTUAL)

                statusOrder.forEach { status ->
                    val albumsInStatus = groupedAlbums[status]
                    if (!albumsInStatus.isNullOrEmpty()) {
                        val isCollapsed = collapsedCategories.contains(status)
                        item(span = { GridItemSpan(maxLineSpan) }, key = "group_${status.name}") {
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
                                        albumsInStatus.chunked(3).forEach { rowAlbums ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                rowAlbums.forEach { album ->
                                                    AlbumGridItem(
                                                        album = album,
                                                        treatedCount = treatedCounts[album.id] ?: 0,
                                                        unsyncedCount = unsyncedChanges[album.id] ?: 0,
                                                        onClick = { onAlbumClick(album) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                repeat(3 - rowAlbums.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
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
                    (firstItem.index / 3f + (-firstItem.offset.y.toFloat() / firstItem.size.height.coerceAtLeast(1).toFloat())) / (totalItems / 3f)
                } else 0f
            }
        }
        val visibleFraction by remember {
            derivedStateOf {
                val layoutInfo = state.layoutInfo
                if (layoutInfo.totalItemsCount > 0 && layoutInfo.visibleItemsInfo.isNotEmpty()) {
                    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                    val averageItemHeight = layoutInfo.visibleItemsInfo.sumOf { it.size.height }.toFloat() / layoutInfo.visibleItemsInfo.size
                    (viewportHeight / averageItemHeight) / (layoutInfo.totalItemsCount / 3f)
                } else 1.0f
            }
        }

        val animatedOffset by animateFloatAsState(targetValue = scrollFraction.coerceIn(0f, 1f), label = "scrollbarOffset")
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
fun AlbumGridItem(
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
    val hasUnsyncedChanges = unsyncedCount > 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (album.albumThumbnailAssetId != null && baseUrl != null) {
                val imageRequest = remember(album.albumThumbnailAssetId, baseUrl, apiKey) {
                    ImageRequest.Builder(context)
                        .data("$baseUrl/api/assets/${album.albumThumbnailAssetId}/thumbnail?format=WEBP")
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
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isCompleted) Modifier.alpha(0.8f) else Modifier)
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
                        tint = tint,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            if (isCompleted) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = Color(0xFF388E3C),
                    shape = CircleShape,
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp).size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                if (hasUnsyncedChanges) {
                    Text(
                        text = stringResource(R.string.home_unsynced_badge),
                        color = Color(0xFFD32F2F),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = album.albumName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.home_sorted_count, treatedCount, album.assetCount),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }

            if (progress > 0 && !isCompleted) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
