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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.ConnectionLevel
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.core.cache.VideoCache
import com.markvoronin.immichswipe.core.cache.VideoPreloader
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.feature.swipe.components.ResetConfirmationDialog
import com.markvoronin.immichswipe.feature.swipe.components.SwipeActionBar
import com.markvoronin.immichswipe.feature.swipe.components.SwipeBulkOverlay
import com.markvoronin.immichswipe.feature.swipe.components.SwipeCardDeck
import androidx.activity.compose.BackHandler
import com.markvoronin.immichswipe.feature.swipe.components.AddToAlbumDialog
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
    sessionKey: String,
    resetSignal: SharedFlow<Unit>,
    modifier: Modifier = Modifier,
    userQuotaBytes: Long? = null,
    onBack: () -> Unit = {}
) {
    val viewModel: SwipeViewModel = hiltViewModel(key = "$sessionKey-${album.id}")

    LaunchedEffect(album, userQuotaBytes) {
        viewModel.initAlbum(album, userQuotaBytes)
    }

    BackHandler(enabled = true) {
        val state = viewModel.uiState.value
        when {
            state.showSummary -> viewModel.toggleSummary(false)
            state.isFullscreenMode -> viewModel.toggleFullscreen(false)
            state.showResetConfirmation -> viewModel.toggleResetConfirmation(false)
            state.isBulkDeleteMode || state.isBulkKeepMode -> viewModel.exitBulkMode()
            else -> onBack()
        }
    }
    
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
        
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        val handleAudioFocus = (playbackBehavior != PlaybackBehavior.IGNORE)

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, handleAudioFocus)
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
            // Do NOT stop/clear the player, as it forces the underlying surface to go blank
            // which causes a flicker when recycling the TextureView.
            // Just update the media source.
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
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        val handleAudioFocus = (playbackBehavior != PlaybackBehavior.IGNORE)
        sharedPlayer.setAudioAttributes(audioAttributes, handleAudioFocus)
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
        var wasPlayingBeforeAppPause = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                wasPlayingBeforeAppPause = sharedPlayer.playWhenReady
                sharedPlayer.playWhenReady = false
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (currentType == "VIDEO" && wasPlayingBeforeAppPause) {
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
            isBulkDelete = uiState.isBulkDeleteMode,
            getRotation = { uiState.getRotation(it) }
        )

        SwipeCardDeck(
            uiState = uiState,
            viewModel = viewModel,
            sharedPlayer = sharedPlayer,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            SwipeActionBar(
                uiState = uiState,
                viewModel = viewModel,
                showSortMenu = showSortMenu,
                onSortMenuToggle = { showSortMenu = it }
            )

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
            onShare = { viewModel.shareAsset(it) },
            rotation = uiState.getRotation(currentAsset.id)
        )
    }

    if (uiState.showSuccessAnimation) {
        SuccessAnimationOverlay()
    }

    if (uiState.showResetConfirmation) {
        ResetConfirmationDialog(
            onDismiss = { viewModel.toggleResetConfirmation(false) },
            onConfirm = { viewModel.resetAlbumDecisions() }
        )
    }

    if (uiState.isBulkDeleteMode || uiState.isBulkKeepMode) {
        val isDelete = uiState.isBulkDeleteMode
        SwipeBulkOverlay(
            isDelete = isDelete,
            selectionCount = uiState.bulkSelection.size,
            onDismiss = { viewModel.exitBulkMode() }
        )
    }

    if (uiState.showAddToAlbumDialog) {
        val successTemplate = stringResource(R.string.add_to_album_success)
        val errorTemplate = stringResource(R.string.add_to_album_error)

        AddToAlbumDialog(
            albums = uiState.albumsForAddToAlbum,
            isLoading = uiState.isFetchingAlbumsForDialog,
            onAlbumSelect = { targetAlbum ->
                viewModel.addCurrentAssetToAlbum(targetAlbum) { success, albumName ->
                    viewModel.dismissAddToAlbumDialog()
                    val message = if (success) {
                        successTemplate.format(albumName)
                    } else {
                        errorTemplate.format(albumName)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { viewModel.dismissAddToAlbumDialog() }
        )
    }
}
