package com.markvoronin.immichswipe.feature.duplicates.components

import com.markvoronin.immichswipe.R

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.markvoronin.immichswipe.core.ImmichLauncher
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision
import com.markvoronin.immichswipe.feature.settings.components.horizontalFadingEdges
import com.markvoronin.immichswipe.feature.swipe.components.ZoomableBox
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenPreviewModal(
    previewData: FullScreenPreviewData?,
    decisions: Map<String, DuplicateDecision>,
    isFavorite: (Asset) -> Boolean,
    baseUrl: String = "",
    apiKey: String = "",
    onDecisionToggle: (String) -> Unit,
    onFavoriteToggle: (Asset) -> Unit,
    onDismiss: () -> Unit
) {
    if (previewData == null) return

    val clusters = remember(previewData) {
        previewData.clusters.ifEmpty { listOf(previewData.cluster) }
    }
    var currentClusterIdx by remember(previewData) {
        mutableIntStateOf(previewData.clusterIndex.coerceIn(0, clusters.size - 1))
    }

    val currentCluster = clusters.getOrNull(currentClusterIdx) ?: previewData.cluster
    val assets = currentCluster.assets
    if (assets.isEmpty()) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val baseUrlClean = baseUrl.removeSuffix("/")

    val currentOnDecisionToggle by rememberUpdatedState(onDecisionToggle)
    val currentOnFavoriteToggle by rememberUpdatedState(onFavoriteToggle)

    val initialIndex = remember(currentCluster.clusterId, previewData) {
        if (currentCluster.clusterId == previewData.cluster.clusterId) {
            previewData.initialIndex.coerceIn(0, assets.size - 1)
        } else {
            0
        }
    }
    var selectedIndex by remember(currentCluster.clusterId) { mutableIntStateOf(initialIndex) }
    val carouselState = remember(currentCluster.clusterId) {
        LazyListState(firstVisibleItemIndex = initialIndex)
    }
    val isUserDragging by carouselState.interactionSource.collectIsDraggedAsState()

    var carouselWidthPx by remember { mutableIntStateOf(0) }
    val itemWidthDp = 54.dp
    val itemWidthPx = with(density) { itemWidthDp.roundToPx() }

    val sidePaddingDp = if (carouselWidthPx > 0) {
        val paddingPx = ((carouselWidthPx - itemWidthPx) / 2).coerceAtLeast(0)
        with(density) { paddingPx.toDp() }
    } else {
        16.dp
    }

    LaunchedEffect(carouselState, isUserDragging, carouselWidthPx) {
        if (!isUserDragging || carouselWidthPx <= 0) return@LaunchedEffect

        snapshotFlow {
            @Suppress("UNUSED_EXPRESSION")
            carouselState.firstVisibleItemIndex
            @Suppress("UNUSED_EXPRESSION")
            carouselState.firstVisibleItemScrollOffset

            val layoutInfo = carouselState.layoutInfo
            val contentStart = layoutInfo.beforeContentPadding
            val viewportCenter = layoutInfo.viewportSize.width / 2

            if (layoutInfo.viewportSize.width > 0) {
                layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    val itemCenter = contentStart + item.offset + item.size / 2
                    abs(itemCenter - viewportCenter)
                }?.index
            } else null
        }.collect { closestIndex ->
            if (closestIndex != null && closestIndex != selectedIndex) {
                selectedIndex = closestIndex
            }
        }
    }

    LaunchedEffect(selectedIndex, carouselWidthPx) {
        if (carouselWidthPx > 0 && !carouselState.isScrollInProgress) {
            carouselState.scrollToItem(selectedIndex, 0)
        }
    }

    var wasCarouselDragged by remember { mutableStateOf(false) }
    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            wasCarouselDragged = true
        } else if (wasCarouselDragged) {
            wasCarouselDragged = false
            if (carouselWidthPx > 0) {
                val layoutInfo = carouselState.layoutInfo
                val contentStart = layoutInfo.beforeContentPadding
                val viewportCenter = layoutInfo.viewportSize.width / 2
                if (layoutInfo.viewportSize.width > 0) {
                    val closest = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                        val itemCenter = contentStart + item.offset + item.size / 2
                        abs(itemCenter - viewportCenter)
                    }?.index
                    if (closest != null) {
                        selectedIndex = closest
                    }
                }
                carouselState.animateScrollToItem(selectedIndex, 0)
            }
        }
    }

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
        val dialogView = LocalView.current
        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, dialogView)
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                insetsController.isAppearanceLightStatusBars = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backdropAlpha))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = currentDismissOffsetY
                    }
            ) {
                val currentAsset = assets.getOrNull(selectedIndex) ?: assets.first()
                val currentDecision = decisions[currentAsset.id] ?: DuplicateDecision.NONE
                val currentIsFav = isFavorite(currentAsset)

                var isFileNameExpanded by remember(currentAsset.id) { mutableStateOf(false) }
                var showMetadataDialog by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isFileNameExpanded = !isFileNameExpanded
                            }
                    ) {
                        val sizeStr = currentAsset.exifInfo?.fileSizeInBytes?.let { formatSizeStr(it) } ?: ""
                        val originalName = currentAsset.originalFileName ?: "Photo"

                        val displayedName = if (isFileNameExpanded) {
                            originalName
                        } else {
                            formatMiddleEllipsisFileName(originalName, maxLength = 22)
                        }

                        Text(
                            text = displayedName,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = if (isFileNameExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
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

                        IconButton(
                            onClick = {
                                showMetadataDialog = true
                            },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Asset Info",
                                tint = Color.White
                            )
                        }

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

                if (showMetadataDialog) {
                    Dialog(onDismissRequest = { showMetadataDialog = false }) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.swipe_metadata_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(onClick = { showMetadataDialog = false }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.common_close)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                val originalName = currentAsset.originalFileName ?: stringResource(R.string.diag_unknown)
                                val formatLabel = if (currentAsset.fileExtension != null) {
                                    "${currentAsset.type} (.${currentAsset.fileExtension.lowercase()})"
                                } else currentAsset.type
                                val sizeLabel = currentAsset.exifInfo?.fileSizeInBytes?.let { formatSizeStr(it) } ?: "Unknown"
                                val resolutionLabel = currentAsset.exifInfo?.let {
                                    "${it.imageWidth ?: "?"} × ${it.imageHeight ?: "?"}"
                                } ?: "Unknown"
                                val dateLabel = currentAsset.effectiveDate.substringBefore("T")

                                MetadataRow(
                                    icon = Icons.Default.Description,
                                    label = stringResource(R.string.swipe_metadata_file),
                                    value = originalName
                                )
                                MetadataRow(
                                    icon = Icons.Default.CalendarToday,
                                    label = stringResource(R.string.swipe_metadata_date),
                                    value = dateLabel
                                )
                                MetadataRow(
                                    icon = Icons.Default.Info,
                                    label = stringResource(R.string.swipe_metadata_format),
                                    value = formatLabel
                                )
                                MetadataRow(
                                    icon = Icons.Default.SdStorage,
                                    label = stringResource(R.string.swipe_metadata_size),
                                    value = sizeLabel
                                )
                                MetadataRow(
                                    icon = Icons.Default.AspectRatio,
                                    label = stringResource(R.string.swipe_metadata_resolution),
                                    value = resolutionLabel
                                )

                                Spacer(Modifier.height(20.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = {
                                                ImmichLauncher.openAssetInImmich(context, baseUrlClean, currentAsset.id)
                                            },
                                            onLongClick = if (baseUrlClean.isNotBlank()) {
                                                {
                                                    ImmichLauncher.openInWeb(context, baseUrlClean, currentAsset.id)
                                                }
                                            } else null
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.settings_card_immich),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }

                                if (baseUrlClean.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.settings_immich_long_press_web),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }
                }

                var isZoomedIn by remember(currentAsset.id) { mutableStateOf(false) }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val containerHeightPx = with(density) { maxHeight.toPx() }

                    val swipeModifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentAsset.id, isZoomedIn) {
                            if (isZoomedIn) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                val startY = down.position.y
                                val controlsHeightPx = with(density) { 70.dp.toPx() }
                                val isTouchInControls = currentAsset.type == "VIDEO" && startY > (containerHeightPx - controlsHeightPx)

                                if (isTouchInControls) {
                                    // Touch started in video controls bar (Play/Pause, Seekbar, Mute). Let child controls handle it.
                                    return@awaitEachGesture
                                }

                                var isDraggingHorizontal = false
                                var isDraggingVertical = false
                                var totalDragX = 0f
                                var totalDragY = 0f
                                val touchSlop = viewConfiguration.touchSlop

                                do {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    if (event.changes.size == 1) {
                                        val change = event.changes[0]
                                        val panChange = event.calculatePan()
                                        totalDragX += panChange.x
                                        totalDragY += panChange.y

                                        val absX = abs(totalDragX)
                                        val absY = abs(totalDragY)

                                        if (!isDraggingHorizontal && !isDraggingVertical) {
                                            if (absX > touchSlop && absX > absY) {
                                                isDraggingHorizontal = true
                                            } else if (absY > touchSlop && absY > absX && totalDragY > 0f) {
                                                isDraggingVertical = true
                                                isDraggingDismiss = true
                                            }
                                        }

                                        if (isDraggingHorizontal) {
                                            change.consume()
                                        } else if (isDraggingVertical) {
                                            dismissOffsetY = (totalDragY - touchSlop).coerceAtLeast(0f)
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                if (isDraggingVertical) {
                                    isDraggingDismiss = false
                                    if (dismissOffsetY > 200f) {
                                        onDismiss()
                                    } else {
                                        dismissOffsetY = 0f
                                    }
                                } else if (isDraggingHorizontal) {
                                    if (totalDragX < -100f && selectedIndex < assets.size - 1) {
                                        selectedIndex++
                                    } else if (totalDragX > 100f && selectedIndex > 0) {
                                        selectedIndex--
                                    }
                                }
                            }
                        }

                    if (currentAsset.type == "VIDEO") {
                        DuplicateVideoPlayer(
                            asset = currentAsset,
                            baseUrl = baseUrlClean,
                            apiKey = apiKey,
                            modifier = swipeModifier,
                            contentScale = ContentScale.Fit,
                            onTap = {
                                currentOnDecisionToggle(currentAsset.id)
                            },
                            videoSurfaceWrapper = { videoSurface ->
                                ZoomableBox(
                                    modifier = Modifier.fillMaxSize(),
                                    resetOnRelease = false,
                                    enableDoubleTapZoom = false,
                                    onIsZoomedChanged = { zoomed ->
                                        isZoomedIn = zoomed
                                    },
                                    onTap = { _, _ ->
                                        currentOnDecisionToggle(currentAsset.id)
                                    }
                                ) {
                                    videoSurface()
                                }
                            }
                        )
                    } else {
                        ZoomableBox(
                            modifier = swipeModifier,
                            resetOnRelease = false,
                            enableDoubleTapZoom = false,
                            onIsZoomedChanged = { zoomed ->
                                isZoomedIn = zoomed
                            },
                            onTap = { _, _ ->
                                currentOnDecisionToggle(currentAsset.id)
                            }
                        ) {
                            val imageRequest = remember(currentAsset.id, baseUrlClean, apiKey) {
                                ImageRequest.Builder(context)
                                    .data("$baseUrlClean/api/assets/${currentAsset.id}/original")
                                    .addHeader("x-api-key", apiKey)
                                    .crossfade(false)
                                    .build()
                            }
                            SubcomposeAsyncImage(
                                model = imageRequest,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .navigationBarsPadding()
                        .padding(top = 8.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clickable { currentOnDecisionToggle(currentAsset.id) }
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(badgeColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    LazyRow(
                        state = carouselState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .horizontalFadingEdges(carouselState, length = 24.dp)
                            .onSizeChanged { size ->
                                carouselWidthPx = size.width
                            },
                        contentPadding = PaddingValues(horizontal = sidePaddingDp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(assets, key = { _, asset -> asset.id }) { index, asset ->
                            val isSelected = index == selectedIndex
                            val assetDecision = decisions[asset.id] ?: DuplicateDecision.NONE
                            val assetFav = isFavorite(asset)

                            val borderColor = when {
                                isSelected -> Color.White
                                assetDecision == DuplicateDecision.KEEP -> Color(0xFF4CAF50)
                                assetDecision == DuplicateDecision.DELETE -> MaterialTheme.colorScheme.error
                                else -> Color.White.copy(alpha = 0.3f)
                            }

                            Box(
                                modifier = Modifier
                                    .size(width = itemWidthDp, height = 68.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.DarkGray)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        selectedIndex = index
                                    }
                            ) {
                                val thumbRequest = remember(asset.id, baseUrlClean, apiKey, asset.isGif) {
                                    ImageRequest.Builder(context)
                                        .data(
                                            if (asset.isGif) "$baseUrlClean/api/assets/${asset.id}/original"
                                            else "$baseUrlClean/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview"
                                        )
                                        .addHeader("x-api-key", apiKey)
                                        .crossfade(false)
                                        .build()
                                }

                                AsyncImage(
                                    model = thumbRequest,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            alpha = if (isSelected) 1f else 0.6f
                                        }
                                )

                                if (assetDecision != DuplicateDecision.NONE) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(16.dp)
                                            .background(
                                                if (assetDecision == DuplicateDecision.DELETE) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (assetDecision == DuplicateDecision.DELETE) Icons.Default.Delete else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }

                                if (assetFav) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(2.dp)
                                            .size(12.dp)
                                    )
                                }

                                if (asset.type == "VIDEO") {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Video",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(2.dp)
                                            .size(12.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    )
                                }

                                val thumbSizeStr = asset.exifInfo?.fileSizeInBytes?.let { formatSizeStr(it) }
                                if (thumbSizeStr != null) {
                                    Text(
                                        text = thumbSizeStr,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .padding(horizontal = 2.dp, vertical = 1.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasPreviousGroup = currentClusterIdx > 0
                        IconButton(
                            onClick = { if (hasPreviousGroup) currentClusterIdx-- },
                            enabled = hasPreviousGroup,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (hasPreviousGroup) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Previous Group",
                                tint = if (hasPreviousGroup) Color.White else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (clusters.size > 1) {
                                    "Group ${currentClusterIdx + 1} of ${clusters.size}"
                                } else {
                                    "${selectedIndex + 1} / ${assets.size}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Swipe down to dismiss",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        val hasNextGroup = currentClusterIdx < clusters.size - 1
                        IconButton(
                            onClick = { if (hasNextGroup) currentClusterIdx++ },
                            enabled = hasNextGroup,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (hasNextGroup) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Next Group",
                                tint = if (hasNextGroup) Color.White else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .padding(top = 2.dp)
                .width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}
