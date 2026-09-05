package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.data.api.DeleteAssetsRequest
import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.api.SearchAssetsRequest
import com.markvoronin.immichswipe.data.api.UpdateAssetsRequest
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.data.local.dao.AlbumAssetDao
import com.markvoronin.immichswipe.data.local.entity.AlbumAssetEntity
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.core.SortOrder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AssetBatch(
    val assets: List<Asset>,
    val total: Int,
    val isLocalCache: Boolean = false,
    val isSyncing: Boolean = false
)

class AssetRepository(
    private val context: android.content.Context,
    private val api: ImmichApi,
    private val albumAssetDao: AlbumAssetDao? = null
) {
    fun getAssetsByAlbum(
        albumId: String,
        includeArchived: Boolean = false,
        userId: String? = null,
        sortOrder: SortOrder = SortOrder.CHRONOLOGICAL_DESC,
        shuffleSeed: Long? = null
    ): Flow<AssetBatch> = channelFlow {
        if (userId == null || albumAssetDao == null) return@channelFlow

        // 1. Emit local cache instantly
        val cachedEntities = albumAssetDao.getAssetsForAlbum(albumId, userId)
        val mappedLocal = withContext(Dispatchers.Default) {
            cachedEntities.map { entity ->
                Asset(
                    id = entity.assetId,
                    ownerId = userId,
                    fileCreatedAt = entity.fileCreatedAt ?: "",
                    type = entity.type ?: "IMAGE",
                    originalFileName = entity.originalFileName,
                    exifInfo = com.markvoronin.immichswipe.domain.model.ExifInfo(
                        fileSizeInBytes = entity.fileSizeInBytes,
                        imageWidth = entity.imageWidth,
                        imageHeight = entity.imageHeight
                    )
                )
            }
        }
        
        if (mappedLocal.isNotEmpty()) {
            send(AssetBatch(mappedLocal, mappedLocal.size, isLocalCache = true, isSyncing = true))
        }

        val needsExif = true
        val visibility = if (includeArchived) null else "timeline"
        
        if (albumId == Album.VIRTUAL_DUPLICATES_ID) {
            // Handle duplicates endpoint differently as it returns clusters
            try {
                val duplicates = api.getDuplicates()
                // Flatten the clusters into a single list
                val flatAssets = duplicates.flatMap { it.assets }
                
                // For duplicates, we don't cache them in the DB yet, just return them directly
                send(AssetBatch(flatAssets, flatAssets.size, isLocalCache = false, isSyncing = false))
            } catch (e: Exception) {
                AppLogger.e("AssetRepo", "Error fetching duplicates: ${e.message}")
                send(AssetBatch(emptyList(), 0, isLocalCache = false, isSyncing = false))
            }
            return@channelFlow
        }

        val baseRequest = when (albumId) {
            Album.VIRTUAL_ALL_ID -> SearchAssetsRequest(visibility = visibility, withExif = needsExif, order = "desc")
            Album.VIRTUAL_ORPHANS_ID -> SearchAssetsRequest(isNotInAlbum = true, visibility = visibility, withExif = needsExif, order = "desc")
            else -> SearchAssetsRequest(albumIds = listOf(albumId), visibility = visibility, withExif = needsExif, order = "desc")
        }

        // 2. Fetch statistics to get the TRUE total (Search metadata total can be capped or inaccurate on some versions)
        val statsResp = try {
            api.getSearchStatistics(baseRequest)
        } catch (e: Exception) {
            AppLogger.e("AssetRepo", "Error fetching stats: ${e.message}")
            null
        }
        
        val totalFromServer = statsResp?.total ?: 0

        // 3. Fetch first page to check if we need a full sync
        val firstPageResp = try {
            api.searchAssets(baseRequest.copy(page = 1, size = 1000))
        } catch (e: Exception) {
            AppLogger.e("AssetRepo", "Error fetching first page: ${e.message}")
            if (mappedLocal.isEmpty()) send(AssetBatch(emptyList(), totalFromServer, isLocalCache = false, isSyncing = false))
            return@channelFlow
        }

        // Use the higher of the two totals if they differ
        val effectiveTotal = maxOf(totalFromServer, firstPageResp.assets.total)
        
        // If the cache is already complete, we can stop here or sync quietly
        val isCacheComplete = cachedEntities.size >= effectiveTotal && effectiveTotal > 0
        
        if (isCacheComplete) {
            // Check if Page 1 matches. if not, maybe some items are new.
            // For now, let's just finish and be fast.
            send(AssetBatch(mappedLocal, effectiveTotal, isLocalCache = true, isSyncing = false))
            return@channelFlow
        }

        // 4. Background Sync (only if cache is incomplete)
        val allFetchedAssets = mappedLocal.toMutableList()
        val fetchedIds = cachedEntities.map { it.assetId }.toMutableSet()
        
        var nextPage: String? = "1"
        while (nextPage != null) {
            try {
                val resp = if (nextPage == "1") firstPageResp else api.searchAssets(baseRequest.copy(page = nextPage.toIntOrNull() ?: 1, size = 1000))
                val items = resp.assets.items
                
                if (items.isNotEmpty()) {
                    val newItems = items.filter { !fetchedIds.contains(it.id) }
                    if (newItems.isNotEmpty()) {
                        fetchedIds.addAll(newItems.map { it.id })
                        allFetchedAssets.addAll(newItems)
                        
                        // Persistence in background
                        launch(Dispatchers.IO) {
                            albumAssetDao.insertAlbumAssets(newItems.map { asset ->
                                AlbumAssetEntity(
                                    albumId = albumId,
                                    assetId = asset.id,
                                    userId = userId,
                                    type = asset.type,
                                    fileCreatedAt = asset.fileCreatedAt,
                                    originalFileName = asset.originalFileName,
                                    fileSizeInBytes = asset.exifInfo?.fileSizeInBytes,
                                    imageWidth = asset.exifInfo?.imageWidth,
                                    imageHeight = asset.exifInfo?.imageHeight
                                )
                            })
                        }
                    }
                    
                    // Progressive emission
                    send(AssetBatch(allFetchedAssets.toList(), effectiveTotal, isLocalCache = false, isSyncing = resp.assets.nextPage != null))
                }
                nextPage = resp.assets.nextPage
                if (allFetchedAssets.size > 500000) break
            } catch (e: Exception) {
                AppLogger.e("AssetRepo", "Error page sync: ${e.message}")
                break
            }
        }

        // Final emission
        if (allFetchedAssets.size < effectiveTotal) {
            AppLogger.w("AssetRepo", "Sync finished but only fetched ${allFetchedAssets.size} / $effectiveTotal. Server might be hiding some items.")
        }
        send(AssetBatch(allFetchedAssets.toList(), effectiveTotal, isLocalCache = false, isSyncing = false))
    }

    fun applySort(allAssets: List<Asset>, sortOrder: SortOrder, shuffleSeed: Long?): List<Asset> {
        return when (sortOrder) {
            SortOrder.SHUFFLED -> {
                val random = if (shuffleSeed != null) java.util.Random(shuffleSeed) else java.util.Random()
                allAssets.shuffled(random)
            }
            SortOrder.SIZE_DESC -> allAssets.sortedByDescending { it.exifInfo?.fileSizeInBytes ?: 0L }
            SortOrder.SIZE_ASC -> allAssets.sortedBy { it.exifInfo?.fileSizeInBytes ?: 0L }
            SortOrder.TYPE_VIDEO_FIRST -> allAssets.sortedWith(compareByDescending<Asset> { it.type == "VIDEO" }.thenByDescending { it.fileCreatedAt })
            SortOrder.TYPE_PHOTO_FIRST -> allAssets.sortedWith(compareByDescending<Asset> { it.type == "IMAGE" }.thenByDescending { it.fileCreatedAt })
            SortOrder.TYPE_VIDEO_FIRST_ASC -> allAssets.sortedWith(compareByDescending<Asset> { it.type == "VIDEO" }.thenBy { it.fileCreatedAt })
            SortOrder.TYPE_PHOTO_FIRST_ASC -> allAssets.sortedWith(compareByDescending<Asset> { it.type == "IMAGE" }.thenBy { it.fileCreatedAt })
            SortOrder.TYPE_VIDEO_FIRST_SHUFFLED -> {
                val random = if (shuffleSeed != null) java.util.Random(shuffleSeed) else java.util.Random()
                val videos = allAssets.filter { it.type == "VIDEO" }.shuffled(random)
                val photos = allAssets.filter { it.type != "VIDEO" }.shuffled(random)
                videos + photos
            }
            SortOrder.TYPE_PHOTO_FIRST_SHUFFLED -> {
                val random = if (shuffleSeed != null) java.util.Random(shuffleSeed) else java.util.Random()
                val photos = allAssets.filter { it.type == "IMAGE" }.shuffled(random)
                val others = allAssets.filter { it.type != "IMAGE" }.shuffled(random)
                photos + others
            }
            SortOrder.CHRONOLOGICAL_DESC -> allAssets.sortedByDescending { it.fileCreatedAt }
            SortOrder.CHRONOLOGICAL_ASC -> allAssets.sortedBy { it.fileCreatedAt }
        }
    }

    suspend fun clearUserData(userId: String) {
        albumAssetDao?.deleteAllAlbumAssetsForUser(userId)
    }

    suspend fun getTotalAssetCount(includeArchived: Boolean = false): Int {
        return try {
            if (includeArchived) {
                coroutineScope {
                    val timeline = async { api.getSearchStatistics(SearchAssetsRequest(visibility = "timeline")).total }
                    val archive = async { api.getSearchStatistics(SearchAssetsRequest(visibility = "archive")).total }
                    timeline.await() + archive.await()
                }
            } else {
                api.getSearchStatistics(SearchAssetsRequest(visibility = "timeline")).total
            }
        } catch (_: Exception) {
            0
        }
    }

    suspend fun getOrphansCount(includeArchived: Boolean = false): Int {
        return try {
            if (includeArchived) {
                coroutineScope {
                    val timeline = async { api.getSearchStatistics(SearchAssetsRequest(isNotInAlbum = true, visibility = "timeline")).total }
                    val archive = async { api.getSearchStatistics(SearchAssetsRequest(isNotInAlbum = true, visibility = "archive")).total }
                    timeline.await() + archive.await()
                }
            } else {
                api.getSearchStatistics(SearchAssetsRequest(isNotInAlbum = true, visibility = "timeline")).total
            }
        } catch (_: Exception) {
            0
        }
    }

    suspend fun getAssetDetail(assetId: String): Asset {
        return api.getAssetDetail(assetId)
    }

    suspend fun deleteAssets(assetIds: List<String>) {
        if (assetIds.isNotEmpty()) {
            api.deleteAssets(DeleteAssetsRequest(ids = assetIds, force = false))
            albumAssetDao?.deleteAssets(assetIds)
        }
    }

    suspend fun updateAssets(
        assetIds: List<String>,
        isFavorite: Boolean? = null,
        visibility: String? = null
    ) {
        if (assetIds.isNotEmpty()) {
            api.updateAssets(
                UpdateAssetsRequest(
                    ids = assetIds,
                    isFavorite = isFavorite,
                    visibility = visibility
                )
            )
        }
    }
    suspend fun getDuplicatesCount(): Int {
        return try {
            val clusters = api.getDuplicates()
            clusters.sumOf { it.assets.size }
        } catch (_: Exception) {
            0
        }
    }

}
