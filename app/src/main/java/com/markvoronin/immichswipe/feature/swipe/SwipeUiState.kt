package com.markvoronin.immichswipe.feature.swipe

import com.markvoronin.immichswipe.core.CardDisplayMode
import com.markvoronin.immichswipe.core.ConnectionStatus
import com.markvoronin.immichswipe.core.DoubleTapAction
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.ImmichOpenMode
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset


enum class SwipeDecision {
    KEEP, DELETE, ARCHIVE, LOCK
}


data class SwipeUiState(
    val isLoading: Boolean = false,
    val isFetchingAssets: Boolean = false,
    val isSyncing: Boolean = false,
    val syncLoadedCount: Int = 0,
    val syncTotalCount: Int = 0,
    val showSuccessAnimation: Boolean = false,
    val showSummary: Boolean = false,
    val albumName: String = "",
    val assets: List<Asset> = emptyList(),
    val masterWorkPile: List<Asset> = emptyList(),
    val remoteTotalCount: Int = 0,
    val currentIndex: Int = 0,
    val decisions: Map<String, SwipeDecision> = emptyMap(),
    val assetSizes: Map<String, Long> = emptyMap(),
    val history: List<String> = emptyList(),
    val error: String? = null,
    val playbackBehavior: PlaybackBehavior = PlaybackBehavior.PAUSE_OTHERS,
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
    val isMuted: Boolean = false,
    val showFavoriteButton: Boolean = true,
    val autoNextOnFav: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus(),
    val baseUrl: String = "",
    val apiKey: String = "",
    val includeArchived: Boolean = false,
    val sortCategory: com.markvoronin.immichswipe.core.SortCategory = com.markvoronin.immichswipe.core.SortCategory.TIME,
    val sortOrder: SortOrder = SortOrder.CHRONOLOGICAL_DESC,
    val localFavorites: Map<String, Boolean> = emptyMap(),
    val localRotations: Map<String, Int> = emptyMap(),
    val cardDisplayMode: CardDisplayMode = CardDisplayMode.FIT,
    val showSwipeButtons: Boolean = false,
    val showArchiveButton: Boolean = true,
    val showLockButton: Boolean = false,
    val showAddToAlbumButton: Boolean = true,
    val showAddToAlbumDialog: Boolean = false,
    val albumsForAddToAlbum: List<Album> = emptyList(),
    val isFetchingAlbumsForDialog: Boolean = false,
    val isFullscreenMode: Boolean = false,
    val showResetConfirmation: Boolean = false,
    val syncLocalDeletion: Boolean = false,
    val trashLocalDeletion: Boolean = true,
    val tapToSwipeEnabled: Boolean = false,
    val doubleTapEnabled: Boolean = false,
    val doubleTapAction: DoubleTapAction = DoubleTapAction.FULLSCREEN,
    val localDeletePendingIntent: android.app.PendingIntent? = null,
    val userQuotaBytes: Long? = null,
    val albumId: String = "",
    val isBulkDeleteMode: Boolean = false,
    val isBulkKeepMode: Boolean = false,
    val bulkSelection: Set<String> = emptySet(),
    val bulkLastIndex: Int? = null,
    val hasCompletedSwipeTutorial: Boolean = true,
) {
    val currentAsset: Asset? get() = assets.getOrNull(bulkLastIndex ?: currentIndex)


    fun getRotation(assetId: String): Int {
        return localRotations[assetId] ?: assets.find { it.id == assetId }?.rotation ?: 0
    }
    

    fun isFavorite(assetId: String): Boolean {
        return localFavorites[assetId] ?: assets.find { it.id == assetId }?.isFavorite ?: false
    }


    fun isArchived(assetId: String): Boolean {
        return decisions[assetId] == SwipeDecision.ARCHIVE || (assets.find { it.id == assetId }?.isArchived ?: false)
    }

    fun isLocked(assetId: String): Boolean {
        return decisions[assetId] == SwipeDecision.LOCK || (assets.find { it.id == assetId }?.isLocked ?: false)
    }


    val totalCount: Int get() = remoteTotalCount
    val processedCount: Int get() = decisions.size
    val remainingCount: Int get() = (totalCount - processedCount).coerceAtLeast(0)

    val keptCount: Int get() = decisions.values.count { it == SwipeDecision.KEEP }
    val allKeptCount: Int get() = decisions.values.count { it == SwipeDecision.KEEP || it == SwipeDecision.ARCHIVE || it == SwipeDecision.LOCK }
    val deletedCount: Int get() = decisions.values.count { it == SwipeDecision.DELETE }
    val archiveCount: Int get() = decisions.values.count { it == SwipeDecision.ARCHIVE }
    val lockedCount: Int get() = decisions.values.count { it == SwipeDecision.LOCK }

    private fun getEffectiveSize(assetId: String): Long {
        return assetSizes[assetId]
            ?: assets.find { it.id == assetId }?.exifInfo?.fileSizeInBytes
            ?: masterWorkPile.find { it.id == assetId }?.exifInfo?.fileSizeInBytes
            ?: 0L
    }


    val keptSize: Long get() = decisions.filter { it.value == SwipeDecision.KEEP }.keys.sumOf { getEffectiveSize(it) }
    val deletedSize: Long get() = decisions.filter { it.value == SwipeDecision.DELETE }.keys.sumOf { getEffectiveSize(it) }
    val archiveSize: Long get() = decisions.filter { it.value == SwipeDecision.ARCHIVE }.keys.sumOf { getEffectiveSize(it) }
    val lockedSize: Long get() = decisions.filter { it.value == SwipeDecision.LOCK }.keys.sumOf { getEffectiveSize(it) }

    val deletedAssets: List<Asset> get() {
        val deletedIds = decisions.filter { it.value == SwipeDecision.DELETE }.keys
        val fromAssets = assets.filter { it.id in deletedIds }
        val missingIds = deletedIds - fromAssets.map { it.id }.toSet()
        if (missingIds.isEmpty()) return fromAssets
        val fromMaster = masterWorkPile.filter { it.id in missingIds }
        return (fromAssets + fromMaster).distinctBy { it.id }
    }


    val isRemainingEstimated: Boolean get() {
        if ((albumId == Album.VIRTUAL_ALL_ID || albumId == Album.VIRTUAL_DUPLICATES_ID) && userQuotaBytes != null && userQuotaBytes > 0 && !includeArchived) {
            return false
        }
        val hasIncompletePile = assets.any { !decisions.containsKey(it.id) && (assetSizes[it.id] ?: 0L) == 0L }
        val hasNonLoadedAssets = remoteTotalCount > assets.size
        return hasIncompletePile || hasNonLoadedAssets
    }

    val progress: Float get() = if (remoteTotalCount > 0) processedCount.toFloat() / remoteTotalCount else 0f
}
