package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.data.local.dao.AlbumDecisionCount
import com.markvoronin.immichswipe.data.local.dao.SwipeDecisionDao
import com.markvoronin.immichswipe.data.local.dao.UnsyncedDecisionCounts
import com.markvoronin.immichswipe.data.local.entity.SwipeDecisionEntity
import com.markvoronin.immichswipe.data.local.entity.SyncHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SwipeDecisionRepository @Inject constructor(
    private val swipeDecisionDao: SwipeDecisionDao
) {

    fun getAllAlbumDecisionCounts(userId: String): Flow<List<AlbumDecisionCount>> {
        return swipeDecisionDao.getAllAlbumDecisionCounts(userId)
    }

    fun getGlobalUniqueTreatedCount(userId: String): Flow<Int> {
        return swipeDecisionDao.getGlobalUniqueTreatedCount(userId)
    }

    fun getGlobalUnsyncedCount(userId: String): Flow<Int> {
        return swipeDecisionDao.getGlobalUnsyncedCount(userId)
    }

    fun getUnsyncedDecisionCounts(userId: String): Flow<UnsyncedDecisionCounts> {
        return swipeDecisionDao.getUnsyncedDecisionCounts(userId)
    }

    fun getAllDecisionsForUser(userId: String): Flow<List<SwipeDecisionEntity>> {
        return swipeDecisionDao.getAllDecisionsForUser(userId)
    }

    /**
     * Enregistre un nouveau swipe en base locale.
     */
    suspend fun saveDecision(assetId: String, albumId: String, userId: String, decision: String, fileSize: Long? = null, isSynced: Boolean = false) {
        val entity = SwipeDecisionEntity(
            assetId = assetId,
            albumId = albumId,
            userId = userId,
            decision = decision,
            fileSize = fileSize,
            createdAt = System.currentTimeMillis(),
            isSynced = isSynced,
            wasSyncedSkip = false
        )
        swipeDecisionDao.insertDecision(entity)
    }


    suspend fun saveDecisions(decisions: List<SwipeDecisionEntity>) {
        if (decisions.isNotEmpty()) {
            decisions.chunked(500).forEach { chunk ->
                swipeDecisionDao.insertDecisions(chunk)
            }
        }
    }


    suspend fun markAsSynced(assetIds: List<String>, userId: String) {
        if (assetIds.isNotEmpty()) {
            assetIds.chunked(500).forEach { chunk ->
                swipeDecisionDao.markAsSynced(chunk, userId)
            }
        }
    }


    fun getDecisionsForAlbum(albumId: String, userId: String): Flow<List<SwipeDecisionEntity>> {
        return swipeDecisionDao.getDecisionsForAlbum(albumId, userId)
    }


    suspend fun removeDecision(assetId: String, userId: String) {
        swipeDecisionDao.deleteDecision(assetId, userId)
    }


    suspend fun removeDecisions(assetIds: List<String>, userId: String) {
        if (assetIds.isNotEmpty()) {
            assetIds.chunked(500).forEach { chunk ->
                swipeDecisionDao.deleteDecisions(chunk, userId)
            }
        }
    }


    suspend fun deleteDecisionsForAlbum(albumId: String, userId: String) {
        swipeDecisionDao.deleteDecisionsForAlbum(albumId, userId)
    }


    suspend fun saveSyncHistory(
        userId: String,
        deletedCount: Int,
        bytesSaved: Long,
        keptCount: Int,
        archivedCount: Int,
        lockedCount: Int
    ) {
        val history = SyncHistoryEntity(
            userId = userId,
            deletedCount = deletedCount,
            bytesSaved = bytesSaved,
            keptCount = keptCount,
            archivedCount = archivedCount,
            lockedCount = lockedCount,
            skippedCount = 0
        )
        swipeDecisionDao.insertSyncHistory(history)
    }


    fun getSyncHistory(userId: String) = swipeDecisionDao.getSyncHistory(userId)


    suspend fun clearAllData() {
        swipeDecisionDao.deleteAllDecisions()
        swipeDecisionDao.deleteAllSyncHistory()
    }

    suspend fun clearUserData(userId: String) {
        swipeDecisionDao.deleteAllDecisionsForUser(userId)
        swipeDecisionDao.deleteAllSyncHistoryForUser(userId)
    }

    suspend fun getAllDecisionsRaw() = swipeDecisionDao.getAllDecisionsRaw()
    suspend fun getAllDecisionsForUserRaw(userId: String) = swipeDecisionDao.getAllDecisionsForUserRaw(userId)
    suspend fun getAllSyncHistoryRaw() = swipeDecisionDao.getAllSyncHistoryRaw()
    suspend fun getAllSyncHistoryForUserRaw(userId: String) = swipeDecisionDao.getAllSyncHistoryForUserRaw(userId)

    suspend fun importData(decisions: List<SwipeDecisionEntity>, history: List<SyncHistoryEntity>) {
        swipeDecisionDao.insertDecisions(decisions)
        swipeDecisionDao.insertSyncHistoryList(history)
    }
}
