package com.markvoronin.immichswipe.feature.duplicates

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.ui.theme.VirtualGold
import kotlinx.coroutines.launch
import java.util.Locale

data class ZoomData(
    val asset: Asset,
    val decision: DuplicateDecision,
    val initialBounds: Rect,
    val scale: Float,
    val offset: Offset,
    val isGestureActive: Boolean
)

data class FullScreenPreviewData(
    val cluster: DuplicateClusterUiModel,
    val initialIndex: Int
)

@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeZoomData by remember { mutableStateOf<ZoomData?>(null) }
    var fullScreenPreviewData by remember { mutableStateOf<FullScreenPreviewData?>(null) }
    var rootWindowOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootWindowOffset = coords.positionInWindow()
            }
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.clusters.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = VirtualGold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No duplicates found!",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Duplicates",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val deleteCount = uiState.decisions.count { it.value == DuplicateDecision.DELETE }
                    Button(
                        onClick = { viewModel.toggleDeleteConfirmation(true) },
                        enabled = (!uiState.isSyncing) && (deleteCount > 0),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onError,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Delete $deleteCount")
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.autoSelect(keepLargest = true) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Keep Largest", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = { viewModel.autoSelect(keepLargest = false) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Keep Smallest", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearAllDecisions() },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.clusters, key = { it.clusterId }) { cluster ->
                        DuplicateClusterCard(
                            cluster = cluster,
                            decisions = uiState.decisions,
                            activeZoomAssetId = activeZoomData?.asset?.id,
                            onDecisionToggle = { assetId -> viewModel.toggleDecision(assetId) },
                            onAssetLongPress = { asset ->
                                val index = cluster.assets.indexOfFirst { it.id == asset.id }.coerceAtLeast(0)
                                fullScreenPreviewData = FullScreenPreviewData(cluster = cluster, initialIndex = index)
                            },
                            onZoomStateUpdate = { zoomData ->
                                activeZoomData = zoomData
                            }
                        )
                    }
                }
            }

            if (uiState.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        if (uiState.showDeleteConfirmation) {
            val deleteCount = uiState.decisions.count { it.value == DuplicateDecision.DELETE }
            AlertDialog(
                onDismissRequest = { viewModel.toggleDeleteConfirmation(false) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        text = "Delete $deleteCount photo${if (deleteCount > 1) "s" else ""}?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("These duplicate photos will be moved to the trash on your Immich server.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.toggleDeleteConfirmation(false)
                            viewModel.syncDeletions()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.toggleDeleteConfirmation(false) }
                    ) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        val activeAssetId = activeZoomData?.asset?.id
        val currentDecision = if (activeAssetId != null) {
            uiState.decisions[activeAssetId] ?: activeZoomData?.decision ?: DuplicateDecision.NONE
        } else {
            DuplicateDecision.NONE
        }

        // Instagram-style full screen pop-out overlay
        InstagramZoomOverlay(
            zoomData = activeZoomData,
            decision = currentDecision,
            rootWindowOffset = rootWindowOffset,
            onDismiss = { activeZoomData = null }
        )

        // Full-screen high quality preview modal
        FullScreenPreviewModal(
            previewData = fullScreenPreviewData,
            decisions = uiState.decisions,
            isFavorite = { asset -> uiState.isFavorite(asset) },
            onDecisionToggle = { assetId -> viewModel.toggleDecision(assetId) },
            onFavoriteToggle = { asset -> viewModel.toggleFavorite(asset) },
            onDismiss = { fullScreenPreviewData = null }
        )
    }
}

