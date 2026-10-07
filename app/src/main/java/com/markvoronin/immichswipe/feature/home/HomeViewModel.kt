package com.markvoronin.immichswipe.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.local.dao.UnsyncedDecisionCounts
import com.markvoronin.immichswipe.data.repository.AccountRepository
import com.markvoronin.immichswipe.data.repository.AlbumRepository
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.data.repository.UserRepository
import com.markvoronin.immichswipe.domain.model.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val albumRepository: AlbumRepository,
    private val swipeDecisionRepository: SwipeDecisionRepository,
    private val assetRepository: AssetRepository,
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val activeUserId: String
        get() = sessionManager.getUserId() ?: ""
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.playbackBehavior.collect { behavior ->
                _uiState.update { it.copy(playbackBehavior = behavior) }
            }
        }
        viewModelScope.launch {
            sessionRepository.themeMode.collect { theme ->
                _uiState.update { it.copy(themeMode = theme) }
            }
        }

        viewModelScope.launch {
            sessionRepository.includeArchived.collect { include ->
                _uiState.update { it.copy(includeArchived = include) }
                refreshAlbums()
            }
        }

        viewModelScope.launch {
            sessionRepository.sortOrder.collect { order ->
                _uiState.update { it.copy(sortOrder = order) }
            }
        }

        viewModelScope.launch {
            sessionRepository.defaultLayoutGrid.collect { isGrid ->
                _uiState.update { it.copy(isGridView = isGrid) }
            }
        }


        viewModelScope.launch {
            sessionManager.connectionStatus.collect { status ->
                _uiState.update { it.copy(connectionStatus = status) }
            }
        }

        viewModelScope.launch {
            sessionManager.sessionConfig.collect { config ->
                if (config != null) {
                    _uiState.update { it.copy(
                        baseUrl = config.baseUrl,
                        apiKey = config.apiKey,
                        user = if (it.user?.id != config.userId) null else it.user,
                        albums = if (it.user?.id != config.userId) emptyList() else it.albums,
                        albumTreatedCounts = if (it.user?.id != config.userId) emptyMap() else it.albumTreatedCounts,
                        albumUnsyncedChanges = if (it.user?.id != config.userId) emptyMap() else it.albumUnsyncedChanges
                    ) }
                    loadUser()
                } else {
                    _uiState.update { it.copy(
                        baseUrl = "",
                        apiKey = "",
                        user = null,
                        albums = emptyList(),
                        albumTreatedCounts = emptyMap(),
                        albumUnsyncedChanges = emptyMap()
                    ) }
                }
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            sessionManager.sessionConfig
                .map { it?.userId ?: "" }
                .distinctUntilChanged()
                .flatMapLatest { userId ->
                    if (userId.isEmpty()) flowOf(Triple(0, 0, emptyList()))
                    else combine(
                        swipeDecisionRepository.getGlobalUniqueTreatedCount(userId),
                        swipeDecisionRepository.getGlobalUnsyncedCount(userId),
                        swipeDecisionRepository.getAllAlbumDecisionCounts(userId)
                    ) { globalTreatedCount, globalUnsyncedCount, albumStats ->
                        Triple(globalTreatedCount, globalUnsyncedCount, albumStats)
                    }
                }.collect { (globalTreatedCount, globalUnsyncedCount, albumStats) ->
                    val treatedMap = albumStats.associateBy { it.albumId }.mapValues { it.value.totalCount }.toMutableMap()
                    val unsyncedMap = albumStats.associateBy { it.albumId }.mapValues { it.value.unsyncedCount }.toMutableMap()
                    
                    treatedMap[Album.VIRTUAL_ALL_ID] = globalTreatedCount
                    unsyncedMap[Album.VIRTUAL_ALL_ID] = globalUnsyncedCount
                    
                    _uiState.update { 
                        it.copy(
                            albumTreatedCounts = treatedMap,
                            albumUnsyncedChanges = unsyncedMap
                        )
                    }
                }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            sessionManager.sessionConfig
                .map { it?.userId ?: "" }
                .distinctUntilChanged()
                .flatMapLatest { userId ->
                    if (userId.isEmpty()) flowOf(Pair(emptyList(), UnsyncedDecisionCounts(0, 0)))
                    else combine(
                        swipeDecisionRepository.getSyncHistory(userId),
                        swipeDecisionRepository.getUnsyncedDecisionCounts(userId)
                    ) { history, unsyncedCounts ->
                        Pair(history, unsyncedCounts)
                    }
                }.collect { (history, unsyncedCounts) ->
                    val albums = _uiState.value.albums
                    val treatedCounts = _uiState.value.albumTreatedCounts
                    val now = System.currentTimeMillis()
                    val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000L)
                    
                    val weeklyHistory = history.filter { it.timestamp >= oneWeekAgo }

                    val totalDeleted = history.sumOf { it.deletedCount }
                    val totalBytes = history.sumOf { it.bytesSaved }
                    val totalLocked = history.sumOf { it.lockedCount }
                    
                    val totalKept = history.sumOf { it.keptCount } + unsyncedCounts.keptCount
                    val totalArchived = history.sumOf { it.archivedCount } + unsyncedCounts.archivedCount
                    
                    val weeklyDeleted = weeklyHistory.sumOf { it.deletedCount }
                    val weeklyBytes = weeklyHistory.sumOf { it.bytesSaved }

                    val completedCount = albums.count { album ->
                        val treated = treatedCounts[album.id] ?: 0
                        (treated >= album.assetCount) && (album.assetCount > 0)
                    }

                    val newStats = StatsUiData(
                        totalDeleted = totalDeleted,
                        totalBytesSaved = totalBytes,
                        totalKept = totalKept,
                        totalArchived = totalArchived,
                        totalLocked = totalLocked,
                        totalAlbums = albums.size,
                        completedAlbums = completedCount,
                        weeklyDeleted = weeklyDeleted,
                        weeklyBytesSaved = weeklyBytes
                    )
                    _uiState.update { it.copy(stats = newStats) }
                }
        }

        viewModelScope.launch {
            accountRepository.allAccounts.collect { accounts ->
                _uiState.update { it.copy(savedAccounts = accounts) }
            }
        }

        viewModelScope.launch {
            sessionRepository.backupWarningShown.collect { shown ->
                _uiState.update { it.copy(showBackupWarning = !shown) }
            }
        }
    }

    fun loadUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                AppLogger.d("Home", "Loading user data and albums")
                
                val user = userRepository.getCurrentUser()
                _uiState.update { it.copy(user = user) }

                val includeArchived = _uiState.value.includeArchived
                
                coroutineScope {
                    val allCountDeferred = async { assetRepository.getTotalAssetCount(includeArchived) }
                    val orphansCountDeferred = async { assetRepository.getOrphansCount(includeArchived) }
                    val duplicatesCountDeferred = async { assetRepository.getDuplicatesCount() }

                    val rawAlbums = albumRepository.getAlbumsRaw()
                    
                    AppLogger.i("Home", "Raw album list retrieved: ${rawAlbums.size}")
                    
                    _uiState.update { 
                        it.copy(
                            albums = rawAlbums,
                            allAssetsCount = allCountDeferred.await(),
                            orphansCount = orphansCountDeferred.await(),
                            duplicatesCount = duplicatesCountDeferred.await(),
                            isLoading = false,
                            error = null
                        )
                    }

                    if (!includeArchived) {
                        val refinedAlbums = albumRepository.refineAlbumCounts(rawAlbums)
                        _uiState.update { it.copy(albums = refinedAlbums) }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Home", "Error during initial loading", e)
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Loading error", 
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refreshAlbums() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val startTime = System.currentTimeMillis()
                
                val includeArchived = _uiState.value.includeArchived
                
                coroutineScope {
                    val allCountDeferred = async { assetRepository.getTotalAssetCount(includeArchived) }
                    val orphansCountDeferred = async { assetRepository.getOrphansCount(includeArchived) }
                    val duplicatesCountDeferred = async { assetRepository.getDuplicatesCount() }

                    val rawAlbums = albumRepository.getAlbumsRaw()
                    
                    _uiState.update { 
                        it.copy(
                            albums = rawAlbums,
                            allAssetsCount = allCountDeferred.await(),
                            orphansCount = orphansCountDeferred.await(),
                            duplicatesCount = duplicatesCountDeferred.await()
                        )
                    }

                    if (!includeArchived) {
                        val refinedAlbums = albumRepository.refineAlbumCounts(rawAlbums)
                        _uiState.update { it.copy(albums = refinedAlbums) }
                    }

                    val duration = System.currentTimeMillis() - startTime
                    if (duration < 800) {
                        delay((800 - duration).milliseconds)
                    }

                    _uiState.update { it.copy(isRefreshing = false, error = null) }
                }
            } catch (e: Exception) {
                AppLogger.e("Home", "Error in refreshAlbums", e)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onAlbumSelected(album: Album) {
        _uiState.update { 
            it.copy(selectedAlbum = album, currentTab = HomeTab.SWIPE, previousTab = HomeTab.HOME)
        }
    }

    fun toggleProfilePopup(visible: Boolean) {
        _uiState.update { it.copy(showProfilePopup = visible) }
    }

    fun toggleStatsPopup(visible: Boolean) {
        _uiState.update { it.copy(showStatsPopup = visible) }
    }

    fun logout() = viewModelScope.launch {
        val currentUserId = _uiState.value.user?.id
        _uiState.update { it.copy(currentTab = HomeTab.HOME, showProfilePopup = false) }
        
         currentUserId?.let { accountRepository.deleteAccount(it) }
        
         sessionRepository.clearSession()
    }

    fun removeAccount(userId: String) = viewModelScope.launch {
        val currentUserId = _uiState.value.user?.id
        if (userId == currentUserId) {
            logout()
        } else {
            accountRepository.deleteAccount(userId)
        }
    }

    fun switchAccount(userId: String) = viewModelScope.launch {
        val account = accountRepository.getAccount(userId) ?: return@launch
        AppLogger.i("Home", "Switching to account ${account.userName} ($userId)")
        
         accountRepository.updateLastActive(userId)
        
         sessionRepository.saveSession(
            baseUrl = account.baseUrl,
            token = account.apiKey,
            userId = account.userId
        )
    }

    fun startAddAccount() {
        _uiState.update { it.copy(isLoggingInToAnotherAccount = true, showProfilePopup = false) }
    }

    fun cancelAddAccount() {
        _uiState.update { it.copy(isLoggingInToAnotherAccount = false) }
    }

    fun onSearchQueryChanged(query: String) = _uiState.update { it.copy(searchQuery = query) }
    
    fun updateVirtualNames(id: String, name: String, description: String? = null) {
        _uiState.update { 
            val newNames = it.virtualNames.toMutableMap()
            newNames[id] = name
            val newDescs = it.virtualDescriptions.toMutableMap()
            if (description != null) {
                newDescs[id] = description
            }
            it.copy(virtualNames = newNames, virtualDescriptions = newDescs)
        }
    }

    fun toggleCategory(status: AlbumStatus) {
        _uiState.update { state ->
            val newCollapsed = state.collapsedCategories.toMutableSet()
            if (newCollapsed.contains(status)) {
                newCollapsed.remove(status)
            } else {
                newCollapsed.add(status)
            }
            state.copy(collapsedCategories = newCollapsed)
        }
    }

    fun dismissBackupWarning() {
        viewModelScope.launch {
            sessionRepository.saveBackupWarningShown(true)
        }
    }

    private val _resetRequestChannel = Channel<Unit>(Channel.BUFFERED)
    val resetRequestSignal = _resetRequestChannel.receiveAsFlow()

    fun requestReset() {
        _resetRequestChannel.trySend(Unit)
    }

    fun toggleGlobalResetConfirmation(visible: Boolean) {
        _uiState.update { it.copy(showGlobalResetConfirmation = visible) }
    }

    fun resetAllDecisions() {
        viewModelScope.launch {
            swipeDecisionRepository.clearUserData(activeUserId)
            toggleGlobalResetConfirmation(false)
             refreshAlbums()
        }
    }

    fun getSessionRepository() = sessionRepository
}
