package com.markvoronin.immichswipe.feature.swipe

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.CardDisplayMode
import com.markvoronin.immichswipe.core.SessionManager
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


@OptIn(UnstableApi::class)
@Composable
fun SharedVideoPlayer(
    player: Player,
    isFullscreen: Boolean,
    assetId: String? = null,
    isMuted: Boolean = false,
    isPaused: Boolean = false,
    isVideoReady: Boolean = true,
    toggleControllerTrigger: Int = 0,
    showControls: Boolean = true,
    cardDisplayMode: CardDisplayMode = CardDisplayMode.FILL,
    fileSize: Long? = null,
    showSize: Boolean = false,
    onControllerVisibilityChanged: ((Boolean) -> Unit)? = null,
    controlsOffset: Dp = 0.dp,
    videoSurfaceWrapper: @Composable (surfaceContent: @Composable () -> Unit) -> Unit = { surfaceContent -> surfaceContent() }
) {
    key(assetId) {
        var currentTime by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var isVideoPlaying by remember { mutableStateOf(player.isPlaying) }
        var userPaused by remember { mutableStateOf(false) }
        var isScrubbing by remember { mutableStateOf(false) }
        var scrubValue by remember { mutableLongStateOf(0L) }

        val togglePlayPause = {
            if (player.isPlaying) {
                userPaused = true
                player.pause()
            } else {
                userPaused = false
                player.play()
            }
        }

        val videoAlpha by animateFloatAsState(
            targetValue = if (isVideoReady) 1f else 0f,
            animationSpec = if (isVideoReady) tween(durationMillis = 200) else snap(),
            label = "VideoAlpha"
        )

        DisposableEffect(player) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isVideoPlaying = isPlaying
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener) }
        }

        LaunchedEffect(player, isPaused, assetId) {
            AppLogger.d("VideoPlayer", "SharedVideoPlayer Effect: asset=$assetId, isPaused=$isPaused, isFullscreen=$isFullscreen")
            if (isPaused) {
                player.pause()
            } else {
                if (!userPaused && player.playbackState == Player.STATE_READY && !player.isPlaying) {
                    player.play()
                }
                while (true) {
                    if (!isScrubbing) {
                        currentTime = player.currentPosition
                        duration = player.duration.coerceAtLeast(0L)
                    }
                    delay(500.milliseconds)
                }
            }
        }

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val baseUrl = remember { SessionManager.getBaseUrl()?.removeSuffix("/") }
        val apiKey = remember { SessionManager.getApiKey() ?: "" }

        Box(modifier = Modifier.fillMaxSize()) {
            videoSurfaceWrapper {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (assetId != null && baseUrl != null) {
                        val context = LocalContext.current
                        val thumbnailRequest = remember(assetId, baseUrl, apiKey) {
                            ImageRequest.Builder(context)
                                .data("$baseUrl/api/assets/$assetId/thumbnail?format=WEBP&size=preview")
                                .addHeader("x-api-key", apiKey)
                                .crossfade(false)
                                .precision(Precision.INEXACT)
                                .build()
                        }
                        AsyncImage(
                            model = thumbnailRequest,
                            contentDescription = null,
                            contentScale = if (isFullscreen) {
                                ContentScale.Fit
                            } else {
                                if (cardDisplayMode == CardDisplayMode.FILL) ContentScale.Crop else ContentScale.Fit
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }
                    
                    LaunchedEffect(toggleControllerTrigger) {
                        if (toggleControllerTrigger > 0 && isFullscreen) {
                            playerViewRef.value?.let { view ->
                                AppLogger.d("VideoPlayer", "Toggling controller: visible=${view.isControllerFullyVisible}")
                                if (view.isControllerFullyVisible) view.hideController() else view.showController()
                            }
                        }
                    }

                    key(isFullscreen) { // Only re-create AndroidView when switching to/from fullscreen. DO NOT key by assetId.
                        AndroidView(
                            factory = { context ->
                                AppLogger.d("VideoPlayer", "AndroidView Factory: isFullscreen=$isFullscreen, asset=$assetId")
                                val view = LayoutInflater.from(context).inflate(R.layout.view_player_texture, null) as PlayerView
                                view.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                                    onControllerVisibilityChanged?.invoke(visibility == View.VISIBLE)
                                })
                                
                                // Keep the texture from clearing to black when media changes
                                view.setKeepContentOnPlayerReset(true)
                                
                                playerViewRef.value = view
                                view
                            },
                            update = { view ->
                                if (view.player != player) {
                                    AppLogger.d("VideoPlayer", "AndroidView Update: Binding player (fullscreen=$isFullscreen), asset=$assetId")
                                    view.player = player
                                }

                                // Force a "nudge" if the player is ready but the surface hasn't updated
                                if (player.playbackState == Player.STATE_READY) {
                                    view.post {
                                        player.seekTo(player.currentPosition)
                                    }
                                }

                                view.useController = false
                                player.volume = if (isMuted) 0f else 1f
                                view.resizeMode = if (isFullscreen) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                                } else {
                                    if (cardDisplayMode == CardDisplayMode.FILL) {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    } else {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                }

                                if (toggleControllerTrigger > 0 && isFullscreen) {
                                    if (view.isControllerFullyVisible) view.hideController() else view.showController()
                                }
                            },
                            onRelease = { view ->
                                AppLogger.d("VideoPlayer", "AndroidView Release: Detaching player (fullscreen=$isFullscreen), asset=$assetId")
                                view.player = null
                            },
                            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = videoAlpha }
                        )
                    }
                }
            }

            val indicatorsVisible = (showSize && fileSize != null) || (duration > 0) || isFullscreen
            // Controls visibility logic - respect showControls even in fullscreen to allow hiding on hold
            val finalShowControls = showControls
            
            if (indicatorsVisible) {
                AnimatedVisibility(
                    visible = finalShowControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = (if (isFullscreen) (if (isLandscape) 12.dp else 24.dp) else 12.dp) + (if (isLandscape) 0.dp else controlsOffset))
                            .padding(horizontal = if (isFullscreen) 24.dp else 16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Indicators Row (Pause, Timestamp, Size) - Now ABOVE
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (duration > 0 || isFullscreen) {
                                val timeToDisplay = if (isScrubbing) scrubValue else currentTime

                                if (!isFullscreen) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable(onClick = togglePlayPause)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isVideoPlaying) "Pause" else "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(6.dp))
                                }

                                Surface(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${formatMediaTime(timeToDisplay)} / ${formatMediaTime(duration)}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (showSize && fileSize != null) {
                                if (duration > 0 || isFullscreen) Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = formatSize(fileSize),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // 2. Progress Bar Row (Slider) - Now BELOW
                        if (isFullscreen) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = togglePlayPause,
                                    modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isVideoPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                Spacer(Modifier.width(12.dp))

                                Slider(
                                    value = (if (isScrubbing) scrubValue else currentTime).toFloat(),
                                    onValueChange = { 
                                        isScrubbing = true
                                        scrubValue = it.toLong()
                                        player.seekTo(scrubValue)
                                    },
                                    onValueChangeFinished = {
                                        isScrubbing = false
                                        player.seekTo(scrubValue)
                                    },
                                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                                    ),
                                    modifier = Modifier.weight(1f).height(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isPaused && finalShowControls,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 1.2f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Paused",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
