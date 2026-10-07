package com.markvoronin.immichswipe.feature.home

import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.core.ConnectionStatus
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.data.local.entity.UserAccountEntity
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.User


enum class HomeTab {
    HOME, SWIPE,
}


data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val user: User? = null,
    val albums: List<Album> = emptyList(),
    val currentTab: HomeTab = HomeTab.HOME,
    val selectedAlbum: Album? = null,
    val error: String? = null,
    val playbackBehavior: PlaybackBehavior = PlaybackBehavior.PAUSE_OTHERS,
    val showProfilePopup: Boolean = false,
    val themeMode: AppTheme = AppTheme.SYSTEM,
    val previousTab: HomeTab = HomeTab.HOME,
    val albumTreatedCounts: Map<String, Int> = emptyMap(),
    val albumUnsyncedChanges: Map<String, Int> = emptyMap(),
    val isGridView: Boolean = false,
    val searchQuery: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus(),
    val baseUrl: String = "",
    val apiKey: String = "",
    val allAssetsCount: Int = 0,
    val orphansCount: Int = 0,
    val duplicatesCount: Int = 0,
    val includeArchived: Boolean = false,
    val sortOrder: SortOrder = SortOrder.CHRONOLOGICAL_DESC,
    val virtualNames: Map<String, String> = emptyMap(),
    val virtualDescriptions: Map<String, String> = emptyMap(),
    val showStatsPopup: Boolean = false,
    val stats: StatsUiData = StatsUiData(),
    val collapsedCategories: Set<AlbumStatus> = setOf(
        AlbumStatus.IN_PROGRESS,
        AlbumStatus.NOT_STARTED,
        AlbumStatus.COMPLETED
    ),
    val savedAccounts: List<UserAccountEntity> = emptyList(),
    val isLoggingInToAnotherAccount: Boolean = false,
    val showBackupWarning: Boolean = false,
    val showGlobalResetConfirmation: Boolean = false
) {

    val filteredAlbums: List<Album>
        get() {
            val virtuals = mutableListOf<Album>()
            
            // 1. All Assets
            if (allAssetsCount > 0) {
                virtuals.add(Album(
                    id = Album.VIRTUAL_ALL_ID,
                    albumName = virtualNames[Album.VIRTUAL_ALL_ID] ?: "All Assets",
                    description = virtualDescriptions[Album.VIRTUAL_ALL_ID],
                    assetCount = allAssetsCount,
                    albumThumbnailAssetId = null
                ))
            }

            // 3. Orphans
            if (orphansCount > 0) {
                virtuals.add(Album(
                    id = Album.VIRTUAL_ORPHANS_ID,
                    albumName = virtualNames[Album.VIRTUAL_ORPHANS_ID] ?: "Orphans",
                    description = virtualDescriptions[Album.VIRTUAL_ORPHANS_ID],
                    assetCount = orphansCount,
                    albumThumbnailAssetId = null
                ))
            }

            // 4. Duplicates
            if (duplicatesCount > 0) {
                virtuals.add(Album(
                    id = Album.VIRTUAL_DUPLICATES_ID,
                    albumName = virtualNames[Album.VIRTUAL_DUPLICATES_ID] ?: "Duplicates",
                    description = virtualDescriptions[Album.VIRTUAL_DUPLICATES_ID],
                    assetCount = duplicatesCount,
                    albumThumbnailAssetId = null
                ))
            }

            val baseList = virtuals + albums

            return if (searchQuery.isBlank()) {
                baseList
            } else {
                baseList.filter { album ->
                    val nameToMatch = virtualNames[album.id] ?: album.albumName
                    val descToMatch = virtualDescriptions[album.id] ?: album.description ?: ""
                    nameToMatch.contains(searchQuery, ignoreCase = true) || 
                            descToMatch.contains(searchQuery, ignoreCase = true)
                }
            }
        }


    val groupedAlbums: Map<AlbumStatus, List<Album>>
        get() {
            val filtered = filteredAlbums
            return filtered.groupBy { album ->
                if (album.id == Album.VIRTUAL_ALL_ID || 
                    album.id == Album.VIRTUAL_ORPHANS_ID ||
                    album.id == Album.VIRTUAL_DUPLICATES_ID) {
                    return@groupBy AlbumStatus.VIRTUAL
                }
                
                val treated = albumTreatedCounts[album.id] ?: 0
                when {
                    treated == 0 -> AlbumStatus.NOT_STARTED
                    treated >= album.assetCount -> AlbumStatus.COMPLETED
                    else -> AlbumStatus.IN_PROGRESS
                }
            }
        }
}


data class StatsUiData(
    val totalDeleted: Int = 0,
    val totalBytesSaved: Long = 0,
    val totalKept: Int = 0,
    val totalArchived: Int = 0,
    val totalLocked: Int = 0,
    val totalAlbums: Int = 0,
    val completedAlbums: Int = 0,
    val weeklyDeleted: Int = 0,
    val weeklyBytesSaved: Long = 0
) {
    val totalSwiped: Int get() = totalDeleted + totalKept + totalArchived + totalLocked
    
    val distribution: Map<String, Float> get() {
        val total = totalSwiped.toFloat()
        if (total == 0f) return emptyMap()
        return mapOf(
            "KEEP" to totalKept / total,
            "DELETE" to totalDeleted / total,
            "ARCHIVE" to totalArchived / total,
            "LOCK" to totalLocked / total
        )
    }
}


enum class AlbumStatus(val label: String) {
    IN_PROGRESS("En cours"),
    NOT_STARTED("Pas commencé"),
    COMPLETED("Terminés"),
    VIRTUAL("Collections")
}
