package com.markvoronin.immichswipe.feature.swipe


import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.compose.ui.draw.clip
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.ConnectionLevel
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.SortCategory
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.core.cache.VideoCache
import com.markvoronin.immichswipe.core.cache.VideoPreloader
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Album
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun SwipeScreen(
    album: Album,
    assetRepository: AssetRepository,
    swipeDecisionRepository: SwipeDecisionRepository,
    sessionRepository: SessionRepository,
    sessionKey: String,
    resetSignal: SharedFlow<Unit>,
    modifier: Modifier = Modifier,
    userQuotaBytes: Long? = null
) {
    val viewModel: SwipeViewModel = viewModel(
        key = "$sessionKey-${album.id}",
        factory = SwipeViewModelFactory(assetRepository, sessionRepository, swipeDecisionRepository, album, userQuotaBytes)
    )
    
    LaunchedEffect(resetSignal) {
        resetSignal.collect {
            viewModel.toggleResetConfirmation(visible = true)
        }
    }

    LaunchedEffect(album.id) {
        viewModel.retryLoading()
        viewModel.resetToFirstUnprocessed()
    }

    LaunchedEffect(Unit) {
        viewModel.resetToFirstUnprocessed()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val localCtx = LocalContext.current
    
    // Background Preloader Trigger
    LaunchedEffect(uiState.currentIndex) {
        val currentIndex = uiState.currentIndex
        val assets = uiState.assets
        val apiKey = SessionManager.getApiKey()
        val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
        
        if (apiKey.isNullOrEmpty() || baseUrl.isNullOrEmpty()) return@LaunchedEffect

        // Cancel preload for the previous asset if it was a video
        if (currentIndex > 0) {
            val prevAsset = assets.getOrNull(currentIndex - 1)
            if (prevAsset?.type == "VIDEO") {
                VideoPreloader.cancel(prevAsset.id)
            }
        }

        // Preload next 3 assets
        for (i in 1..3) {
            val nextIndex = currentIndex + i
            if (nextIndex < assets.size) {
                val nextAsset = assets[nextIndex]
                if (nextAsset.type == "VIDEO") {
                    val videoUrl = "$baseUrl/api/assets/${nextAsset.id}/video/playback"
                    VideoPreloader.preload(
                        context = localCtx.applicationContext,
                        assetId = nextAsset.id,
                        videoUrl = videoUrl,
                        apiKey = apiKey
                    )
                } else {
                    // Preload Image to Coil RAM Cache
                    val request = ImageRequest.Builder(localCtx)
                        .data("$baseUrl/api/assets/${nextAsset.id}/thumbnail?format=WEBP&size=preview")
                        .addHeader("x-api-key", apiKey)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build()
                    localCtx.imageLoader.enqueue(request)
                }
            }
        }
    }

    // Gestion partagée de l'ExoPlayer pour l'asset courant (Regular <-> Fullscreen)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playbackBehavior = uiState.playbackBehavior
    val currentAsset = uiState.currentAsset
    
    val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
    val apiKey = SessionManager.getApiKey() ?: ""

    // On crée l'ExoPlayer une seule fois pour tout l'écran Swipe et on change juste la source
    val sharedPlayer: ExoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(25_000, 60_000, 1_000, 1_000)
            .setBackBuffer(60_000, true) // 1 minute back-buffer
            .build()
        
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        AppLogger.d("ExoPlayer", "State changed: $state (ready=${state == Player.STATE_READY})")
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        AppLogger.e("ExoPlayer", "Error: ${error.message}", error)
                    }
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        AppLogger.d("ExoPlayer", "Video size: ${videoSize.width}x${videoSize.height}")
                    }
                })
            }
    }

    // Mise à jour de la source du player quand l'asset change
    LaunchedEffect(currentAsset?.id) {
        val asset = currentAsset
        if (asset?.type == "VIDEO") {
            val videoUrl = "$baseUrl/api/assets/${asset.id}/video/playback"

            val dataSourceFactory = VideoCache.getCacheDataSourceFactory(context, apiKey)
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.Builder()
                    .setUri(videoUrl)
                    .setMediaId(asset.id)
                    .setCustomCacheKey(asset.id) // Ensure consistent cache mapping
                    .build())
            
            // Using setMediaSource with resetPosition=true is smoother than stop()+clear()
            sharedPlayer.setMediaSource(mediaSource, true)
            sharedPlayer.prepare()
            sharedPlayer.playWhenReady = true
        } else {
            sharedPlayer.stop()
            sharedPlayer.clearMediaItems()
        }
    }

    // Configuration des attributs audio selon le comportement choisi
    LaunchedEffect(playbackBehavior) {
        if (playbackBehavior != PlaybackBehavior.IGNORE) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            sharedPlayer.setAudioAttributes(audioAttributes, true)
        }
    }

    // Mise à jour du volume quand isMuted change
    LaunchedEffect(uiState.isMuted) {
        sharedPlayer.volume = if (uiState.isMuted) 0f else 1f
    }

    DisposableEffect(Unit) {
        onDispose { sharedPlayer.release() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentType by rememberUpdatedState(currentAsset?.type)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                sharedPlayer.playWhenReady = false
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (currentType == "VIDEO") {
                    sharedPlayer.playWhenReady = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { _ ->
        viewModel.onLocalDeleteIntentHandled()
    }

    LaunchedEffect(uiState.localDeletePendingIntent) {
        uiState.localDeletePendingIntent?.let { pendingIntent ->
            try {
                deleteLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } catch (_: Exception) {
                viewModel.onLocalDeleteIntentHandled()
            }
        }
    }

    var showSortMenu by remember { mutableStateOf(value = false) }
    val connectionStatus by SessionManager.connectionStatus.collectAsState()

    LaunchedEffect(connectionStatus.level) {
        if ((uiState.error != null) && (connectionStatus.level == ConnectionLevel.ONLINE)) {
            viewModel.retryLoading()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.downloadRequestSignal.collect { asset ->
            val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/") ?: return@collect
            val apiKey = SessionManager.getApiKey() ?: return@collect

            // Using /original for the raw file, which is often more reliable than /download (which can return a zip)
            val downloadUrl = "$baseUrl/api/assets/${asset.id}/original"

            // Sanitize filename: remove path components and keep only the name
            val rawName = asset.originalFileName ?: "immich_${asset.id}.${asset.fileExtension ?: "jpg"}"
            val fileName = rawName.substringAfterLast('/').substringAfterLast('\\')

            try {
                val request = DownloadManager.Request(downloadUrl.toUri())
                    .setTitle(fileName)
                    .setDescription("Downloading from Immich")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .addRequestHeader("x-api-key", apiKey)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)

                Toast.makeText(context, "Download started: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                AppLogger.e("Download", "Error starting download", e)
                Toast.makeText(context, "Error starting download: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.shareRequestSignal.collect { asset ->
            val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/") ?: return@collect
            val apiKey = SessionManager.getApiKey() ?: return@collect
            val shareUrl = "$baseUrl/api/assets/${asset.id}/original"

            Toast.makeText(context, "Preparing share...", Toast.LENGTH_SHORT).show()

            scope.launch(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url(shareUrl)
                        .addHeader("x-api-key", apiKey)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Server returned ${response.code}")

                        val body = response.body ?: throw Exception("Empty response body")
                        val fileName = asset.originalFileName?.substringAfterLast('/')?.substringAfterLast('\\')
                            ?: "immich_${asset.id}.${asset.fileExtension ?: "jpg"}"

                        val cacheDir = File(context.cacheDir, "shared_assets").apply { mkdirs() }
                        val file = File(cacheDir, fileName)

                        file.outputStream().use { output ->
                            body.byteStream().use { input ->
                                input.copyTo(output)
                            }
                        }

                        val contentUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )

                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = if (asset.type == "VIDEO") "video/*" else "image/*"
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share media"))
                    }
                } catch (e: Exception) {
                    AppLogger.e("Share", "Error sharing asset", e)
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SwipeHeader(
            uiState = uiState,
            onSummaryClick = { viewModel.toggleSummary(visible = true) }
        )

        AssetTimeline(
            assets = uiState.assets,
            decisions = uiState.decisions,
            currentIndex = uiState.currentIndex,
            isFavorite = { uiState.isFavorite(it) },
            isArchived = { uiState.isArchived(it) },
            isLocked = { uiState.isLocked(it) },
            onAssetClick = { viewModel.onMoveToAsset(it) },
            isBulkMode = uiState.isBulkDeleteMode || uiState.isBulkKeepMode,
            bulkSelection = uiState.bulkSelection,
            isBulkDelete = uiState.isBulkDeleteMode
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading && uiState.assets.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    if (uiState.syncTotalCount > 0) {
                        Text(
                            text = "Loading album metadata... ${uiState.syncLoadedCount} / ${uiState.syncTotalCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { (uiState.syncLoadedCount.toFloat() / uiState.syncTotalCount).coerceIn(0f, 1f) },
                            modifier = Modifier.width(200.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            } else if (uiState.error != null && uiState.assets.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { viewModel.retryLoading() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.common_retry))
                    }
                }
            } else if (uiState.currentIndex < uiState.assets.size) {
                val currentIndex = uiState.currentIndex
                val assets = uiState.assets

                val isBulk = uiState.isBulkDeleteMode || uiState.isBulkKeepMode
                val mainIndex = uiState.bulkLastIndex ?: currentIndex
                val nextUnprocessedIndex = if (isBulk) {
                    val candidate = (uiState.bulkLastIndex ?: currentIndex) + 1
                    if (candidate < assets.size) candidate else -1
                } else {
                    viewModel.getNextUnprocessedIndex()
                }
                
                val visibleIndices = listOfNotNull(
                    mainIndex,
                    nextUnprocessedIndex.takeIf { it != -1 && it != mainIndex }
                ).distinct().reversed()

                var topCardOffsetX by remember(mainIndex) { mutableFloatStateOf(0f) }

                visibleIndices.forEach { index ->
                    val asset = assets[index]
                    val isNextCard = index > mainIndex
                    key(asset.id) {
                        SwipeCard(
                            asset = asset,
                            isNext = isNextCard,
                            isFullscreenOpen = uiState.isFullscreenMode,
                            providedPlayer = if (!isNextCard && !uiState.isFullscreenMode) sharedPlayer else null,
                            isMuted = uiState.isMuted,
                            topCardSwipeOffset = if (isNextCard) topCardOffsetX else 0f,
                            config = SwipeCardConfig(
                                playbackBehavior = uiState.playbackBehavior,
                                fullscreenButtonPosition = uiState.fullscreenButtonPosition,
                                immichButtonPosition = uiState.immichButtonPosition,
                                cardDisplayButtonPosition = uiState.cardDisplayButtonPosition,
                                muteButtonPosition = uiState.muteButtonPosition,
                                downloadButtonPosition = uiState.downloadButtonPosition,
                                shareButtonPosition = uiState.shareButtonPosition,
                                showFullscreenButton = uiState.showFullscreenButton,
                                showImmichButton = uiState.showImmichButton,
                                showCardDisplayButton = uiState.showCardDisplayButton,
                                showMuteButton = uiState.showMuteButton,
                                showDownloadButton = uiState.showDownloadButton,
                                showShareButton = uiState.showShareButton,
                                cardDisplayMode = uiState.cardDisplayMode,
                                tapToSwipeEnabled = uiState.tapToSwipeEnabled,
                                showSizeIndicator = (uiState.sortOrder == SortOrder.SIZE_DESC) || (uiState.sortOrder == SortOrder.SIZE_ASC)
                            ),
                            actions = SwipeCardActions(
                                onSwipe = { viewModel.onSwipe(it) },
                                onToggleDisplayMode = { viewModel.toggleDisplayMode() },
                                onDoubleTap = { viewModel.toggleFavorite() },
                                onOpenFullscreen = { viewModel.toggleFullscreen(true) },
                                onDownload = { viewModel.downloadAsset(it) },
                                onShare = { viewModel.shareAsset(it) },
                                onToggleMute = { viewModel.toggleMute() },
                                onSwipeOffsetChanged = { offset ->
                                    if (!isNextCard) {
                                        topCardOffsetX = offset
                                    }
                                }
                            )
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.swipe_congratulations),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.toggleSummary(true) },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.swipe_sync_changes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if ((uiState.isFetchingAssets || uiState.isLoading) && uiState.assets.isNotEmpty()) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .zIndex(10f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.swipe_fetching_assets),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (uiState.showSwipeButtons) 16.dp else 24.dp),
                horizontalArrangement = if (uiState.showSwipeButtons) Arrangement.SpaceEvenly else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.showSwipeButtons) {
                    val haptic = LocalHapticFeedback.current
                    var isLongPressActive by remember { mutableStateOf(false) }
                    var dragStartedX by remember { mutableFloatStateOf(0f) }
                    val currentUiStateDelete by rememberUpdatedState(uiState)
                    val densityDelete = LocalDensity.current
                    val stepPxDelete = with(densityDelete) { 36.dp.toPx() }

                    FloatingActionButton(
                        onClick = { if (!uiState.isBulkDeleteMode && !uiState.isBulkKeepMode) viewModel.onSwipe(SwipeDecision.DELETE) },
                        containerColor = if (uiState.isBulkDeleteMode) MaterialRed else MaterialTheme.colorScheme.errorContainer,
                        contentColor = if (uiState.isBulkDeleteMode) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        isLongPressActive = true
                                        dragStartedX = offset.x
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.enterBulkMode(isDelete = true)
                                    },
                                    onDragEnd = {
                                        isLongPressActive = false
                                        viewModel.executeBulkAction()
                                    },
                                    onDragCancel = {
                                        isLongPressActive = false
                                        viewModel.exitBulkMode()
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        if (isLongPressActive) {
                                            val state = currentUiStateDelete
                                            val totalDrag = change.position.x - dragStartedX
                                            if (totalDrag > 0) {
                                                val itemsToSelect = (totalDrag / stepPxDelete).toInt()
                                                val selection = mutableSetOf<String>()
                                                var lastIdx = state.currentIndex
                                                for (i in 0..itemsToSelect) {
                                                    val idx = state.currentIndex + i
                                                    if (idx < state.assets.size) {
                                                        selection.add(state.assets[idx].id)
                                                        lastIdx = idx
                                                    }
                                                }
                                                viewModel.setBulkSelection(selection, lastIdx)
                                            } else {
                                                viewModel.setBulkSelection(
                                                    setOfNotNull(state.assets.getOrNull(state.currentIndex)?.id),
                                                    state.currentIndex
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        Icon(
                            imageVector = if (uiState.isBulkDeleteMode) Icons.Default.DeleteSweep else Icons.Default.Delete,
                            contentDescription = stringResource(R.string.swipe_delete)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = uiState.currentIndex > 0 || uiState.history.isNotEmpty(),
                    modifier = Modifier.size(if (uiState.showSwipeButtons) 36.dp else 44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.nav_back),
                        modifier = Modifier.size(if (uiState.showSwipeButtons) 22.dp else 26.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (!uiState.swapSummaryArchive) viewModel.toggleArchive()
                        else viewModel.toggleSummary(true)
                    },
                    modifier = Modifier.size(if (uiState.showSwipeButtons) 36.dp else 44.dp)
                ) {
                    Icon(
                        imageVector = if (!uiState.swapSummaryArchive) Icons.Default.Archive else Icons.Default.Assessment,
                        contentDescription = if (!uiState.swapSummaryArchive) stringResource(R.string.swipe_archive) else stringResource(R.string.swipe_summary_title),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(if (uiState.showSwipeButtons) 22.dp else 26.dp)
                    )
                }

                if (uiState.showFavoriteButton) {
                    val isFav = uiState.currentAsset?.let { uiState.isFavorite(it.id) } ?: false
                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        modifier = Modifier.size(if (uiState.showSwipeButtons) 36.dp else 44.dp)
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.swipe_favorite),
                            tint = if (isFav) Color.Red else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(if (uiState.showSwipeButtons) 22.dp else 26.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.toggleLock() },
                    modifier = Modifier.size(if (uiState.showSwipeButtons) 36.dp else 44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.swipe_locked),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(if (uiState.showSwipeButtons) 22.dp else 26.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.size(if (uiState.showSwipeButtons) 36.dp else 44.dp)
                    ) {
                        val icon = when (uiState.sortOrder) {
                            SortOrder.SHUFFLED -> Icons.Default.Shuffle
                            SortOrder.CHRONOLOGICAL_ASC -> Icons.Default.ArrowUpward
                            SortOrder.CHRONOLOGICAL_DESC -> Icons.Default.ArrowDownward
                            SortOrder.SIZE_DESC -> Icons.Default.ExpandMore
                            SortOrder.SIZE_ASC -> Icons.Default.ExpandLess
                            SortOrder.TYPE_VIDEO_FIRST, SortOrder.TYPE_VIDEO_FIRST_ASC, SortOrder.TYPE_VIDEO_FIRST_SHUFFLED -> Icons.Default.Videocam
                            SortOrder.TYPE_PHOTO_FIRST, SortOrder.TYPE_PHOTO_FIRST_ASC, SortOrder.TYPE_PHOTO_FIRST_SHUFFLED -> Icons.Default.Image
                        }

                        val tint = if (uiState.sortOrder != SortOrder.CHRONOLOGICAL_DESC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        val baseSize = if (uiState.showSwipeButtons) 24.dp else 28.dp

                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(R.string.settings_sort_order_label),
                            tint = tint,
                            modifier = Modifier.size(baseSize)
                        )
                    }

                    if (showSortMenu) {
                        Popup(
                            alignment = Alignment.BottomCenter,
                            offset = IntOffset(0, -110),
                            onDismissRequest = { showSortMenu = false },
                            properties = PopupProperties(focusable = true)
                        ) {
                            Surface(
                                modifier = Modifier.width(300.dp),
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp,
                                shadowElevation = 12.dp,
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CategoryButton(
                                            text = stringResource(R.string.sort_category_time),
                                            selected = uiState.sortCategory == SortCategory.TIME,
                                            onClick = { viewModel.setSortCategory(SortCategory.TIME) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        CategoryButton(
                                            text = stringResource(R.string.sort_category_size),
                                            selected = uiState.sortCategory == SortCategory.SIZE,
                                            onClick = { viewModel.setSortCategory(SortCategory.SIZE) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        CategoryButton(
                                            text = stringResource(R.string.sort_category_type),
                                            selected = uiState.sortCategory == SortCategory.TYPE,
                                            onClick = { viewModel.setSortCategory(SortCategory.TYPE) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp)
                                    Spacer(Modifier.height(8.dp))

                                    when (uiState.sortCategory) {
                                        SortCategory.TIME -> {
                                            SortPopupItem(R.string.settings_sort_newest, Icons.Default.ArrowDownward, uiState.sortOrder == SortOrder.CHRONOLOGICAL_DESC) {
                                                viewModel.setSortOrder(SortOrder.CHRONOLOGICAL_DESC)
                                                showSortMenu = false
                                            }
                                            SortPopupItem(R.string.settings_sort_oldest, Icons.Default.ArrowUpward, uiState.sortOrder == SortOrder.CHRONOLOGICAL_ASC) {
                                                viewModel.setSortOrder(SortOrder.CHRONOLOGICAL_ASC)
                                                showSortMenu = false
                                            }
                                            SortPopupItem(R.string.settings_sort_shuffled, Icons.Default.Shuffle, uiState.sortOrder == SortOrder.SHUFFLED) {
                                                viewModel.setSortOrder(SortOrder.SHUFFLED)
                                                showSortMenu = false
                                            }
                                        }
                                        SortCategory.SIZE -> {
                                            SortPopupItem(R.string.settings_sort_biggest, Icons.Default.ExpandMore, uiState.sortOrder == SortOrder.SIZE_DESC) {
                                                viewModel.setSortOrder(SortOrder.SIZE_DESC)
                                                showSortMenu = false
                                            }
                                            SortPopupItem(R.string.settings_sort_smallest, Icons.Default.ExpandLess, uiState.sortOrder == SortOrder.SIZE_ASC) {
                                                viewModel.setSortOrder(SortOrder.SIZE_ASC)
                                                showSortMenu = false
                                            }
                                        }
                                        SortCategory.TYPE -> {
                                            val currentIsPhoto = uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST || 
                                                                uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST_ASC || 
                                                                uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST_SHUFFLED

                                            SortPopupItem(R.string.settings_sort_videos, Icons.Default.Videocam, !currentIsPhoto) {
                                                val subOrder = when(uiState.sortOrder) {
                                                    SortOrder.TYPE_PHOTO_FIRST_ASC -> SortOrder.TYPE_VIDEO_FIRST_ASC
                                                    SortOrder.TYPE_PHOTO_FIRST_SHUFFLED -> SortOrder.TYPE_VIDEO_FIRST_SHUFFLED
                                                    else -> SortOrder.TYPE_VIDEO_FIRST
                                                }
                                                viewModel.setSortOrder(subOrder)
                                                showSortMenu = false
                                            }
                                            SortPopupItem(R.string.settings_sort_photos, Icons.Default.Image, currentIsPhoto) {
                                                val subOrder = when(uiState.sortOrder) {
                                                    SortOrder.TYPE_VIDEO_FIRST_ASC -> SortOrder.TYPE_PHOTO_FIRST_ASC
                                                    SortOrder.TYPE_VIDEO_FIRST_SHUFFLED -> SortOrder.TYPE_PHOTO_FIRST_SHUFFLED
                                                    else -> SortOrder.TYPE_PHOTO_FIRST
                                                }
                                                viewModel.setSortOrder(subOrder)
                                                showSortMenu = false
                                            }

                                            Spacer(Modifier.height(8.dp))
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp)
                                            Spacer(Modifier.height(8.dp))

                                            SortPopupItem(
                                                R.string.settings_sort_newest, 
                                                Icons.Default.ArrowDownward, 
                                                uiState.sortOrder == SortOrder.TYPE_VIDEO_FIRST || uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST
                                            ) {
                                                viewModel.setSortOrder(if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST else SortOrder.TYPE_VIDEO_FIRST)
                                                showSortMenu = false
                                            }
                                            SortPopupItem(
                                                R.string.settings_sort_oldest, 
                                                Icons.Default.ArrowUpward, 
                                                uiState.sortOrder == SortOrder.TYPE_VIDEO_FIRST_ASC || uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST_ASC
                                            ) {
                                                viewModel.setSortOrder(if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST_ASC else SortOrder.TYPE_VIDEO_FIRST_ASC)
                                                showSortMenu = false
                                            }
                                            SortPopupItem(
                                                R.string.settings_sort_shuffled, 
                                                Icons.Default.Shuffle, 
                                                uiState.sortOrder == SortOrder.TYPE_VIDEO_FIRST_SHUFFLED || uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST_SHUFFLED
                                            ) {
                                                viewModel.setSortOrder(if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST_SHUFFLED else SortOrder.TYPE_VIDEO_FIRST_SHUFFLED)
                                                showSortMenu = false
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.showSwipeButtons) {
                    val haptic = LocalHapticFeedback.current
                    var isLongPressActive by remember { mutableStateOf(false) }
                    var dragStartedX by remember { mutableFloatStateOf(0f) }
                    val currentUiStateKeep by rememberUpdatedState(uiState)
                    val densityKeep = LocalDensity.current
                    val stepPxKeep = with(densityKeep) { 36.dp.toPx() }

                    FloatingActionButton(
                        onClick = { if (!uiState.isBulkKeepMode && !uiState.isBulkDeleteMode) viewModel.onSwipe(SwipeDecision.KEEP) },
                        containerColor = if (uiState.isBulkKeepMode) MaterialGreen else MaterialGreen,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        isLongPressActive = true
                                        dragStartedX = offset.x
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.enterBulkMode(isDelete = false)
                                    },
                                    onDragEnd = {
                                        isLongPressActive = false
                                        viewModel.executeBulkAction()
                                    },
                                    onDragCancel = {
                                        isLongPressActive = false
                                        viewModel.exitBulkMode()
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        if (isLongPressActive) {
                                            val state = currentUiStateKeep
                                            val totalDrag = dragStartedX - change.position.x
                                            if (totalDrag > 0) {
                                                val itemsToSelect = (totalDrag / stepPxKeep).toInt()
                                                val selection = mutableSetOf<String>()
                                                var lastIdx = state.currentIndex
                                                for (i in 0..itemsToSelect) {
                                                    val idx = state.currentIndex + i
                                                    if (idx < state.assets.size) {
                                                        selection.add(state.assets[idx].id)
                                                        lastIdx = idx
                                                    }
                                                }
                                                viewModel.setBulkSelection(selection, lastIdx)
                                            } else {
                                                viewModel.setBulkSelection(
                                                    setOfNotNull(state.assets.getOrNull(state.currentIndex)?.id),
                                                    state.currentIndex
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        Icon(
                            imageVector = if (uiState.isBulkKeepMode) Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = stringResource(R.string.swipe_keep)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(80.dp))
        }
    }

    if (uiState.showSummary) {
        SummaryDialog(
            uiState = uiState,
            onDismiss = { viewModel.toggleSummary(false) },
            onApply = { viewModel.applyChanges() },
            onUndoDecision = { viewModel.undoSpecificDecision(it) }
        )
    }

    if (uiState.isFullscreenMode && uiState.currentAsset != null) {
        val currentAsset = uiState.currentAsset!!
        val nextUnprocessedIndex = viewModel.getNextUnprocessedIndex()
        val nextAsset = if (nextUnprocessedIndex != -1 && nextUnprocessedIndex < uiState.assets.size) {
            uiState.assets[nextUnprocessedIndex]
        } else null

        FullscreenViewer(
            asset = currentAsset,
            nextAsset = nextAsset,
            isFavorite = uiState.isFavorite(currentAsset.id),
            muteButtonPosition = uiState.muteButtonPosition,
            onSwipe = {
                viewModel.onSwipe(it)
            },
            onUndo = { viewModel.undo() },
            onDoubleTap = { viewModel.toggleFavorite() },
            onClose = { viewModel.toggleFullscreen(false) },
            tapToSwipeEnabled = uiState.tapToSwipeEnabled,
            providedPlayer = sharedPlayer,
            showSizeIndicator = uiState.sortOrder == SortOrder.SIZE_DESC || uiState.sortOrder == SortOrder.SIZE_ASC,
            isMuted = uiState.isMuted,
            onToggleMute = { viewModel.toggleMute() },
            showMuteButton = true, // Mute button is always shown in fullscreen for videos
            downloadButtonPosition = uiState.downloadButtonPosition,
            showDownloadButton = false, // Download button hidden in fullscreen mode as requested
            onDownload = { viewModel.downloadAsset(it) },
            shareButtonPosition = uiState.shareButtonPosition,
            showShareButton = false, // Share button hidden in fullscreen mode for consistency
            onShare = { viewModel.shareAsset(it) }
        )
    }

    if (uiState.showSuccessAnimation) {
        SuccessAnimationOverlay()
    }

    if (uiState.showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleResetConfirmation(false) },
            title = { Text(stringResource(R.string.swipe_reset_confirm_title)) },
            text = { Text(stringResource(R.string.swipe_reset_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetAlbumDecisions() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.swipe_reset_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleResetConfirmation(false) }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (uiState.isBulkDeleteMode || uiState.isBulkKeepMode) {
        val isDelete = uiState.isBulkDeleteMode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable { viewModel.exitBulkMode() }
                .zIndex(500f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isDelete) Icons.Default.DeleteSweep else Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (isDelete) "Bulk Delete: ${uiState.bulkSelection.size} selected" else "Bulk Keep: ${uiState.bulkSelection.size} selected",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isDelete) "Slide right to select, release to delete" else "Slide left to select, release to keep",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
