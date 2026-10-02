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
import androidx.compose.material.icons.filled.Pending
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
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.components.DuplicateVideoPlayer


@Composable
fun SummaryDialog(
    uiState: SwipeUiState,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onUndoDecision: (String) -> Unit
) {
    var previewAsset by remember { mutableStateOf<Asset?>(null) }

    AlertDialog(
        onDismissRequest = if (uiState.isSyncing) ({}) else onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, enabled = !uiState.isSyncing) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                }
                Text(
                    text = stringResource(R.string.swipe_summary_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.albumName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    val stats = listOf(
                        Triple(stringResource(R.string.swipe_keep), Triple(uiState.keptCount, uiState.keptSize, MaterialGreen), Icons.Default.Check),
                        Triple(stringResource(R.string.swipe_delete), Triple(uiState.deletedCount, uiState.deletedSize, MaterialRed), Icons.Default.Delete),
                        Triple(stringResource(R.string.swipe_archive), Triple(uiState.archiveCount, uiState.archiveSize, MaterialTheme.colorScheme.primary), Icons.Default.Archive),
                        Triple(stringResource(R.string.swipe_locked), Triple(uiState.lockedCount, uiState.lockedSize, MaterialTheme.colorScheme.outline), Icons.Default.Lock),
                        Triple(stringResource(R.string.swipe_remaining), Triple(uiState.remainingCount, uiState.remainingSize, MaterialTheme.colorScheme.outlineVariant), Icons.Default.Pending)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0 until 2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val left = stats[i * 2]
                                val right = stats[i * 2 + 1]
                                StatSummaryBox(
                                    label = left.first,
                                    count = left.second.first,
                                    size = left.second.second,
                                    color = left.second.third,
                                    icon = left.third,
                                    isEstimated = left.first == stringResource(R.string.swipe_remaining) && uiState.isRemainingEstimated,
                                    modifier = Modifier.weight(1f)
                                )
                                StatSummaryBox(
                                    label = right.first,
                                    count = right.second.first,
                                    size = right.second.second,
                                    color = right.second.third,
                                    icon = right.third,
                                    isEstimated = right.first == stringResource(R.string.swipe_remaining) && uiState.isRemainingEstimated,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        val last = stats.last()
                        StatSummaryBox(
                            label = last.first,
                            count = last.second.first,
                            size = last.second.second,
                            color = last.second.third,
                            icon = last.third,
                            isEstimated = last.first == stringResource(R.string.swipe_remaining) && uiState.isRemainingEstimated,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.swipe_check_before_delete),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                val deletedAssets = remember(uiState) {
                    uiState.deletedAssets
                }
                val isLoadingDeletedAssets = uiState.deletedCount > 0 && deletedAssets.size < uiState.deletedCount

                if (deletedAssets.isNotEmpty()) {
                    Box(modifier = Modifier.height(220.dp).fillMaxWidth()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(
                                items = deletedAssets,
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
            asset = previewAsset!!,
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
    asset: Asset,
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
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        val context = LocalContext.current
        val baseUrlClean = uiState.baseUrl.removeSuffix("/")
        val apiKey = uiState.apiKey

        var isZoomedIn by remember(asset.id) { mutableStateOf(false) }
        var dismissOffsetY by remember(asset.id) { mutableFloatStateOf(0f) }

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = animOffsetY
                    }
                    .pointerInput(asset.id, isZoomedIn) {
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
            ) {
                ZoomableBox(
                    modifier = Modifier.fillMaxSize(),
                    resetOnRelease = false,
                    onIsZoomedChanged = { isZoomedIn = it }
                ) {
                    if (asset.type == "VIDEO") {
                        DuplicateVideoPlayer(
                            asset = asset,
                            baseUrl = baseUrlClean,
                            apiKey = apiKey,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        val photoRequest = remember(asset.id, baseUrlClean, apiKey) {
                            ImageRequest.Builder(context)
                                .data("$baseUrlClean/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview")
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
                val fileSize = asset.exifInfo?.fileSizeInBytes ?: 0L
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
                } else {
                    Spacer(Modifier.width(1.dp))
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
                    .padding(bottom = if (asset.type == "VIDEO") 88.dp else 24.dp)
            ) {
                Button(
                    onClick = {
                        onRevert(asset)
                        onDismiss()
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
