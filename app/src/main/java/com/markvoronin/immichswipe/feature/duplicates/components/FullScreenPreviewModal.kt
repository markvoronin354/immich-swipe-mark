package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markvoronin.immichswipe.core.ImmichLauncher
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision
import com.markvoronin.immichswipe.feature.settings.components.horizontalFadingEdges
import kotlin.math.abs

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

    val assets = previewData.cluster.assets
    if (assets.isEmpty()) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val baseUrlClean = baseUrl.removeSuffix("/")

    val currentOnDecisionToggle by rememberUpdatedState(onDecisionToggle)
    val currentOnFavoriteToggle by rememberUpdatedState(onFavoriteToggle)

    val initialIndex = remember(previewData) { previewData.initialIndex.coerceIn(0, assets.size - 1) }
    var selectedIndex by remember(previewData) { mutableIntStateOf(initialIndex) }
    val carouselState = remember(previewData) {
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
                                ImmichLauncher.openAssetInImmich(context, baseUrlClean, currentAsset.id)
                            },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in Immich",
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

                val imageRequest = remember(currentAsset.id, baseUrlClean, apiKey) {
                    ImageRequest.Builder(context)
                        .data("$baseUrlClean/api/assets/${currentAsset.id}/original")
                        .addHeader("x-api-key", apiKey)
                        .crossfade(false)
                        .build()
                }

                if (currentAsset.type == "VIDEO") {
                    DuplicateVideoPlayer(
                        asset = currentAsset,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val containerWidthPx = with(density) { maxWidth.toPx() }
                        val containerHeightPx = with(density) { maxHeight.toPx() }
                        var totalHorizontalSwipe by remember { mutableFloatStateOf(0f) }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(currentAsset.id) {
                                    detectTapGestures(
                                        onTap = {
                                            currentOnDecisionToggle(currentAsset.id)
                                        },
                                        onDoubleTap = {
                                            if (scale > 1.05f) {
                                                scale = 1f
                                                panX = 0f
                                                panY = 0f
                                            } else {
                                                scale = 2.5f
                                                panX = 0f
                                                panY = 0f
                                            }
                                        }
                                    )
                                }
                                .pointerInput(currentAsset.id) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                                        scale = newScale
                                        if (newScale > 1.05f) {
                                            val maxPanX = (containerWidthPx * (newScale - 1f)) / 2f
                                            val maxPanY = (containerHeightPx * (newScale - 1f)) / 2f
                                            panX = (panX + pan.x).coerceIn(-maxPanX, maxPanX)
                                            panY = (panY + pan.y).coerceIn(-maxPanY, maxPanY)
                                        } else {
                                            panX = 0f
                                            panY = 0f
                                        }
                                    }
                                }
                                .pointerInput(currentAsset.id, scale) {
                                    if (scale <= 1.05f) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = { totalHorizontalSwipe = 0f },
                                            onDragCancel = { totalHorizontalSwipe = 0f },
                                            onHorizontalDrag = { change, dragAmount ->
                                                totalHorizontalSwipe += dragAmount
                                                if (totalHorizontalSwipe < -100f) {
                                                    if (selectedIndex < assets.size - 1) {
                                                        selectedIndex++
                                                        totalHorizontalSwipe = 0f
                                                    }
                                                } else if (totalHorizontalSwipe > 100f) {
                                                    if (selectedIndex > 0) {
                                                        selectedIndex--
                                                        totalHorizontalSwipe = 0f
                                                    }
                                                }
                                                change.consume()
                                            }
                                        )
                                    }
                                }
                                .pointerInput(currentAsset.id, scale) {
                                    if (scale <= 1.05f) {
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
                                }
                        ) {
                            AsyncImage(
                                model = imageRequest,
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
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .navigationBarsPadding()
                        .padding(vertical = 8.dp),
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Swipe down to dismiss",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
