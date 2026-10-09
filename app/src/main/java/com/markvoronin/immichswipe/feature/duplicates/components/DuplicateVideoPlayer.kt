package com.markvoronin.immichswipe.feature.duplicates.components

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.cache.VideoCache
import com.markvoronin.immichswipe.core.player.PlayerLoadControlFactory
import com.markvoronin.immichswipe.data.datastore.SessionDataStore
import com.markvoronin.immichswipe.domain.model.Asset
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Composable
fun DuplicateVideoPlayer(
    asset: Asset,
    modifier: Modifier = Modifier,
    baseUrl: String = "",
    apiKey: String = "",
    contentScale: ContentScale = ContentScale.Fit,
    onTap: (() -> Unit)? = null,
    videoSurfaceWrapper: @Composable (surfaceContent: @Composable () -> Unit) -> Unit = { surface -> surface() }
) {
    val context = LocalContext.current
    val sessionDataStore = remember(context) { SessionDataStore(context.applicationContext) }
    val storedBaseUrl by sessionDataStore.getBaseUrl().collectAsState(initial = null)
    val storedApiKey by sessionDataStore.getApiKey().collectAsState(initial = null)

    val effectiveBaseUrl = (baseUrl.ifBlank { storedBaseUrl ?: "" }).removeSuffix("/")
    val effectiveApiKey = apiKey.ifBlank { storedApiKey ?: "" }

    val playbackBehaviorStr by sessionDataStore.getAudioFocusMode().collectAsState(initial = null)
    val playbackBehavior = remember(playbackBehaviorStr) {
        playbackBehaviorStr?.let { try { PlaybackBehavior.valueOf(it) } catch(_: Exception) { PlaybackBehavior.PAUSE_OTHERS } } ?: PlaybackBehavior.PAUSE_OTHERS
    }
    val handleAudioFocus = (playbackBehavior != PlaybackBehavior.IGNORE)

    var isVideoReady by remember(asset.id) { mutableStateOf(false) }
    var isMuted by remember(asset.id) { mutableStateOf(false) }
    var isPlaying by remember(asset.id) { mutableStateOf(true) }

    var currentTime by remember(asset.id) { mutableLongStateOf(0L) }
    var duration by remember(asset.id) { mutableLongStateOf(0L) }
    var isScrubbing by remember(asset.id) { mutableStateOf(false) }
    var scrubValue by remember(asset.id) { mutableLongStateOf(0L) }

    var exoPlayer by remember(asset.id, handleAudioFocus, effectiveBaseUrl, effectiveApiKey) { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(asset.id, handleAudioFocus, effectiveBaseUrl, effectiveApiKey) {
        if (effectiveBaseUrl.isEmpty() || effectiveApiKey.isEmpty()) {
            return@DisposableEffect onDispose {}
        }

        val loadControl = PlayerLoadControlFactory.createDuplicateLoadControl(context)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, handleAudioFocus)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                val videoUrl = "$effectiveBaseUrl/api/assets/${asset.id}/video/playback"
                val dataSourceFactory = VideoCache.getCacheDataSourceFactory(context, effectiveApiKey)
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(
                        MediaItem.Builder()
                            .setUri(videoUrl)
                            .setMediaId(asset.id)
                            .setCustomCacheKey(asset.id)
                            .build()
                    )
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    isVideoReady = true
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    isVideoReady = true
                }
            }
        }
        player.addListener(listener)
        exoPlayer = player

        onDispose {
            player.removeListener(listener)
            player.stop()
            player.release()
            exoPlayer = null
        }
    }

    LaunchedEffect(exoPlayer, asset.id) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            if (!isScrubbing) {
                currentTime = player.currentPosition
                duration = player.duration.coerceAtLeast(0L)
            }
            delay(200.milliseconds)
        }
    }

    val outerBoxModifier = if (onTap != null) {
        modifier
    } else {
        modifier.clickable {
            val player = exoPlayer ?: return@clickable
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    Box(
        modifier = outerBoxModifier
    ) {
        videoSurfaceWrapper {
            Box(modifier = Modifier.fillMaxSize()) {
                if (effectiveBaseUrl.isNotEmpty() && effectiveApiKey.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("$effectiveBaseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview")
                            .addHeader("x-api-key", effectiveApiKey)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = contentScale,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                @SuppressLint("InflateParams")
                AndroidView(
                    factory = { ctx ->
                        val view = LayoutInflater.from(ctx).inflate(R.layout.view_player_texture, null) as PlayerView
                        view.useController = false
                        view.resizeMode = if (contentScale == ContentScale.Crop) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        view.player = exoPlayer
                        view
                    },
                    update = { view ->
                        if (view.player != exoPlayer) {
                            view.player = exoPlayer
                        }
                        exoPlayer?.volume = if (isMuted) 0f else 1f
                    },
                    onRelease = { view ->
                        view.player = null
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (isVideoReady) 1f else 0f
                        }
                )
            }
        }

        if (!isVideoReady) {
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (duration > 0) {
                val timeToDisplay = if (isScrubbing) scrubValue else currentTime
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${formatMediaTime(timeToDisplay)} / ${formatMediaTime(duration)}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        val player = exoPlayer ?: return@IconButton
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Slider(
                    value = (if (isScrubbing) scrubValue else currentTime).toFloat(),
                    onValueChange = {
                        isScrubbing = true
                        scrubValue = it.toLong()
                        exoPlayer?.seekTo(scrubValue)
                    },
                    onValueChangeFinished = {
                        isScrubbing = false
                        exoPlayer?.seekTo(scrubValue)
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        exoPlayer?.volume = if (isMuted) 0f else 1f
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
