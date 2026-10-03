package com.markvoronin.immichswipe.feature.duplicates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.feature.duplicates.components.DuplicateClusterCard
import com.markvoronin.immichswipe.feature.duplicates.components.FullScreenPreviewData
import com.markvoronin.immichswipe.feature.duplicates.components.FullScreenPreviewModal
import com.markvoronin.immichswipe.feature.duplicates.components.InstagramZoomOverlay
import com.markvoronin.immichswipe.feature.duplicates.components.ZoomData
import com.markvoronin.immichswipe.feature.duplicates.components.formatSizeStr
import com.markvoronin.immichswipe.feature.settings.components.verticalFadingEdges
import com.markvoronin.immichswipe.ui.theme.VirtualGold
import kotlinx.coroutines.flow.Flow

@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel,
    modifier: Modifier = Modifier,
    resetSignal: Flow<Unit>? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeZoomData by remember { mutableStateOf<ZoomData?>(null) }
    var fullScreenPreviewData by remember { mutableStateOf<FullScreenPreviewData?>(null) }
    var rootWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(resetSignal) {
        resetSignal?.collect {
            showResetDialog = true
        }
    }

    val itemsToDelete = remember(uiState.decisions, uiState.clusters) {
        val deleteAssetIds = uiState.decisions.filterValues { it == DuplicateDecision.DELETE }.keys
        uiState.clusters.flatMap { it.assets }.filter { it.id in deleteAssetIds }
    }
    val deleteCount = itemsToDelete.size
    val deleteBytes = itemsToDelete.sumOf { it.exifInfo?.fileSizeInBytes ?: 0L }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootWindowOffset = coords.positionInWindow()
            }
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.clusters.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = VirtualGold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No duplicates found!",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Duplicates",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.clusters.size} clusters (${uiState.totalAssetsCount} items)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = deleteCount > 0,
                        enter = fadeIn() + slideInHorizontally { it },
                        exit = fadeOut() + slideOutHorizontally { it }
                    ) {
                        Button(
                            onClick = { viewModel.toggleDeleteConfirmation(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (deleteCount > 1) {
                                    "Delete ($deleteCount)"
                                } else {
                                    stringResource(R.string.swipe_delete)
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalFadingEdges(listState, length = 32.dp)
                    .navigationBarsPadding(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.clusters, key = { it.clusterId }) { cluster ->
                        DuplicateClusterCard(
                            cluster = cluster,
                            decisions = uiState.decisions,
                            activeZoomAssetId = activeZoomData?.asset?.id,
                            baseUrl = uiState.baseUrl,
                            apiKey = uiState.apiKey,
                            onDecisionToggle = { assetId -> viewModel.toggleDecision(assetId) },
                            onAssetLongPress = { clickedAsset ->
                                val index = cluster.assets.indexOfFirst { it.id == clickedAsset.id }.coerceAtLeast(0)
                                fullScreenPreviewData = FullScreenPreviewData(cluster = cluster, initialIndex = index)
                            },
                            onZoomStateUpdate = { zoomData ->
                                activeZoomData = zoomData
                            }
                        )
                    }
                }
            }
        }

        InstagramZoomOverlay(
            zoomData = activeZoomData,
            decision = activeZoomData?.let { uiState.decisions[it.asset.id] ?: DuplicateDecision.NONE } ?: DuplicateDecision.NONE,
            rootWindowOffset = rootWindowOffset,
            baseUrl = uiState.baseUrl,
            apiKey = uiState.apiKey,
            onDismiss = { activeZoomData = null }
        )

        FullScreenPreviewModal(
            previewData = fullScreenPreviewData,
            decisions = uiState.decisions,
            isFavorite = { uiState.isFavorite(it) },
            baseUrl = uiState.baseUrl,
            apiKey = uiState.apiKey,
            onDecisionToggle = { assetId -> viewModel.toggleDecision(assetId) },
            onFavoriteToggle = { asset -> viewModel.toggleFavorite(asset) },
            onDismiss = { fullScreenPreviewData = null }
        )

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.duplicates_reset_confirm_title))
                    }
                },
                text = { Text(stringResource(R.string.duplicates_reset_confirm_msg)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetDialog = false
                            viewModel.resetDecisions()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        if (uiState.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleDeleteConfirmation(false) },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.duplicates_delete_confirm_title))
                    }
                },
                text = {
                    Text(
                        pluralStringResource(
                            R.plurals.duplicates_delete_confirm_msg,
                            deleteCount,
                            deleteCount,
                            formatSizeStr(deleteBytes)
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.toggleDeleteConfirmation(false)
                            viewModel.syncDeletions()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.swipe_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.toggleDeleteConfirmation(false) }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        if (uiState.isSyncing) {
            Dialog(onDismissRequest = {}) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.swipe_syncing),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
