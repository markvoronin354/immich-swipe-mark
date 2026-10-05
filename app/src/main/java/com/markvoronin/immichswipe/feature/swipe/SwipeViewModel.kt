package com.markvoronin.immichswipe.feature.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.SortCategory
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.data.repository.AlbumRepository
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    private val sessionRepository: SessionRepository,
    private val swipeDecisionRepository: SwipeDecisionRepository,
    private val albumRepository: AlbumRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwipeUiState())
    val uiState: StateFlow<SwipeUiState> = _uiState.asStateFlow()

    private var currentAlbum: Album? = null

    fun initAlbum(album: Album, userQuotaBytes: Long? = null) {
        if (this.currentAlbum?.id == album.id) return
        this.currentAlbum = album
        _uiState.update {
            it.copy(
                albumName = album.albumName,
                albumId = album.id,
                userQuotaBytes = userQuotaBytes,
                remoteTotalCount = album.assetCount
            )
        }
        loadAssetsAndDecisions()
    }

    private var masterWorkPile: List<Asset> = emptyList()
    private val allAssetsFoundFlow = MutableStateFlow<List<Asset>>(emptyList())
    private val isAssetsLoadingFlow = MutableStateFlow(true)
    private val isFetchingAssetsFlow = MutableStateFlow(true)
    private var assetsJob: Job? = null
    private var sortingJob: Job? = null
    private var pendingJumpToFirstUnprocessed = false
    
    private val _downloadRequestChannel = Channel<Asset>(Channel.BUFFERED)
    val downloadRequestSignal = _downloadRequestChannel.receiveAsFlow()

    private val _shareRequestChannel = Channel<Asset>(Channel.BUFFERED)
    val shareRequestSignal = _shareRequestChannel.receiveAsFlow()

    init {
        loadAssetsAndDecisions()
        observeSettings()
        observeSession()
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
            sessionManager.connectionStatus.collect { status ->
                _uiState.update { it.copy(connectionStatus = status) }
            }
        }
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
                sessionRepository.showArchiveButton,
                sessionRepository.syncLocalDeletion,
                sessionRepository.trashLocalDeletion,
                sessionRepository.sortOrder,
                sessionRepository.tapToSwipeEnabled,
                sessionRepository.showFavoriteButton,
                sessionRepository.showLockButton
            ) { values ->
                updateSettingsState(values)
            }.collect {}
        }
        viewModelScope.launch {
            sessionRepository.immichOpenMode.collect { mode ->
                _uiState.update { it.copy(immichOpenMode = mode) }
            }
        }
        viewModelScope.launch {
            sessionRepository.immichLongPressWeb.collect { enabled ->
                _uiState.update { it.copy(immichLongPressWeb = enabled) }
            }
        }
        viewModelScope.launch {
            sessionRepository.showAddToAlbumButton.collect { show ->
                _uiState.update { it.copy(showAddToAlbumButton = show) }
            }
        }
        viewModelScope.launch {
            sessionRepository.hasCompletedSwipeTutorial.collect { completed ->
                _uiState.update { it.copy(hasCompletedSwipeTutorial = completed) }
            }
        }
    }

    fun completeSwipeTutorial() {
        viewModelScope.launch {
            sessionRepository.setHasCompletedSwipeTutorial(true)
        }
    }

    private fun updateSettingsState(values: Array<*>) {
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
            rotationButtonPosition = values[3] as IconPosition,
            muteButtonPosition = values[4] as IconPosition,
            showFullscreenButton = values[5] as Boolean,
            showImmichButton = values[6] as Boolean,
            showRotationButton = values[7] as Boolean,
            showMuteButton = values[8] as Boolean,
            showDownloadButton = values[9] as Boolean,
            downloadButtonPosition = values[10] as IconPosition,
            showShareButton = values[11] as Boolean,
            shareButtonPosition = values[12] as IconPosition,
            showSwipeButtons = values[13] as Boolean,
            autoNextOnFav = values[14] as Boolean,
            showArchiveButton = values[15] as Boolean,
            syncLocalDeletion = values[16] as Boolean,
            trashLocalDeletion = values[17] as Boolean,
            sortOrder = order,
            sortCategory = category,
            tapToSwipeEnabled = values[19] as Boolean,
            showFavoriteButton = values[20] as Boolean,
            showLockButton = values[21] as Boolean
        )}
        if (oldOrder != order) {
            pendingJumpToFirstUnprocessed = true
        }
    }

    private fun loadAssetsAndDecisions() {
        assetsJob?.cancel()
        assetsJob = viewModelScope.launch {
            try {
                val config = sessionRepository.sessionConfig.first() ?: return@launch
                
                if (allAssetsFoundFlow.value.isEmpty()) {
                    _uiState.update { it.copy(isLoading = true, isFetchingAssets = true) }
                }
                isAssetsLoadingFlow.value = true
                isFetchingAssetsFlow.value = true

                launch { fetchAssetsLoop(config.userId) }
                observeDecisionsAndAssets(config.userId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.e("Swipe", "Error loading album", e)
                _uiState.update { it.copy(isLoading = false, isFetchingAssets = false, error = e.message) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun fetchAssetsLoop(userId: String) {
        try {
            sessionRepository.sortOrder.flatMapLatest { sortOrder ->
                pendingJumpToFirstUnprocessed = true
                isAssetsLoadingFlow.value = true
                isFetchingAssetsFlow.value = true
                assetRepository.getAssetsByAlbum(
                    albumId = _uiState.value.albumId,
                    userId = userId,
                    sortOrder = sortOrder,
                    shuffleSeed = sessionManager.globalShuffleSeed
                )
            }.collect { batch ->
                _uiState.update { it.copy(
                    remoteTotalCount = if (!batch.isLocalCache || batch.total > it.remoteTotalCount) batch.total else it.remoteTotalCount,
                    syncLoadedCount = batch.assets.size,
                    syncTotalCount = batch.total,
                    isFetchingAssets = batch.isSyncing
                )}
                
                allAssetsFoundFlow.value = batch.assets
                
                if (!batch.isLocalCache || !batch.isSyncing) {
                    isAssetsLoadingFlow.value = false
                }
                isFetchingAssetsFlow.value = batch.isSyncing
            }
        } finally {
            isAssetsLoadingFlow.value = false
            isFetchingAssetsFlow.value = false
        }
    }

    private suspend fun observeDecisionsAndAssets(userId: String) {
        combine(
            swipeDecisionRepository.getDecisionsForAlbum(_uiState.value.albumId, userId),
            allAssetsFoundFlow,
            isAssetsLoadingFlow,
            isFetchingAssetsFlow
        ) { localDecisions, allAssetsFound, isInitialLoading, isFetching ->
            val validAssetIds = allAssetsFound.map { it.id }.toSet()
            val decisionMap = localDecisions
                .filter { it.assetId in validAssetIds }
                .associate { entity ->
                    val d = try { SwipeDecision.valueOf(entity.decision) } catch (_: Exception) { SwipeDecision.KEEP }
                    entity.assetId to d
                }
            val sizeMap = localDecisions.associate { it.assetId to (it.fileSize ?: 0L) }
            
            val currentDecisions = _uiState.value.decisions.filterKeys { it in validAssetIds }
            val currentSizes = _uiState.value.assetSizes
            val mergedDecisions = decisionMap + currentDecisions
            val mergedSizes = sizeMap + currentSizes

            val derivedHistory = if (_uiState.value.history.isEmpty()) {
                localDecisions.filter { !it.isSynced && it.assetId in validAssetIds }.map { it.assetId }
            } else _uiState.value.history.filter { it in validAssetIds }

            val existingDetailsMap = masterWorkPile.associateBy { it.id }
            masterWorkPile = allAssetsFound.map { asset -> existingDetailsMap[asset.id] ?: asset }
            
            val isResetting = localDecisions.isEmpty() && currentDecisions.isNotEmpty()
            val shouldJump = pendingJumpToFirstUnprocessed || _uiState.value.assets.isEmpty() || isResetting
            if (shouldJump && !isInitialLoading) {
                pendingJumpToFirstUnprocessed = false
            }
            
            refreshSortedWorkPile(
                jumpToFirstUnprocessed = shouldJump,
                overrideDecisions = mergedDecisions,
                overrideSizes = mergedSizes,
                overrideHistory = derivedHistory,
                overrideIsLoading = isInitialLoading,
                overrideIsFetchingAssets = isFetching || isInitialLoading
            )
        }.collect {}
    }

    private fun refreshSortedWorkPile(
        jumpToFirstUnprocessed: Boolean = false,
        overrideDecisions: Map<String, SwipeDecision>? = null,
        overrideSizes: Map<String, Long>? = null,
        overrideHistory: List<String>? = null,
        overrideIsLoading: Boolean? = null,
        overrideIsFetchingAssets: Boolean? = null
    ) {
        val decisions = overrideDecisions ?: _uiState.value.decisions
        val assetSizes = overrideSizes ?: _uiState.value.assetSizes
        val history = overrideHistory ?: _uiState.value.history
        val order = _uiState.value.sortOrder

        sortingJob?.cancel()
        sortingJob = viewModelScope.launch {
            val sorted = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                assetRepository.applySort(masterWorkPile, order, sessionManager.globalShuffleSeed)
            }

            _uiState.update { state ->
                val currentAssetId = state.assets.getOrNull(state.currentIndex)?.id
                val shouldForceJump = jumpToFirstUnprocessed || pendingJumpToFirstUnprocessed || state.sortOrder != order
                
                var nextIndex = if (shouldForceJump) {
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
                    masterWorkPile = masterWorkPile,
                    sortOrder = order,
                    currentIndex = nextIndex,
                    decisions = decisions,
                    assetSizes = assetSizes,
                    history = history,
                    isLoading = overrideIsLoading ?: state.isLoading,
                    isFetchingAssets = overrideIsFetchingAssets ?: state.isFetchingAssets
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
                
                // Skip emission if asset detail is identical to current asset in state
                val currentAssets = _uiState.value.assets
                val existingAsset = currentAssets.getOrNull(index)?.takeIf { it.id == assetId }
                if (existingAsset != null && existingAsset == detail) {
                    return@launch
                }

                val masterIndex = masterWorkPile.indexOfFirst { it.id == assetId }
                if (masterIndex != -1) {
                    val newMaster = masterWorkPile.toMutableList()
                    newMaster[masterIndex] = detail
                    masterWorkPile = newMaster
                }

                val updatedAssets = currentAssets.toMutableList()
                if (index < updatedAssets.size && updatedAssets[index].id == assetId) {
                    updatedAssets[index] = detail
                }
                val newSizes = _uiState.value.assetSizes.toMutableMap()
                detail.exifInfo?.fileSizeInBytes?.let { newSizes[assetId] = it }
                _uiState.update { it.copy(assets = updatedAssets, masterWorkPile = masterWorkPile, assetSizes = newSizes) }

                // Call preloader logic for the next items in queue
                preloadNextAssets(index)
            } catch (_: Exception) {}
        }
    }

    private fun preloadNextAssets(currentIndex: Int) {
        val state = _uiState.value
        val prefetchCount = 3
        
        // Context needs to be provided somehow for VideoPreloader and Coil.
        // Usually ViewModels shouldn't have Android context. 
        // We will emit an event or state to handle it in Compose instead.
    }

    fun onSwipe(decision: SwipeDecision) {
        pendingJumpToFirstUnprocessed = false
        val currentState = _uiState.value
        val currentAsset = currentState.currentAsset ?: return
        
        AppLogger.d("SwipeViewModel", "Swiped asset ${currentAsset.id}: $decision")

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
            swipeDecisionRepository.saveDecision(currentAsset.id, _uiState.value.albumId, config.userId, decision.name, currentAsset.exifInfo?.fileSizeInBytes)
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

    fun setSortOrder(order: SortOrder) {
        if (_uiState.value.sortOrder != order) {
            pendingJumpToFirstUnprocessed = true
            _uiState.update { it.copy(isLoading = true, sortOrder = order) }
            viewModelScope.launch { sessionRepository.saveSortOrder(order) }
        }
    }
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
        pendingJumpToFirstUnprocessed = true
        refreshSortedWorkPile(jumpToFirstUnprocessed = true)
    }
    fun toggleFavorite() {
        val asset = _uiState.value.currentAsset ?: return
        val current = _uiState.value.isFavorite(asset.id)
        val newFav = !current
        val newFavs = _uiState.value.localFavorites.toMutableMap()
        newFavs[asset.id] = newFav
        _uiState.update { it.copy(localFavorites = newFavs) }

        if (newFav && _uiState.value.autoNextOnFav) {
            onSwipe(SwipeDecision.KEEP)
        }
    }
    fun toggleSummary(visible: Boolean) { _uiState.update { it.copy(showSummary = visible) } }
    fun toggleFullscreen(visible: Boolean) { _uiState.update { it.copy(isFullscreenMode = visible) } }
    fun toggleResetConfirmation(visible: Boolean) { _uiState.update { it.copy(showResetConfirmation = visible) } }
    fun toggleMute() { _uiState.update { it.copy(isMuted = !_uiState.value.isMuted) } }

    fun openAddToAlbumDialog() {
        _uiState.update { it.copy(showAddToAlbumDialog = true, isFetchingAlbumsForDialog = true) }
        viewModelScope.launch {
            try {
                val albums = albumRepository.getAlbumsRaw().filter { album ->
                    album.id != Album.VIRTUAL_ALL_ID &&
                    album.id != Album.VIRTUAL_ORPHANS_ID &&
                    album.id != Album.VIRTUAL_DUPLICATES_ID
                }
                _uiState.update { it.copy(albumsForAddToAlbum = albums, isFetchingAlbumsForDialog = false) }
            } catch (e: Exception) {
                AppLogger.e("SwipeViewModel", "Error loading albums for dialog", e)
                _uiState.update { it.copy(isFetchingAlbumsForDialog = false) }
            }
        }
    }

    fun dismissAddToAlbumDialog() {
        _uiState.update { it.copy(showAddToAlbumDialog = false) }
    }

    fun addCurrentAssetToAlbum(targetAlbum: Album, onResult: (Boolean, String) -> Unit) {
        val asset = _uiState.value.currentAsset ?: return
        viewModelScope.launch {
            try {
                val success = albumRepository.addAssetToAlbum(targetAlbum.id, asset.id)
                onResult(success, targetAlbum.albumName)
            } catch (e: Exception) {
                AppLogger.e("SwipeViewModel", "Error adding asset to album ${targetAlbum.albumName}", e)
                onResult(false, targetAlbum.albumName)
            }
        }
    }
    private val assetsEditedThisSession = mutableSetOf<String>()

    fun rotateCurrentAsset() {
        val asset = _uiState.value.currentAsset ?: return
        val currentRot = _uiState.value.getRotation(asset.id)
        val nextRot = (currentRot + 90) % 360
        val newRotations = _uiState.value.localRotations.toMutableMap()
        newRotations[asset.id] = nextRot
        _uiState.update { it.copy(localRotations = newRotations) }
        assetsEditedThisSession.add(asset.id)

        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            assetRepository.updateAssetRotation(asset.id, config.userId, nextRot)
        }
    }
    fun toggleDisplayMode() { rotateCurrentAsset() }
    fun toggleArchive() { onSwipe(SwipeDecision.ARCHIVE) }
    fun toggleLock() { onSwipe(SwipeDecision.LOCK) }
    fun enterBulkMode(isDelete: Boolean) { _uiState.update { it.copy(isBulkDeleteMode = isDelete, isBulkKeepMode = !isDelete, bulkSelection = emptySet()) } }
    fun exitBulkMode() { _uiState.update { it.copy(isBulkDeleteMode = false, isBulkKeepMode = false, bulkSelection = emptySet()) } }
    fun setBulkSelection(ids: Set<String>, last: Int? = null) { _uiState.update { it.copy(bulkSelection = ids, bulkLastIndex = last) } }
    fun onLocalDeleteIntentHandled() { _uiState.update { it.copy(localDeletePendingIntent = null) } }
    fun downloadAsset(asset: Asset) { _downloadRequestChannel.trySend(asset) }
    fun shareAsset(asset: Asset) { _shareRequestChannel.trySend(asset) }
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
            try {
                val config = sessionRepository.sessionConfig.first() ?: return@launch
                
                // 1. Delete ALL decisions for this album from SQLite Room DB
                swipeDecisionRepository.deleteDecisionsForAlbum(_uiState.value.albumId, config.userId)
                
                // 2. Also delete decisions for any assets currently in masterWorkPile
                val currentIds = masterWorkPile.map { it.id }
                if (currentIds.isNotEmpty()) {
                    swipeDecisionRepository.removeDecisions(currentIds, config.userId)
                }

                pendingJumpToFirstUnprocessed = true
                _uiState.update { it.copy(
                    showResetConfirmation = false, 
                    history = emptyList(), 
                    decisions = emptyMap(),
                    currentIndex = 0
                ) }
                
                refreshSortedWorkPile(jumpToFirstUnprocessed = true)
            } catch (e: Exception) {
                AppLogger.e("SwipeViewModel", "Error resetting album decisions", e)
                _uiState.update { it.copy(showResetConfirmation = false) }
            }
        }
    }

    fun executeBulkAction() {
        val s = _uiState.value
        val selection = s.bulkSelection
        val isDelete = s.isBulkDeleteMode
        val lastIdx = s.bulkLastIndex ?: s.currentIndex
        if (selection.isEmpty()) { exitBulkMode(); return }

        val decision = if (isDelete) SwipeDecision.DELETE else SwipeDecision.KEEP

        // 1. Optimistic memory update
        val newDecisions = s.decisions.toMutableMap()
        val newHistory = s.history.toMutableList()
        selection.forEach { id ->
            newDecisions[id] = decision
            if (!newHistory.contains(id)) {
                newHistory.add(id)
            }
        }

        // Calculate next unprocessed index after the bulk selection
        val targetIndex = (lastIdx + 1).coerceAtMost(s.assets.size)
        val nextUnprocessedIndex = s.assets.indices.firstOrNull { i ->
            i >= targetIndex && !newDecisions.containsKey(s.assets[i].id)
        } ?: s.assets.size

        _uiState.update { it.copy(
            isBulkDeleteMode = false,
            isBulkKeepMode = false,
            bulkSelection = emptySet(),
            bulkLastIndex = null,
            currentIndex = nextUnprocessedIndex,
            decisions = newDecisions,
            history = newHistory
        ) }

        if (nextUnprocessedIndex < s.assets.size) {
            loadAssetDetail(s.assets[nextUnprocessedIndex].id, nextUnprocessedIndex)
        }

        // 2. Persist decisions to DB asynchronously
        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            selection.forEach { id ->
                val asset = s.assets.find { it.id == id }
                swipeDecisionRepository.saveDecision(id, _uiState.value.albumId, config.userId, decision.name, asset?.exifInfo?.fileSizeInBytes)
            }
        }
    }

    fun applyChanges() {
        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            val currentState = _uiState.value
            _uiState.update { it.copy(isSyncing = true) }
            try {
                val currentDecisions = currentState.decisions
                val toDelete = currentDecisions.filter { it.value == SwipeDecision.DELETE }.keys.toList()
                val toArchive = currentDecisions.filter { it.value == SwipeDecision.ARCHIVE }.keys.toList()
                val toLock = currentDecisions.filter { it.value == SwipeDecision.LOCK }.keys.toList()
                val allSwipedIds = currentDecisions.keys.toList()
                
                AppLogger.i("SwipeViewModel", "Applying changes: ${toDelete.size} to delete, ${currentState.keptCount} kept, ${currentState.archiveCount} archive, ${currentState.lockedCount} locked")

                // 1. Delete on server (and local cache via AssetRepository)
                if (toDelete.isNotEmpty()) {
                    try {
                        assetRepository.deleteAssets(toDelete)
                        // Also delete these decisions from local DB as they are no longer relevant
                        swipeDecisionRepository.removeDecisions(toDelete, config.userId)
                    } catch (e: Exception) {
                        AppLogger.e("SwipeViewModel", "Failed to delete assets on server: ${e.message}", e)
                    }
                }

                // 1b. Sync archive decisions to Immich server
                if (toArchive.isNotEmpty()) {
                    try {
                        assetRepository.updateAssets(toArchive, visibility = "archive")
                    } catch (e: Exception) {
                        AppLogger.e("SwipeViewModel", "Failed to archive assets on server: ${e.message}", e)
                    }
                }

                // 1c. Sync lock decisions to Immich server (Immich API expects "hidden" for locked/hidden assets)
                if (toLock.isNotEmpty()) {
                    try {
                        assetRepository.updateAssets(toLock, visibility = "hidden")
                        assetRepository.deleteLocalAssets(toLock)
                        swipeDecisionRepository.removeDecisions(toLock, config.userId)
                    } catch (e: Exception) {
                        AppLogger.e("SwipeViewModel", "Failed to lock assets on server: ${e.message}", e)
                    }
                }
                
                // 2. Mark remaining decisions as synced in DB (KEPT, ARCHIVE)
                val toMarkSynced = allSwipedIds.filter { it !in toDelete && it !in toLock }
                if (toMarkSynced.isNotEmpty()) {
                    swipeDecisionRepository.markAsSynced(toMarkSynced, config.userId)
                }

                // 2b. Persist rotation changes permanently locally and sync to Immich server
                currentState.localRotations.forEach { (assetId, rot) ->
                    assetRepository.updateAssetRotation(assetId, config.userId, rot)
                    if (rot != 0 || assetsEditedThisSession.contains(assetId)) {
                        try {
                            assetRepository.syncAssetRotationToServer(assetId, rot)
                        } catch (e: Exception) {
                            AppLogger.e("SwipeViewModel", "Failed to sync rotation for $assetId: ${e.message}", e)
                        }
                    }
                }
                
                // 2c. Sync favorite changes to Immich server
                val favsToAdd = currentState.localFavorites.filter { it.value }.keys.toList()
                val favsToRemove = currentState.localFavorites.filter { !it.value }.keys.toList()
                if (favsToAdd.isNotEmpty()) {
                    try {
                        assetRepository.updateAssets(favsToAdd, isFavorite = true)
                    } catch (e: Exception) {
                        AppLogger.e("SwipeViewModel", "Failed to sync favorite add: ${e.message}", e)
                    }
                }
                if (favsToRemove.isNotEmpty()) {
                    try {
                        assetRepository.updateAssets(favsToRemove, isFavorite = false)
                    } catch (e: Exception) {
                        AppLogger.e("SwipeViewModel", "Failed to sync favorite remove: ${e.message}", e)
                    }
                }

                // Update memory work pile with favorite changes
                if (currentState.localFavorites.isNotEmpty()) {
                    masterWorkPile = masterWorkPile.map { asset ->
                        val newFav = currentState.localFavorites[asset.id]
                        if (newFav != null) asset.copy(isFavorite = newFav) else asset
                    }
                    val updatedWorkPile = allAssetsFoundFlow.value.map { asset ->
                        val newFav = currentState.localFavorites[asset.id]
                        if (newFav != null) asset.copy(isFavorite = newFav) else asset
                    }
                    allAssetsFoundFlow.value = updatedWorkPile
                }
                
                // 3. Save sync history
                swipeDecisionRepository.saveSyncHistory(
                    userId = config.userId,
                    deletedCount = currentState.deletedCount,
                    bytesSaved = currentState.deletedSize,
                    keptCount = currentState.keptCount,
                    archivedCount = currentState.archiveCount,
                    lockedCount = currentState.lockedCount
                )

                // 4. Update local work pile to remove deleted and locked assets permanently for this session
                val toRemoveFromWorkPile = (toDelete + toLock).toSet()
                if (toRemoveFromWorkPile.isNotEmpty()) {
                    masterWorkPile = masterWorkPile.filter { it.id !in toRemoveFromWorkPile }
                    val updatedWorkPile = allAssetsFoundFlow.value.filter { it.id !in toRemoveFromWorkPile }
                    allAssetsFoundFlow.value = updatedWorkPile
                    
                    // Update the total count to reflect removals
                    _uiState.update { it.copy(remoteTotalCount = (it.remoteTotalCount - toRemoveFromWorkPile.size).coerceAtLeast(0)) }
                }

                // 5. Success state
                val remainingDecisions = currentState.decisions.filterKeys { id -> id !in toRemoveFromWorkPile }
                _uiState.update { it.copy(
                    isSyncing = false, 
                    showSummary = false, 
                    showSuccessAnimation = true,
                    // Remove deleted & locked assets from current UI decisions too
                    decisions = remainingDecisions,
                    history = emptyList(), // Reset history for undo after sync
                    localFavorites = emptyMap()
                ) }

                // Force refresh sorted work pile so `_uiState.value.assets` and `currentIndex` are updated immediately
                pendingJumpToFirstUnprocessed = true
                refreshSortedWorkPile(
                    jumpToFirstUnprocessed = true,
                    overrideDecisions = remainingDecisions,
                    overrideHistory = emptyList()
                )

                delay(2000)
                _uiState.update { it.copy(showSuccessAnimation = false) }
            } catch (e: Exception) {
                AppLogger.e("SwipeViewModel", "Error applying changes: ${e.message}", e)
                _uiState.update { it.copy(isSyncing = false, error = e.message) }
            }
        }
    }
}
