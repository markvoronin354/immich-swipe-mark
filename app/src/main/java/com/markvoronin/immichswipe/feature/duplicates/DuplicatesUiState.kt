package com.markvoronin.immichswipe.feature.duplicates

import com.markvoronin.immichswipe.domain.model.Asset

data class DuplicatesUiState(
    val isLoading: Boolean = true,
    val clusters: List<DuplicateClusterUiModel> = emptyList(),
    val decisions: Map<String, DuplicateDecision> = emptyMap(), // assetId -> Decision
    val favorites: Map<String, Boolean> = emptyMap(), // assetId -> isFavorite
    val isSyncing: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val error: String? = null
) {
    fun isFavorite(asset: Asset): Boolean {
        return favorites[asset.id] ?: asset.isFavorite
    }
}

data class DuplicateClusterUiModel(
    val clusterId: String,
    val assets: List<Asset>
)

enum class DuplicateDecision {
    KEEP,
    DELETE,
    NONE
}
