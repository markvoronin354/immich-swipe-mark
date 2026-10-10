package com.markvoronin.immichswipe.feature.settings

import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.core.CardDisplayMode
import com.markvoronin.immichswipe.core.DoubleTapAction
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.ImmichOpenMode
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.SortOrder

enum class DatabaseScope {
    ALL, USER
}

enum class DatabaseAction {
    DELETE, EXPORT, IMPORT
}


data class SettingsUiState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val userQuotaBytes: Long? = null,
    val savedAccountsCount: Int = 1,
    val playbackBehavior: PlaybackBehavior = PlaybackBehavior.PAUSE_OTHERS,
    val themeMode: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = true,
    val fullscreenButtonPosition: IconPosition = IconPosition.TOP_RIGHT,
    val immichButtonPosition: IconPosition = IconPosition.BOTTOM_LEFT,
    val immichOpenMode: ImmichOpenMode = ImmichOpenMode.APP,
    val immichLongPressWeb: Boolean = true,
    val rotationButtonPosition: IconPosition = IconPosition.TOP_LEFT,
    val muteButtonPosition: IconPosition = IconPosition.BOTTOM_RIGHT,
    val downloadButtonPosition: IconPosition = IconPosition.BOTTOM_LEFT,
    val shareButtonPosition: IconPosition = IconPosition.BOTTOM_RIGHT,
    val showFullscreenButton: Boolean = true,
    val showImmichButton: Boolean = false,
    val showRotationButton: Boolean = true,
    val showMuteButton: Boolean = false,
    val showDownloadButton: Boolean = true,
    val showShareButton: Boolean = true,
    val isDefaultLayoutGrid: Boolean = false,
    val showFavoriteButton: Boolean = true,
    val autoNextOnFav: Boolean = false,
    val includeArchived: Boolean = false,
    val sortOrder: SortOrder = SortOrder.CHRONOLOGICAL_DESC,
    val showLogsDialog: Boolean = false,
    val defaultCardDisplayMode: CardDisplayMode = CardDisplayMode.FIT,
    val showSwipeButtons: Boolean = false,
    val showArchiveButton: Boolean = true,
    val showLockButton: Boolean = false,
    val showAddToAlbumButton: Boolean = true,
    val syncLocalDeletion: Boolean = false,
    val trashLocalDeletion: Boolean = true,
    val tapToSwipeEnabled: Boolean = false,
    val doubleTapEnabled: Boolean = false,
    val doubleTapAction: DoubleTapAction = DoubleTapAction.FULLSCREEN,
    val showActionButtonsDialog: Boolean = false,
    val showInteractionsDialog: Boolean = false,
    val showClearCacheConfirmation: Boolean = false,
    
    // Database actions
    val pendingDatabaseAction: DatabaseAction? = null,
    val pendingDatabaseScope: DatabaseScope? = null,
    val databaseActionStatus: String? = null,
)
