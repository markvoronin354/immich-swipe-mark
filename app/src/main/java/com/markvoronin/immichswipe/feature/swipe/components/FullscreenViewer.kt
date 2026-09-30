package com.markvoronin.immichswipe.feature.swipe

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.domain.model.Asset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt


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
fun FullscreenViewer(
    asset: Asset,
    nextAsset: Asset? = null,
    isFavorite: Boolean,
    onSwipe: (SwipeDecision) -> Unit,
    onUndo: () -> Unit,
    onDoubleTap: () -> Unit,
    onClose: () -> Unit,
    tapToSwipeEnabled: Boolean = false,
    providedPlayer: ExoPlayer? = null,
    showSizeIndicator: Boolean = false,
    isMuted: Boolean = false,
    onToggleMute: () -> Unit = {},
    showMuteButton: Boolean = true,
    downloadButtonPosition: IconPosition = IconPosition.TOP_LEFT,
    showDownloadButton: Boolean = false,
    shareButtonPosition: IconPosition = IconPosition.TOP_RIGHT,
    showShareButton: Boolean = false,
    onDownload: (Asset) -> Unit = {},
    onShare: (Asset) -> Unit = {},
    rotation: Int = 0
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()

    var isHoldingByPress by remember(asset.id) { mutableStateOf(false) }
    var wasHoldDetected by remember(asset.id) { mutableStateOf(false) }
    val currentIsHolding by rememberUpdatedState(isHoldingByPress || wasHoldDetected)
    var pausedByHoldState by remember(asset.id) { mutableStateOf(false) }
    var ignoreNextTap by remember { mutableStateOf(false) }
    var isZoomedIn by remember(asset.id) { mutableStateOf(false) }

    val swipeY = remember { Animatable(0f) }
    val swipeX = remember { Animatable(0f) }

    var toggleControllerTrigger by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(asset.id, isVideoReady) {
        AppLogger.d("Fullscreen", "Video readiness: ready=$isVideoReady, indicator=$showLoadingIndicator, asset=${asset.id}")
        if (isVideoReady) {
            showLoadingIndicator = false
        } else if (asset.type == "VIDEO") {
            // Delay showing the indicator to avoid flickering on fast transitions
            delay(500)
            if (!isVideoReady) {
                showLoadingIndicator = true
            }
        }
    }

    val currentOnSwipe by rememberUpdatedState(onSwipe)

    var controlsVisible by remember { mutableStateOf(true) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val controlsOffset by animateDpAsState(
        targetValue = if (controlsVisible && isLandscape) 64.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ControlsShift"
    )

    val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
    val apiKey = SessionManager.getApiKey() ?: ""

    val exoPlayer = providedPlayer // Use shared player exclusively for Fullscreen to avoid heavy instantiations

    LaunchedEffect(asset.id) {
        swipeX.snapTo(0f)
        swipeY.snapTo(0f)
        if (exoPlayer?.playbackState != Player.STATE_READY) {
            isVideoReady = false
            showLoadingIndicator = true
        }
    }

    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
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

    // Manage orientation for the lifetime of the FullscreenViewer
    DisposableEffect(Unit) {
        @SuppressLint("SourceLockedOrientationActivity")
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        onDispose {
            @SuppressLint("SourceLockedOrientationActivity")
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    DisposableEffect(exoPlayer, asset.id) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val isSameAsset = exoPlayer?.currentMediaItem?.mediaId == asset.id
                AppLogger.d("Fullscreen", "Playback state changed: $state, asset=${asset.id}, same=$isSameAsset")
                if (state == Player.STATE_READY && isSameAsset) {
                    isVideoReady = true
                    showLoadingIndicator = false
                } else if (state == Player.STATE_BUFFERING) {
                    if (isSameAsset) showLoadingIndicator = true
                } else if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                    showLoadingIndicator = false
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val isSameAsset = exoPlayer?.currentMediaItem?.mediaId == asset.id
                AppLogger.d("Fullscreen", "Is playing changed: $isPlaying, asset=${asset.id}, same=$isSameAsset")
                if (isPlaying && isSameAsset) {
                    isVideoReady = true
                    showLoadingIndicator = false
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                AppLogger.e("Fullscreen", "Player error: ${error.message}", error)
                showLoadingIndicator = false
            }
        }

        val isSameAssetInit = exoPlayer?.currentMediaItem?.mediaId == asset.id
        if (exoPlayer?.playbackState == Player.STATE_READY && isSameAssetInit) {
            isVideoReady = true
        }

        exoPlayer?.addListener(listener)

        onDispose {
            exoPlayer?.removeListener(listener)
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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

        val fadeAlpha = (1f - (swipeY.value / 1000f)).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = fadeAlpha))
        ) {
            if (nextAsset != null) {
                val swipeProgress = (abs(swipeX.value) / 500f).coerceIn(0f, 1f)
                val nextScale = 0.85f + (0.15f * swipeProgress)
                val nextAlpha = (0.6f + (0.4f * swipeProgress)) * fadeAlpha

                val nextPhotoRequest = remember(nextAsset.id, baseUrl, apiKey) {
                    ImageRequest.Builder(context)
                        .data("$baseUrl/api/assets/${nextAsset.id}/thumbnail?format=WEBP&size=preview")
                        .addHeader("x-api-key", apiKey)
                        .crossfade(false)
                        .precision(Precision.INEXACT)
                        .build()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = nextScale
                            scaleY = nextScale
                            alpha = nextAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = nextPhotoRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isZoomedIn) {
                        if (isZoomedIn) return@pointerInput
                        detectDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (currentIsHolding) {
                                        launch { swipeY.animateTo(0f) }
                                        launch { swipeX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
                                        wasHoldDetected = false
                                        pausedByHoldState = false
                                        return@launch
                                    }
                                    val currentX = swipeX.value
                                    val currentY = swipeY.value

                                    if (currentY > 120 && abs(currentX) < 100) {
                                        onClose()
                                    } else if (currentX > 250) {
                                        launch { swipeY.animateTo(0f, tween(200)) }
                                        swipeX.animateTo(2000f, tween(200))
                                        swipeX.snapTo(0f)
                                        swipeY.snapTo(0f)
                                        currentOnSwipe(SwipeDecision.KEEP)
                                    } else if (currentX < -250) {
                                        launch { swipeY.animateTo(0f, tween(200)) }
                                        swipeX.animateTo(-2000f, tween(200))
                                        swipeX.snapTo(0f)
                                        swipeY.snapTo(0f)
                                        currentOnSwipe(SwipeDecision.DELETE)
                                    } else {
                                        launch { swipeY.animateTo(0f) }
                                        launch { swipeX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    launch { swipeY.animateTo(0f) }
                                    launch { swipeX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
                                    wasHoldDetected = false
                                    pausedByHoldState = false
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (currentIsHolding) {
                                    return@detectDragGestures
                                }
                                scope.launch {
                                    swipeY.snapTo((swipeY.value + dragAmount.y).coerceAtLeast(0f))
                                    swipeX.snapTo(swipeX.value + dragAmount.x)
                                }
                            }
                        )
                    }
                    .offset { IntOffset(swipeX.value.roundToInt(), swipeY.value.roundToInt()) }
                    .graphicsLayer {
                        rotationZ = swipeX.value / 60f
                    }
                    .background(Color.Black.copy(alpha = fadeAlpha)),
                contentAlignment = Alignment.Center
            ) {
            val finalControlsVisible = controlsVisible && !isHoldingByPress && !wasHoldDetected

            if (asset.type == "VIDEO" && exoPlayer != null) {
                SharedVideoPlayer(
                    player = exoPlayer,
                    isFullscreen = true,
                    assetId = asset.id,
                    isMuted = isMuted,
                    isPaused = pausedByHoldState,
                    isVideoReady = isVideoReady,
                    toggleControllerTrigger = toggleControllerTrigger,
                    showControls = finalControlsVisible,
                    fileSize = asset.exifInfo?.fileSizeInBytes,
                    showSize = showSizeIndicator,
                    onControllerVisibilityChanged = { /* Now handled externally via controlsVisible */ },
                    controlsOffset = controlsOffset,
                    videoSurfaceWrapper = { surface ->
                        ZoomableBox(
                            modifier = Modifier.fillMaxSize(),
                            resetOnRelease = false,
                            onIsZoomedChanged = { isZoomedIn = it },
                            onTap = { offset, size ->
                                if (!ignoreNextTap) {
                                    val width = size.width.toFloat()
                                    if (tapToSwipeEnabled && !isZoomedIn && (offset.x < width * 0.3f || offset.x > width * 0.7f)) {
                                        when {
                                            offset.x < width * 0.3f -> currentOnSwipe(SwipeDecision.DELETE)
                                            offset.x > width * 0.7f -> currentOnSwipe(SwipeDecision.KEEP)
                                        }
                                    } else if (asset.type == "VIDEO") {
                                        onToggleMute()
                                        showMuteIndicator = true
                                        toggleControllerTrigger++
                                    }
                                }
                                ignoreNextTap = false
                            },
                            onDoubleTap = onDoubleTap,
                            onPress = { offset, size ->
                                ignoreNextTap = false
                                wasHoldDetected = false
                                val wasReleased = withTimeoutOrNull(500) {
                                    awaitRelease()
                                    true
                                }
                                if (wasReleased == true) {
                                    // Fast tap detected
                                    val width = size.width.toFloat()
                                    if (tapToSwipeEnabled && !isZoomedIn && (offset.x < width * 0.3f || offset.x > width * 0.7f)) {
                                        when {
                                            offset.x < width * 0.3f -> {
                                                currentOnSwipe(SwipeDecision.DELETE)
                                                ignoreNextTap = true
                                            }
                                            offset.x > width * 0.7f -> {
                                                currentOnSwipe(SwipeDecision.KEEP)
                                                ignoreNextTap = true
                                            }
                                        }
                                    }
                                } else {
                                    // Hold detected
                                    ignoreNextTap = true
                                    isHoldingByPress = true
                                    wasHoldDetected = true
                                    if (asset.type == "VIDEO") {
                                        pausedByHoldState = true
                                    }
                                    try {
                                        awaitRelease()
                                        isHoldingByPress = false
                                        pausedByHoldState = false
                                        wasHoldDetected = false
                                    } catch (e: GestureCancellationException) {
                                        isHoldingByPress = false
                                    }
                                }
                            },
                            aspectRatio = asset.exifInfo?.let {
                                val baseAR = (it.imageWidth?.toFloat() ?: 1f) / (it.imageHeight?.toFloat() ?: 1f)
                                if (rotation % 180 != 0) 1f / baseAR else baseAR
                            }
                        ) {
                            val animatedRotation by animateFloatAsState(targetValue = rotation.toFloat(), label = "FullscreenVideoRotation")

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationZ = animatedRotation }
                                    .rotateLayout(rotation)
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
            } else {
                ZoomableBox(
                    modifier = Modifier.fillMaxSize(),
                    resetOnRelease = false,
                    onIsZoomedChanged = { isZoomedIn = it },
                    onTap = { offset, size ->
                        if (!ignoreNextTap) {
                            val width = size.width.toFloat()
                            if (tapToSwipeEnabled && !isZoomedIn && (offset.x < width * 0.3f || offset.x > width * 0.7f)) {
                                when {
                                    offset.x < width * 0.3f -> currentOnSwipe(SwipeDecision.DELETE)
                                    offset.x > width * 0.7f -> currentOnSwipe(SwipeDecision.KEEP)
                                }
                            }
                        }
                        ignoreNextTap = false
                    },
                    onDoubleTap = onDoubleTap,
                    onPress = { offset, size ->
                        ignoreNextTap = false
                        wasHoldDetected = false
                        val wasReleased = withTimeoutOrNull(500) {
                            awaitRelease()
                            true
                        }
                        if (wasReleased == true) {
                            val width = size.width.toFloat()
                            if (tapToSwipeEnabled && !isZoomedIn && (offset.x < width * 0.3f || offset.x > width * 0.7f)) {
                                when {
                                    offset.x < width * 0.3f -> {
                                        currentOnSwipe(SwipeDecision.DELETE)
                                        ignoreNextTap = true
                                    }
                                    offset.x > width * 0.7f -> {
                                        currentOnSwipe(SwipeDecision.KEEP)
                                        ignoreNextTap = true
                                    }
                                }
                            }
                        } else {
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
                    },
                    aspectRatio = asset.exifInfo?.let {
                        val baseAR = (it.imageWidth?.toFloat() ?: 1f) / (it.imageHeight?.toFloat() ?: 1f)
                        if (rotation % 180 != 0) 1f / baseAR else baseAR
                    }
                ) {
                    val baseUrlClean = SessionManager.getBaseUrl()?.removeSuffix("/")
                    val apiKeyLocal = SessionManager.getApiKey() ?: ""

                    val animatedRotation by animateFloatAsState(targetValue = rotation.toFloat(), label = "FullscreenRotation")

                    val photoRequest = remember(asset.id, baseUrlClean, apiKeyLocal) {
                        ImageRequest.Builder(context)
                            .data("$baseUrlClean/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview&edited=true")
                            .addHeader("x-api-key", apiKeyLocal)
                            .crossfade(false)
                            .precision(Precision.INEXACT)
                            .build()
                    }

                    AsyncImage(
                        model = photoRequest,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = animatedRotation }
                            .rotateLayout(rotation),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            if (showSizeIndicator && asset.type != "VIDEO") {
                AnimatedVisibility(
                    visible = controlsVisible && !isHoldingByPress && !wasHoldDetected,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(bottom = (if (isLandscape) 80.dp else 80.dp) + controlsOffset),
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

            if (swipeX.value > 0f) IndicatorBadge(stringResource(R.string.swipe_keep_upper), MaterialGreen, Alignment.TopStart) { (swipeX.value / 200f).coerceIn(0f, 1f) * 0.9f }
            else if (swipeX.value < 0f) IndicatorBadge(stringResource(R.string.swipe_delete_upper), MaterialRed, Alignment.TopEnd) { (-swipeX.value / 200f).coerceIn(0f, 1f) * 0.9f }

                AnimatedVisibility(
                    visible = controlsVisible && !isHoldingByPress && !wasHoldDetected,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Indicateur de Favori (Coeur au centre en bas)
                        val heartScale = animateFloatAsState(if (isFavorite) 1.2f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "HeartScale").value

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = (if (isLandscape) 20.dp else 106.dp) + controlsOffset)
                                .graphicsLayer {
                                    scaleX = heartScale
                                    scaleY = heartScale
                                }
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                .clip(CircleShape)
                                .clickable { onDoubleTap() }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) Color.Red else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(vertical = 50.dp, horizontal = 20.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, stringResource(R.string.common_close), tint = Color.White)
                        }

                        IconButton(
                            onClick = onUndo,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 24.dp, bottom = (if (isLandscape) 20.dp else 100.dp) + controlsOffset)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = stringResource(R.string.nav_back),
                                tint = Color.White
                            )
                        }

                        if (showMuteButton && asset.type == "VIDEO") {
                            IconButton(
                                onClick = onToggleMute,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = (if (isLandscape) 20.dp else 100.dp) + controlsOffset, end = 24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = Color.White
                                )
                            }
                        }

                        if (showDownloadButton) {
                            val dlAlign = when (downloadButtonPosition) {
                                IconPosition.TOP_LEFT -> Alignment.TopStart
                                IconPosition.TOP_RIGHT -> Alignment.TopEnd
                                IconPosition.BOTTOM_LEFT -> Alignment.BottomStart
                                IconPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                            }

                            val dlPadding = when (downloadButtonPosition) {
                                IconPosition.TOP_LEFT -> Modifier.padding(top = 50.dp, start = 20.dp)
                                IconPosition.TOP_RIGHT -> Modifier.padding(top = 110.dp, end = 20.dp)
                                IconPosition.BOTTOM_LEFT -> Modifier.padding(bottom = (if (isLandscape) 80.dp else 160.dp) + controlsOffset, start = 24.dp)
                                IconPosition.BOTTOM_RIGHT -> Modifier.padding(bottom = (if (isLandscape) 20.dp else 100.dp) + controlsOffset, end = 24.dp)
                            }

                            IconButton(
                                onClick = { onDownload(asset) },
                                modifier = Modifier
                                    .align(dlAlign)
                                    .then(dlPadding)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Download",
                                    tint = Color.White
                                )
                            }
                        }

                        if (showShareButton) {
                            val shareAlign = when (shareButtonPosition) {
                                IconPosition.TOP_LEFT -> Alignment.TopStart
                                IconPosition.TOP_RIGHT -> Alignment.TopEnd
                                IconPosition.BOTTOM_LEFT -> Alignment.BottomStart
                                IconPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                            }

                            val sharePadding = when (shareButtonPosition) {
                                IconPosition.TOP_LEFT -> Modifier.padding(top = 50.dp, start = 20.dp)
                                IconPosition.TOP_RIGHT -> Modifier.padding(top = 110.dp, end = 20.dp)
                                IconPosition.BOTTOM_LEFT -> Modifier.padding(bottom = (if (isLandscape) 80.dp else 160.dp) + controlsOffset, start = 24.dp)
                                IconPosition.BOTTOM_RIGHT -> Modifier.padding(bottom = (if (isLandscape) 20.dp else 100.dp) + controlsOffset, end = 24.dp)
                            }

                            IconButton(
                                onClick = { onShare(asset) },
                                modifier = Modifier
                                    .align(shareAlign)
                                    .then(sharePadding)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

            // Mute/Unmute popup indicator
                AnimatedVisibility(
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
