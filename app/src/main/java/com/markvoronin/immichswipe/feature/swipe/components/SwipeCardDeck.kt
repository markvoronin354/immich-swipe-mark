package com.markvoronin.immichswipe.feature.swipe.components

import com.markvoronin.immichswipe.core.DoubleTapAction
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.exoplayer.ExoPlayer
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.feature.home.components.ErrorView
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.SwipeViewModel
import com.markvoronin.immichswipe.feature.swipe.models.SwipeCardActions
import com.markvoronin.immichswipe.feature.swipe.models.SwipeCardConfig

@Composable
fun SwipeCardDeck(
    uiState: SwipeUiState,
    viewModel: SwipeViewModel,
    sharedPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
    onTopCardSwipeOffsetChanged: (Float) -> Unit = {}
) {
    Box(
        modifier = modifier,
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
            ErrorView(error = uiState.error) {
                viewModel.retryLoading()
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
                            baseUrl = uiState.baseUrl,
                            apiKey = uiState.apiKey,
                            playbackBehavior = uiState.playbackBehavior,
                            fullscreenButtonPosition = uiState.fullscreenButtonPosition,
                            immichButtonPosition = uiState.immichButtonPosition,
                            rotationButtonPosition = uiState.rotationButtonPosition,
                            muteButtonPosition = uiState.muteButtonPosition,
                            downloadButtonPosition = uiState.downloadButtonPosition,
                            shareButtonPosition = uiState.shareButtonPosition,
                            showFullscreenButton = uiState.showFullscreenButton,
                            showImmichButton = uiState.showImmichButton,
                            immichOpenMode = uiState.immichOpenMode,
                            immichLongPressWeb = uiState.immichLongPressWeb,
                            showRotationButton = uiState.showRotationButton,
                            showMuteButton = uiState.showMuteButton,
                            showDownloadButton = uiState.showDownloadButton,
                            showShareButton = uiState.showShareButton,
                            cardDisplayMode = uiState.cardDisplayMode,
                            tapToSwipeEnabled = uiState.tapToSwipeEnabled,
                            showSizeIndicator = (uiState.sortOrder == SortOrder.SIZE_DESC) || (uiState.sortOrder == SortOrder.SIZE_ASC),
                            rotationAngle = uiState.getRotation(asset.id)
                        ),
                        actions = SwipeCardActions(
                            onSwipe = { viewModel.onSwipe(it) },
                            onRotateAsset = { viewModel.rotateCurrentAsset() },
                            onDoubleTap = {
                                if (uiState.doubleTapEnabled) {
                                    when (uiState.doubleTapAction) {
                                        DoubleTapAction.FULLSCREEN -> viewModel.toggleFullscreen(true)
                                        DoubleTapAction.FAVORITE -> viewModel.toggleFavorite()
                                    }
                                }
                            },
                            onOpenFullscreen = { viewModel.toggleFullscreen(true) },
                            onDownload = { viewModel.downloadAsset(it) },
                            onShare = { viewModel.shareAsset(it) },
                            onToggleMute = { viewModel.toggleMute() },
                            onSwipeOffsetChanged = { offset ->
                                if (!isNextCard) {
                                    topCardOffsetX = offset
                                    onTopCardSwipeOffsetChanged(offset)
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
            AnimatedVisibility(
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
}
