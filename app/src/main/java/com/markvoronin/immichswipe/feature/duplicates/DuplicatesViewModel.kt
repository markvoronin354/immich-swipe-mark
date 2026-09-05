package com.markvoronin.immichswipe.feature.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.api.UpdateAssetsRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import com.markvoronin.immichswipe.domain.model.Asset

class DuplicatesViewModel(
    private val api: ImmichApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    init {
        loadDuplicates()
    }

    private fun loadDuplicates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch clusters directly from Immich API
                val duplicateClusters = api.getDuplicates()
                
                // Map to UI model and assign a unique ID to each cluster
                val mappedClusters = duplicateClusters.map { cluster ->
                    DuplicateClusterUiModel(
                        clusterId = UUID.randomUUID().toString(),
                        assets = cluster.assets
                    )
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        clusters = mappedClusters,
                        // Set default decisions: Keep the first (newest usually), delete the rest
                        decisions = generateDefaultDecisions(mappedClusters)
                    ) 
                }
            } catch (e: Exception) {
                AppLogger.e("Duplicates", "Failed to load duplicates", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Unknown error occurred"
                    ) 
                }
            }
        }
    }

    private fun generateDefaultDecisions(@Suppress("UNUSED_PARAMETER") clusters: List<DuplicateClusterUiModel>): Map<String, DuplicateDecision> {
        // Default mode: All assets unselected (clean slate)
        return emptyMap()
    }

    fun toggleDecision(assetId: String) {
        _uiState.update { state ->
            val current = state.decisions[assetId] ?: DuplicateDecision.NONE
            val next = when (current) {
                DuplicateDecision.NONE -> DuplicateDecision.KEEP
                DuplicateDecision.KEEP -> DuplicateDecision.DELETE
                DuplicateDecision.DELETE -> DuplicateDecision.NONE
            }
            state.copy(
                decisions = state.decisions.toMutableMap().apply { put(assetId, next) }
            )
        }
    }

    fun clearAllDecisions() {
        _uiState.update { state ->
            state.copy(decisions = emptyMap())
        }
    }

    fun toggleFavorite(asset: Asset) {
        viewModelScope.launch {
            val currentFav = _uiState.value.isFavorite(asset)
            val newFav = !currentFav

            _uiState.update { state ->
                state.copy(
                    favorites = state.favorites.toMutableMap().apply { put(asset.id, newFav) }
                )
            }

            try {
                api.updateAssets(
                    UpdateAssetsRequest(
                        ids = listOf(asset.id),
                        isFavorite = newFav
                    )
                )
            } catch (e: Exception) {
                AppLogger.e("Duplicates", "Failed to update favorite status", e)
                _uiState.update { state ->
                    state.copy(
                        favorites = state.favorites.toMutableMap().apply { put(asset.id, currentFav) }
                    )
                }
            }
        }
    }
    
    fun setDecision(assetId: String, decision: DuplicateDecision) {
        _uiState.update { state ->
            state.copy(
                decisions = state.decisions.toMutableMap().apply { put(assetId, decision) }
            )
        }
    }

    fun syncDeletions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                // Get all asset IDs marked for deletion
                val toDelete = _uiState.value.decisions.filter { it.value == DuplicateDecision.DELETE }.keys.toList()
                
                if (toDelete.isNotEmpty()) {
                    api.deleteAssets(com.markvoronin.immichswipe.data.api.DeleteAssetsRequest(ids = toDelete, force = false))
                }
                
                // Refresh list after successful deletion
                loadDuplicates()
            } catch (e: Exception) {
                AppLogger.e("Duplicates", "Failed to sync deletions", e)
                _uiState.update { 
                    it.copy(
                        isSyncing = false, 
                        error = "Failed to sync: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun toggleDeleteConfirmation(show: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmation = show) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetDecisions() {
        _uiState.update { state ->
            state.copy(decisions = generateDefaultDecisions(state.clusters))
        }
    }

    fun autoSelect(keepLargest: Boolean) {
        _uiState.update { state ->
            val newDecisions = state.decisions.toMutableMap()
            state.clusters.forEach { cluster ->
                val sorted = if (keepLargest) {
                    cluster.assets.sortedByDescending { it.exifInfo?.fileSizeInBytes ?: 0L }
                } else {
                    // Items with 0L size (unknown) should probably go to the bottom of "smallest"
                    cluster.assets.sortedWith(compareBy<Asset> { if ((it.exifInfo?.fileSizeInBytes ?: 0L) == 0L) Long.MAX_VALUE else it.exifInfo!!.fileSizeInBytes }.thenBy { it.fileCreatedAt })
                }
                
                val toKeep = sorted.firstOrNull()
                cluster.assets.forEach { asset ->
                    newDecisions[asset.id] = if (asset.id == toKeep?.id) DuplicateDecision.KEEP else DuplicateDecision.DELETE
                }
            }
            state.copy(decisions = newDecisions)
        }
    }
}

class DuplicatesViewModelFactory(private val api: ImmichApi) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DuplicatesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DuplicatesViewModel(api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}