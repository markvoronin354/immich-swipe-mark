package com.markvoronin.immichswipe.feature.swipe

import com.markvoronin.immichswipe.core.CardDisplayMode
import com.markvoronin.immichswipe.core.ConnectionStatus
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.ImmichOpenMode
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset

/**
 * Les différentes décisions possibles pour un asset.
 */
enum class SwipeDecision {
    KEEP, DELETE, ARCHIVE, LOCK
}

/**
 * État de la session de tri (Swipe).
 */
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
    val assetSizes: Map<String, Long> = emptyMap(), // Map de AssetID -> Taille connue (persitée ou chargée)
    val history: List<String> = emptyList(), // Liste des IDs swipés pour l'undo
    val error: String? = null,
    val playbackBehavior: PlaybackBehavior = PlaybackBehavior.PAUSE_OTHERS,
    val fullscreenButtonPosition: IconPosition = IconPosition.TOP_RIGHT,
    val immichButtonPosition: IconPosition = IconPosition.TOP_LEFT,
    val immichOpenMode: ImmichOpenMode = ImmichOpenMode.APP,
    val immichLongPressWeb: Boolean = true,
    val rotationButtonPosition: IconPosition = IconPosition.BOTTOM_LEFT,
    val muteButtonPosition: IconPosition = IconPosition.BOTTOM_RIGHT,
    val downloadButtonPosition: IconPosition = IconPosition.BOTTOM_LEFT,
    val shareButtonPosition: IconPosition = IconPosition.BOTTOM_RIGHT,
    val showFullscreenButton: Boolean = true,
    val showImmichButton: Boolean = true,
    val showRotationButton: Boolean = false,
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
    val localFavorites: Map<String, Boolean> = emptyMap(), // Map de AssetID -> Nouveau statut favori
    val localRotations: Map<String, Int> = emptyMap(), // Map de AssetID -> Rotation cumulative (0, 90, 180, 270)
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
    val localDeletePendingIntent: android.app.PendingIntent? = null,
    val userQuotaBytes: Long? = null,
    val albumId: String = "",
    val isBulkDeleteMode: Boolean = false,
    val isBulkKeepMode: Boolean = false,
    val bulkSelection: Set<String> = emptySet(),
    val bulkLastIndex: Int? = null,
    val hasCompletedSwipeTutorial: Boolean = true
) {
    val currentAsset: Asset? get() = assets.getOrNull(bulkLastIndex ?: currentIndex)
    val cardDisplayButtonPosition: IconPosition get() = rotationButtonPosition
    val showCardDisplayButton: Boolean get() = showRotationButton

    /**
     * Retourne la rotation d'un asset (0, 90, 180, 270) en tenant compte des modifs locales.
     */
    fun getRotation(assetId: String): Int {
        return localRotations[assetId] ?: assets.find { it.id == assetId }?.rotation ?: 0
    }
    
    /**
     * Retourne si un asset est favori en tenant compte des modifs locales.
     */
    fun isFavorite(assetId: String): Boolean {
        return localFavorites[assetId] ?: assets.find { it.id == assetId }?.isFavorite ?: false
    }

    /**
     * Retourne si un asset est archivé en tenant compte des modifs locales.
     */
    fun isArchived(assetId: String): Boolean {
        return decisions[assetId] == SwipeDecision.ARCHIVE || (assets.find { it.id == assetId }?.isArchived ?: false)
    }

    /**
     * Retourne si un asset est verrouillé en tenant compte des modifs locales.
     */
    fun isLocked(assetId: String): Boolean {
        return decisions[assetId] == SwipeDecision.LOCK || (assets.find { it.id == assetId }?.isLocked ?: false)
    }

    // Statistiques de tri basées sur les décisions locales non synchronisées.
    // 'assets' représente la pile de travail (non synchronisée).
    // 'decisions' contient les actions déjà effectuées sur cette pile.
    val totalCount: Int get() = remoteTotalCount
    val processedCount: Int get() = decisions.size
    val remainingCount: Int get() = (totalCount - processedCount).coerceAtLeast(0)

    val keptCount: Int get() = decisions.values.count { it == SwipeDecision.KEEP }
    val allKeptCount: Int get() = decisions.values.count { it == SwipeDecision.KEEP || it == SwipeDecision.ARCHIVE || it == SwipeDecision.LOCK }
    val deletedCount: Int get() = decisions.values.count { it == SwipeDecision.DELETE }
    val favoriteCount: Int get() = assets.count { isFavorite(it.id) && isProcessedKeep(it.id) }
    val favoritesAddedCount: Int get() = localFavorites.count { (id, fav) -> fav && !(assets.find { it.id == id }?.isFavorite ?: false) }
    val favoritesRemovedCount: Int get() = localFavorites.count { (id, fav) -> !fav && (assets.find { it.id == id }?.isFavorite ?: false) }
    val archiveCount: Int get() = decisions.values.count { it == SwipeDecision.ARCHIVE }
    val lockedCount: Int get() = decisions.values.count { it == SwipeDecision.LOCK }

    // Calcul de l'état "traité" (Gardé)
    private fun isProcessedKeep(assetId: String): Boolean {
        val d = decisions[assetId]
        return d == SwipeDecision.KEEP || d == SwipeDecision.ARCHIVE || d == SwipeDecision.LOCK
    }
    
    private fun getEffectiveSize(assetId: String): Long {
        return assetSizes[assetId]
            ?: assets.find { it.id == assetId }?.exifInfo?.fileSizeInBytes
            ?: masterWorkPile.find { it.id == assetId }?.exifInfo?.fileSizeInBytes
            ?: 0L
    }

    /**
     * Calcule la taille moyenne des assets dont le poids est connu.
     */
    private val averageKnownSize: Long get() {
        val knownSizes = assetSizes.values.filter { it > 0 }
        return if (knownSizes.isEmpty()) 0L else knownSizes.sum() / knownSizes.size
    }
    
    // Calcul des poids (en bytes) basés sur l'ensemble des décisions
    val keptSize: Long get() = decisions.filter { it.value == SwipeDecision.KEEP }.keys.sumOf { getEffectiveSize(it) }
    val deletedSize: Long get() = decisions.filter { it.value == SwipeDecision.DELETE }.keys.sumOf { getEffectiveSize(it) }
    val favoriteSize: Long get() = decisions.filter { (id, _) -> isFavorite(id) && isProcessedKeep(id) }.keys.sumOf { getEffectiveSize(it) }
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
    
    /**
     * Taille restante : Somme des tailles connues + estimation (moyenne) pour les inconnues.
     */
    val remainingSize: Long get() {
        if ((albumId == com.markvoronin.immichswipe.domain.model.Album.VIRTUAL_ALL_ID || albumId == com.markvoronin.immichswipe.domain.model.Album.VIRTUAL_DUPLICATES_ID) && userQuotaBytes != null && userQuotaBytes > 0 && !includeArchived) {
            // Pour "Tous les médias", on utilise le quota serveur si disponible (plus précis)
            // On soustrait les décisions déjà prises dans la session actuelle
            val processedSize = decisions.keys.sumOf { getEffectiveSize(it) }
            return (userQuotaBytes - processedSize).coerceAtLeast(0L)
        }

        val unprocessedInPile = assets.filter { !decisions.containsKey(it.id) }
        val avg = averageKnownSize
        
        val sizeInPile = unprocessedInPile.sumOf { asset ->
            val size = getEffectiveSize(asset.id)
            if (size > 0) size else avg
        }
        
        // Estimate size for assets not yet loaded in the work pile
        val nonLoadedCount = (remoteTotalCount - assets.size).coerceAtLeast(0)
        val estimatedNonLoadedSize = nonLoadedCount * avg
        
        return sizeInPile + estimatedNonLoadedSize
    }

    /**
     * Indique si la taille "Restant" contient des estimations.
     */
    val isRemainingEstimated: Boolean get() {
        if ((albumId == com.markvoronin.immichswipe.domain.model.Album.VIRTUAL_ALL_ID || albumId == com.markvoronin.immichswipe.domain.model.Album.VIRTUAL_DUPLICATES_ID) && userQuotaBytes != null && userQuotaBytes > 0 && !includeArchived) {
            return false // On se base sur une valeur réelle du serveur
        }
        val hasIncompletePile = assets.any { !decisions.containsKey(it.id) && (assetSizes[it.id] ?: 0L) == 0L }
        val hasNonLoadedAssets = remoteTotalCount > assets.size
        return hasIncompletePile || hasNonLoadedAssets
    }

    // Progression (0.0f à 1.0f)
    val progress: Float get() = if (remoteTotalCount > 0) processedCount.toFloat() / remoteTotalCount else 0f
}
