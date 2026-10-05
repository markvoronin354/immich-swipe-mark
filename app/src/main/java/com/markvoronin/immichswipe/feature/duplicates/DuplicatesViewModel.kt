package com.markvoronin.immichswipe.feature.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.api.DeleteAssetsRequest
import com.markvoronin.immichswipe.data.api.DuplicateCluster
import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.api.UpdateAssetsRequest
import com.markvoronin.immichswipe.data.local.entity.SwipeDecisionEntity
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    private val swipeDecisionRepository: SwipeDecisionRepository,
    private val sessionRepository: SessionRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val api: ImmichApi
        get() = sessionManager.api ?: error("No active API session")

    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    init {
        observeSavedDecisions()
        observeSession()
        loadDuplicates()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.sessionConfig.collect { config ->
                _uiState.update { it.copy(
                    baseUrl = config?.baseUrl ?: "",
                    apiKey = config?.apiKey ?: ""
                ) }
            }
        }
        viewModelScope.launch {
            sessionRepository.hasCompletedDuplicatesTutorial.collect { completed ->
                _uiState.update { it.copy(hasCompletedDuplicatesTutorial = completed) }
            }
        }
    }

    fun completeDuplicatesTutorial() {
        viewModelScope.launch {
            sessionRepository.setHasCompletedDuplicatesTutorial(true)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSavedDecisions() {
        viewModelScope.launch {
            sessionManager.sessionConfig
                .map { it?.userId ?: "" }
                .distinctUntilChanged()
                .flatMapLatest { userId ->
                    if (userId.isEmpty()) flowOf(emptyList())
                    else swipeDecisionRepository.getAllDecisionsForUser(userId)
                }.collect { savedDecisions ->
                    val decisionMap = savedDecisions.associate { entity ->
                        val d = when (entity.decision) {
                            "KEEP" -> DuplicateDecision.KEEP
                            "DELETE" -> DuplicateDecision.DELETE
                            else -> DuplicateDecision.NONE
                        }
                        entity.assetId to d
                    }.filterValues { it != DuplicateDecision.NONE }

                    _uiState.update { state ->
                        state.copy(decisions = decisionMap)
                    }
                }
        }
    }

    fun loadDuplicates() {
        viewModelScope.launch {
            fetchDuplicates()
        }
    }

    private suspend fun fetchDuplicates() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            // Fetch clusters directly from Immich API
            val duplicateClusters = api.getDuplicates()
            
            // Map to UI model and assign a unique ID to each cluster (sorted with most duplicates on top)
            val mappedClusters = duplicateClusters
                .sortedWith(
                    compareByDescending<DuplicateCluster> { it.assets.size }
                        .thenByDescending { cluster -> cluster.assets.sumOf { it.exifInfo?.fileSizeInBytes ?: 0L } }
                )
                .map { cluster ->
                    DuplicateClusterUiModel(
                        clusterId = UUID.randomUUID().toString(),
                        assets = cluster.assets.sortedWith(
                            compareByDescending<Asset> { it.exifInfo?.fileSizeInBytes ?: 0L }
                                .thenByDescending { (it.exifInfo?.imageWidth ?: 0) * (it.exifInfo?.imageHeight ?: 0) }
                        )
                    )
                }
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    isSyncing = false,
                    clusters = mappedClusters,
                    selectedAutoSelectMode = null
                ) 
            }
        } catch (e: Exception) {
            AppLogger.e("Duplicates", "Failed to load duplicates", e)
            _uiState.update { 
                it.copy(
                    isLoading = false, 
                    isSyncing = false,
                    error = e.message ?: "Unknown error occurred"
                ) 
            }
        }
    }

    fun toggleDecision(assetId: String) {
        val current = _uiState.value.decisions[assetId] ?: DuplicateDecision.NONE
        val next = when (current) {
            DuplicateDecision.NONE -> DuplicateDecision.KEEP
            DuplicateDecision.KEEP -> DuplicateDecision.DELETE
            DuplicateDecision.DELETE -> DuplicateDecision.NONE
        }

        _uiState.update { state ->
            state.copy(
                decisions = state.decisions.toMutableMap().apply { put(assetId, next) }
            )
        }

        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val asset = _uiState.value.clusters.flatMap { it.assets }.find { it.id == assetId }
            val fileSize = asset?.exifInfo?.fileSizeInBytes
            when (next) {
                DuplicateDecision.KEEP -> swipeDecisionRepository.saveDecision(
                    assetId = assetId,
                    albumId = Album.VIRTUAL_DUPLICATES_ID,
                    userId = userId,
                    decision = "KEEP",
                    fileSize = fileSize
                )
                DuplicateDecision.DELETE -> swipeDecisionRepository.saveDecision(
                    assetId = assetId,
                    albumId = Album.VIRTUAL_DUPLICATES_ID,
                    userId = userId,
                    decision = "DELETE",
                    fileSize = fileSize
                )
                DuplicateDecision.NONE -> swipeDecisionRepository.removeDecision(
                    assetId = assetId,
                    userId = userId
                )
            }
        }
    }

    fun clearAllDecisions() {
        val currentDecidedAssetIds = _uiState.value.decisions.keys.toList()
        _uiState.update { state ->
            state.copy(
                decisions = emptyMap(),
                selectedAutoSelectMode = null
            )
        }
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            if (currentDecidedAssetIds.isNotEmpty()) {
                swipeDecisionRepository.removeDecisions(currentDecidedAssetIds, userId)
            }
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

        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val asset = _uiState.value.clusters.flatMap { it.assets }.find { it.id == assetId }
            val fileSize = asset?.exifInfo?.fileSizeInBytes
            when (decision) {
                DuplicateDecision.KEEP -> swipeDecisionRepository.saveDecision(
                    assetId = assetId,
                    albumId = Album.VIRTUAL_DUPLICATES_ID,
                    userId = userId,
                    decision = "KEEP",
                    fileSize = fileSize
                )
                DuplicateDecision.DELETE -> swipeDecisionRepository.saveDecision(
                    assetId = assetId,
                    albumId = Album.VIRTUAL_DUPLICATES_ID,
                    userId = userId,
                    decision = "DELETE",
                    fileSize = fileSize
                )
                DuplicateDecision.NONE -> swipeDecisionRepository.removeDecision(
                    assetId = assetId,
                    userId = userId
                )
            }
        }
    }

    fun syncDeletions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                // Get all asset IDs marked for deletion and keep
                val currentDecisions = _uiState.value.decisions
                val toDeleteSet = currentDecisions.filter { it.value == DuplicateDecision.DELETE }.keys.toSet()
                val toKeepSet = currentDecisions.filter { it.value == DuplicateDecision.KEEP }.keys.toSet()

                AppLogger.i("Duplicates", "Syncing duplicate resolutions: ${toDeleteSet.size} to delete, ${toKeepSet.size} to keep")

                if (toDeleteSet.isNotEmpty()) {
                    val allAssets = _uiState.value.clusters.flatMap { it.assets }
                    val assetsToDelete = allAssets.filter { it.id in toDeleteSet }
                    
                    val deletedCount = assetsToDelete.size
                    val bytesSaved = assetsToDelete.sumOf { it.exifInfo?.fileSizeInBytes ?: 0L }

                    api.deleteAssets(DeleteAssetsRequest(ids = toDeleteSet.toList(), force = false))

                    val userId = sessionManager.getUserId()
                    if (userId != null) {
                        // Remove deleted assets from decisions DB
                        swipeDecisionRepository.removeDecisions(toDeleteSet.toList(), userId)

                        // Mark kept assets as synced
                        if (toKeepSet.isNotEmpty()) {
                            swipeDecisionRepository.markAsSynced(toKeepSet.toList(), userId)
                        }

                        // Save sync history
                        swipeDecisionRepository.saveSyncHistory(
                            userId = userId,
                            deletedCount = deletedCount,
                            bytesSaved = bytesSaved,
                            keptCount = toKeepSet.size,
                            archivedCount = 0,
                            lockedCount = 0
                        )
                    }
                } else {
                    val userId = sessionManager.getUserId()
                    if (userId != null && toKeepSet.isNotEmpty()) {
                        swipeDecisionRepository.markAsSynced(toKeepSet.toList(), userId)
                    }
                }
                
                AppLogger.i("Duplicates", "Successfully synced duplicate deletions")
                // Refresh list after successful deletion
                fetchDuplicates()
            } catch (e: Exception) {
                AppLogger.e("Duplicates", "Failed to sync deletions", e)
                _uiState.update { 
                    it.copy(
                        isSyncing = false, 
                        error = "Failed to sync: ${e.message}"
                    ) 
                }
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
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
        clearAllDecisions()
    }

    fun autoSelect(keepLargest: Boolean) {
        val newDecisions = _uiState.value.decisions.toMutableMap()
        _uiState.value.clusters.forEach { cluster ->
            val sorted = if (keepLargest) {
                cluster.assets.sortedWith(
                    compareByDescending<Asset> { it.exifInfo?.fileSizeInBytes ?: 0L }
                        .thenByDescending { (it.exifInfo?.imageWidth ?: 0) * (it.exifInfo?.imageHeight ?: 0) }
                )
            } else {
                // Items with 0L size (unknown) should probably go to the bottom of "smallest"
                cluster.assets.sortedWith(compareBy<Asset> { if ((it.exifInfo?.fileSizeInBytes ?: 0L) == 0L) Long.MAX_VALUE else it.exifInfo!!.fileSizeInBytes }.thenBy { it.fileCreatedAt })
            }
            
            val toKeep = sorted.firstOrNull()
            cluster.assets.forEach { asset ->
                newDecisions[asset.id] = if (asset.id == toKeep?.id) DuplicateDecision.KEEP else DuplicateDecision.DELETE
            }
        }
        val mode = if (keepLargest) AutoSelectMode.KEEP_LARGEST else AutoSelectMode.KEEP_SMALLEST
        _uiState.update { state ->
            state.copy(
                decisions = newDecisions,
                selectedAutoSelectMode = mode
            )
        }

        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val allAssetsMap = _uiState.value.clusters.flatMap { it.assets }.associateBy { it.id }
            val entitiesToSave = newDecisions.mapNotNull { (assetId, decision) ->
                if (decision == DuplicateDecision.NONE) null
                else {
                    val asset = allAssetsMap[assetId]
                    SwipeDecisionEntity(
                        assetId = assetId,
                        albumId = Album.VIRTUAL_DUPLICATES_ID,
                        userId = userId,
                        decision = decision.name,
                        fileSize = asset?.exifInfo?.fileSizeInBytes,
                        createdAt = System.currentTimeMillis()
                    )
                }
            }
            if (entitiesToSave.isNotEmpty()) {
                swipeDecisionRepository.saveDecisions(entitiesToSave)
            }
        }
    }
}