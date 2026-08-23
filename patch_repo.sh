#!/bin/bash
cat << 'KOTLIN' > app/src/main/java/com/markvoronin/immichswipe/feature/swipe/SwipeScreen.kt
package com.markvoronin.immichswipe.feature.swipe

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.*
import com.markvoronin.immichswipe.core.cache.VideoCache
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SharedTransitionScope.SwipeScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    album: Album,
    assetRepository: AssetRepository,
    swipeDecisionRepository: SwipeDecisionRepository,
    sessionRepository: SessionRepository,
    sessionKey: String,
    resetSignal: kotlinx.coroutines.flow.SharedFlow<Unit>,
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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playbackBehavior = uiState.playbackBehavior
    val currentAsset = uiState.currentAsset

    val sharedPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(uiState.isLoading, currentAsset?.id) {
        if (uiState.isLoading) {
            sharedPlayer.playWhenReady = false
        } else if (currentAsset?.type == "VIDEO") {
            sharedPlayer.playWhenReady = true
        }
    }

    val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
    val apiKey = SessionManager.getApiKey() ?: ""

    LaunchedEffect(currentAsset?.id) {
        val asset = currentAsset
        if (asset?.type == "VIDEO") {
            val videoUrl = "$baseUrl/api/assets/${asset.id}/video/playback"
            val dataSourceFactory = VideoCache.getCacheDataSourceFactory(context, apiKey)
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.Builder()
                    .setUri(videoUrl)
                    .setMediaId(asset.id)
                    .setCustomCacheKey(asset.id)
                    .build())
            
            sharedPlayer.setMediaSource(mediaSource, true)
            sharedPlayer.prepare()
            sharedPlayer.playWhenReady = !uiState.isLoading
        } else {
            sharedPlayer.stop()
            sharedPlayer.clearMediaItems()
        }
    }

    LaunchedEffect(playbackBehavior) {
        if (playbackBehavior != PlaybackBehavior.IGNORE) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            sharedPlayer.setAudioAttributes(audioAttributes, true)
        }
    }

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
                if (currentType == "VIDEO" && !uiState.isFullscreenMode) {
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
            val downloadUrl = "$baseUrl/api/assets/${asset.id}/original"
            val rawName = asset.originalFileName ?: "immich_${asset.id}.${asset.fileExtension ?: "jpg"}"
            val fileName = rawName.substringAfterLast('/').substringAfterLast('\\')

            try {
                val request = android.app.DownloadManager.Request(downloadUrl.toUri())
                    .setTitle(fileName)
                    .setDescription("Downloading from Immich")
                    .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                    .addRequestHeader("x-api-key", apiKey)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

                val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                dm.enqueue(request)
                android.widget.Toast.makeText(context, "Download started", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                AppLogger.e("Download", "Error downloading asset", e)
                android.widget.Toast.makeText(context, "Failed to download: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.shareRequestSignal.collect { asset ->
            try {
                val file = assetRepository.downloadAssetForSharing(context, asset)
                if (file != null) {
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
                android.widget.Toast.makeText(context, "Failed to share: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (uiState.isLoading && uiState.assets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                    }
                } else if (uiState.error != null && uiState.assets.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    val mainIndex = uiState.bulkLastIndex ?: currentIndex
                    val nextUnprocessedIndex = viewModel.getNextUnprocessedIndex()
                    
                    val visibleIndices = listOfNotNull(
                        mainIndex,
                        nextUnprocessedIndex.takeIf { it != -1 && it != mainIndex }
                    ).distinct().reversed()

                    visibleIndices.forEach { index ->
                        val asset = assets[index]
                        val isNextCard = index > mainIndex
                        key(asset.id) {
                            SwipeCard(
                                asset = asset,
                                onSwipe = { viewModel.onSwipe(it) },
                                isNext = isNextCard,
                                playbackBehavior = uiState.playbackBehavior,
                                fullscreenButtonPosition = uiState.fullscreenButtonPosition,
                                immichButtonPosition = uiState.immichButtonPosition,
                                cardDisplayButtonPosition = uiState.cardDisplayButtonPosition,
                                muteButtonPosition = uiState.muteButtonPosition,
                                showFullscreenButton = uiState.showFullscreenButton,
                                showImmichButton = uiState.showImmichButton,
                                showCardDisplayButton = uiState.showCardDisplayButton,
                                showMuteButton = uiState.showMuteButton,
                                cardDisplayMode = uiState.cardDisplayMode,
                                onToggleDisplayMode = { viewModel.toggleDisplayMode() },
                                isFullscreenOpen = uiState.isFullscreenMode,
                                onDoubleTap = { viewModel.toggleFavorite() },
                                onOpenFullscreen = { viewModel.toggleFullscreen(true) },
                                tapToSwipeEnabled = uiState.tapToSwipeEnabled,
                                providedPlayer = if (!isNextCard && !uiState.isFullscreenMode) sharedPlayer else null,
                                showSizeIndicator = (uiState.sortOrder == SortOrder.SIZE_DESC) || (uiState.sortOrder == SortOrder.SIZE_ASC),
                                isMuted = uiState.isMuted,
                                onToggleMute = { viewModel.toggleMute() },
                                onDownload = { viewModel.downloadAsset(it) },
                                onShare = { viewModel.shareAsset(it) }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialGreen.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.swipe_all_done),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.swipe_all_done_subtitle, uiState.assets.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.toggleSummary(true) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.swipe_summary_button))
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                if (uiState.showSwipeButtons) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.onSwipe(SwipeDecision.DELETE) },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialRed.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.undo() },
                                enabled = uiState.history.isNotEmpty(),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    tint = if (uiState.history.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            
                            Spacer(Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = { viewModel.toggleSummary(true) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "Summary",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.onSwipe(SwipeDecision.KEEP) },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialGreen.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Keep",
                                tint = MaterialGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = uiState.history.isNotEmpty(),
                            modifier = Modifier.size(if (uiState.showSwipeButtons) 36.dp else 44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (uiState.history.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        IconButton(
                            onClick = { viewModel.toggleSummary(true) },
                            modifier = Modifier.size(if (uiState.showSwipeButtons) 36.dp else 44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Summary",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
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
                                
                                Icon(
                                    imageVector = icon,
                                    contentDescription = stringResource(R.string.settings_sort_order_label),
                                    tint = tint
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                offset = DpOffset(0.dp, 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.width(220.dp).background(MaterialTheme.colorScheme.surface)
                            ) {
                                val categories = listOf(
                                    Triple(SortCategory.TIME, stringResource(R.string.settings_sort_category_time), Icons.Default.Schedule),
                                    Triple(SortCategory.SIZE, stringResource(R.string.settings_sort_category_size), Icons.Default.SdStorage),
                                    Triple(SortCategory.TYPE, stringResource(R.string.settings_sort_category_type), Icons.Default.Category)
                                )
                                
                                categories.forEach { (cat, label, icon) ->
                                    val isSelected = uiState.sortCategory == cat
                                    DropdownMenuItem(
                                        text = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                                Spacer(Modifier.width(12.dp))
                                                Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                            }
                                        },
                                        onClick = {
                                            viewModel.setSortCategory(cat)
                                            showSortMenu = false
                                        }
                                    )
                                    
                                    if (isSelected) {
                                        when (cat) {
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
                                                SortPopupItem(R.string.settings_sort_largest, Icons.Default.ExpandMore, uiState.sortOrder == SortOrder.SIZE_DESC) {
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
                                                }
                                                SortPopupItem(R.string.settings_sort_photos, Icons.Default.Image, currentIsPhoto) {
                                                    val subOrder = when(uiState.sortOrder) {
                                                        SortOrder.TYPE_VIDEO_FIRST_ASC -> SortOrder.TYPE_PHOTO_FIRST_ASC
                                                        SortOrder.TYPE_VIDEO_FIRST_SHUFFLED -> SortOrder.TYPE_PHOTO_FIRST_SHUFFLED
                                                        else -> SortOrder.TYPE_PHOTO_FIRST
                                                    }
                                                    viewModel.setSortOrder(subOrder)
                                                }

                                                Spacer(Modifier.height(8.dp))
                                                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp)
                                                Spacer(Modifier.height(8.dp))

                                                SortPopupItem(R.string.settings_sort_newest, Icons.Default.ArrowDownward, uiState.sortOrder == SortOrder.TYPE_VIDEO_FIRST || uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST) {
                                                    viewModel.setSortOrder(if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST else SortOrder.TYPE_VIDEO_FIRST)
                                                }
                                                SortPopupItem(R.string.settings_sort_oldest, Icons.Default.ArrowUpward, uiState.sortOrder == SortOrder.TYPE_VIDEO_FIRST_ASC || uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST_ASC) {
                                                    viewModel.setSortOrder(if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST_ASC else SortOrder.TYPE_VIDEO_FIRST_ASC)
                                                }
                                                SortPopupItem(R.string.settings_sort_shuffled, Icons.Default.Shuffle, uiState.sortOrder == SortOrder.TYPE_VIDEO_FIRST_SHUFFLED || uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST_SHUFFLED) {
                                                    viewModel.setSortOrder(if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST_SHUFFLED else SortOrder.TYPE_VIDEO_FIRST_SHUFFLED)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (uiState.assets.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "${uiState.currentIndex + 1} / ${uiState.assets.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
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

        if (uiState.showResetConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleResetConfirmation(false) },
                title = { Text(stringResource(R.string.swipe_reset_confirm_title)) },
                text = { Text(stringResource(R.string.swipe_reset_confirm_text)) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetAlbumDecisions() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.common_reset))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.toggleResetConfirmation(false) }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun SortPopupItem(labelRes: Int, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
        },
        onClick = onClick,
        modifier = Modifier.padding(start = 24.dp)
    )
}

@OptIn(UnstableApi::class)
@Composable
fun SwipeCard(
    asset: Asset,
    onSwipe: (SwipeDecision) -> Unit,
    isNext: Boolean,
    playbackBehavior: PlaybackBehavior,
    fullscreenButtonPosition: IconPosition,
    immichButtonPosition: IconPosition,
    cardDisplayButtonPosition: IconPosition,
    muteButtonPosition: IconPosition,
    showFullscreenButton: Boolean = true,
    showImmichButton: Boolean = true,
    showCardDisplayButton: Boolean = true,
    showMuteButton: Boolean = true,
    downloadButtonPosition: IconPosition = IconPosition.TOP_LEFT,
    showDownloadButton: Boolean = false,
    shareButtonPosition: IconPosition = IconPosition.TOP_RIGHT,
    showShareButton: Boolean = false,
    cardDisplayMode: CardDisplayMode,
    onToggleDisplayMode: () -> Unit,
    isFullscreenOpen: Boolean,
    onDoubleTap: () -> Unit,
    onOpenFullscreen: () -> Unit,
    tapToSwipeEnabled: Boolean = false,
    onDownload: (Asset) -> Unit = {},
    onShare: (Asset) -> Unit = {},
    providedPlayer: ExoPlayer? = null,
    showSizeIndicator: Boolean = false,
    isMuted: Boolean = false,
    onToggleMute: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
    val apiKey = SessionManager.getApiKey() ?: ""
    val lifecycleOwner = LocalLifecycleOwner.current

    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    var dragDirection by remember { mutableIntStateOf(0) } 

    var isHoldingByPress by remember(asset.id) { mutableStateOf(false) }
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
                if (playbackBehavior != PlaybackBehavior.IGNORE) {
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
        if (isVideoReady) {
            showLoadingIndicator = false
        } else if (asset.type == "VIDEO" && !isNext) {
            delay(500)
            if (!isVideoReady) {
                showLoadingIndicator = true
            }
        }
    }

    LaunchedEffect(internalExoPlayer, isMuted) {
        internalExoPlayer?.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(pausedByHoldState, exoPlayer, asset.id) {
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

    val animatedScale by animateFloatAsState(
        targetValue = if (isNext) 0.85f else 1f,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "ScaleAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isNext) 0.6f else 1f)
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
                                val currentX = offsetX.value
                                val currentY = offsetY.value
                                val totalH = if (metadataHeightPx > 0) metadataHeightPx else 400f

                                if (dragDirection == 1) { // Horizontal swipe
                                    if (currentX > 250) {
                                        offsetX.animateTo(1500f, tween(150))
                                        onSwipe(SwipeDecision.KEEP)
                                    } else if (currentX < -250) {
                                        offsetX.animateTo(-1500f, tween(150))
                                        onSwipe(SwipeDecision.DELETE)
                                    } else {
                                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                } else if (dragDirection == 2) { // Vertical metadata
                                    val dragDeltaY = currentY - dragStartY
                                    val wasOpen = dragStartY < -totalH / 2
                                    
                                    if (wasOpen) {
                                        // If it was open, close it if we dragged down past 100px
                                        if (dragDeltaY > 100) {
                                            offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                        } else {
                                            offsetY.animateTo(-totalH, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                        }
                                    } else {
                                        // If it was closed, open it if we dragged up past 100px
                                        if (dragDeltaY < -100) {
                                            offsetY.animateTo(-totalH, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                        } else {
                                            offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                        }
                                    }
                                } else {
                                    // Small movement, reset everything
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    offsetY.animateTo(dragStartY, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                }
                                dragDirection = 0
                            }
                        },
                        onDragCancel = {
                            dragDirection = 0
                            scope.launch {
                                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                offsetY.animateTo(dragStartY, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            
                            val totalH = if (metadataHeightPx > 0) metadataHeightPx else 400f
                            
                            if (dragDirection == 0) {
                                // Priority: If panel is already open, strictly lock to vertical drag
                                if (abs(offsetY.value) > totalH * 0.1f) {
                                     dragDirection = 2
                                } else {
                                    val accumulatedDX = abs(offsetX.value - dragStartX)
                                    val accumulatedDY = abs(offsetY.value - dragStartY)
                                    
                                    if (accumulatedDX > 20 || accumulatedDY > 20) {
                                        // Lock to the axis that moved the most
                                        dragDirection = if (accumulatedDX > accumulatedDY) 1 else 2
                                    }
                                }
                            }

                            scope.launch {
                                if (dragDirection == 1) {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                } else if (dragDirection == 2) {
                                    offsetY.snapTo((offsetY.value + dragAmount.y).coerceIn(-totalH, 0f))
                                } else {
                                    // Track both until locked
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo((offsetY.value + dragAmount.y).coerceIn(-totalH, 0f))
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
                                if (!ignoreNextTap) {
                                    val width = size.width.toFloat()
                                    if (tapToSwipeEnabled) {
                                        when {
                                            offset.x < width / 3 -> onSwipe(SwipeDecision.DELETE)
                                            offset.x > 2 * width / 3 -> onSwipe(SwipeDecision.KEEP)
                                            else -> {
                                                onToggleMute()
                                                showMuteIndicator = true
                                            }
                                        }
                                    } else {
                                        onToggleMute()
                                        showMuteIndicator = true
                                    }
                                }
                                ignoreNextTap = false
                            },
                            onDoubleTap = onDoubleTap,
                            onPress = { offset, size ->
                                ignoreNextTap = false
                                val wasReleased = withTimeoutOrNull(500) {
                                    awaitRelease()
                                    true
                                }
                                if (wasReleased == true) {
                                    // Fast tap detected
                                    if (tapToSwipeEnabled) {
                                        val width = size.width.toFloat()
                                        when {
                                            offset.x < width / 3 -> {
                                                onSwipe(SwipeDecision.DELETE)
                                                ignoreNextTap = true
                                            }
                                            offset.x > 2 * width / 3 -> {
                                                onSwipe(SwipeDecision.KEEP)
                                                ignoreNextTap = true
                                            }
                                        }
                                    }
                                } else {
                                    // Hold detected
                                    ignoreNextTap = true
                                    pausedByHoldState = true
                                    isHoldingByPress = true
                                    try {
                                        awaitRelease()
                                    } catch (e: Exception) {
                                        // Ignore cancellation
                                    } finally {
                                        isHoldingByPress = false
                                        pausedByHoldState = false
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
                                showControls = !isHoldingByPress,
                                cardDisplayMode = cardDisplayMode,
                                fileSize = asset.exifInfo?.fileSizeInBytes,
                                showSize = showSizeIndicator
                            )

                            if (showLoadingIndicator) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    val photoRequest = ImageRequest.Builder(context)
                        .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview")
                        .addHeader("x-api-key", apiKey)
                        .crossfade(true)
                        .build()

                    ZoomableBox(
                        modifier = Modifier.fillMaxSize(),
                        resetOnRelease = true,
                        onTap = { offset, size ->
                            if (!ignoreNextTap && tapToSwipeEnabled) {
                                val width = size.width.toFloat()
                                when {
                                    offset.x < width / 3 -> onSwipe(SwipeDecision.DELETE)
                                    offset.x > 2 * width / 3 -> onSwipe(SwipeDecision.KEEP)
                                }
                            }
                            ignoreNextTap = false
                        },
                        onDoubleTap = onDoubleTap,
                        onPress = { offset, size ->
                            ignoreNextTap = false
                            val wasReleased = withTimeoutOrNull(500) {
                                awaitRelease()
                                true
                            }
                            if (wasReleased == true) {
                                // Fast tap detected
                                if (tapToSwipeEnabled) {
                                    val width = size.width.toFloat()
                                    when {
                                        offset.x < width / 3 -> {
                                            onSwipe(SwipeDecision.DELETE)
                                            ignoreNextTap = true
                                        }
                                        offset.x > 2 * width / 3 -> {
                                            onSwipe(SwipeDecision.KEEP)
                                            ignoreNextTap = true
                                        }
                                    }
                                }
                            } else {
                                // Hold detected
                                ignoreNextTap = true
                                isHoldingByPress = true
                                try {
                                    awaitRelease()
                                } catch (e: Exception) {
                                    // Ignore cancellation
                                } finally {
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

                    if (showSizeIndicator && !isNext) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isHoldingByPress,
                            enter = androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.fadeOut(),
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
                    // Tap-to-close area above the panel
                    if (offsetY.value < -10f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        scope.launch { 
                                            offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) 
                                        }
                                    }
                                }
                        )
                    }

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
                                    if (currentY <= -metadataHeightPx * 0.3f) {
                                        offsetY.animateTo(-metadataHeightPx, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    } else {
                                        offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                }
                            }
                        )
                    }
                }

                if (!isNext) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isHoldingByPress,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val densityLocal = LocalDensity.current
                        val panelPushDp = with(densityLocal) { (-offsetY.value).toDp() }

                        Box(modifier = Modifier.fillMaxSize()) {
                            // Top left side
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 16.dp, start = 16.dp)
                            ) {
                                val side = IconPosition.TOP_LEFT
                                if (showImmichButton && immichButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Open in Immich",
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, "$baseUrl/photos/${asset.id}".toUri())
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                                if (showMuteButton && muteButtonPosition == side && asset.type == "VIDEO") {
                                    SwipeActionIconButton(
                                        icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Mute",
                                        onClick = {
                                            onToggleMute()
                                            showMuteIndicator = true
                                        }
                                    )
                                }
                                if (showDownloadButton && downloadButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.Default.Download,
                                        contentDescription = "Download",
                                        onClick = { onDownload(asset) }
                                    )
                                }
                                if (showShareButton && shareButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.Default.Share,
                                        contentDescription = "Share",
                                        onClick = { onShare(asset) }
                                    )
                                }
                                if (showFullscreenButton && fullscreenButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        onClick = onOpenFullscreen
                                    )
                                }
                                if (showCardDisplayButton && cardDisplayButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = if (cardDisplayMode == CardDisplayMode.FILL) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Toggle Display Mode",
                                        onClick = onToggleDisplayMode
                                    )
                                }
                            }

                            // Top right side
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 16.dp, end = 16.dp)
                            ) {
                                val side = IconPosition.TOP_RIGHT
                                if (showImmichButton && immichButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Open in Immich",
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, "$baseUrl/photos/${asset.id}".toUri())
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                                if (showMuteButton && muteButtonPosition == side && asset.type == "VIDEO") {
                                    SwipeActionIconButton(
                                        icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Mute",
                                        onClick = {
                                            onToggleMute()
                                            showMuteIndicator = true
                                        }
                                    )
                                }
                                if (showDownloadButton && downloadButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.Default.Download,
                                        contentDescription = "Download",
                                        onClick = { onDownload(asset) }
                                    )
                                }
                                if (showShareButton && shareButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.Default.Share,
                                        contentDescription = "Share",
                                        onClick = { onShare(asset) }
                                    )
                                }
                                if (showFullscreenButton && fullscreenButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        onClick = onOpenFullscreen
                                    )
                                }
                                if (showCardDisplayButton && cardDisplayButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = if (cardDisplayMode == CardDisplayMode.FILL) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Toggle Display Mode",
                                        onClick = onToggleDisplayMode
                                    )
                                }
                            }

                            // Bottom left side
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 16.dp, bottom = 16.dp + panelPushDp)
                            ) {
                                val side = IconPosition.BOTTOM_LEFT
                                if (showImmichButton && immichButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Open in Immich",
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, "$baseUrl/photos/${asset.id}".toUri())
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                                if (showMuteButton && muteButtonPosition == side && asset.type == "VIDEO") {
                                    SwipeActionIconButton(
                                        icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Mute",
                                        onClick = {
                                            onToggleMute()
                                            showMuteIndicator = true
                                        }
                                    )
                                }
                                if (showDownloadButton && downloadButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.Default.Download,
                                        contentDescription = "Download",
                                        onClick = { onDownload(asset) }
                                    )
                                }
                                if (showShareButton && shareButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.Default.Share,
                                        contentDescription = "Share",
                                        onClick = { onShare(asset) }
                                    )
                                }
                                if (showFullscreenButton && fullscreenButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        onClick = onOpenFullscreen
                                    )
                                }
                                if (showCardDisplayButton && cardDisplayButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = if (cardDisplayMode == CardDisplayMode.FILL) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Toggle Display Mode",
                                        onClick = onToggleDisplayMode
                                    )
                                }
                            }

                            // Bottom right side
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = 16.dp + panelPushDp)
                            ) {
                                val side = IconPosition.BOTTOM_RIGHT
                                if (showImmichButton && immichButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Open in Immich",
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, "$baseUrl/photos/${asset.id}".toUri())
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                                if (showMuteButton && muteButtonPosition == side && asset.type == "VIDEO") {
                                    SwipeActionIconButton(
                                        icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Mute",
                                        onClick = {
                                            onToggleMute()
                                            showMuteIndicator = true
                                        }
                                    )
                                }
                                if (showDownloadButton && downloadButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.Default.Download,
                                        contentDescription = "Download",
                                        onClick = { onDownload(asset) }
                                    )
                                }
                                if (showShareButton && shareButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.Default.Share,
                                        contentDescription = "Share",
                                        onClick = { onShare(asset) }
                                    )
                                }
                                if (showFullscreenButton && fullscreenButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        onClick = onOpenFullscreen
                                    )
                                }
                                if (showCardDisplayButton && cardDisplayButtonPosition == side) {
                                    SwipeActionIconButton(
                                        icon = if (cardDisplayMode == CardDisplayMode.FILL) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Toggle Display Mode",
                                        onClick = onToggleDisplayMode
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (showMuteIndicator) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(4.dp)
            .size(44.dp)
            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun MetadataPanel(
    asset: Asset,
    onClose: () -> Unit,
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // Connection to pass downward drags to the parent when at the top of the scroll
    val nestedScrollConnection = remember(scrollState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && scrollState.value == 0) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0 && scrollState.value == 0) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                onDragEnd()
                return super.onPostFling(consumed, available)
            }
        }
    }

    Card(
        modifier = Modifier
            .wrapContentHeight()
            .nestedScroll(nestedScrollConnection),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 48.dp)
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.size(40.dp, 4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.swipe_metadata_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(R.string.common_close), modifier = Modifier.size(20.dp)) }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            MetadataRow(Icons.Default.Description, stringResource(R.string.swipe_metadata_file), asset.originalFileName ?: stringResource(R.string.diag_unknown))
            MetadataRow(Icons.Default.CalendarToday, stringResource(R.string.swipe_metadata_date), asset.fileCreatedAt.substringBefore("T"))
            val formatLabel = if (asset.fileExtension != null) "${asset.type} (.${asset.fileExtension.lowercase()})" else asset.type
            MetadataRow(Icons.Default.Info, stringResource(R.string.swipe_metadata_format), formatLabel)
            asset.exifInfo?.let { exif ->
                val sizeLabel = exif.fileSizeInBytes?.let { formatSize(it) } ?: "N/A"
                MetadataRow(Icons.Default.SdStorage, stringResource(R.string.swipe_metadata_size), sizeLabel)
                MetadataRow(Icons.Default.AspectRatio, stringResource(R.string.swipe_metadata_resolution), "${exif.imageWidth ?: "?"} x ${exif.imageHeight ?: "?"}")
            } ?: run {
                MetadataRow(Icons.Default.SdStorage, stringResource(R.string.swipe_metadata_size), stringResource(R.string.swipe_loading))
                MetadataRow(Icons.Default.AspectRatio, stringResource(R.string.swipe_metadata_resolution), stringResource(R.string.swipe_loading))
            }
        }
    }
}

