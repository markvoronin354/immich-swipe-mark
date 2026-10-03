package com.markvoronin.immichswipe.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.ImmichOpenMode
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.core.cache.CacheManager
import com.markvoronin.immichswipe.data.local.model.DatabaseExport
import com.markvoronin.immichswipe.data.repository.AccountRepository
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val swipeDecisionRepository: SwipeDecisionRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        observeSettings()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                _uiState.value = _uiState.value.copy(
                    userName = user.name ?: "",
                    userQuotaBytes = user.quotaUsageInBytes
                )
            } catch (e: Exception) {
                AppLogger.e("SettingsVM", "Error loading user", e)
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            accountRepository.allAccounts.collect { accounts ->
                _uiState.update { it.copy(savedAccountsCount = accounts.size) }
            }
        }
        viewModelScope.launch {
            sessionRepository.playbackBehavior.collect { behavior ->
                _uiState.value = _uiState.value.copy(playbackBehavior = behavior)
            }
        }
        viewModelScope.launch {
            sessionRepository.themeMode.collect { theme ->
                _uiState.value = _uiState.value.copy(themeMode = theme)
            }
        }
        viewModelScope.launch {
            sessionRepository.dynamicColor.collect { enabled ->
                _uiState.value = _uiState.value.copy(dynamicColor = enabled)
            }
        }
        viewModelScope.launch {
            sessionRepository.fullscreenButtonPosition.collect { pos ->
                _uiState.value = _uiState.value.copy(fullscreenButtonPosition = pos)
            }
        }
        viewModelScope.launch {
            sessionRepository.immichButtonPosition.collect { pos ->
                _uiState.value = _uiState.value.copy(immichButtonPosition = pos)
            }
        }
        viewModelScope.launch {
            sessionRepository.immichOpenMode.collect { mode ->
                _uiState.value = _uiState.value.copy(immichOpenMode = mode)
            }
        }
        viewModelScope.launch {
            sessionRepository.immichLongPressWeb.collect { enabled ->
                _uiState.value = _uiState.value.copy(immichLongPressWeb = enabled)
            }
        }
        viewModelScope.launch {
            sessionRepository.rotationButtonPosition.collect { pos ->
                _uiState.value = _uiState.value.copy(rotationButtonPosition = pos)
            }
        }
        viewModelScope.launch {
            sessionRepository.muteButtonPosition.collect { pos ->
                _uiState.value = _uiState.value.copy(muteButtonPosition = pos)
            }
        }
        viewModelScope.launch {
            sessionRepository.downloadButtonPosition.collect { pos ->
                _uiState.value = _uiState.value.copy(downloadButtonPosition = pos)
            }
        }
        viewModelScope.launch {
            sessionRepository.shareButtonPosition.collect { pos ->
                _uiState.value = _uiState.value.copy(shareButtonPosition = pos)
            }
        }
        viewModelScope.launch {
            sessionRepository.showFullscreenButton.collect { show ->
                _uiState.value = _uiState.value.copy(showFullscreenButton = show)
            }
        }
        viewModelScope.launch {
            sessionRepository.showImmichButton.collect { show ->
                _uiState.value = _uiState.value.copy(showImmichButton = show)
            }
        }
        viewModelScope.launch {
            sessionRepository.showRotationButton.collect { show ->
                _uiState.value = _uiState.value.copy(showRotationButton = show)
            }
        }
        viewModelScope.launch {
            sessionRepository.showMuteButton.collect { show ->
                _uiState.value = _uiState.value.copy(showMuteButton = show)
            }
        }
        viewModelScope.launch {
            sessionRepository.showDownloadButton.collect { show ->
                _uiState.value = _uiState.value.copy(showDownloadButton = show)
            }
        }
        viewModelScope.launch {
            sessionRepository.showShareButton.collect { show ->
                _uiState.value = _uiState.value.copy(showShareButton = show)
            }
        }
        viewModelScope.launch {
            sessionRepository.defaultLayoutGrid.collect { isGrid ->
                _uiState.value = _uiState.value.copy(isDefaultLayoutGrid = isGrid)
            }
        }
        viewModelScope.launch {
            sessionRepository.showFavoriteButton.collect { show ->
                _uiState.value = _uiState.value.copy(showFavoriteButton = show)
            }
        }
        viewModelScope.launch {
            sessionRepository.autoNextOnFav.collect { autoNextOnFav ->
                _uiState.value = _uiState.value.copy(autoNextOnFav = autoNextOnFav)
            }
        }
        viewModelScope.launch {
            sessionRepository.includeArchived.collect { include ->
                _uiState.value = _uiState.value.copy(includeArchived = include)
            }
        }
        viewModelScope.launch {
            sessionRepository.sortOrder.collect { order ->
                _uiState.update { it.copy(sortOrder = order) }
            }
        }
        viewModelScope.launch {
            sessionRepository.defaultCardDisplayMode.collect { mode ->
                _uiState.update { it.copy(defaultCardDisplayMode = mode) }
            }
        }
        viewModelScope.launch {
            sessionRepository.showSwipeButtons.collect { show ->
                _uiState.update { it.copy(showSwipeButtons = show) }
            }
        }
        viewModelScope.launch {
            sessionRepository.showArchiveButton.collect { show ->
                _uiState.update { it.copy(showArchiveButton = show) }
            }
        }
        viewModelScope.launch {
            sessionRepository.showLockButton.collect { show ->
                _uiState.update { it.copy(showLockButton = show) }
            }
        }
        viewModelScope.launch {
            sessionRepository.showAddToAlbumButton.collect { show ->
                _uiState.update { it.copy(showAddToAlbumButton = show) }
            }
        }
        viewModelScope.launch {
            sessionRepository.syncLocalDeletion.collect { sync ->
                _uiState.update { it.copy(syncLocalDeletion = sync) }
            }
        }
        viewModelScope.launch {
            sessionRepository.trashLocalDeletion.collect { trash ->
                _uiState.update { it.copy(trashLocalDeletion = trash) }
            }
        }
        viewModelScope.launch {
            sessionRepository.tapToSwipeEnabled.collect { enabled ->
                _uiState.update { it.copy(tapToSwipeEnabled = enabled) }
            }
        }
    }

    fun setPlaybackBehavior(behavior: PlaybackBehavior) {
        viewModelScope.launch {
            sessionRepository.savePlaybackBehavior(behavior)
        }
    }

    fun setThemeMode(theme: AppTheme) {
        viewModelScope.launch {
            sessionRepository.saveThemeMode(theme)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            sessionRepository.saveDynamicColor(enabled)
        }
    }

    fun setFullscreenButtonPosition(pos: IconPosition) {
        viewModelScope.launch {
            sessionRepository.saveFullscreenButtonPosition(pos)
        }
    }

    fun setImmichButtonPosition(pos: IconPosition) {
        viewModelScope.launch {
            sessionRepository.saveImmichButtonPosition(pos)
        }
    }

    fun setImmichOpenMode(mode: ImmichOpenMode) {
        viewModelScope.launch {
            sessionRepository.saveImmichOpenMode(mode)
        }
    }

    fun setImmichLongPressWeb(enabled: Boolean) {
        viewModelScope.launch {
            sessionRepository.saveImmichLongPressWeb(enabled)
        }
    }

    fun setRotationButtonPosition(pos: IconPosition) {
        viewModelScope.launch {
            sessionRepository.saveRotationButtonPosition(pos)
        }
    }
    fun setCardDisplayButtonPosition(pos: IconPosition) { setRotationButtonPosition(pos) }

    fun setMuteButtonPosition(pos: IconPosition) {
        viewModelScope.launch {
            sessionRepository.saveMuteButtonPosition(pos)
        }
    }

    fun setDownloadButtonPosition(pos: IconPosition) {
        viewModelScope.launch {
            sessionRepository.saveDownloadButtonPosition(pos)
        }
    }

    fun setShareButtonPosition(pos: IconPosition) {
        viewModelScope.launch {
            sessionRepository.saveShareButtonPosition(pos)
        }
    }

    fun setShowFullscreenButton(show: Boolean) {
        viewModelScope.launch { sessionRepository.saveShowFullscreenButton(show) }
    }

    fun setShowImmichButton(show: Boolean) {
        viewModelScope.launch { sessionRepository.saveShowImmichButton(show) }
    }

    fun setShowRotationButton(show: Boolean) {
        viewModelScope.launch { sessionRepository.saveShowRotationButton(show) }
    }
    fun setShowCardDisplayButton(show: Boolean) { setShowRotationButton(show) }

    fun setShowMuteButton(show: Boolean) {
        viewModelScope.launch { sessionRepository.saveShowMuteButton(show) }
    }

    fun setShowDownloadButton(show: Boolean) {
        viewModelScope.launch { sessionRepository.saveShowDownloadButton(show) }
    }

    fun setShowShareButton(show: Boolean) {
        viewModelScope.launch { sessionRepository.saveShowShareButton(show) }
    }

    fun setDefaultLayoutGrid(isGrid: Boolean) {
        viewModelScope.launch {
            sessionRepository.saveDefaultLayoutGrid(isGrid)
        }
    }

    fun setShowFavorite(show: Boolean) {
        viewModelScope.launch { sessionRepository.saveShowFavorite(show) }
    }

    fun setAutoNextOnFav(autoNextOnFav: Boolean) {
        viewModelScope.launch { sessionRepository.saveAutoNextOnFav(autoNextOnFav) }
    }

    fun setIncludeArchived(include: Boolean) {
        viewModelScope.launch { sessionRepository.saveIncludeArchived(include) }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { sessionRepository.saveSortOrder(order) }
    }

    fun setDefaultCardDisplayMode(mode: com.markvoronin.immichswipe.core.CardDisplayMode) {
        viewModelScope.launch {
            sessionRepository.saveDefaultCardDisplayMode(mode)
        }
    }

    fun setShowSwipeButtons(show: Boolean) {
        viewModelScope.launch {
            sessionRepository.saveShowSwipeButtons(show)
        }
    }

    fun setShowArchiveButton(show: Boolean) {
        viewModelScope.launch {
            sessionRepository.saveShowArchiveButton(show)
        }
    }

    fun setShowLockButton(show: Boolean) {
        viewModelScope.launch {
            sessionRepository.saveShowLockButton(show)
        }
    }

    fun setShowAddToAlbumButton(show: Boolean) {
        viewModelScope.launch {
            sessionRepository.saveShowAddToAlbumButton(show)
        }
    }

    fun setSyncLocalDeletion(sync: Boolean) {
        viewModelScope.launch {
            sessionRepository.saveSyncLocalDeletion(sync)
        }
    }


    fun setTapToSwipeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionRepository.saveTapToSwipeEnabled(enabled)
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionRepository.clearSession()
        }
    }

    fun removeAllAccounts() {
        viewModelScope.launch {
            accountRepository.deleteAllAccounts()
            sessionRepository.clearSession()
        }
    }

    // --- Gestion de la Base de Données ---

    fun requestDatabaseAction(action: DatabaseAction, scope: DatabaseScope) {
        _uiState.value = _uiState.value.copy(
            pendingDatabaseAction = action,
            pendingDatabaseScope = scope
        )
    }

    fun dismissDatabaseConfirmation() {
        _uiState.value = _uiState.value.copy(
            pendingDatabaseAction = null,
            pendingDatabaseScope = null
        )
    }

    fun executeDelete(scope: DatabaseScope, context: Context) {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (scope == DatabaseScope.ALL) {
                swipeDecisionRepository.clearAllData()
            } else {
                userId?.let { swipeDecisionRepository.clearUserData(it) }
            }
            dismissDatabaseConfirmation()
            _uiState.value = _uiState.value.copy(
                databaseActionStatus = context.getString(R.string.settings_db_delete_success)
            )
        }
    }

    fun exportDatabase(scope: DatabaseScope, outputStream: OutputStream, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val userId = sessionManager.getUserId()
                val decisions = if (scope == DatabaseScope.ALL) {
                    swipeDecisionRepository.getAllDecisionsRaw()
                } else {
                    userId?.let { swipeDecisionRepository.getAllDecisionsForUserRaw(it) } ?: emptyList()
                }
                val history = if (scope == DatabaseScope.ALL) {
                    swipeDecisionRepository.getAllSyncHistoryRaw()
                } else {
                    userId?.let { swipeDecisionRepository.getAllSyncHistoryForUserRaw(it) } ?: emptyList()
                }

                val export = DatabaseExport(
                    swipeDecisions = decisions,
                    syncHistory = history,
                    scope = scope.name,
                    userId = if (scope == DatabaseScope.USER) userId else null
                )

                val json = com.google.gson.Gson().toJson(export)
                outputStream.use { it.write(json.toByteArray()) }

                _uiState.value = _uiState.value.copy(
                    databaseActionStatus = context.resources.getQuantityString(R.plurals.settings_db_export_success, decisions.size, decisions.size)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    databaseActionStatus = context.getString(R.string.settings_db_export_error, e.message ?: "")
                )
            } finally {
                dismissDatabaseConfirmation()
            }
        }
    }

    fun importDatabase(inputStream: InputStream, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val json = inputStream.bufferedReader().use { it.readText() }
                val export = com.google.gson.Gson().fromJson(json, DatabaseExport::class.java)

                swipeDecisionRepository.importData(export.swipeDecisions, export.syncHistory)

                _uiState.value = _uiState.value.copy(
                    databaseActionStatus = context.resources.getQuantityString(R.plurals.settings_db_import_success, export.swipeDecisions.size, export.swipeDecisions.size)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    databaseActionStatus = context.getString(R.string.settings_db_import_error, e.message ?: "")
                )
            } finally {
                dismissDatabaseConfirmation()
            }
        }
    }

    fun clearDatabaseActionStatus() {
        _uiState.value = _uiState.value.copy(databaseActionStatus = null)
    }

    fun setShowLogs(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLogsDialog = show)
    }

    fun setShowActionButtonsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showActionButtonsDialog = show)
    }

    fun setShowInteractionsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showInteractionsDialog = show)
    }

    fun setShowClearCacheConfirmation(show: Boolean) {
        _uiState.value = _uiState.value.copy(showClearCacheConfirmation = show)
    }

    fun clearLogs() {
        AppLogger.clearLogs()
    }

    fun getLogs(): String {
        return AppLogger.getLogs()
    }

    fun clearAppCache(context: android.content.Context) {
        viewModelScope.launch {
            try {
                CacheManager.clearAllCache(context)
                val successMessage = context.getString(R.string.settings_clear_cache_success)
                _uiState.value = _uiState.value.copy(
                    databaseActionStatus = successMessage,
                    showClearCacheConfirmation = false
                )
            } catch (e: Exception) {
                AppLogger.e("SettingsVM", "Failed to clear cache", e)
                _uiState.value = _uiState.value.copy(
                    databaseActionStatus = "Failed to clear cache: ${e.message}",
                    showClearCacheConfirmation = false
                )
            }
        }
    }
}
