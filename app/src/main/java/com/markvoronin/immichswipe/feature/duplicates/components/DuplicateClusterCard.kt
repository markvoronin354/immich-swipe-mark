package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.DuplicateClusterUiModel
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision
import com.markvoronin.immichswipe.feature.settings.components.horizontalFadingEdges

@Composable
fun DuplicateClusterCard(
    cluster: DuplicateClusterUiModel,
    decisions: Map<String, DuplicateDecision>,
    activeZoomAssetId: String?,
    baseUrl: String = "",
    apiKey: String = "",
    onDecisionToggle: (String) -> Unit,
    onAssetLongPress: (Asset) -> Unit,
    onZoomStateUpdate: (ZoomData?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val unsortedInCluster = cluster.assets.count { asset ->
                decisions[asset.id] == null || decisions[asset.id] == DuplicateDecision.NONE
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${cluster.assets.size} similar photos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (unsortedInCluster == 0) {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Sorted",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "$unsortedInCluster left to sort",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (cluster.assets.size <= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cluster.assets.forEach { asset ->
                        val decision = decisions[asset.id] ?: DuplicateDecision.NONE
                        val isBeingZoomed = activeZoomAssetId == asset.id
                        DuplicateAssetItem(
                            asset = asset,
                            decision = decision,
                            isBeingZoomed = isBeingZoomed,
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            modifier = Modifier.weight(1f),
                            onToggle = { onDecisionToggle(asset.id) },
                            onLongPress = { onAssetLongPress(asset) },
                            onZoomStateUpdate = onZoomStateUpdate
                        )
                    }
                }
            } else {
                val rowState = rememberLazyListState()
                LazyRow(
                    state = rowState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalFadingEdges(rowState, length = 24.dp)
                ) {
                    items(cluster.assets, key = { it.id }) { asset ->
                        val decision = decisions[asset.id] ?: DuplicateDecision.NONE
                        val isBeingZoomed = activeZoomAssetId == asset.id
                        DuplicateAssetItem(
                            asset = asset,
                            decision = decision,
                            isBeingZoomed = isBeingZoomed,
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            modifier = Modifier.width(135.dp),
                            onToggle = { onDecisionToggle(asset.id) },
                            onLongPress = { onAssetLongPress(asset) },
                            onZoomStateUpdate = onZoomStateUpdate
                        )
                    }
                }
            }
        }
    }
}