@Composable
fun DuplicateClusterCard(
    cluster: DuplicateClusterUiModel,
    decisions: Map<String, DuplicateDecision>,
    activeZoomAssetId: String?,
    onDecisionToggle: (String) -> Unit,
    onAssetLongPress: (Asset) -> Unit,
    onZoomStateUpdate: (ZoomData?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${cluster.assets.size} similar photos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (cluster.assets.size <= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cluster.assets.forEach { asset ->
                        val decision = decisions[asset.id] ?: DuplicateDecision.NONE
                        val isBeingZoomed = activeZoomAssetId == asset.id
                        DuplicateAssetItem(
                            asset = asset,
                            decision = decision,
                            isBeingZoomed = isBeingZoomed,
                            modifier = Modifier.weight(1f),
                            onToggle = { onDecisionToggle(asset.id) },
                            onLongPress = { onAssetLongPress(asset) },
                            onZoomStateUpdate = onZoomStateUpdate
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(cluster.assets, key = { it.id }) { asset ->
                        val decision = decisions[asset.id] ?: DuplicateDecision.NONE
                        val isBeingZoomed = activeZoomAssetId == asset.id
                        DuplicateAssetItem(
                            asset = asset,
                            decision = decision,
                            isBeingZoomed = isBeingZoomed,
                            modifier = Modifier.width(135.dp),
                            onToggle = { onDecisionToggle(asset.id) },
                            onLongPress = { onAssetLongPress(asset) },
                            onZoomStateUpdate = onZoomStateUpdate
                        )
                    }
                }
            }
        }
    }
}

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
                        var lastPressedCount = 0

                        do {
                            val event = awaitPointerEvent()
                            val pressedPointers = event.changes.filter { it.pressed }
                            val pressedCount = pressedPointers.size

                            if (pressedCount >= 2) {
                                if (!activeZooming) {
                                    activeZooming = true
                                }
                            }

                            if (activeZooming) {
                                if (pressedCount >= 1) {
                                    if (pressedCount == lastPressedCount) {
                                        val zoom = if (pressedCount >= 2) event.calculateZoom() else 1f
                                        val pan = event.calculatePan()

                                        currentScale = (currentScale * zoom).coerceIn(1f, 5f)
                                        currentOffset += pan
                                    }

                                    lastPressedCount = pressedCount

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
                                } else {
                                    activeZooming = false
                                    lastPressedCount = 0
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
                            } else {
                                lastPressedCount = pressedCount
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

@Composable
fun InstagramZoomOverlay(
    zoomData: ZoomData?,
    decision: DuplicateDecision,
    rootWindowOffset: Offset,
    onDismiss: () -> Unit
) {
    if (zoomData == null) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val baseUrl = remember { SessionManager.getBaseUrl()?.removeSuffix("/") }
    val apiKey = remember { SessionManager.getApiKey() ?: "" }

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
            val imageRequest = remember(zoomData.asset.id, baseUrl, apiKey) {
                ImageRequest.Builder(context)
                    .data("$baseUrl/api/assets/${zoomData.asset.id}/thumbnail?format=WEBP&size=preview")
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

@Composable
fun FullScreenPreviewModal(
    previewData: FullScreenPreviewData?,
    decisions: Map<String, DuplicateDecision>,
    isFavorite: (Asset) -> Boolean,
    onDecisionToggle: (String) -> Unit,
    onFavoriteToggle: (Asset) -> Unit,
    onDismiss: () -> Unit
) {
    if (previewData == null) return

    val assets = previewData.cluster.assets
    if (assets.isEmpty()) return

    val context = LocalContext.current
    val baseUrl = remember { SessionManager.getBaseUrl()?.removeSuffix("/") }
    val apiKey = remember { SessionManager.getApiKey() ?: "" }

    val currentOnDecisionToggle by rememberUpdatedState(onDecisionToggle)
    val currentOnFavoriteToggle by rememberUpdatedState(onFavoriteToggle)

    val pagerState = rememberPagerState(
        initialPage = previewData.initialIndex.coerceIn(0, assets.size - 1),
        pageCount = { assets.size }
    )

    var dismissOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingDismiss by remember { mutableStateOf(false) }

    val animDismissOffsetY by animateFloatAsState(
        targetValue = dismissOffsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "FullScreenDismiss"
    )

    val currentDismissOffsetY = if (isDraggingDismiss) dismissOffsetY else animDismissOffsetY
    val backdropAlpha = (1f - (currentDismissOffsetY / 800f)).coerceIn(0f, 0.95f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backdropAlpha))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { isDraggingDismiss = true },
                        onDragEnd = {
                            isDraggingDismiss = false
                            if (dismissOffsetY > 200f) {
                                onDismiss()
                            } else {
                                dismissOffsetY = 0f
                            }
                        },
                        onDragCancel = {
                            isDraggingDismiss = false
                            dismissOffsetY = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount > 0 || dismissOffsetY > 0) {
                                isDraggingDismiss = true
                                dismissOffsetY = (dismissOffsetY + dragAmount).coerceAtLeast(0f)
                                change.consume()
                            }
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = currentDismissOffsetY
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true
                ) { page ->
                    val asset = assets[page]
                    var scale by remember { mutableFloatStateOf(1f) }
                    var panX by remember { mutableFloatStateOf(0f) }
                    var panY by remember { mutableFloatStateOf(0f) }

                    val animScale by animateFloatAsState(
                        targetValue = scale,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                        label = "PageScale"
                    )
                    val animPanX by animateFloatAsState(
                        targetValue = panX,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                        label = "PagePanX"
                    )
                    val animPanY by animateFloatAsState(
                        targetValue = panY,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                        label = "PagePanY"
                    )

                    val fullImageRequest = remember(asset.id, baseUrl, apiKey) {
                        ImageRequest.Builder(context)
                            .data("$baseUrl/api/assets/${asset.id}/original")
                            .addHeader("x-api-key", apiKey)
                            .crossfade(true)
                            .build()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(asset.id) {
                                detectTapGestures(
                                    onTap = {
                                        currentOnDecisionToggle(asset.id)
                                    },
                                    onDoubleTap = {
                                        if (scale > 1.05f) {
                                            scale = 1f
                                            panX = 0f
                                            panY = 0f
                                        } else {
                                            scale = 3f
                                            panX = 0f
                                            panY = 0f
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    var lastPressedCount = 0

                                    do {
                                        val event = awaitPointerEvent()
                                        val pressedPointers = event.changes.filter { it.pressed }
                                        val pressedCount = pressedPointers.size

                                        if (pressedCount >= 2) {
                                            if (pressedCount == lastPressedCount) {
                                                val zoom = event.calculateZoom()
                                                val pan = event.calculatePan()

                                                scale = (scale * zoom).coerceIn(1f, 5f)
                                                if (scale > 1.05f) {
                                                    panX += pan.x
                                                    panY += pan.y
                                                } else {
                                                    panX = 0f
                                                    panY = 0f
                                                }
                                            }
                                            lastPressedCount = pressedCount
                                            event.changes.forEach { it.consume() }
                                        } else if (pressedCount == 1 && scale > 1.05f) {
                                            if (pressedCount == lastPressedCount) {
                                                val pan = event.calculatePan()
                                                panX += pan.x
                                                panY += pan.y
                                                event.changes.forEach { it.consume() }
                                            }
                                            lastPressedCount = pressedCount
                                        } else {
                                            lastPressedCount = pressedCount
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                    ) {
                        AsyncImage(
                            model = fullImageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = animScale
                                    scaleY = animScale
                                    translationX = animPanX
                                    translationY = animPanY
                                }
                        )
                    }
                }

                val currentAsset = assets.getOrNull(pagerState.currentPage) ?: assets.first()
                val currentDecision = decisions[currentAsset.id] ?: DuplicateDecision.NONE
                val currentIsFav = isFavorite(currentAsset)

                // Top overlay bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val sizeStr = currentAsset.exifInfo?.fileSizeInBytes?.let { formatSizeStr(it) } ?: ""
                        Text(
                            text = "${currentAsset.originalFileName ?: "Photo"} (${pagerState.currentPage + 1}/${assets.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (sizeStr.isNotEmpty()) {
                            Text(
                                text = sizeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Like / Favorite Button
                        IconButton(
                            onClick = { currentOnFavoriteToggle(currentAsset) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (currentIsFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (currentIsFav) Color.Red else Color.White
                            )
                        }

                        // Decision status badge
                        val badgeColor = when (currentDecision) {
                            DuplicateDecision.DELETE -> MaterialTheme.colorScheme.error
                            DuplicateDecision.KEEP -> Color(0xFF4CAF50)
                            DuplicateDecision.NONE -> Color.White.copy(alpha = 0.7f)
                        }
                        val badgeText = when (currentDecision) {
                            DuplicateDecision.DELETE -> "DELETE"
                            DuplicateDecision.KEEP -> "KEEP"
                            DuplicateDecision.NONE -> "NONE"
                        }

                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { currentOnDecisionToggle(currentAsset.id) }
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Down-swipe hint at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Swipe down to dismiss",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

fun formatSizeStr(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.2f MB", mb)
        else -> String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}
