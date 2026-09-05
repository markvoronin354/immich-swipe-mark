package com.markvoronin.immichswipe.feature.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.data.api.ImmichApi
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

    private fun generateDefaultDecisions(clusters: List<DuplicateClusterUiModel>): Map<String, DuplicateDecision> {
        val map = mutableMapOf<String, DuplicateDecision>()
        clusters.forEach { cluster ->
            // Sort by filesize or creation date if desired, but for now just use order returned by server.
            // First item = Keep. Others = Delete.
            cluster.assets.forEachIndexed { index, asset ->
                map[asset.id] = if (index == 0) DuplicateDecision.KEEP else DuplicateDecision.DELETE
            }
        }
        return map
    }

    fun toggleDecision(assetId: String) {
        _uiState.update { state ->
            val current = state.decisions[assetId] ?: DuplicateDecision.KEEP
            val next = if (current == DuplicateDecision.KEEP) DuplicateDecision.DELETE else DuplicateDecision.KEEP
            state.copy(
                decisions = state.decisions.toMutableMap().apply { put(assetId, next) }
            )
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