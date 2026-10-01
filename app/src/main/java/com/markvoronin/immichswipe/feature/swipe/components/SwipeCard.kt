package com.markvoronin.immichswipe.feature.swipe

import android.content.Intent
import com.markvoronin.immichswipe.core.ImmichLauncher
import com.markvoronin.immichswipe.feature.swipe.components.SwipeActionIconButton
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.markvoronin.immichswipe.core.cache.VideoCache
import com.markvoronin.immichswipe.domain.model.Asset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    val baseUrl = config.baseUrl.removeSuffix("/")
    val apiKey = config.apiKey
    val lifecycleOwner = LocalLifecycleOwner.current

    val scope = rememberCoroutineScope()
    val offsetX = remember(asset.id) { Animatable(0f) }
    val panelProgress = remember(asset.id) { Animatable(0f) }

    // Directional lock for gestures
    var dragDirection by remember { mutableIntStateOf(0) } // 0: undecided, 1: horizontal, 2: vertical

    var isHoldingByPress by remember(asset.id) { mutableStateOf(false) }
    var wasHoldDetected by remember(asset.id) { mutableStateOf(false) }
    val currentIsHolding by rememberUpdatedState(isHoldingByPress || wasHoldDetected)
    var pausedByHoldState by remember(asset.id) { mutableStateOf(false) }
    var ignoreNextTap by remember(asset.id) { mutableStateOf(false) }

    var isVideoReady by remember(asset.id) { mutableStateOf(false) }
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

    var metadataHeightPx by remember(asset.id) { mutableFloatStateOf(0f) }
    val maxHeightPx = with(density) { 300.dp.toPx() }

    val internalExoPlayer = remember(asset.id, isNext, isFullscreenOpen) {
        if (asset.type == "VIDEO" && !isNext && providedPlayer == null && !isFullscreenOpen) {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(30_000, 120_000, 1_000, 1_000)
                .setBackBuffer(120_000, true)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            val handleAudioFocus = (config.playbackBehavior != PlaybackBehavior.IGNORE)

            ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .setAudioAttributes(audioAttributes, handleAudioFocus)
                .build().apply {

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

    var wasPlayingBeforeHold by remember(asset.id) { mutableStateOf(false) }

    LaunchedEffect(pausedByHoldState) {
        if (pausedByHoldState) {
            wasPlayingBeforeHold = exoPlayer?.playWhenReady == true
            exoPlayer?.pause()
        } else if (wasPlayingBeforeHold) {
            exoPlayer?.play()
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner, asset.id) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                val isSameAsset = exoPlayer?.currentMediaItem?.mediaId == asset.id
                if (isSameAsset) {
                    isVideoReady = true
                    showLoadingIndicator = false
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                val isSameAsset = exoPlayer?.currentMediaItem?.mediaId == asset.id
                if (state == Player.STATE_READY && isSameAsset) {
                    if ((exoPlayer?.videoSize?.height ?: 0) == 0) {
                        isVideoReady = true
                    }
                    showLoadingIndicator = false
                } else if (state == Player.STATE_BUFFERING) {
                    // Only show indicator if buffering the correct asset, but keep isVideoReady true if it was already ready
                    if (isSameAsset) showLoadingIndicator = true
                } else if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                    showLoadingIndicator = false
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val isSameAsset = exoPlayer?.currentMediaItem?.mediaId == asset.id
                if (isPlaying && isSameAsset) {
                    showLoadingIndicator = false
                }
            }
        }
        exoPlayer?.addListener(listener)
        var wasPlayingBeforeAppPause = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!isFullscreenOpen) {
                        wasPlayingBeforeAppPause = exoPlayer?.playWhenReady == true
                        exoPlayer?.playWhenReady = false
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!isNext && !isFullscreenOpen && wasPlayingBeforeAppPause) {
                        exoPlayer?.playWhenReady = true
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        exoPlayer?.volume = if (isMuted) 0f else 1f

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
                .pointerInput(isNext, asset.id) {
                    if (isNext) return@pointerInput
                    var startProgress = 0f
                    var startTouchPos = Offset.Zero
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragDirection = 0
                            startProgress = panelProgress.value
                            startTouchPos = offset
                        },
                        onDragEnd = {
                            scope.launch {
                                if (currentIsHolding) {
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    panelProgress.animateTo(startProgress, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    dragDirection = 0
                                    wasHoldDetected = false
                                    pausedByHoldState = false
                                    return@launch
                                }
                                val currentX = offsetX.value
                                val currentProgress = panelProgress.value

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
                                    // Reset horizontal offset so card doesn't stay shifted/rotated
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    // Snap based on final progress
                                    if (currentProgress >= 0.4f) {
                                        panelProgress.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    } else {
                                        panelProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                } else {
                                    // Small movement, reset both
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    panelProgress.animateTo(startProgress, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                }
                                dragDirection = 0
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                if (currentIsHolding) {
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    panelProgress.animateTo(startProgress, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    dragDirection = 0
                                    wasHoldDetected = false
                                    pausedByHoldState = false
                                    return@launch
                                }
                                val currentProgress = panelProgress.value
                                // Always reset horizontal offset when canceling or in vertical mode
                                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                if (dragDirection == 2 || currentProgress > 0.1f) {
                                    if (currentProgress >= 0.4f) {
                                        panelProgress.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    } else {
                                        panelProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                } else {
                                    panelProgress.animateTo(startProgress, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                }
                                dragDirection = 0
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (currentIsHolding) {
                                return@detectDragGestures
                            }
                            
                            val totalDX = change.position.x - startTouchPos.x
                            val totalDY = change.position.y - startTouchPos.y
                            val accumulatedDX = abs(totalDX)
                            val accumulatedDY = abs(totalDY)

                            if (dragDirection == 0) {
                                // Decision threshold: 20 pixels of movement
                                if (accumulatedDX > 20f || accumulatedDY > 20f) {
                                    // If panel is open or moving, strictly lock to vertical drag if user is moving vertically
                                    if (startProgress > 0.1f && accumulatedDY > 5f) {
                                        dragDirection = 2
                                    } else {
                                        // Require accumulatedDY > 1.73f * accumulatedDX (angle within ~30° of vertical / > 60° from horizontal)
                                        // to lock to vertical drag (metadata panel). Otherwise, lock to horizontal swipe.
                                        dragDirection = if (accumulatedDY > 1.73f * accumulatedDX) 2 else 1
                                    }

                                    // Immediately animate back the non-chosen axis when direction locks
                                    if (dragDirection == 1) {
                                        scope.launch { panelProgress.animateTo(startProgress, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
                                    } else {
                                        scope.launch {
                                            offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                            if (metadataHeightPx > 0f) {
                                                val initialProgress = (startProgress - totalDY / metadataHeightPx).coerceIn(0f, 1f)
                                                panelProgress.snapTo(initialProgress)
                                            }
                                        }
                                    }
                                }
                            }

                            scope.launch {
                                if (dragDirection == 1) {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                } else if (dragDirection == 2) {
                                    if (metadataHeightPx > 0f) {
                                        val deltaProgress = -dragAmount.y / metadataHeightPx
                                        panelProgress.snapTo((panelProgress.value + deltaProgress).coerceIn(0f, 1f))
                                    }
                                } else {
                                    // While direction is undecided, only update horizontal offset so metadata panel doesn't twitch
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                }
                            }
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 0.dp else 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            val animatedRotation by animateFloatAsState(targetValue = config.rotationAngle.toFloat(), label = "rotationZ")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                if (asset.type == "VIDEO") {
                    val placeholderRequest = remember(asset.id, baseUrl, apiKey) {
                        ImageRequest.Builder(context)
                            .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview&edited=true")
                            .addHeader("x-api-key", apiKey)
                            .memoryCacheKey("${asset.id}-preview")
                            .placeholderMemoryCacheKey("${asset.id}-preview")
                            .crossfade(false)
                            .precision(Precision.INEXACT)
                            .build()
                    }

                    // Always render base thumbnail to prevent 1-frame unmount flicker when transitioning from next -> top card
                    AsyncImage(
                        model = placeholderRequest,
                        contentDescription = null,
                        contentScale = if (config.cardDisplayMode == CardDisplayMode.FILL) ContentScale.Crop else ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = animatedRotation }
                            .rotateLayout(config.rotationAngle)
                    )

                    if (!isNext && exoPlayer != null && !isFullscreenOpen) {
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
                            showSize = config.showSizeIndicator,
                            videoSurfaceWrapper = { surface ->
                                ZoomableBox(
                                    modifier = Modifier.fillMaxSize(),
                                    resetOnRelease = true,
                                    aspectRatio = asset.exifInfo?.let {
                                        val baseAR = (it.imageWidth?.toFloat() ?: 1f) / (it.imageHeight?.toFloat() ?: 1f)
                                        if (config.rotationAngle % 180 != 0) 1f / baseAR else baseAR
                                    },
                                    isFillMode = config.cardDisplayMode == CardDisplayMode.FILL,
                                    onTap = { offset, size ->
                                        if (panelProgress.value > 0.1f) {
                                            scope.launch { panelProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
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
                                            if (panelProgress.value > 0.1f) {
                                                scope.launch { panelProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { rotationZ = animatedRotation }
                                            .rotateLayout(config.rotationAngle)
                                    ) {
                                        surface()
                                    }

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
                            }
                        )
                    }
                } else {
                    val photoRequest = remember(asset.id, baseUrl, apiKey) {
                        ImageRequest.Builder(context)
                            .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview&edited=true")
                            .addHeader("x-api-key", apiKey)
                            .memoryCacheKey("${asset.id}-preview")
                            .placeholderMemoryCacheKey("${asset.id}-preview")
                            .crossfade(false)
                            .precision(Precision.INEXACT)
                            .build()
                    }
                    ZoomableBox(
                        modifier = Modifier.fillMaxSize(),
                        resetOnRelease = true,
                        enabled = !isNext,
                        aspectRatio = asset.exifInfo?.let {
                            val baseAR = (it.imageWidth?.toFloat() ?: 1f) / (it.imageHeight?.toFloat() ?: 1f)
                            if (config.rotationAngle % 180 != 0) 1f / baseAR else baseAR
                        },
                        isFillMode = config.cardDisplayMode == CardDisplayMode.FILL,
                        onTap = { offset, size ->
                            if (panelProgress.value > 0.1f) {
                                scope.launch { panelProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
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
                                if (panelProgress.value > 0.1f) {
                                    scope.launch { panelProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
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
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationZ = animatedRotation }
                                .rotateLayout(config.rotationAngle)
                        )
                    }
                }

                if (config.showSizeIndicator && !isNext && asset.type != "VIDEO") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedVisibility(
                            visible = !isHoldingByPress && !wasHoldDetected,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
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
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isHoldingByPress && !wasHoldDetected,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
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
                                    if (config.showRotationButton && asset.type != "VIDEO" && config.rotationButtonPosition.toHorizontalAlignment() == side && (config.rotationButtonPosition == IconPosition.TOP_LEFT || config.rotationButtonPosition == IconPosition.TOP_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = Icons.AutoMirrored.Filled.RotateRight,
                                            contentDescription = stringResource(R.string.swipe_rotate_asset),
                                            onClick = actions.onRotateAsset
                                        )
                                    }
                                    if (config.showImmichButton && config.immichButtonPosition.toHorizontalAlignment() == side && (config.immichButtonPosition == IconPosition.TOP_LEFT || config.immichButtonPosition == IconPosition.TOP_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = stringResource(R.string.settings_immich_pos_label),
                                            onClick = {
                                                ImmichLauncher.openAssetInImmich(context, baseUrl, asset.id, mode = config.immichOpenMode)
                                            },
                                            onLongClick = if (config.immichLongPressWeb && !baseUrl.isNullOrBlank()) {
                                                { ImmichLauncher.openInWeb(context, baseUrl, asset.id) }
                                            } else null
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
                                    if (config.showRotationButton && asset.type != "VIDEO" && config.rotationButtonPosition.toHorizontalAlignment() == side && (config.rotationButtonPosition == IconPosition.BOTTOM_LEFT || config.rotationButtonPosition == IconPosition.BOTTOM_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = Icons.AutoMirrored.Filled.RotateRight,
                                            contentDescription = stringResource(R.string.swipe_rotate_asset),
                                            onClick = actions.onRotateAsset
                                        )
                                    }
                                    if (config.showImmichButton && config.immichButtonPosition.toHorizontalAlignment() == side && (config.immichButtonPosition == IconPosition.BOTTOM_LEFT || config.immichButtonPosition == IconPosition.BOTTOM_RIGHT)) {
                                        SwipeActionIconButton(
                                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = stringResource(R.string.settings_immich_pos_label),
                                            onClick = {
                                                ImmichLauncher.openAssetInImmich(context, baseUrl, asset.id, mode = config.immichOpenMode)
                                            },
                                            onLongClick = if (config.immichLongPressWeb && !baseUrl.isNullOrBlank()) {
                                                { ImmichLauncher.openInWeb(context, baseUrl, asset.id) }
                                            } else null
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
                            }
                        }
                    }

                if (!isNext) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .onSizeChanged { metadataHeightPx = it.height.toFloat() }
                                .offset {
                                    val yOffset = (metadataHeightPx * (1f - panelProgress.value)).toInt()
                                    IntOffset(0, yOffset)
                                }
                        ) {
                            MetadataPanel(
                                asset = asset,
                                onClose = {
                                    scope.launch {
                                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                        panelProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                },
                                onDrag = { deltaY ->
                                    scope.launch {
                                        if (offsetX.value != 0f) {
                                            offsetX.snapTo(0f)
                                        }
                                        if (metadataHeightPx > 0f) {
                                            val deltaProgress = -deltaY / metadataHeightPx
                                            panelProgress.snapTo((panelProgress.value + deltaProgress).coerceIn(0f, 1f))
                                        }
                                    }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                        if (panelProgress.value >= 0.4f) {
                                            panelProgress.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                        } else {
                                            panelProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                }
                            },
                            panelProgress = panelProgress.value,
                            maxHeightPx = metadataHeightPx
                        )
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
                            .requiredSize(64.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.requiredSize(32.dp)
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


