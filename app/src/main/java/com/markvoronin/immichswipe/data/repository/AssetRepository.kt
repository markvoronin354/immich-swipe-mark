package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.data.api.AssetEditAction
import com.markvoronin.immichswipe.data.api.AssetEditActionItem
import com.markvoronin.immichswipe.data.api.DeleteAssetsRequest
import com.markvoronin.immichswipe.data.api.EditAssetRequest
import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.api.SearchAssetsRequest
import com.markvoronin.immichswipe.data.api.UpdateAssetsRequest
import com.markvoronin.immichswipe.data.local.dao.AlbumAssetDao
import com.markvoronin.immichswipe.data.local.entity.AlbumAssetEntity
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.domain.model.ExifInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

data class AssetBatch(
    val assets: List<Asset>,
    val total: Int,
    val isLocalCache: Boolean = false,
    val isSyncing: Boolean = false
)

@Singleton
class AssetRepository @Inject constructor(
    private val sessionManager: SessionManager,
    private val customApi: ImmichApi? = null,
    private val albumAssetDao: AlbumAssetDao? = null
) {
    private val api: ImmichApi
        get() = customApi ?: sessionManager.api ?: error("No active API session")
    fun getAssetsByAlbum(
        albumId: String,
        includeArchived: Boolean = false,
        userId: String? = null,
        sortOrder: SortOrder = SortOrder.CHRONOLOGICAL_DESC,
        shuffleSeed: Long? = null
    ): Flow<AssetBatch> = channelFlow {
        if (userId == null || albumAssetDao == null) return@channelFlow
        if (sessionManager.getUserId() != userId) {
            AppLogger.w("AssetRepo", "Session mismatch at start: requested $userId but active is ${sessionManager.getUserId()}")
            return@channelFlow
        }

        // 1. Emit local cache instantly (up to 100,000 assets from DB)
        val maxDbLimit = 100000
        val cachedEntities = when (sortOrder) {
            SortOrder.CHRONOLOGICAL_DESC -> albumAssetDao.getAssetsChronologicalDesc(albumId, userId, limit = maxDbLimit, offset = 0)
            SortOrder.CHRONOLOGICAL_ASC -> albumAssetDao.getAssetsChronologicalAsc(albumId, userId, limit = maxDbLimit, offset = 0)
            SortOrder.SIZE_DESC -> albumAssetDao.getAssetsSizeDesc(albumId, userId, limit = maxDbLimit, offset = 0)
            SortOrder.SIZE_ASC -> albumAssetDao.getAssetsSizeAsc(albumId, userId, limit = maxDbLimit, offset = 0)
            SortOrder.TYPE_VIDEO_FIRST -> albumAssetDao.getAssetsTypeVideoFirstDesc(albumId, userId, limit = maxDbLimit, offset = 0)
            SortOrder.TYPE_PHOTO_FIRST -> albumAssetDao.getAssetsTypePhotoFirstDesc(albumId, userId, limit = maxDbLimit, offset = 0)
            SortOrder.TYPE_VIDEO_FIRST_ASC -> albumAssetDao.getAssetsTypeVideoFirstAsc(albumId, userId, limit = maxDbLimit, offset = 0)
            SortOrder.TYPE_PHOTO_FIRST_ASC -> albumAssetDao.getAssetsTypePhotoFirstAsc(albumId, userId, limit = maxDbLimit, offset = 0)
            SortOrder.SHUFFLED -> albumAssetDao.getAssetsShuffled(albumId, userId, shuffleSeed ?: 1L, limit = maxDbLimit, offset = 0)
            SortOrder.TYPE_VIDEO_FIRST_SHUFFLED -> albumAssetDao.getAssetsTypeVideoFirstShuffled(albumId, userId, shuffleSeed ?: 1L, limit = maxDbLimit, offset = 0)
            SortOrder.TYPE_PHOTO_FIRST_SHUFFLED -> albumAssetDao.getAssetsTypePhotoFirstShuffled(albumId, userId, shuffleSeed ?: 1L, limit = maxDbLimit, offset = 0)
        }
        
        val mappedLocal = withContext(Dispatchers.Default) {
            cachedEntities.map { entity ->
                Asset(
                    id = entity.assetId,
                    ownerId = userId,
                    fileCreatedAt = entity.fileCreatedAt ?: "",
                    type = entity.type ?: "IMAGE",
                    originalFileName = entity.originalFileName,
                    fileExtension = entity.originalFileName?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() },
                    exifInfo = com.markvoronin.immichswipe.domain.model.ExifInfo(
                        fileSizeInBytes = entity.fileSizeInBytes,
                        imageWidth = entity.imageWidth,
                        imageHeight = entity.imageHeight
                    ),
                    rotation = entity.rotation
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
                // Flatten the clusters into a single list with assets sorted largest to smallest per cluster
                val flatAssets = duplicates.flatMap { cluster ->
                    cluster.assets.sortedWith(
                        compareByDescending<Asset> { it.exifInfo?.fileSizeInBytes ?: 0L }
                            .thenByDescending { (it.exifInfo?.imageWidth ?: 0) * (it.exifInfo?.imageHeight ?: 0) }
                    )
                }
                
                // For duplicates, we don't cache them in the DB yet, just return them directly
                send(AssetBatch(flatAssets, flatAssets.size, isLocalCache = false, isSyncing = false))
            } catch (e: Exception) {
                AppLogger.e("AssetRepo", "Error fetching duplicates: ${e.message}")
                send(AssetBatch(emptyList(), 0, isLocalCache = false, isSyncing = false))
            }
            return@channelFlow
        }

        val apiOrder = when (sortOrder) {
            SortOrder.CHRONOLOGICAL_ASC,
            SortOrder.TYPE_VIDEO_FIRST_ASC,
            SortOrder.TYPE_PHOTO_FIRST_ASC,
            SortOrder.SIZE_ASC -> "asc"
            else -> "desc"
        }

        val baseRequest = when (albumId) {
            Album.VIRTUAL_ALL_ID -> SearchAssetsRequest(visibility = visibility, withExif = needsExif, order = apiOrder)
            Album.VIRTUAL_ORPHANS_ID -> SearchAssetsRequest(isNotInAlbum = true, visibility = visibility, withExif = needsExif, order = apiOrder)
            else -> SearchAssetsRequest(albumIds = listOf(albumId), visibility = visibility, withExif = needsExif, order = apiOrder)
        }

        val statsRequest = when (albumId) {
            Album.VIRTUAL_ALL_ID -> SearchAssetsRequest(visibility = visibility)
            Album.VIRTUAL_ORPHANS_ID -> SearchAssetsRequest(isNotInAlbum = true, visibility = visibility)
            else -> SearchAssetsRequest(albumIds = listOf(albumId), visibility = visibility)
        }

        // 2. Fetch statistics to get the TRUE total (Search metadata total can be capped or inaccurate on some versions)
        val statsResp = try {
            api.getSearchStatistics(statsRequest)
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
            else send(AssetBatch(mappedLocal, mappedLocal.size, isLocalCache = true, isSyncing = false))
            return@channelFlow
        }

        // Use the higher of the two totals if they differ
        val effectiveTotal = maxOf(totalFromServer, firstPageResp.assets.total)
        
        // Use total DB count rather than capped cachedEntities.size
        val totalInDb = albumAssetDao.getAssetCountForAlbum(albumId, userId)
        val isCacheComplete = totalInDb >= (effectiveTotal - 10) && effectiveTotal > 0
        
        if (isCacheComplete) {
            // Check if Page 1 has any new items
            val fetchedIds = cachedEntities.map { it.assetId }.toSet()
            val newFromPage1 = firstPageResp.assets.items.filter { it.id !in fetchedIds }
            if (newFromPage1.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    albumAssetDao.insertAlbumAssets(newFromPage1.map { asset ->
                        AlbumAssetEntity(
                            albumId = albumId,
                            assetId = asset.id,
                            userId = userId,
                            type = asset.type,
                            fileCreatedAt = asset.effectiveDate,
                            originalFileName = asset.originalFileName,
                            fileSizeInBytes = asset.exifInfo?.fileSizeInBytes,
                            imageWidth = asset.exifInfo?.imageWidth,
                            imageHeight = asset.exifInfo?.imageHeight
                        )
                    })
                }
            }
            send(AssetBatch(mappedLocal, maxOf(effectiveTotal, mappedLocal.size), isLocalCache = true, isSyncing = false))
            return@channelFlow
        }

        // 4. Background Sync (only if cache is incomplete)
        val allFetchedAssets = mappedLocal.toMutableList()
        val fetchedIds = cachedEntities.map { it.assetId }.toMutableSet()
        
        var nextPage: String? = "1"
        while (nextPage != null) {
            if (sessionManager.getUserId() != userId) {
                AppLogger.w("AssetRepo", "Session switched from $userId to ${sessionManager.getUserId()}. Aborting sync.")
                return@channelFlow
            }
            try {
                val resp = if (nextPage == "1") firstPageResp else api.searchAssets(baseRequest.copy(page = nextPage.toIntOrNull() ?: 1, size = 1000))
                val items = resp.assets.items
                
                if (items.isNotEmpty()) {
                    val newItems = items.filter { !fetchedIds.contains(it.id) }
                    if (newItems.isNotEmpty()) {
                        fetchedIds.addAll(newItems.map { it.id })
                        allFetchedAssets.addAll(newItems)
                        
                        // Synchronous persistence to ensure Room DB has all items before final query
                        withContext(Dispatchers.IO) {
                            newItems.chunked(500).forEach { chunk ->
                                albumAssetDao.insertAlbumAssets(chunk.map { asset ->
                                    AlbumAssetEntity(
                                        albumId = albumId,
                                        assetId = asset.id,
                                        userId = userId,
                                        type = asset.type,
                                        fileCreatedAt = asset.effectiveDate,
                                        originalFileName = asset.originalFileName,
                                        fileSizeInBytes = asset.exifInfo?.fileSizeInBytes,
                                        imageWidth = asset.exifInfo?.imageWidth,
                                        imageHeight = asset.exifInfo?.imageHeight
                                    )
                                })
                            }
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
        
        // Re-read from DB to ensure sort order is applied to new items
        val finalEntities = when (sortOrder) {
            SortOrder.CHRONOLOGICAL_DESC -> albumAssetDao.getAssetsChronologicalDesc(albumId, userId, limit = 5000, offset = 0)
            SortOrder.CHRONOLOGICAL_ASC -> albumAssetDao.getAssetsChronologicalAsc(albumId, userId, limit = 5000, offset = 0)
            SortOrder.SIZE_DESC -> albumAssetDao.getAssetsSizeDesc(albumId, userId, limit = 5000, offset = 0)
            SortOrder.SIZE_ASC -> albumAssetDao.getAssetsSizeAsc(albumId, userId, limit = 5000, offset = 0)
            SortOrder.TYPE_VIDEO_FIRST -> albumAssetDao.getAssetsTypeVideoFirstDesc(albumId, userId, limit = 5000, offset = 0)
            SortOrder.TYPE_PHOTO_FIRST -> albumAssetDao.getAssetsTypePhotoFirstDesc(albumId, userId, limit = 5000, offset = 0)
            SortOrder.TYPE_VIDEO_FIRST_ASC -> albumAssetDao.getAssetsTypeVideoFirstAsc(albumId, userId, limit = 5000, offset = 0)
            SortOrder.TYPE_PHOTO_FIRST_ASC -> albumAssetDao.getAssetsTypePhotoFirstAsc(albumId, userId, limit = 5000, offset = 0)
            SortOrder.SHUFFLED -> albumAssetDao.getAssetsShuffled(albumId, userId, shuffleSeed ?: 1L, limit = 5000, offset = 0)
            SortOrder.TYPE_VIDEO_FIRST_SHUFFLED -> albumAssetDao.getAssetsTypeVideoFirstShuffled(albumId, userId, shuffleSeed ?: 1L, limit = 5000, offset = 0)
            SortOrder.TYPE_PHOTO_FIRST_SHUFFLED -> albumAssetDao.getAssetsTypePhotoFirstShuffled(albumId, userId, shuffleSeed ?: 1L, limit = 5000, offset = 0)
        }
        
        val mappedFinal = withContext(Dispatchers.Default) {
            finalEntities.map { entity ->
                Asset(
                    id = entity.assetId,
                    ownerId = userId,
                    fileCreatedAt = entity.fileCreatedAt ?: "",
                    type = entity.type ?: "IMAGE",
                    originalFileName = entity.originalFileName,
                    fileExtension = entity.originalFileName?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() },
                    exifInfo = ExifInfo(
                        fileSizeInBytes = entity.fileSizeInBytes,
                        imageWidth = entity.imageWidth,
                        imageHeight = entity.imageHeight
                    ),
                    rotation = entity.rotation
                )
            }
        }
        
        send(AssetBatch(mappedFinal, effectiveTotal, isLocalCache = false, isSyncing = false))
    }

    fun applySort(allAssets: List<Asset>, sortOrder: SortOrder, shuffleSeed: Long?): List<Asset> {
        val seed = shuffleSeed ?: 1L
        return when (sortOrder) {
            SortOrder.CHRONOLOGICAL_DESC -> allAssets.sortedByDescending { it.effectiveDate }
            SortOrder.CHRONOLOGICAL_ASC -> allAssets.sortedBy { it.effectiveDate }
            SortOrder.SIZE_DESC -> allAssets.sortedByDescending { it.exifInfo?.fileSizeInBytes ?: 0L }
            SortOrder.SIZE_ASC -> allAssets.sortedBy { it.exifInfo?.fileSizeInBytes ?: 0L }
            SortOrder.SHUFFLED -> allAssets.sortedBy { getShuffleHash(it.id, seed) }
            else -> applyTypeSort(allAssets, sortOrder, seed)
        }
    }

    private fun getShuffleHash(assetId: String, seed: Long): Long {
        var h = seed xor 0x5DEECE66DL
        for (i in assetId.indices) {
            h = (h * 31L) + assetId[i].code
        }
        h = h xor (h ushr 33)
        h = h * 0xff51afd7ed558ccdUL.toLong()
        h = h xor (h ushr 33)
        h = h * 0xc4ceb9fe1a85ec53UL.toLong()
        h = h xor (h ushr 33)
        return h
    }

    private fun applyTypeSort(allAssets: List<Asset>, sortOrder: SortOrder, seed: Long): List<Asset> {
        val isVideoFirst = sortOrder in listOf(
            SortOrder.TYPE_VIDEO_FIRST,
            SortOrder.TYPE_VIDEO_FIRST_ASC,
            SortOrder.TYPE_VIDEO_FIRST_SHUFFLED
        )
        val targetType = if (isVideoFirst) "VIDEO" else "IMAGE"
        val isAsc = sortOrder == SortOrder.TYPE_VIDEO_FIRST_ASC || sortOrder == SortOrder.TYPE_PHOTO_FIRST_ASC
        val isShuffled = sortOrder == SortOrder.TYPE_VIDEO_FIRST_SHUFFLED || sortOrder == SortOrder.TYPE_PHOTO_FIRST_SHUFFLED

        if (isShuffled) {
            val primary = allAssets.filter { it.type.equals(targetType, ignoreCase = true) }
                .sortedBy { getShuffleHash(it.id, seed) }
            val secondary = allAssets.filter { !it.type.equals(targetType, ignoreCase = true) }
                .sortedBy { getShuffleHash(it.id, seed) }
            return primary + secondary
        }

        return if (isAsc) {
            allAssets.sortedWith(
                compareBy<Asset> { if (it.type.equals(targetType, ignoreCase = true)) 0 else 1 }
                    .thenBy { it.effectiveDate }
            )
        } else {
            allAssets.sortedWith(
                compareBy<Asset> { if (it.type.equals(targetType, ignoreCase = true)) 0 else 1 }
                    .thenByDescending { it.effectiveDate }
            )
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
            AppLogger.i("AssetRepo", "Deleting ${assetIds.size} assets from Immich server...")
            try {
                api.deleteAssets(DeleteAssetsRequest(ids = assetIds, force = false))
                assetIds.chunked(500).forEach { chunk ->
                    albumAssetDao?.deleteAssets(chunk)
                }
                AppLogger.i("AssetRepo", "Successfully deleted ${assetIds.size} assets from Immich server")
            } catch (e: Exception) {
                AppLogger.e("AssetRepo", "Failed to delete ${assetIds.size} assets from Immich server: ${e.message}", e)
                throw e
            }
        }
    }

    suspend fun updateAssets(
        assetIds: List<String>,
        isFavorite: Boolean? = null,
        visibility: String? = null
    ) {
        if (assetIds.isNotEmpty()) {
            AppLogger.i("AssetRepo", "Updating ${assetIds.size} assets on Immich server (isFavorite=$isFavorite, visibility=$visibility)...")
            try {
                assetIds.chunked(500).forEach { chunk ->
                    api.updateAssets(
                        UpdateAssetsRequest(
                            ids = chunk,
                            isFavorite = isFavorite,
                            visibility = visibility
                        )
                    )
                }
                AppLogger.i("AssetRepo", "Successfully updated ${assetIds.size} assets on Immich server")
            } catch (e: Exception) {
                AppLogger.e("AssetRepo", "Failed to update ${assetIds.size} assets on Immich server: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Met à jour les édits d'un asset (rotation).
     */
    suspend fun updateAssetEdits(assetId: String, rotation: Int) {
        val normalizedRotation = ((rotation % 360) + 360) % 360
        val edit = AssetEditActionItem(
            action = AssetEditAction.rotate,
            parameters = mapOf("angle" to normalizedRotation)
        )
        api.editAsset(assetId, EditAssetRequest(edits = listOf(edit)))
    }

    suspend fun updateAssetRotation(assetId: String, userId: String, rotation: Int) {
        albumAssetDao?.updateRotation(assetId, userId, rotation)
    }

    suspend fun syncAssetRotationToServer(assetId: String, rotation: Int) {
        try {
            updateAssetEdits(assetId, rotation)
            AppLogger.i("AssetRepo", "Successfully synced asset rotation edits to Immich server for asset $assetId")
        } catch (e: Exception) {
            val errBody = (e as? HttpException)?.response()?.errorBody()?.string()
            AppLogger.e("AssetRepo", "Failed to sync rotation edits to Immich server for asset $assetId: ${e.message} body=$errBody", e)
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
