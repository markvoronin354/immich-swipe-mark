package com.markvoronin.immichswipe.feature.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.CardDisplayMode
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.core.SortCategory
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.swipe.SwipeDecision
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import kotlin.time.Duration.Companion.milliseconds

class SwipeViewModel(
    private val assetRepository: AssetRepository,
    private val sessionRepository: SessionRepository,
    private val swipeDecisionRepository: SwipeDecisionRepository,
    private val album: Album,
    private val userQuotaBytes: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwipeUiState(
        albumName = album.albumName,
        albumId = album.id,
        userQuotaBytes = userQuotaBytes
    ))
    val uiState: StateFlow<SwipeUiState> = _uiState.asStateFlow()

    private var masterWorkPile: List<Asset> = emptyList()
    private val allAssetsFoundFlow = MutableStateFlow<List<Asset>>(emptyList())
    private val isAssetsLoadingFlow = MutableStateFlow(true)
    private var assetsJob: Job? = null
    private var sortingJob: Job? = null
    
    private val _downloadRequestSignal = MutableSharedFlow<Asset>(extraBufferCapacity = 1)
    val downloadRequestSignal = _downloadRequestSignal.asSharedFlow()

    private val _shareRequestSignal = MutableSharedFlow<Asset>(extraBufferCapacity = 1)
    val shareRequestSignal = _shareRequestSignal.asSharedFlow()

    init {
        loadAssetsAndDecisions()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                sessionRepository.playbackBehavior,
                sessionRepository.fullscreenButtonPosition,
                sessionRepository.immichButtonPosition,
                sessionRepository.cardDisplayButtonPosition,
                sessionRepository.muteButtonPosition,
                sessionRepository.showFullscreenButton,
                sessionRepository.showImmichButton,
                sessionRepository.showCardDisplayButton,
                sessionRepository.showMuteButton,
                sessionRepository.showDownloadButton,
                sessionRepository.downloadButtonPosition,
                sessionRepository.showShareButton,
                sessionRepository.shareButtonPosition,
                sessionRepository.showSwipeButtons,
                sessionRepository.autoNextOnFav,
                sessionRepository.swapSummaryArchive,
                sessionRepository.syncLocalDeletion,
                sessionRepository.trashLocalDeletion,
                sessionRepository.sortOrder,
                sessionRepository.tapToSwipeEnabled
            ) { values ->
                val order = values[18] as SortOrder
                val category = when (order) {
                    SortOrder.CHRONOLOGICAL_DESC, SortOrder.CHRONOLOGICAL_ASC, SortOrder.SHUFFLED -> SortCategory.TIME
                    SortOrder.SIZE_DESC, SortOrder.SIZE_ASC -> SortCategory.SIZE
                    else -> SortCategory.TYPE
                }

                val oldOrder = _uiState.value.sortOrder
                _uiState.update { it.copy(
                    playbackBehavior = values[0] as PlaybackBehavior,
                    fullscreenButtonPosition = values[1] as IconPosition,
                    immichButtonPosition = values[2] as IconPosition,
                    cardDisplayButtonPosition = values[3] as IconPosition,
                    muteButtonPosition = values[4] as IconPosition,
                    showFullscreenButton = values[5] as Boolean,
                    showImmichButton = values[6] as Boolean,
                    showCardDisplayButton = values[7] as Boolean,
                    showMuteButton = values[8] as Boolean,
                    showDownloadButton = values[9] as Boolean,
                    downloadButtonPosition = values[10] as IconPosition,
                    showShareButton = values[11] as Boolean,
                    shareButtonPosition = values[12] as IconPosition,
                    showSwipeButtons = values[13] as Boolean,
                    autoNextOnFav = values[14] as Boolean,
                    swapSummaryArchive = values[15] as Boolean,
                    syncLocalDeletion = values[16] as Boolean,
                    trashLocalDeletion = values[17] as Boolean,
                    sortOrder = order,
                    sortCategory = category,
                    tapToSwipeEnabled = values[19] as Boolean
                )}
                
                if (oldOrder != order) {
                    refreshSortedWorkPile(jumpToFirstUnprocessed = true)
                }
            }.collect {}
        }
    }

    private fun loadAssetsAndDecisions() {
        assetsJob?.cancel()
        assetsJob = viewModelScope.launch {
            try {
                val config = sessionRepository.sessionConfig.first() ?: return@launch
                
                if (allAssetsFoundFlow.value.isEmpty()) {
                    _uiState.update { it.copy(isLoading = true) }
                }
                isAssetsLoadingFlow.value = true

                launch {
                    try {
                        assetRepository.getAssetsByAlbum(
                            albumId = album.id,
                            userId = config.userId,
                            sortOrder = sessionRepository.sortOrder.first(),
                            shuffleSeed = SessionManager.globalShuffleSeed
                        ).collect { batch ->
                            allAssetsFoundFlow.value = batch.assets
                            if (batch.assets.isNotEmpty()) {
                                isAssetsLoadingFlow.value = false
                            }
                        }
                    } finally {
                        isAssetsLoadingFlow.value = false
                    }
                }

                combine(
                    swipeDecisionRepository.getDecisionsForAlbum(album.id, config.userId),
                    allAssetsFoundFlow,
                    isAssetsLoadingFlow
                ) { localDecisions, allAssetsFound, isFetching ->
                    val decisionMap = localDecisions.associate { entity ->
                        val d = try { SwipeDecision.valueOf(entity.decision) } catch (_: Exception) { SwipeDecision.KEEP }
                        entity.assetId to d
                    }
                    val sizeMap = localDecisions.associate { it.assetId to (it.fileSize ?: 0L) }
                    
                    // Deriving history from decisions if empty
                    val derivedHistory = if (_uiState.value.history.isEmpty()) {
                         localDecisions.map { it.assetId }
                    } else _uiState.value.history

                    masterWorkPile = allAssetsFound
                    
                    val isResetting = localDecisions.isEmpty() && _uiState.value.decisions.isNotEmpty()
                    
                    refreshSortedWorkPile(
                        jumpToFirstUnprocessed = _uiState.value.assets.isEmpty() || isResetting,
                        overrideDecisions = decisionMap,
                        overrideSizes = sizeMap,
                        overrideHistory = derivedHistory,
                        overrideIsLoading = isFetching && allAssetsFound.isEmpty()
                    )
                }.collect {}
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.e("Swipe", "Error loading album", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun refreshSortedWorkPile(
        jumpToFirstUnprocessed: Boolean = false,
        overrideDecisions: Map<String, SwipeDecision>? = null,
        overrideSizes: Map<String, Long>? = null,
        overrideHistory: List<String>? = null,
        overrideIsLoading: Boolean? = null
    ) {
        val decisions = overrideDecisions ?: _uiState.value.decisions
        val assetSizes = overrideSizes ?: _uiState.value.assetSizes
        val history = overrideHistory ?: _uiState.value.history
        val order = _uiState.value.sortOrder

        sortingJob?.cancel()
        sortingJob = viewModelScope.launch {
            val sorted = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                assetRepository.applySort(masterWorkPile, order, SessionManager.globalShuffleSeed)
            }

            _uiState.update { state ->
                val currentAssetId = state.assets.getOrNull(state.currentIndex)?.id
                
                var nextIndex = if (jumpToFirstUnprocessed) {
                    sorted.indexOfFirst { !decisions.containsKey(it.id) }
                } else {
                    // Try to find where the current asset moved to in the new list
                    val indexInNewList = if (currentAssetId != null) sorted.indexOfFirst { it.id == currentAssetId } else -1
                    
                    // If we found it and it's still unprocessed, stay on it
                    if (indexInNewList != -1 && !decisions.containsKey(currentAssetId)) {
                        indexInNewList
                    } else {
                        // Otherwise, find the first unprocessed item in the whole list
                        sorted.indexOfFirst { !decisions.containsKey(it.id) }
                    }
                }

                if (nextIndex == -1) nextIndex = sorted.size

                state.copy(
                    assets = sorted,
                    currentIndex = nextIndex,
                    decisions = decisions,
                    assetSizes = assetSizes,
                    history = history,
                    isLoading = overrideIsLoading ?: state.isLoading
                )
            }
            
            // Preload details for new current index
            val state = _uiState.value
            if (state.currentIndex < state.assets.size) {
                loadAssetDetail(state.assets[state.currentIndex].id, state.currentIndex)
            }
        }
    }

    private fun loadAssetDetail(assetId: String, index: Int) {
        viewModelScope.launch {
            try {
                val detail = assetRepository.getAssetDetail(assetId)
                val currentAssets = _uiState.value.assets.toMutableList()
                if (index < currentAssets.size && currentAssets[index].id == assetId) {
                    currentAssets[index] = detail
                    val newSizes = _uiState.value.assetSizes.toMutableMap()
                    detail.exifInfo?.fileSizeInBytes?.let { newSizes[assetId] = it }
                    _uiState.update { it.copy(assets = currentAssets, assetSizes = newSizes) }
                }
            } catch (_: Exception) {}
        }
    }

    fun onSwipe(decision: SwipeDecision) {
        val currentState = _uiState.value
        val currentAsset = currentState.currentAsset ?: return
        
        // Optimistic UI update for immediate response
        val newDecisions = currentState.decisions.toMutableMap()
        newDecisions[currentAsset.id] = decision
        val newHistory = currentState.history.toMutableList()
        newHistory.add(currentAsset.id)
        
        // Find next unprocessed
        val nextIndex = currentState.assets.indices.firstOrNull { i ->
            i > currentState.currentIndex && !newDecisions.containsKey(currentState.assets[i].id)
        } ?: currentState.assets.size

        _uiState.update { it.copy(
            currentIndex = nextIndex,
            decisions = newDecisions,
            history = newHistory
        )}

        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            swipeDecisionRepository.saveDecision(currentAsset.id, album.id, config.userId, decision.name, currentAsset.exifInfo?.fileSizeInBytes)
        }
        
        if (nextIndex < currentState.assets.size) {
            loadAssetDetail(currentState.assets[nextIndex].id, nextIndex)
        }
    }

    fun undo() {
        val currentState = _uiState.value
        val lastAssetId = currentState.history.lastOrNull() ?: return
        
        // Optimistic UI update
        val newHistory = currentState.history.toMutableList()
        newHistory.removeAt(newHistory.size - 1)
        val newDecisions = currentState.decisions.toMutableMap()
        newDecisions.remove(lastAssetId)
        
        val prevIndex = currentState.assets.indexOfFirst { it.id == lastAssetId }
        
        _uiState.update { it.copy(
            currentIndex = if (prevIndex != -1) prevIndex else it.currentIndex,
            history = newHistory,
            decisions = newDecisions
        )}

        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            swipeDecisionRepository.removeDecision(lastAssetId, config.userId)
        }
    }
    
    fun undoSpecificDecision(assetId: String) {
        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            swipeDecisionRepository.removeDecision(assetId, config.userId)
            
            _uiState.update { state ->
                val newHistory = state.history.toMutableList()
                newHistory.remove(assetId)
                val newDecisions = state.decisions.toMutableMap()
                newDecisions.remove(assetId)
                state.copy(history = newHistory, decisions = newDecisions)
            }
        }
    }

    fun setSortOrder(order: SortOrder) = viewModelScope.launch { sessionRepository.saveSortOrder(order) }
    fun setSortCategory(category: SortCategory) {
        val defaultOrder = when (category) {
            SortCategory.TIME -> SortOrder.CHRONOLOGICAL_DESC
            SortCategory.SIZE -> SortOrder.SIZE_DESC
            SortCategory.TYPE -> SortOrder.TYPE_VIDEO_FIRST
        }
        setSortOrder(defaultOrder)
    }

    fun retryLoading() { loadAssetsAndDecisions() }
    fun resetToFirstUnprocessed() {
        refreshSortedWorkPile(jumpToFirstUnprocessed = true)
    }
    fun toggleFavorite() {
        val asset = _uiState.value.currentAsset ?: return
        val current = _uiState.value.localFavorites[asset.id] ?: false
        val newFavs = _uiState.value.localFavorites.toMutableMap()
        newFavs[asset.id] = !current
        _uiState.update { it.copy(localFavorites = newFavs) }
    }
    fun toggleSummary(visible: Boolean) { _uiState.update { it.copy(showSummary = visible) } }
    fun toggleFullscreen(visible: Boolean) { _uiState.update { it.copy(isFullscreenMode = visible) } }
    fun toggleResetConfirmation(visible: Boolean) { _uiState.update { it.copy(showResetConfirmation = visible) } }
    fun toggleMute() { _uiState.update { it.copy(isMuted = !_uiState.value.isMuted) } }
    fun toggleDisplayMode() {
        val next = if (_uiState.value.cardDisplayMode == CardDisplayMode.FILL) CardDisplayMode.FIT else CardDisplayMode.FILL
        _uiState.update { it.copy(cardDisplayMode = next) }
    }
    fun toggleArchive() { onSwipe(SwipeDecision.ARCHIVE) }
    fun toggleLock() { onSwipe(SwipeDecision.LOCK) }
    fun enterBulkMode(isDelete: Boolean) { _uiState.update { it.copy(isBulkDeleteMode = isDelete, isBulkKeepMode = !isDelete, bulkSelection = emptySet()) } }
    fun exitBulkMode() { _uiState.update { it.copy(isBulkDeleteMode = false, isBulkKeepMode = false, bulkSelection = emptySet()) } }
    fun setBulkSelection(ids: Set<String>, last: Int? = null) { _uiState.update { it.copy(bulkSelection = ids, bulkLastIndex = last) } }
    fun onLocalDeleteIntentHandled() { _uiState.update { it.copy(localDeletePendingIntent = null) } }
    fun downloadAsset(asset: Asset) { viewModelScope.launch { _downloadRequestSignal.emit(asset) } }
    fun shareAsset(asset: Asset) { viewModelScope.launch { _shareRequestSignal.emit(asset) } }
    fun onMoveToAsset(index: Int) {
        if (index in _uiState.value.assets.indices) {
            _uiState.update { it.copy(currentIndex = index) }
            loadAssetDetail(_uiState.value.assets[index].id, index)
        }
    }
    fun getNextUnprocessedIndex(): Int {
        val s = _uiState.value
        return s.assets.indices.firstOrNull { i -> i > s.currentIndex && !s.decisions.containsKey(s.assets[i].id) } ?: -1
    }

    fun resetAlbumDecisions() {
        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            val ids = _uiState.value.assets.map { it.id }
            swipeDecisionRepository.removeDecisions(ids, config.userId)
            _uiState.update { it.copy(
                showResetConfirmation = false, 
                history = emptyList(), 
                decisions = emptyMap(),
                currentIndex = 0
            ) }
        }
    }

    fun executeBulkAction() {
        val s = _uiState.value
        val selection = s.bulkSelection
        val isDelete = s.isBulkDeleteMode
        if (selection.isEmpty()) { exitBulkMode(); return }
        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            val decision = if (isDelete) SwipeDecision.DELETE else SwipeDecision.KEEP
            selection.forEach { id ->
                val asset = s.assets.find { it.id == id }
                swipeDecisionRepository.saveDecision(id, album.id, config.userId, decision.name, asset?.exifInfo?.fileSizeInBytes)
            }
            exitBulkMode()
        }
    }

    fun applyChanges() {
        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            _uiState.update { it.copy(isSyncing = true) }
            try {
                val decisions = _uiState.value.decisions
                val toDelete = decisions.filter { it.value == SwipeDecision.DELETE }.keys.toList()
                if (toDelete.isNotEmpty()) assetRepository.deleteAssets(toDelete)
                swipeDecisionRepository.markAsSynced(decisions.keys.toList(), config.userId)
                _uiState.update { it.copy(isSyncing = false, showSummary = false, showSuccessAnimation = true) }
                delay(2000)
                _uiState.update { it.copy(showSuccessAnimation = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, error = e.message) }
            }
        }
    }
}
