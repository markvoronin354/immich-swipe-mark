package com.markvoronin.immichswipe.feature.swipe

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.components.DuplicateVideoPlayer
import kotlinx.coroutines.launch


@Composable
fun SummaryDialog(
    uiState: SwipeUiState,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onUndoDecision: (String) -> Unit
) {
    var previewAsset by remember { mutableStateOf<Asset?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close)
                    )
                }
                Text(
                    text = stringResource(R.string.swipe_summary_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 36.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatSummaryBox(
                        label = stringResource(R.string.swipe_keep),
                        count = uiState.keptCount,
                        size = uiState.keptSize,
                        color = MaterialGreen,
                        icon = Icons.Default.Check,
                        modifier = Modifier.weight(1f),
                        isEstimated = uiState.isRemainingEstimated
                    )
                    StatSummaryBox(
                        label = stringResource(R.string.swipe_delete),
                        count = uiState.deletedCount,
                        size = uiState.deletedSize,
                        color = MaterialRed,
                        icon = Icons.Default.Delete,
                        modifier = Modifier.weight(1f),
                        isEstimated = uiState.isRemainingEstimated
                    )
                }

                if (uiState.showArchiveButton || uiState.showLockButton) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.showArchiveButton) {
                            StatSummaryBox(
                                label = stringResource(R.string.swipe_archive),
                                count = uiState.archiveCount,
                                size = uiState.archiveSize,
                                color = Color(0xFF1565C0),
                                icon = Icons.Default.Archive,
                                modifier = Modifier.weight(1f),
                                isEstimated = uiState.isRemainingEstimated
                            )
                        }
                        if (uiState.showLockButton) {
                            StatSummaryBox(
                                label = stringResource(R.string.swipe_locked),
                                count = uiState.lockedCount,
                                size = uiState.lockedSize,
                                color = Color(0xFF7B1FA2),
                                icon = Icons.Default.Lock,
                                modifier = Modifier.weight(1f),
                                isEstimated = uiState.isRemainingEstimated
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.swipe_check_before_delete),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val isLoadingDeletedAssets = uiState.deletedCount > uiState.deletedAssets.size
                    if (isLoadingDeletedAssets) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${uiState.deletedAssets.size} / ${uiState.deletedCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (uiState.deletedAssets.isNotEmpty()) {
                        Text(
                            "${uiState.deletedAssets.size} photos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.deletedAssets.isNotEmpty()) {
                    val isLoadingDeletedAssets = uiState.deletedCount > uiState.deletedAssets.size
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        ) {
                            items(
                                items = uiState.deletedAssets,
                                key = { it.id }
                            ) { asset ->
                                DeletedAssetThumbnail(
                                    asset = asset,
                                    uiState = uiState,
                                    onClick = { previewAsset = asset },
                                    modifier = Modifier.animateItem()
                                )
                            }
                            if (isLoadingDeletedAssets) {
                                item(key = "loading_indicator") {
                                    Surface(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp)),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.5.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (uiState.deletedCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.swipe_no_deletions),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                if (uiState.isSyncing) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(CircleShape),
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        },
        confirmButton = {
            val hasChanges = uiState.processedCount > 0 || uiState.localFavorites.isNotEmpty()

            Button(
                onClick = onApply,
                enabled = !uiState.isSyncing && hasChanges,
                colors = ButtonDefaults.buttonColors(containerColor = if (uiState.deletedCount > 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                val totalSize = formatSize(uiState.deletedSize)
                Text(
                    text = if (uiState.isSyncing) stringResource(R.string.swipe_syncing) else stringResource(R.string.swipe_liberate_button, totalSize, uiState.deletedCount),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        shape = RoundedCornerShape(28.dp)
    )

    if (previewAsset != null) {
        SummaryFullscreenPreviewDialog(
            initialAsset = previewAsset!!,
            assets = if (uiState.deletedAssets.isNotEmpty()) uiState.deletedAssets else listOf(previewAsset!!),
            uiState = uiState,
            onDismiss = { previewAsset = null },
            onRevert = { assetToRevert ->
                onUndoDecision(assetToRevert.id)
            }
        )
    }
}

@Composable
fun StatSummaryBox(
    label: String,
    count: Int,
    size: Long,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isEstimated: Boolean = false,
) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.padding(10.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = 0.35f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            )
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 1f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isEstimated) "~ ${formatSize(size)}" else formatSize(size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun DeletedAssetThumbnail(
    asset: Asset,
    uiState: SwipeUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val baseUrl = uiState.baseUrl.removeSuffix("/")
    val apiKey = uiState.apiKey

    val hasHeart = uiState.isFavorite(asset.id)
    val hasArchive = asset.isArchived
    val hasLock = asset.isLocked

    val imageRequest = remember(asset.id, baseUrl, apiKey) {
        ImageRequest.Builder(context)
            .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP")
            .addHeader("x-api-key", apiKey)
            .crossfade(true)
            .precision(Precision.INEXACT)
            .build()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (hasHeart) TimelineMiniBadge(Icons.Default.Favorite, Color.Red)
            if (hasArchive) TimelineMiniBadge(Icons.Default.Archive, Color.Black)
            if (hasLock) TimelineMiniBadge(Icons.Default.Lock, Color.Black)
        }

        if (asset.type == "VIDEO") {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            )
        } else if (asset.isGif) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(3.dp))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "GIF",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 10.sp
                )
            }
        }

        val assetSize = asset.exifInfo?.fileSizeInBytes ?: 0L
        if (assetSize > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
                    .padding(4.dp)
            ) {
                Text(
                    text = formatSize(assetSize),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun SummaryFullscreenPreviewDialog(
    initialAsset: Asset,
    assets: List<Asset>,
    uiState: SwipeUiState,
    onDismiss: () -> Unit,
    onRevert: (Asset) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current

        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, dialogView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val baseUrlClean = uiState.baseUrl.removeSuffix("/")
        val apiKey = uiState.apiKey

        val initialIndex = remember(initialAsset.id) {
            assets.indexOfFirst { it.id == initialAsset.id }.coerceAtLeast(0)
        }
        val pagerState = rememberPagerState(initialPage = initialIndex) { assets.size }
        val currentAsset = assets.getOrNull(pagerState.currentPage) ?: initialAsset

        var isZoomedIn by remember(currentAsset.id) { mutableStateOf(false) }
        var dismissOffsetY by remember(currentAsset.id) { mutableFloatStateOf(0f) }

        val animOffsetY by animateFloatAsState(
            targetValue = dismissOffsetY,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "DismissOffsetY"
        )

        val bgAlpha = (1f - (animOffsetY / 600f)).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = bgAlpha))
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isZoomedIn,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = animOffsetY
                    }
                    .pointerInput(currentAsset.id, isZoomedIn) {
                        if (isZoomedIn) return@pointerInput
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dismissOffsetY > 150f) {
                                    onDismiss()
                                } else {
                                    dismissOffsetY = 0f
                                }
                            },
                            onDragCancel = {
                                dismissOffsetY = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount > 0f || dismissOffsetY > 0f) {
                                    dismissOffsetY = (dismissOffsetY + dragAmount).coerceAtLeast(0f)
                                    change.consume()
                                }
                            }
                        )
                    }
            ) { page ->
                val pageAsset = assets.getOrNull(page) ?: currentAsset
                ZoomableBox(
                    modifier = Modifier.fillMaxSize(),
                    resetOnRelease = false,
                    onIsZoomedChanged = { isZoomedIn = it }
                ) {
                    if (pageAsset.type == "VIDEO") {
                        DuplicateVideoPlayer(
                            asset = pageAsset,
                            baseUrl = baseUrlClean,
                            apiKey = apiKey,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        val photoRequest = remember(pageAsset.id, baseUrlClean, apiKey, pageAsset.isGif) {
                            ImageRequest.Builder(context)
                                .data(
                                    if (pageAsset.isGif) "$baseUrlClean/api/assets/${pageAsset.id}/original"
                                    else "$baseUrlClean/api/assets/${pageAsset.id}/thumbnail?format=WEBP&size=preview"
                                )
                                .addHeader("x-api-key", apiKey)
                                .crossfade(true)
                                .build()
                        }

                        AsyncImage(
                            model = photoRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val fileSize = currentAsset.exifInfo?.fileSizeInBytes ?: 0L
                    if (fileSize > 0) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = formatSize(fileSize),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    if (assets.size > 1) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${assets.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom Action Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (currentAsset.type == "VIDEO") 88.dp else 24.dp)
            ) {
                Button(
                    onClick = {
                        val assetToRevert = currentAsset
                        onRevert(assetToRevert)
                        if (assets.size <= 1) {
                            onDismiss()
                        } else {
                            val nextTargetPage = (pagerState.currentPage).coerceAtMost(assets.size - 2)
                            if (nextTargetPage >= 0) {
                                scope.launch {
                                    pagerState.scrollToPage(nextTargetPage)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.swipe_revert_delete),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
