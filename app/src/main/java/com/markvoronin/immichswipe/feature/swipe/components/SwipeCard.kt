package com.markvoronin.immichswipe.feature.swipe


import android.content.Intent
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.CardDisplayMode
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.cache.VideoCache
import com.markvoronin.immichswipe.domain.model.Asset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs


@OptIn(UnstableApi::class)
@Composable
fun SwipeCard(
    asset: Asset,
    isNext: Boolean,
    isFullscreenOpen: Boolean,
    providedPlayer: ExoPlayer? = null,
    isMuted: Boolean = false,
    topCardSwipeOffset: Float = 0f,
    config: SwipeCardConfig,
    actions: SwipeCardActions
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
    val apiKey = SessionManager.getApiKey() ?: ""
    val lifecycleOwner = LocalLifecycleOwner.current

    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // Directional lock for gestures
    var dragDirection by remember { mutableIntStateOf(0) } // 0: undecided, 1: horizontal, 2: vertical

    var isHoldingByPress by remember(asset.id) { mutableStateOf(false) }
    var wasHoldDetected by remember(asset.id) { mutableStateOf(false) }
    val currentIsHolding by rememberUpdatedState(isHoldingByPress || wasHoldDetected)
    var pausedByHoldState by remember(asset.id) { mutableStateOf(false) }
    var ignoreNextTap by remember(asset.id) { mutableStateOf(false) }

    var isVideoReady by remember(asset.id, providedPlayer) {
        val isSameAsset = providedPlayer?.currentMediaItem?.mediaId == asset.id
        mutableStateOf(isSameAsset && providedPlayer?.playbackState == Player.STATE_READY)
    }
    var showLoadingIndicator by remember(asset.id, providedPlayer) {
        val isSameAsset = providedPlayer?.currentMediaItem?.mediaId == asset.id
        mutableStateOf(asset.type == "VIDEO" && !(isSameAsset && providedPlayer?.playbackState == Player.STATE_READY))
    }
    var showMuteIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(showMuteIndicator) {
        if (showMuteIndicator) {
            delay(1000)
            showMuteIndicator = false
        }
    }

    var metadataHeightPx by remember { mutableStateOf(0f) }
    val configuration = LocalConfiguration.current
    val maxHeightPx = with(density) { (configuration.screenHeightDp * 0.6f).dp.toPx() }

    val internalExoPlayer = remember(asset.id, isNext, isFullscreenOpen) {
        if (asset.type == "VIDEO" && !isNext && providedPlayer == null && !isFullscreenOpen) {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(30_000, 120_000, 1_000, 1_000)
                .setBackBuffer(120_000, true)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            
            ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build().apply {
                if (config.playbackBehavior != PlaybackBehavior.IGNORE) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build()
                    setAudioAttributes(audioAttributes, true)
                }

                repeatMode = Player.REPEAT_MODE_ONE
                val videoUrl = "$baseUrl/api/assets/${asset.id}/video/playback"
                val dataSourceFactory = VideoCache.getCacheDataSourceFactory(context, apiKey)
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.Builder()
                        .setUri(videoUrl)
                        .setMediaId(asset.id)
                        .setCustomCacheKey(asset.id)
                        .build())
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }
        } else null
    }

    val exoPlayer = providedPlayer ?: internalExoPlayer

    LaunchedEffect(asset.id, isVideoReady) {
        AppLogger.d("SwipeCard", "Video readiness: ready=$isVideoReady, indicator=$showLoadingIndicator, asset=${asset.id}")
        if (isVideoReady) {
            showLoadingIndicator = false
        } else if (asset.type == "VIDEO" && !isNext) {
            delay(500)
            if (!isVideoReady) {
                showLoadingIndicator = true
            }
        }
    }

    // Mise à jour du volume quand isMuted change pour le player interne
    LaunchedEffect(internalExoPlayer, isMuted) {
        internalExoPlayer?.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(pausedByHoldState, exoPlayer, asset.id) {
        // Log to verify if state is changing
        if (pausedByHoldState) {
            exoPlayer?.pause()
        } else if (!isNext && !isFullscreenOpen) {
            exoPlayer?.play()
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner, asset.id) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isVideoReady = state == Player.STATE_READY
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) isVideoReady = true
            }
        }

        if (exoPlayer?.playbackState == Player.STATE_READY) {
            isVideoReady = true
        }
        exoPlayer?.addListener(listener)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Si on est en fullscreen, on laisse le fullscreenViewer gérer la pause
                    if (!isFullscreenOpen) exoPlayer?.playWhenReady = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!isNext && !isFullscreenOpen) exoPlayer?.playWhenReady = true
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        exoPlayer?.volume = if (isMuted) 0f else 1f
        if (pausedByHoldState) exoPlayer?.pause() else if (!isNext && !isFullscreenOpen) exoPlayer?.play()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.removeListener(listener)
            internalExoPlayer?.stop()
            internalExoPlayer?.release()
        }
    }

    LaunchedEffect(isNext) {
        if (!isNext) {
            snapshotFlow { offsetX.value }.collect { offset ->
                actions.onSwipeOffsetChanged(offset)
            }
        }
    }

    val swipeProgress = (abs(topCardSwipeOffset) / 500f).coerceIn(0f, 1f)
    val targetScale = if (isNext) 0.85f + (0.15f * swipeProgress) else 1f
    val targetAlpha = if (isNext) 0.6f + (0.4f * swipeProgress) else 1f

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ScaleAnimation"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "AlphaAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .alpha(animatedAlpha)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    if (!isNext) {
                        translationX = offsetX.value
                        rotationZ = offsetX.value / 40f
                    }
                }
                .pointerInput(isNext) {
                    if (isNext) return@pointerInput
                    var dragStartY = 0f
                    var dragStartX = 0f
                    detectDragGestures(
                        onDragStart = { 
                            dragDirection = 0
                            dragStartY = offsetY.value
                            dragStartX = offsetX.value
                        },
                        onDragEnd = {
                            scope.launch {
                                if (currentIsHolding) {
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    offsetY.animateTo(dragStartY, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    dragDirection = 0
                                    wasHoldDetected = false
                                    pausedByHoldState = false
                                    return@launch
                                }
                                val currentX = offsetX.value
                                val currentY = offsetY.value

                                if (dragDirection == 1) { // Horizontal swipe
                                    if (currentX > 250) {
                                        offsetX.animateTo(1500f, tween(150))
                                        actions.onSwipe(SwipeDecision.KEEP)
                                    } else if (currentX < -250) {
                                        offsetX.animateTo(-1500f, tween(150))
                                        actions.onSwipe(SwipeDecision.DELETE)
                                    } else {
                                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                } else if (dragDirection == 2) { // Vertical metadata
                                    // Snap based on final position
                                    if (currentY <= -metadataHeightPx * 0.4f) {
                                        offsetY.animateTo(-metadataHeightPx, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    } else {
                                        offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                } else {
                                    // Small movement, reset both
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    offsetY.animateTo(dragStartY, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                }
                                dragDirection = 0
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                if (currentIsHolding) {
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    offsetY.animateTo(dragStartY, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    dragDirection = 0
                                    wasHoldDetected = false
                                    pausedByHoldState = false
                                    return@launch
                                }
                                val currentY = offsetY.value
                                if (dragDirection == 2 || currentY < -10f) {
                                    if (currentY <= -metadataHeightPx * 0.4f) {
                                        offsetY.animateTo(-metadataHeightPx, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    } else {
                                        offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                } else {
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    offsetY.animateTo(dragStartY, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                }
                                dragDirection = 0
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (currentIsHolding) {
                                return@detectDragGestures
                            }
                            
                            if (dragDirection == 0) {
                                val accumulatedDX = abs(offsetX.value - dragStartX)
                                val accumulatedDY = abs(offsetY.value - dragStartY)
                                
                                // Decision threshold: 20 pixels of movement
                                if (accumulatedDX > 20 || accumulatedDY > 20) {
                                    // If panel is open or moving (dragStartY < -10f), strictly lock to vertical drag
                                    if (dragStartY < -10f && accumulatedDY > 5) {
                                        dragDirection = 2
                                    } else {
                                        dragDirection = if (accumulatedDX > accumulatedDY) 1 else 2
                                    }
                                }
                            }

                            scope.launch {
                                if (dragDirection == 1) {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                } else if (dragDirection == 2) {
                                    offsetY.snapTo((offsetY.value + dragAmount.y).coerceIn(-metadataHeightPx, 0f))
                                } else {
                                    // Track both until locked
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo((offsetY.value + dragAmount.y).coerceIn(-metadataHeightPx, 0f))
                                }
                            }
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 0.dp else 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
                if (asset.type == "VIDEO" && !isNext && exoPlayer != null) {
                    if (!isFullscreenOpen) {
                        ZoomableBox(
                            modifier = Modifier.fillMaxSize(),
                            resetOnRelease = true,
                            onTap = { offset, size ->
                                if (offsetY.value < -20f) {
                                    scope.launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
                                } else if (!ignoreNextTap) {
                                    val width = size.width.toFloat()
                                    if (config.tapToSwipeEnabled) {
                                        when {
                                            offset.x < width * 0.3f -> actions.onSwipe(SwipeDecision.DELETE)
                                            offset.x > width * 0.7f -> actions.onSwipe(SwipeDecision.KEEP)
                                            else -> {
                                                actions.onToggleMute()
                                                showMuteIndicator = true
                                            }
                                        }
                                    } else {
                                        actions.onToggleMute()
                                        showMuteIndicator = true
                                    }
                                }
                                ignoreNextTap = false
                            },
                            onDoubleTap = actions.onDoubleTap,
                            onPress = { offset, size ->
                                ignoreNextTap = false
                                wasHoldDetected = false
                                val wasReleased = withTimeoutOrNull(500) {
                                    awaitRelease()
                                    true
                                }
                                if (wasReleased == true) {
                                    // Fast tap detected
                                    if (offsetY.value < -20f) {
                                        scope.launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
                                        ignoreNextTap = true
                                    } else if (config.tapToSwipeEnabled) {
                                        val width = size.width.toFloat()
                                        when {
                                            offset.x < width * 0.3f -> {
                                                actions.onSwipe(SwipeDecision.DELETE)
                                                ignoreNextTap = true
                                            }
                                            offset.x > width * 0.7f -> {
                                                actions.onSwipe(SwipeDecision.KEEP)
                                                ignoreNextTap = true
                                            }
                                        }
                                    }
                                } else {
                                    // Hold detected
                                    ignoreNextTap = true
                                    isHoldingByPress = true
                                    wasHoldDetected = true
                                    pausedByHoldState = true
                                    try {
                                        awaitRelease()
                                        isHoldingByPress = false
                                        pausedByHoldState = false
                                        wasHoldDetected = false
                                    } catch (e: GestureCancellationException) {
                                        isHoldingByPress = false
                                    }
                                }
                            }
                        ) {
                            SharedVideoPlayer(
                                player = exoPlayer,
                                isFullscreen = false,
                                assetId = asset.id,
                                isMuted = isMuted,
                                isPaused = pausedByHoldState,
                                isVideoReady = isVideoReady,
                                showControls = !isHoldingByPress && !wasHoldDetected,
                                cardDisplayMode = config.cardDisplayMode,
                                fileSize = asset.exifInfo?.fileSizeInBytes,
                                showSize = config.showSizeIndicator
                            )

                            if (showLoadingIndicator) {
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
                        }
                    } else {
                        val placeholderRequest = remember(asset.id, baseUrl, apiKey) {
                            ImageRequest.Builder(context)
                                .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview")
                                .addHeader("x-api-key", apiKey)
                                .crossfade(true)
                                .precision(Precision.INEXACT)
                                .build()
                        }
                        AsyncImage(
                            model = placeholderRequest,
                            contentDescription = null,
                            contentScale = if (config.cardDisplayMode == CardDisplayMode.FILL) ContentScale.Crop else ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    val photoRequest = remember(asset.id, baseUrl, apiKey) {
                        ImageRequest.Builder(context)
                            .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview")
                            .addHeader("x-api-key", apiKey)
                            .crossfade(true)
                            .precision(Precision.INEXACT)
                            .build()
                    }
                    ZoomableBox(
                        modifier = Modifier.fillMaxSize(),
                        resetOnRelease = true,
                        enabled = !isNext,
                        aspectRatio = asset.exifInfo?.let { it.imageWidth?.toFloat()?.div(it.imageHeight?.toFloat() ?: 1f) },
                        isFillMode = config.cardDisplayMode == CardDisplayMode.FILL,
                        onTap = { offset, size ->
                            if (offsetY.value < -20f) {
                                scope.launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
                            } else if (!ignoreNextTap) {
                                val width = size.width.toFloat()
                                if (config.tapToSwipeEnabled) {
                                    when {
                                        offset.x < width * 0.3f -> actions.onSwipe(SwipeDecision.DELETE)
                                        offset.x > width * 0.7f -> actions.onSwipe(SwipeDecision.KEEP)
                                        else -> { /* Middle tap for photos */ }
                                    }
                                }
                            }
                            ignoreNextTap = false
                        },
                        onDoubleTap = actions.onDoubleTap,
                        onPress = { offset, size ->
                            ignoreNextTap = false
                            wasHoldDetected = false
                            val wasReleased = withTimeoutOrNull(500) {
                                awaitRelease()
                                true
                            }
                            if (wasReleased == true) {
                                // Fast tap detected
                                if (offsetY.value < -20f) {
                                    scope.launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
                                    ignoreNextTap = true
                                } else if (config.tapToSwipeEnabled) {
                                    val width = size.width.toFloat()
                                    when {
                                        offset.x < width * 0.3f -> {
                                            actions.onSwipe(SwipeDecision.DELETE)
                                            ignoreNextTap = true
                                        }
                                        offset.x > width * 0.7f -> {
                                            actions.onSwipe(SwipeDecision.KEEP)
                                            ignoreNextTap = true
                                        }
                                    }
                                }
                            } else {
                                // Hold detected
                                ignoreNextTap = true
                                isHoldingByPress = true
                                wasHoldDetected = true
                                try {
                                    awaitRelease()
                                    isHoldingByPress = false
                                    wasHoldDetected = false
                                } catch (e: GestureCancellationException) {
                                    isHoldingByPress = false
                                }
                            }
                        }
                    ) {
                        AsyncImage(
                            model = photoRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (config.showSizeIndicator && !isNext) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isHoldingByPress && !wasHoldDetected,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .padding(bottom = 12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = formatSize(asset.exifInfo?.fileSizeInBytes ?: 0L),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (!isNext) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .heightIn(max = with(density) { maxHeightPx.toDp() })
                            .onSizeChanged { metadataHeightPx = it.height.toFloat() }
                            .graphicsLayer { translationY = metadataHeightPx + offsetY.value }
                    ) {
                        MetadataPanel(
                            asset = asset,
                            onClose = { scope.launch { offsetY.animateTo(0f) } },
                            onDrag = { delta ->
                                scope.launch {
                                    offsetY.snapTo((offsetY.value + delta).coerceIn(-metadataHeightPx, 0f))
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    val currentY = offsetY.value
                                    // Snap based on position
                                    if (currentY <= -metadataHeightPx * 0.4f) {
                                        offsetY.animateTo(-metadataHeightPx, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    } else {
                                        offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                }
                            },
                            offsetYValue = offsetY.value,
                            maxHeightPx = metadataHeightPx
                        )
                    }
                }

                if (!isNext) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isHoldingByPress && !wasHoldDetected,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val densityLocal = LocalDensity.current
                        val panelPushDp = with(densityLocal) { (-offsetY.value).toDp() }

                        listOf(Alignment.Start, Alignment.End).forEach { side ->
                            Column(
                                modifier = Modifier
                                    .align(if (side == Alignment.Start) Alignment.TopStart else Alignment.TopEnd)
                                    .fillMaxHeight()
                                    .padding(8.dp),
                                horizontalAlignment = side
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (config.showFullscreenButton && config.fullscreenButtonPosition.toHorizontalAlignment() == side && (config.fullscreenButtonPosition == IconPosition.TOP_LEFT || config.fullscreenButtonPosition == IconPosition.TOP_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = Icons.Default.Fullscreen,
                                            contentDescription = stringResource(R.string.settings_fullscreen_pos_label),
                                            onClick = actions.onOpenFullscreen
                                        )
                                    }
                                    if (config.showCardDisplayButton && config.cardDisplayButtonPosition.toHorizontalAlignment() == side && (config.cardDisplayButtonPosition == IconPosition.TOP_LEFT || config.cardDisplayButtonPosition == IconPosition.TOP_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = if (config.cardDisplayMode == CardDisplayMode.FILL)
                                                Icons.Default.FitScreen else Icons.Default.AspectRatio,
                                            contentDescription = stringResource(R.string.swipe_toggle_display),
                                            onClick = actions.onToggleDisplayMode
                                        )
                                    }
                                    if (config.showImmichButton && config.immichButtonPosition.toHorizontalAlignment() == side && (config.immichButtonPosition == IconPosition.TOP_LEFT || config.immichButtonPosition == IconPosition.TOP_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = stringResource(R.string.settings_immich_pos_label),
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, "$baseUrl/photos/${asset.id}".toUri())
                                                context.startActivity(intent)
                                            }
                                        )
                                    }
                                    if (config.showMuteButton && config.muteButtonPosition.toHorizontalAlignment() == side && (config.muteButtonPosition == IconPosition.TOP_LEFT || config.muteButtonPosition == IconPosition.TOP_RIGHT) && asset.type == "VIDEO") {
                                        SwipeActionIconButton(
                                            icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Mute",
                                            onClick = {
                                                actions.onToggleMute()
                                                showMuteIndicator = true
                                            }
                                        )
                                    }
                                    if (config.showDownloadButton && config.downloadButtonPosition.toHorizontalAlignment() == side && (config.downloadButtonPosition == IconPosition.TOP_LEFT || config.downloadButtonPosition == IconPosition.TOP_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = Icons.Default.FileDownload,
                                            contentDescription = "Download",
                                            onClick = { actions.onDownload(asset) }
                                        )
                                    }
                                    if (config.showShareButton && config.shareButtonPosition.toHorizontalAlignment() == side && (config.shareButtonPosition == IconPosition.TOP_LEFT || config.shareButtonPosition == IconPosition.TOP_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = Icons.Default.Share,
                                            contentDescription = "Share",
                                            onClick = { actions.onShare(asset) }
                                        )
                                    }
                                }

                                Spacer(Modifier.weight(1f))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (config.showFullscreenButton && config.fullscreenButtonPosition.toHorizontalAlignment() == side && (config.fullscreenButtonPosition == IconPosition.BOTTOM_LEFT || config.fullscreenButtonPosition == IconPosition.BOTTOM_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = Icons.Default.Fullscreen,
                                            contentDescription = stringResource(R.string.settings_fullscreen_pos_label),
                                            onClick = actions.onOpenFullscreen
                                        )
                                    }
                                    if (config.showCardDisplayButton && config.cardDisplayButtonPosition.toHorizontalAlignment() == side && (config.cardDisplayButtonPosition == IconPosition.BOTTOM_LEFT || config.cardDisplayButtonPosition == IconPosition.BOTTOM_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = if (config.cardDisplayMode == CardDisplayMode.FILL)
                                                Icons.Default.FitScreen else Icons.Default.AspectRatio,
                                            contentDescription = stringResource(R.string.swipe_toggle_display),
                                            onClick = actions.onToggleDisplayMode
                                        )
                                    }
                                    if (config.showImmichButton && config.immichButtonPosition.toHorizontalAlignment() == side && (config.immichButtonPosition == IconPosition.BOTTOM_LEFT || config.immichButtonPosition == IconPosition.BOTTOM_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = stringResource(R.string.settings_immich_pos_label),
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, "$baseUrl/photos/${asset.id}".toUri())
                                                context.startActivity(intent)
                                            }
                                        )
                                    }
                                    if (config.showMuteButton && config.muteButtonPosition.toHorizontalAlignment() == side && (config.muteButtonPosition == IconPosition.BOTTOM_LEFT || config.muteButtonPosition == IconPosition.BOTTOM_RIGHT) && asset.type == "VIDEO") {
                                        SwipeActionIconButton(
                                            icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Mute",
                                            onClick = {
                                                actions.onToggleMute()
                                                showMuteIndicator = true
                                            }
                                        )
                                    }
                                    if (config.showDownloadButton && config.downloadButtonPosition.toHorizontalAlignment() == side && (config.downloadButtonPosition == IconPosition.BOTTOM_LEFT || config.downloadButtonPosition == IconPosition.BOTTOM_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = Icons.Default.FileDownload,
                                            contentDescription = "Download",
                                            onClick = { actions.onDownload(asset) }
                                        )
                                    }
                                    if (config.showShareButton && config.shareButtonPosition.toHorizontalAlignment() == side && (config.shareButtonPosition == IconPosition.BOTTOM_LEFT || config.shareButtonPosition == IconPosition.BOTTOM_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = Icons.Default.Share,
                                            contentDescription = "Share",
                                            onClick = { actions.onShare(asset) }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(panelPushDp))
                            }
                        }
                    }
                }

                if (!isNext) {
                    if (offsetX.value > 0f) {
                        IndicatorBadge(stringResource(R.string.swipe_keep_upper), MaterialGreen, Alignment.TopStart) { (offsetX.value / 200f).coerceIn(0f, 1f) * 0.9f }
                    } else if (offsetX.value < 0f) {
                        IndicatorBadge(stringResource(R.string.swipe_delete_upper), MaterialRed, Alignment.TopEnd) { (-offsetX.value / 200f).coerceIn(0f, 1f) * 0.9f }
                    }
                }

                // Mute/Unmute popup indicator
                androidx.compose.animation.AnimatedVisibility(
                    visible = showMuteIndicator,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 1.2f),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}