@Composable
fun MetadataRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.width(80.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SummaryDialog(
    uiState: SwipeUiState,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onUndoDecision: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = if (uiState.isSyncing) ({}) else onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.swipe_summary_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(onClick = onDismiss, enabled = !uiState.isSyncing) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.albumName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    val stats = listOf(
                        Triple(stringResource(R.string.swipe_keep), Triple(uiState.keptCount, uiState.keptSize, MaterialGreen), Icons.Default.Check),
                        Triple(stringResource(R.string.swipe_delete), Triple(uiState.deletedCount, uiState.deletedSize, MaterialRed), Icons.Default.Delete),
                        Triple(stringResource(R.string.swipe_archive), Triple(uiState.archiveCount, uiState.archiveSize, MaterialTheme.colorScheme.primary), Icons.Default.Archive),
                        Triple(stringResource(R.string.swipe_locked), Triple(uiState.lockedCount, uiState.lockedSize, MaterialTheme.colorScheme.outline), Icons.Default.Lock),
                        Triple(stringResource(R.string.swipe_remaining), Triple(uiState.remainingCount, uiState.remainingSize, MaterialTheme.colorScheme.outlineVariant), Icons.Default.Pending)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0 until 2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val left = stats[i * 2]
                                val right = stats[i * 2 + 1]
                                StatSummaryBox(
                                    label = left.first,
                                    count = left.second.first,
                                    size = left.second.second,
                                    color = left.second.third,
                                    icon = left.third,
                                    isEstimated = left.first == stringResource(R.string.swipe_remaining) && uiState.isRemainingEstimated,
                                    modifier = Modifier.weight(1f)
                                )
                                StatSummaryBox(
                                    label = right.first,
                                    count = right.second.first,
                                    size = right.second.second,
                                    color = right.second.third,
                                    icon = right.third,
                                    isEstimated = right.first == stringResource(R.string.swipe_remaining) && uiState.isRemainingEstimated,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        val last = stats.last()
                        StatSummaryBox(
                            label = last.first,
                            count = last.second.first,
                            size = last.second.second,
                            color = last.second.third,
                            icon = last.third,
                            isEstimated = last.first == stringResource(R.string.swipe_remaining) && uiState.isRemainingEstimated,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.swipe_check_before_delete),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                val deletedAssets = remember(uiState.decisions) {
                    uiState.assets.filter { uiState.decisions[it.id] == SwipeDecision.DELETE }
                }

                if (deletedAssets.isNotEmpty()) {
                    Box(modifier = Modifier.height(220.dp).fillMaxWidth()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(
                                items = deletedAssets,
                                key = { it.id }
                            ) { asset ->
                                DeletedAssetThumbnail(
                                    asset = asset,
                                    uiState = uiState,
                                    onUndo = { onUndoDecision(asset.id) }
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.swipe_no_deletions),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                if (uiState.isSyncing) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(CircleShape),
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        },
        confirmButton = {
            val hasChanges = uiState.processedCount > 0 || uiState.localFavorites.isNotEmpty()

            Button(
                onClick = onApply,
                enabled = !uiState.isSyncing && hasChanges,
                colors = ButtonDefaults.buttonColors(containerColor = if (uiState.deletedCount > 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                val totalSize = formatSize(uiState.deletedSize)
                Text(
                    text = if (uiState.isSyncing) stringResource(R.string.swipe_syncing) else stringResource(R.string.swipe_liberate_button, totalSize, uiState.deletedCount),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        shape = RoundedCornerShape(32.dp)
    )
}

@Composable
fun StatSummaryBox(
    label: String,
    count: Int,
    size: Long,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isEstimated: Boolean = false,
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
                Spacer(Modifier.width(8.dp))
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(4.dp))
                Text(text = stringResource(R.string.swipe_assets_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text(
                text = (if (isEstimated) "~" else "") + formatSize(size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DeletedAssetThumbnail(
    asset: Asset,
    uiState: SwipeUiState,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
    val apiKey = SessionManager.getApiKey() ?: ""
    
    val thumbRequest = ImageRequest.Builder(context)
        .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=thumbnail")
        .addHeader("x-api-key", apiKey)
        .crossfade(true)
        .build()

    Box(modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))) {
        AsyncImage(
            model = thumbRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        IconButton(
            onClick = onUndo,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Undo, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }

        if (asset.type == "VIDEO") {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            )
        }
    }
}

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
    controlsOffset: Dp = 0.dp
) {
    key(assetId) {
        var currentTime by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var isVideoPlaying by remember { mutableStateOf(player.isPlaying) }
        var isScrubbing by remember { mutableStateOf(false) }
        var scrubValue by remember { mutableLongStateOf(0L) }

        val videoAlpha by animateFloatAsState(
            targetValue = if (isVideoReady) 1f else 0f,
            animationSpec = tween(durationMillis = 400),
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
            if (isPaused) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_READY && !player.isPlaying) {
                    player.play()
                }
                while (true) {
                    if (!isScrubbing) {
                        currentTime = player.currentPosition
                        duration = player.duration.coerceAtLeast(0L)
                    }
                    delay(500)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }
            
            LaunchedEffect(toggleControllerTrigger) {
                if (toggleControllerTrigger > 0 && isFullscreen) {
                    playerViewRef.value?.let { view ->
                        if (view.isControllerFullyVisible) view.hideController() else view.showController()
                    }
                }
            }

            key(isFullscreen, assetId) {
                AndroidView(
                    factory = { context ->
                        val view = LayoutInflater.from(context).inflate(R.layout.view_player_texture, null) as PlayerView
                        view.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                            onControllerVisibilityChanged?.invoke(visibility == android.view.View.VISIBLE)
                        })
                        playerViewRef.value = view
                        view
                    },
                    update = { view ->
                        if (view.player != player) {
                            view.player = player
                            if (player.playbackState == Player.STATE_READY) {
                                view.post {
                                    player.seekTo(player.currentPosition)
                                }
                            }
                        }

                        view.useController = false
                        player.volume = if (isMuted) 0f else 1f
                        view.resizeMode = if (isFullscreen) {
                            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        } else {
                            if (cardDisplayMode == CardDisplayMode.FILL) {
                                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            } else {
                                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }

                        if (toggleControllerTrigger > 0 && isFullscreen) {
                            if (view.isControllerFullyVisible) view.hideController() else view.showController()
                        }
                    },
                    onRelease = { view ->
                        view.player = null
                    },
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = videoAlpha }
                )
            }

            val indicatorsVisible = (showSize && fileSize != null) || (duration > 0) || isFullscreen
            val finalShowControls = showControls
            
            if (indicatorsVisible) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = finalShowControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = controlsOffset)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isFullscreen) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(formatDuration(if (isScrubbing) scrubValue else currentTime), color = Color.White, style = MaterialTheme.typography.labelSmall)
                                Text(formatDuration(duration), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                            
                            Spacer(Modifier.height(4.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { if (player.isPlaying) player.pause() else player.play() },
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
                                    },
                                    onValueChangeFinished = {
                                        player.seekTo(scrubValue)
                                        isScrubbing = false
                                    },
                                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        } else if (showSize && fileSize != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp)
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
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%d:%02d".format(mins, secs)
}
KOTLIN
