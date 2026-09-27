package com.markvoronin.immichswipe.feature.duplicates

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.feature.duplicates.components.DuplicateClusterCard
import com.markvoronin.immichswipe.feature.duplicates.components.FullScreenPreviewData
import com.markvoronin.immichswipe.feature.duplicates.components.FullScreenPreviewModal
import com.markvoronin.immichswipe.feature.duplicates.components.InstagramZoomOverlay
import com.markvoronin.immichswipe.feature.duplicates.components.ZoomData
import com.markvoronin.immichswipe.ui.theme.VirtualGold

@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeZoomData by remember { mutableStateOf<ZoomData?>(null) }
    var fullScreenPreviewData by remember { mutableStateOf<FullScreenPreviewData?>(null) }
    var rootWindowOffset by remember { mutableStateOf(Offset.Zero) }

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
                        .padding(horizontal = 16.dp, vertical = 4.dp),
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
                    IconButton(onClick = { viewModel.loadDuplicates() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh duplicates"
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.clusters, key = { it.clusterId }) { cluster ->
                        DuplicateClusterCard(
                            cluster = cluster,
                            decisions = uiState.decisions,
                            activeZoomAssetId = activeZoomData?.asset?.id,
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
            onDismiss = { activeZoomData = null }
        )

        FullScreenPreviewModal(
            previewData = fullScreenPreviewData,
            decisions = uiState.decisions,
            isFavorite = { uiState.isFavorite(it) },
            onDecisionToggle = { assetId -> viewModel.toggleDecision(assetId) },
            onFavoriteToggle = { asset -> viewModel.toggleFavorite(asset) },
            onDismiss = { fullScreenPreviewData = null }
        )
    }
}
