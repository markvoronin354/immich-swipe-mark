package com.markvoronin.immichswipe.feature.duplicates

import com.markvoronin.immichswipe.domain.model.Asset

data class DuplicatesUiState(
    val isLoading: Boolean = true,
    val clusters: List<DuplicateClusterUiModel> = emptyList(),
    val decisions: Map<String, DuplicateDecision> = emptyMap(), // assetId -> Decision
    val isSyncing: Boolean = false,
    val error: String? = null
)

data class DuplicateClusterUiModel(
    val clusterId: String, // We'll generate a UUID or use the first asset's ID as the cluster ID
    val assets: List<Asset>
)

enum class DuplicateDecision {
    KEEP,
    DELETE
}