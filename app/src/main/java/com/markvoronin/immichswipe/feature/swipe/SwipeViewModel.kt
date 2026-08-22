package com.markvoronin.immichswipe.feature.swipe

import kotlin.time.Duration.Companion.milliseconds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.CardDisplayMode
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.core.SortCategory
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.AssetBatch
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset

/**
 * ViewModel de l'écran de tri (Swipe).
 */
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
                sessionRepository.autoNextOnRating,
                sessionRepository.swapSummaryArchive,
                sessionRepository.syncLocalDeletion,
                sessionRepository.trashLocalDeletion,
                sessionRepository.sortOrder,
                sessionRepository.tapToSwipeEnabled
            ) { values ->
                // On regroupe toutes les mises à jour en un seul bloc pour optimiser les recompositions
                _uiState.update { state ->
                    val order = values[19] as SortOrder
                    val category = when (order) {
                        SortOrder.CHRONOLOGICAL_DESC, SortOrder.CHRONOLOGICAL_ASC, SortOrder.SHUFFLED -> SortCategory.TIME
                        SortOrder.SIZE_DESC, SortOrder.SIZE_ASC -> SortCategory.SIZE
                        SortOrder.TYPE_VIDEO_FIRST, SortOrder.TYPE_PHOTO_FIRST,
                        SortOrder.TYPE_VIDEO_FIRST_ASC, SortOrder.TYPE_PHOTO_FIRST_ASC,
                        SortOrder.TYPE_VIDEO_FIRST_SHUFFLED, SortOrder.TYPE_PHOTO_FIRST_SHUFFLED -> SortCategory.TYPE
                        SortOrder.RATING_DESC, SortOrder.RATING_ASC -> SortCategory.RATING
                    }

                    // On déclenche le rechargement si l'ordre change (géré plus bas)
                    val needsReload = state.sortOrder != order

                    state.copy(
                        playbackBehavior = values[0] as com.markvoronin.immichswipe.core.PlaybackBehavior,
                        fullscreenButtonPosition = values[1] as com.markvoronin.immichswipe.core.IconPosition,
                        immichButtonPosition = values[2] as com.markvoronin.immichswipe.core.IconPosition,
                        cardDisplayButtonPosition = values[3] as com.markvoronin.immichswipe.core.IconPosition,
                        muteButtonPosition = values[4] as com.markvoronin.immichswipe.core.IconPosition,
                        showFullscreenButton = values[5] as Boolean,
                        showImmichButton = values[6] as Boolean,
                        showCardDisplayButton = values[7] as Boolean,
                        showMuteButton = values[8] as Boolean,
                        showDownloadButton = values[9] as Boolean,
                        downloadButtonPosition = values[10] as com.markvoronin.immichswipe.core.IconPosition,
                        showShareButton = values[11] as Boolean,
                        shareButtonPosition = values[12] as com.markvoronin.immichswipe.core.IconPosition,
                        showSwipeButtons = values[13] as Boolean,
                        autoNextOnFav = values[14] as Boolean,
                        autoNextOnRating = values[15] as Boolean,
                        swapSummaryArchive = values[16] as Boolean,
                        syncLocalDeletion = values[17] as Boolean,
                        trashLocalDeletion = values[18] as Boolean,
                        sortOrder = order,
                        sortCategory = category,
                        tapToSwipeEnabled = values[20] as Boolean
                    ).also { 
                        if (needsReload) loadAssetsAndDecisions()
                    }
                }
            }.collect {}
        }
    }

    fun setSortOrder(order: SortOrder) = viewModelScope.launch {
        sessionRepository.saveSortOrder(order)
    }

    fun setSortCategory(category: SortCategory) {
        val currentCategory = _uiState.value.sortCategory
        if (currentCategory == category) return

        _uiState.update { it.copy(sortCategory = category) }
        
        // On définit un ordre par défaut pour la nouvelle catégorie
        val defaultOrder = when (category) {
            SortCategory.TIME -> SortOrder.CHRONOLOGICAL_DESC
            SortCategory.SIZE -> SortOrder.SIZE_DESC
            SortCategory.TYPE -> SortOrder.TYPE_VIDEO_FIRST
            SortCategory.RATING -> SortOrder.RATING_DESC
        }
        setSortOrder(defaultOrder)
    }

    /**
     * Retente le chargement des données si une erreur a eu lieu.
     */
    fun retryLoading() {
        loadAssetsAndDecisions()
    }

    // On garde en mémoire les décisions qui étaient déjà synchronisées au début de la session
    private var initialSyncedDecisions = mapOf<String, SwipeDecision>()
    private var currentShuffleSeed: Long? = null
    private var loadingJob: Job? = null

    private fun loadAssetsAndDecisions() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true, 
                assets = emptyList(), 
                decisions = emptyMap(), 
                assetSizes = emptyMap(), 
                localRatings = emptyMap(),
                localFavorites = emptyMap(),
                history = emptyList(),
                currentIndex = 0, 
                remoteTotalCount = 0
            ) }
            initialSyncedDecisions = emptyMap()
            try {
                AppLogger.d("Swipe", "Chargement de l'album ${album.albumName} (ID: ${album.id})")
                val config = sessionRepository.sessionConfig.first() ?: return@launch
                val includeArchived = sessionRepository.includeArchived.first()
                val currentSortOrder = sessionRepository.sortOrder.first()

                // Si on est en mode SHUFFLE mais qu'on n'a pas encore de seed, on en crée un
                if (currentSortOrder == SortOrder.SHUFFLED && currentShuffleSeed == null) {
                    currentShuffleSeed = System.currentTimeMillis()
                }

                // On charge TOUTES les décisions locales de l'utilisateur une fois au début
                val allLocalDecisionsList = swipeDecisionRepository.getAllDecisionsForUser(config.userId).first()
                val allLocalDecisions = allLocalDecisionsList.associateBy { it.assetId }
                
                var isFirstBatch = true

                // On charge les assets depuis l'API de manière progressive
                assetRepository.getAssetsByAlbum(
                    album.id,
                    includeArchived,
                    config.userId,
                    sortOrder = currentSortOrder,
                    shuffleSeed = currentShuffleSeed
                ).collect { batch ->
                    val chunk = batch.assets
                    val remoteTotal = batch.total
                    val localDecisionsForChunk = chunk.mapNotNull { allLocalDecisions[it.id] }

                    // On mémorise l'état synchronisé pour calculer les deltas lors de la synchronisation.
                    val newSynced = localDecisionsForChunk
                        .filter { it.isSynced }
                        .associate { entity ->
                            val decision = try { SwipeDecision.valueOf(entity.decision ?: "") } catch (_: Exception) { SwipeDecision.KEEP }
                            entity.assetId to decision
                        }
                    initialSyncedDecisions += newSynced

                    // On transforme les décisions du chunk en Map
                    val chunkDecisionMap = mutableMapOf<String, SwipeDecision>()
                    val chunkSizeMap = mutableMapOf<String, Long>()
                    val chunkRatingMap = mutableMapOf<String, Int>()
                    val chunkFavoriteMap = mutableMapOf<String, Boolean>()

                    localDecisionsForChunk.forEach { entity ->
                        entity.decision?.let { dec ->
                            try {
                                chunkDecisionMap[entity.assetId] = SwipeDecision.valueOf(dec)
                            } catch (_: Exception) {}
                        }
                        
                        entity.fileSize?.let { chunkSizeMap[entity.assetId] = it }
                        chunkRatingMap[entity.assetId] = entity.rating ?: 0
                        chunkFavoriteMap[entity.assetId] = entity.isFavorite ?: (chunk.find { it.id == entity.assetId }?.isFavorite ?: false)
                    }

                    _uiState.update { state ->
                        val updatedAssets = state.assets + chunk
                        val updatedDecisions = state.decisions + chunkDecisionMap
                        val updatedSizes = state.assetSizes + chunkSizeMap

                        // Calcul de l'index : 
                        // - Si c'est le premier batch, on cherche le premier non traité.
                        // - Si on était au bout de la liste précédente, on regarde si le nouveau chunk apporte des photos à traiter.
                        var nextIndex = state.currentIndex
                        if (isFirstBatch || (state.currentIndex >= state.assets.size && updatedAssets.size > state.assets.size)) {
                            val firstUnprocessed = updatedAssets.indexOfFirst { !updatedDecisions.containsKey(it.id) }
                            if (firstUnprocessed != -1) {
                                nextIndex = firstUnprocessed
                            } else if (isFirstBatch) {
                                nextIndex = updatedAssets.size // Tout est déjà traité dans ce premier batch
                            }
                        }

                        state.copy(
                            assets = updatedAssets,
                            decisions = updatedDecisions,
                            assetSizes = updatedSizes,
                            localRatings = state.localRatings + chunkRatingMap,
                            localFavorites = state.localFavorites + chunkFavoriteMap,
                            currentIndex = nextIndex,
                            remoteTotalCount = remoteTotal,
                            isLoading = false
                        )
                    }

                    // Chargement des détails pour l'asset actuel si c'est le début
                    if (isFirstBatch) {
                        val state = _uiState.value
                        if (state.currentIndex < state.assets.size) {
                            loadAssetDetail(state.assets[state.currentIndex].id, state.currentIndex)
                        }
                        isFirstBatch = false
                    }
                }
                
                _uiState.update { it.copy(isLoading = false) }
                AppLogger.d("Swipe", "Chargement de l'album terminé : ${_uiState.value.assets.size} assets récupérés")

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.e("Swipe", "Erreur lors du chargement de l'album", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erreur lors du chargement des photos"
                )
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
        
        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            
            // 1. Sauvegarde locale Room
            swipeDecisionRepository.saveDecision(
                assetId = currentAsset.id,
                albumId = album.id,
                userId = config.userId,
                decision = decision.name,
                fileSize = currentAsset.exifInfo?.fileSizeInBytes,
                rating = currentState.getRating(currentAsset.id).takeIf { it > 0 },
                isFavorite = currentState.localFavorites[currentAsset.id] ?: currentAsset.isFavorite
            )

            // 2. Mise à jour UI
            val newDecisions = currentState.decisions.toMutableMap()
            newDecisions[currentAsset.id] = decision

            val newHistory = currentState.history.toMutableList()
            newHistory.add(currentAsset.id)

            // On avance vers le prochain non traité
            val nextIndex = currentState.assets.indices.firstOrNull { i ->
                i > currentState.currentIndex && !newDecisions.containsKey(currentState.assets[i].id)
            } ?: currentState.assets.size

            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    decisions = newDecisions,
                    history = newHistory
                )
            }
            
            // Pré-chargement du prochain asset si besoin
            if (nextIndex < currentState.assets.size) {
                loadAssetDetail(currentState.assets[nextIndex].id, nextIndex)
            }
        }
    }

    fun toggleFavorite() {
        val currentState = _uiState.value
        val currentAsset = currentState.currentAsset ?: return
        
        val currentFav = currentState.isFavorite(currentAsset.id)
        val newFav = !currentFav
        val newFavorites = currentState.localFavorites.toMutableMap()
        newFavorites[currentAsset.id] = newFav
        
        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            swipeDecisionRepository.saveDecision(
                assetId = currentAsset.id,
                albumId = album.id,
                userId = config.userId,
                decision = currentState.decisions[currentAsset.id]?.name,
                fileSize = currentAsset.exifInfo?.fileSizeInBytes,
                rating = currentState.getRating(currentAsset.id).takeIf { it > 0 },
                isFavorite = newFav
            )
            
            _uiState.update { it.copy(localFavorites = newFavorites) }
            if (currentState.autoNextOnFav) {
                onSwipe(SwipeDecision.KEEP) // Avance à la suivante
            }
        }
    }

    fun setRating(rating: Int) {
        val currentAsset = _uiState.value.currentAsset ?: return
        
        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            
            // Persist locally
            val decision = _uiState.value.decisions[currentAsset.id]?.name ?: "KEEP"
            swipeDecisionRepository.saveDecision(
                assetId = currentAsset.id,
                albumId = album.id,
                userId = config.userId,
                decision = decision,
                fileSize = currentAsset.exifInfo?.fileSizeInBytes,
                rating = if (rating == 0) null else rating,
                isFavorite = _uiState.value.localFavorites[currentAsset.id] ?: currentAsset.isFavorite
            )

            _uiState.update { 
                val newRatings = it.localRatings.toMutableMap()
                if (rating == 0) {
                    newRatings[currentAsset.id] = 0
                } else {
                    newRatings[currentAsset.id] = rating
                }
                it.copy(localRatings = newRatings)
            }

            if (rating > 0 && _uiState.value.autoNextOnRating) {
                onSwipe(SwipeDecision.KEEP)
            }
        }
    }

    fun toggleArchive() {
        onSwipe(SwipeDecision.ARCHIVE)
    }

    fun toggleLock() {
        onSwipe(SwipeDecision.LOCK)
    }

    fun toggleRatingMode() {
        _uiState.update { it.copy(isRatingMode = !it.isRatingMode) }
    }

    fun toggleDisplayMode() {
        val nextMode = if (_uiState.value.cardDisplayMode == CardDisplayMode.FILL) {
            CardDisplayMode.FIT
        } else {
            CardDisplayMode.FILL
        }
        _uiState.update { it.copy(cardDisplayMode = nextMode) }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun enterBulkMode(isDelete: Boolean) {
        _uiState.update { 
            it.copy(
                isBulkDeleteMode = isDelete,
                isBulkKeepMode = !isDelete,
                bulkSelection = emptySet(),
                bulkLastIndex = null
            )
        }
    }

    fun exitBulkMode() {
        _uiState.update { 
            it.copy(
                isBulkDeleteMode = false,
                isBulkKeepMode = false,
                bulkSelection = emptySet(),
                bulkLastIndex = null
            )
        }
    }

    fun setBulkSelection(assetIds: Set<String>, lastIndex: Int? = null) {
        _uiState.update { it.copy(bulkSelection = assetIds, bulkLastIndex = lastIndex) }
    }

    fun executeBulkAction() {
        val currentState = _uiState.value
        val selection = currentState.bulkSelection
        val isDelete = currentState.isBulkDeleteMode
        
        if (selection.isEmpty()) {
            exitBulkMode()
            return
        }

        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            
            val newDecisions = currentState.decisions.toMutableMap()
            val newHistory = currentState.history.toMutableList()
            val decision = if (isDelete) SwipeDecision.DELETE else SwipeDecision.KEEP

            selection.forEach { assetId ->
                if (!newDecisions.containsKey(assetId)) {
                    val asset = currentState.assets.find { it.id == assetId }
                    swipeDecisionRepository.saveDecision(
                        assetId = assetId,
                        albumId = album.id,
                        userId = config.userId,
                        decision = decision.name,
                        fileSize = asset?.exifInfo?.fileSizeInBytes,
                        rating = currentState.getRating(assetId).takeIf { it > 0 },
                        isFavorite = currentState.localFavorites[assetId] ?: asset?.isFavorite
                    )
                    newDecisions[assetId] = decision
                    newHistory.add(assetId)
                }
            }

            // Move to next unprocessed if current was handled
            var nextIndex = currentState.currentIndex
            if (newDecisions.containsKey(currentState.assets.getOrNull(currentState.currentIndex)?.id)) {
                nextIndex = currentState.assets.indices.firstOrNull { i ->
                    i >= currentState.currentIndex && !newDecisions.containsKey(currentState.assets[i].id)
                } ?: currentState.assets.size
            }

            _uiState.update {
                it.copy(
                    isBulkDeleteMode = false,
                    isBulkKeepMode = false,
                    bulkSelection = emptySet(),
                    bulkLastIndex = null,
                    currentIndex = nextIndex,
                    decisions = newDecisions,
                    history = newHistory
                )
            }
        }
    }

    private val _downloadRequestSignal = MutableSharedFlow<Asset>(extraBufferCapacity = 1)
    val downloadRequestSignal = _downloadRequestSignal.asSharedFlow()

    fun downloadAsset(asset: Asset) {
        viewModelScope.launch {
            _downloadRequestSignal.emit(asset)
        }
    }

    private val _shareRequestSignal = MutableSharedFlow<Asset>(extraBufferCapacity = 1)
    val shareRequestSignal = _shareRequestSignal.asSharedFlow()

    fun shareAsset(asset: Asset) {
        viewModelScope.launch {
            _shareRequestSignal.emit(asset)
        }
    }

    fun undo() {
        val currentState = _uiState.value
        val lastAssetIdFromHistory = currentState.history.lastOrNull()

        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            if (lastAssetIdFromHistory != null) {
                // On vérifie si l'action annulée était une décision fraîche ou une modification d'un état synchronisé
                val previouslySynced = initialSyncedDecisions[lastAssetIdFromHistory]
                
                if (previouslySynced == null) {
                    // C'était une nouvelle décision : on supprime totalement de la base locale SAUF s'il y a un rating
                    val currentRating = currentState.getRating(lastAssetIdFromHistory)
                    val currentFav = currentState.localFavorites[lastAssetIdFromHistory]
                    if (currentRating > 0 || currentFav != null) {
                        swipeDecisionRepository.saveDecision(
                            assetId = lastAssetIdFromHistory,
                            albumId = album.id,
                            userId = config.userId,
                            decision = null,
                            fileSize = currentState.assets.find { it.id == lastAssetIdFromHistory }?.exifInfo?.fileSizeInBytes,
                            rating = currentRating.takeIf { it > 0 },
                            isFavorite = currentFav
                        )
                    } else {
                        swipeDecisionRepository.removeDecision(lastAssetIdFromHistory, config.userId)
                    }
                } else {
                    // C'était la modification d'un état déjà synchronisé : on restaure l'ancien état
                    swipeDecisionRepository.saveDecision(
                        assetId = lastAssetIdFromHistory,
                        albumId = album.id,
                        userId = config.userId,
                        decision = previouslySynced.name,
                        fileSize = currentState.assets.find { it.id == lastAssetIdFromHistory }?.exifInfo?.fileSizeInBytes,
                        rating = currentState.getRating(lastAssetIdFromHistory).takeIf { it > 0 },
                        isFavorite = currentState.localFavorites[lastAssetIdFromHistory],
                        isSynced = true
                    )
                }
                
                val newDecisions = currentState.decisions.toMutableMap()
                if (previouslySynced == null) {
                    newDecisions.remove(lastAssetIdFromHistory)
                } else {
                    newDecisions[lastAssetIdFromHistory] = previouslySynced
                }

                val newHistory = currentState.history.toMutableList()
                newHistory.removeAt(newHistory.size - 1)

                val previousIndex = currentState.assets.indexOfFirst { it.id == lastAssetIdFromHistory }

                _uiState.update {
                    it.copy(
                        currentIndex = if (previousIndex != -1) previousIndex else currentState.currentIndex,
                        decisions = newDecisions,
                        history = newHistory
                    )
                }
                
                if (previousIndex != -1) {
                    loadAssetDetail(lastAssetIdFromHistory, previousIndex)
                }
            }
        }
    }

    /**
     * Permet de sauter directement à un asset précis (via la timeline).
     */
    fun onMoveToAsset(index: Int) {
        if (index in _uiState.value.assets.indices) {
            _uiState.update { it.copy(currentIndex = index) }
            loadAssetDetail(_uiState.value.assets[index].id, index)
        }
    }

    /**
     * Affiche ou cache l'écran de résumé.
     */
    fun toggleSummary(visible: Boolean) {
        _uiState.update { it.copy(showSummary = visible) }
    }

    /**
     * Affiche ou cache le dialogue de confirmation de reset.
     */
    fun toggleResetConfirmation(visible: Boolean) {
        _uiState.update { it.copy(showResetConfirmation = visible) }
    }

    /**
     * Affiche ou cache le dialogue de confirmation de reset des ratings.
     */
    fun toggleRatingResetConfirmation(visible: Boolean) {
        _uiState.update { it.copy(showRatingResetConfirmation = visible) }
    }

    fun resetAlbumDecisions() {
        val currentState = _uiState.value
        val assetIds = currentState.assets.map { it.id }
        
        viewModelScope.launch {
            try {
                val config = sessionRepository.sessionConfig.first() ?: return@launch
                
                // 1. Supprimer de la base Room
                swipeDecisionRepository.removeDecisions(assetIds, config.userId)
                
                // 2. Recharger les données pour repartir de zéro
                _uiState.update { it.copy(showResetConfirmation = false) }
                loadAssetsAndDecisions()
                
                AppLogger.i("Swipe", "Album ${album.albumName} réinitialisé avec succès")
            } catch (e: Exception) {
                AppLogger.e("Swipe", "Erreur lors du reset de l'album", e)
            }
        }
    }

    /**
     * Réinitialise tous les ratings pour l'album actuel.
     */
    fun resetAlbumRatings() {
        val currentState = _uiState.value
        val assetIds = currentState.assets.map { it.id }
        
        viewModelScope.launch {
            try {
                val config = sessionRepository.sessionConfig.first() ?: return@launch
                
                // 1. Reset des ratings en base pour les assets actuels
                swipeDecisionRepository.clearRatingsForAssets(assetIds, config.userId)
                
                // 2. Recharger les données pour mettre à jour l'UI
                _uiState.update { it.copy(showRatingResetConfirmation = false) }
                loadAssetsAndDecisions()
                
                AppLogger.i("Swipe", "Ratings de l'album ${album.albumName} réinitialisés")
            } catch (e: Exception) {
                AppLogger.e("Swipe", "Erreur lors du reset des ratings", e)
            }
        }
    }

    /**
     * Active ou désactive le mode plein écran.
     */
    fun toggleFullscreen(enabled: Boolean) {
        _uiState.update { it.copy(isFullscreenMode = enabled) }
    }

    /**
     * Annule une décision spécifique (utilisé depuis le résumé).
     */
    fun undoSpecificDecision(assetId: String) {
        val currentState = _uiState.value
        viewModelScope.launch {
            val config = sessionRepository.sessionConfig.first() ?: return@launch
            
            val previouslySynced = initialSyncedDecisions[assetId]
            if (previouslySynced == null) {
                val currentRating = currentState.getRating(assetId)
                val currentFav = currentState.localFavorites[assetId]
                if (currentRating > 0 || currentFav != null) {
                    swipeDecisionRepository.saveDecision(
                        assetId = assetId,
                        albumId = album.id,
                        userId = config.userId,
                        decision = null,
                        fileSize = currentState.assets.find { it.id == assetId }?.exifInfo?.fileSizeInBytes,
                        rating = currentRating.takeIf { it > 0 },
                        isFavorite = currentFav
                    )
                } else {
                    swipeDecisionRepository.removeDecision(assetId, config.userId)
                }
            } else {
                swipeDecisionRepository.saveDecision(
                    assetId = assetId,
                    albumId = album.id,
                    userId = config.userId,
                    decision = previouslySynced.name,
                    fileSize = currentState.assets.find { it.id == assetId }?.exifInfo?.fileSizeInBytes,
                    rating = currentState.getRating(assetId).takeIf { it > 0 },
                    isFavorite = currentState.localFavorites[assetId],
                    isSynced = true
                )
            }
            
            val newDecisions = currentState.decisions.toMutableMap()
            if (previouslySynced == null) {
                newDecisions.remove(assetId)
            } else {
                newDecisions[assetId] = previouslySynced
            }
            
            val newHistory = currentState.history.toMutableList()
            newHistory.remove(assetId)
            
            _uiState.update {
                it.copy(
                    decisions = newDecisions,
                    history = newHistory
                )
            }
        }
    }

    /**
     * Applique les décisions (Suppression sur Immich) et marque les assets comme traités localement.
     */
    fun applyChanges() {
        val currentState = _uiState.value
        val decisions = currentState.decisions
        
        // On ne synchronise que ce qui a changé par rapport à l'état initial
        val unsyncedDecisions = decisions.filter { (id, decision) ->
            initialSyncedDecisions[id] != decision
        }
        
        val toDeleteIds = unsyncedDecisions.filter { it.value == SwipeDecision.DELETE }.keys.toList()
        val toArchive = unsyncedDecisions.filter { it.value == SwipeDecision.ARCHIVE }.keys.toList()
        val toLock = unsyncedDecisions.filter { it.value == SwipeDecision.LOCK }.keys.toList()
        val toKeep = unsyncedDecisions.filter { it.value == SwipeDecision.KEEP }.keys.toList()
        
        // Gestion des favoris (toujours synchronisés car ils sont volatiles dans l'UI)
        val toFavorite = currentState.localFavorites.filter { it.value }.keys.toList()
        val toUnfavorite = currentState.localFavorites.filter { !it.value }.keys.toList()
        val toUpdateRating = currentState.localRatings

        if (toDeleteIds.isEmpty() && toArchive.isEmpty() && toLock.isEmpty() && toKeep.isEmpty() && 
            toFavorite.isEmpty() && toUnfavorite.isEmpty() && toUpdateRating.isEmpty()) {
            AppLogger.d("Swipe", "Aucun changement à synchroniser")
            _uiState.update { it.copy(showSummary = false) }
            return
        }

        viewModelScope.launch {
            AppLogger.i("Swipe", "Application des changements : DELETE(${toDeleteIds.size}), ARCHIVE(${toArchive.size}), LOCK(${toLock.size}), KEEP(${toKeep.size})")
            _uiState.update { it.copy(isSyncing = true) }
            try {
                val config = sessionRepository.sessionConfig.first() ?: return@launch
                
                // 0. Préparation de la suppression locale (si activée)
                var pendingIntent: android.app.PendingIntent? = null
                if (currentState.syncLocalDeletion && toDeleteIds.isNotEmpty()) {
                    val assetsToDelete = currentState.assets.filter { toDeleteIds.contains(it.id) }
                    val localUris = assetRepository.findLocalUris(assetsToDelete)
                    if (localUris.isNotEmpty()) {
                        pendingIntent = if (currentState.trashLocalDeletion) {
                            assetRepository.createLocalTrashRequest(localUris, trash = true)
                        } else {
                            assetRepository.createLocalDeleteRequest(localUris)
                        }
                    }
                }

                // 1. Appels API
                if (toDeleteIds.isNotEmpty()) assetRepository.deleteAssets(toDeleteIds)
                if (toFavorite.isNotEmpty()) assetRepository.updateAssets(toFavorite, isFavorite = true)
                if (toUnfavorite.isNotEmpty()) assetRepository.updateAssets(toUnfavorite, isFavorite = false)
                if (toArchive.isNotEmpty()) assetRepository.updateAssets(toArchive, visibility = "archive")
                if (toLock.isNotEmpty()) assetRepository.updateAssets(toLock, visibility = "locked")

                // Update ratings
                toUpdateRating.entries.groupBy { it.value }.forEach { (rating, ids) ->
                    // For Immich, rating 0 often means 'unrated'. 
                    // Some versions prefer null, but 0 is generally accepted as 'none'.
                    assetRepository.updateAssets(ids.map { it.key }, rating = rating)
                }

                // 2. Vérification et mise à jour de la base locale
                val freshAssets = mutableListOf<Asset>()
                assetRepository.getAssetsByAlbum(album.id, includeArchived = true, userId = config.userId).collect { batch ->
                    freshAssets.addAll(batch.assets)
                }
                val freshIds = freshAssets.map { it.id }.toSet()

                // - Identification des succès (ceux qui ont disparu de l'album)
                val successfullyDisappeared = (toDeleteIds + toLock).filter { !freshIds.contains(it) }
                
                // Tout ce qui a été traité avec succès et reste dans l'album (Swipes, Ratings, Favoris)
                val successfulKeeps = (toKeep + toArchive + toUpdateRating.keys + toFavorite + toUnfavorite)
                    .distinct()
                    .filter { freshIds.contains(it) }

                // 3. Mise à jour de la base de données locale
                if (successfullyDisappeared.isNotEmpty()) {
                    swipeDecisionRepository.removeDecisions(successfullyDisappeared, config.userId)
                }

                if (successfulKeeps.isNotEmpty()) {
                    swipeDecisionRepository.markAsSynced(successfulKeeps, config.userId)
                }

                // 4. Statistiques de session (delta)
                var deltaKeep = toKeep.size
                var deltaArchive = toArchive.size

                (toKeep + toArchive + toDeleteIds + toLock).forEach { id ->
                    initialSyncedDecisions[id]?.let { previous ->
                        when (previous) {
                            SwipeDecision.KEEP -> deltaKeep--
                            SwipeDecision.ARCHIVE -> deltaArchive--
                            else -> {}
                        }
                    }
                }

                swipeDecisionRepository.saveSyncHistory(
                    userId = config.userId,
                    deletedCount = successfullyDisappeared.count { toDeleteIds.contains(it) },
                    bytesSaved = toDeleteIds.filter { successfullyDisappeared.contains(it) }.sumOf { currentState.assetSizes[it] ?: 0L },
                    keptCount = deltaKeep,
                    archivedCount = deltaArchive,
                    lockedCount = successfullyDisappeared.count { toLock.contains(it) }
                )

                // Mise à jour de l'état local pour refléter la synchronisation
                val currentAssetId = currentState.currentAsset?.id
                val newSyncedDecisions = initialSyncedDecisions.toMutableMap()
                
                // On met à jour les décisions synchronisées (uniquement pour les vrais swipes)
                (toKeep + toArchive).filter { freshIds.contains(it) }.forEach { id -> 
                    newSyncedDecisions[id] = decisions[id] ?: SwipeDecision.KEEP 
                }
                successfullyDisappeared.forEach { newSyncedDecisions.remove(it) }
                initialSyncedDecisions = newSyncedDecisions

                val freshAssetsMap = freshAssets.associateBy { it.id }
                _uiState.update { state ->
                    // On met à jour la liste des assets en fusionnant les données fraîches du serveur
                    // et nos modifs locales qu'on vient de synchroniser pour éviter tout saut visuel.
                    val updatedAssets = state.assets.mapNotNull { asset ->
                        if (successfullyDisappeared.contains(asset.id)) {
                            null
                        } else {
                            val fresh = freshAssetsMap[asset.id]
                            val base = fresh ?: asset
                            base.copy(
                                rating = state.localRatings[asset.id] ?: base.rating,
                                isFavorite = state.localFavorites[asset.id] ?: base.isFavorite
                            )
                        }
                    }

                    // Recalcul de l'index pour éviter les sauts lors du filtrage
                    val newIndex = if (currentAssetId != null) {
                        val foundIndex = updatedAssets.indexOfFirst { a -> a.id == currentAssetId }
                        if (foundIndex != -1) foundIndex else state.currentIndex.coerceAtMost(updatedAssets.size)
                    } else {
                        state.currentIndex.coerceAtMost(updatedAssets.size)
                    }

                    state.copy(
                        assets = updatedAssets,
                        currentIndex = newIndex,
                        isSyncing = false,
                        showSuccessAnimation = true,
                        showSummary = false,
                        localFavorites = emptyMap(),
                        localRatings = emptyMap(),
                        localDeletePendingIntent = pendingIntent
                    )
                }
                
                delay(2500.milliseconds)
                _uiState.update { it.copy(showSuccessAnimation = false) }

            } catch (e: Exception) {
                AppLogger.e("Swipe", "Erreur lors de la synchronisation", e)
                _uiState.update { it.copy(isSyncing = false, error = "Erreur synchro: ${e.message}") }
            }
        }
    }

    /**
     * Une fois que l'Intent de suppression locale a été traité par l'UI, on le vide.
     */
    fun onLocalDeleteIntentHandled() {
        _uiState.update { it.copy(localDeletePendingIntent = null) }
    }

    /**
     * Calcule l'index du prochain asset à afficher en arrière-plan.
     * Priorité aux non-traités, sinon le suivant dans la liste.
     */
    fun getNextUnprocessedIndex(): Int {
        val state = _uiState.value
        val assets = state.assets
        val decisions = state.decisions
        val current = state.currentIndex

        // 1. Chercher le prochain non-traité après
        for (i in (current + 1) until assets.size) {
            if (!decisions.containsKey(assets[i].id)) return i
        }
        
        // 2. Chercher le prochain non-traité avant
        for (i in 0 until current) {
            if (!decisions.containsKey(assets[i].id)) return i
        }

        // 3. Si tout est traité, on affiche simplement la carte suivante dans la liste
        if (current + 1 < assets.size) {
            return current + 1
        }
        
        return -1
    }
}
