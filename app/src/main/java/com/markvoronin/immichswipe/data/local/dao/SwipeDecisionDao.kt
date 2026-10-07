package com.markvoronin.immichswipe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.markvoronin.immichswipe.data.local.entity.SwipeDecisionEntity
import com.markvoronin.immichswipe.data.local.entity.SyncHistoryEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface SwipeDecisionDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: SwipeDecisionEntity)


    @Query("""
        SELECT * FROM swipe_decisions
        WHERE userId = :userId AND albumId = :albumId
        UNION
        SELECT sd.* FROM swipe_decisions sd
        JOIN album_assets aa ON sd.assetId = aa.assetId AND aa.userId = :userId
        WHERE sd.userId = :userId AND aa.albumId = :albumId
    """)
    fun getDecisionsForAlbum(albumId: String, userId: String): Flow<List<SwipeDecisionEntity>>


    @Query("SELECT * FROM swipe_decisions WHERE assetId = :assetId AND userId = :userId")
    suspend fun getDecisionForAsset(assetId: String, userId: String): SwipeDecisionEntity?
    

    @Query("DELETE FROM swipe_decisions WHERE assetId = :assetId AND userId = :userId")
    suspend fun deleteDecision(assetId: String, userId: String)


    @Query("DELETE FROM swipe_decisions WHERE assetId IN (:assetIds) AND userId = :userId")
    suspend fun deleteDecisions(assetIds: List<String>, userId: String)


    @Query("DELETE FROM swipe_decisions WHERE assetId IN (:assetIds) AND userId = :userId")
    suspend fun deleteDecisionsForAllAlbums(assetIds: List<String>, userId: String)


    @Query("""
        DELETE FROM swipe_decisions 
        WHERE userId = :userId 
        AND assetId IN (
            SELECT assetId FROM swipe_decisions WHERE userId = :userId AND albumId = :albumId
            UNION
            SELECT assetId FROM album_assets WHERE userId = :userId AND albumId = :albumId
        )
    """)
    suspend fun deleteDecisionsForAlbum(albumId: String, userId: String)
    

    @Query("SELECT COUNT(*) FROM swipe_decisions WHERE albumId = :albumId AND userId = :userId")
    suspend fun getDecisionCountForAlbum(albumId: String, userId: String): Int


    @Query("UPDATE swipe_decisions SET isSynced = 1 WHERE assetId IN (:assetIds) AND userId = :userId")
    suspend fun markAsSynced(assetIds: List<String>, userId: String)


    @Query("UPDATE swipe_decisions SET userId = :userId WHERE userId = 'legacy_user'")
    suspend fun migrateLegacyData(userId: String)


    @Insert
    suspend fun insertSyncHistory(history: SyncHistoryEntity)


    @Query("SELECT * FROM sync_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getSyncHistory(userId: String): Flow<List<SyncHistoryEntity>>

    @Query("SELECT COUNT(DISTINCT assetId) FROM swipe_decisions WHERE userId = :userId")
    fun getGlobalUniqueTreatedCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(DISTINCT assetId) FROM swipe_decisions WHERE userId = :userId AND isSynced = 0")
    fun getGlobalUnsyncedCount(userId: String): Flow<Int>

    @Query("""
        SELECT 
            COUNT(CASE WHEN decision = 'KEEP' AND isSynced = 0 THEN 1 END) as keptCount,
            COUNT(CASE WHEN decision = 'ARCHIVE' AND isSynced = 0 THEN 1 END) as archivedCount
        FROM swipe_decisions 
        WHERE userId = :userId
    """)
    fun getUnsyncedDecisionCounts(userId: String): Flow<UnsyncedDecisionCounts>

    @Query("SELECT * FROM swipe_decisions WHERE userId = :userId")
    fun getAllDecisionsForUser(userId: String): Flow<List<SwipeDecisionEntity>>


    @Query("""
        WITH all_album_decisions AS (
            SELECT aa.albumId AS albumId, sd.assetId AS assetId, sd.isSynced AS isSynced
            FROM album_assets aa
            JOIN swipe_decisions sd ON aa.assetId = sd.assetId AND aa.userId = sd.userId
            WHERE sd.userId = :userId
            UNION
            SELECT sd.albumId AS albumId, sd.assetId AS assetId, sd.isSynced AS isSynced
            FROM swipe_decisions sd
            WHERE sd.userId = :userId
        )
        SELECT albumId,
               COUNT(DISTINCT assetId) AS totalCount,
               SUM(CASE WHEN isSynced = 0 THEN 1 ELSE 0 END) AS unsyncedCount
        FROM all_album_decisions
        GROUP BY albumId
    """)
    fun getAllAlbumDecisionCounts(userId: String): Flow<List<AlbumDecisionCount>>


    @Query("DELETE FROM swipe_decisions")
    suspend fun deleteAllDecisions()

    @Query("DELETE FROM swipe_decisions WHERE userId = :userId")
    suspend fun deleteAllDecisionsForUser(userId: String)

    @Query("DELETE FROM sync_history")
    suspend fun deleteAllSyncHistory()

    @Query("DELETE FROM sync_history WHERE userId = :userId")
    suspend fun deleteAllSyncHistoryForUser(userId: String)

    @Query("SELECT * FROM swipe_decisions")
    suspend fun getAllDecisionsRaw(): List<SwipeDecisionEntity>

    @Query("SELECT * FROM swipe_decisions WHERE userId = :userId")
    suspend fun getAllDecisionsForUserRaw(userId: String): List<SwipeDecisionEntity>

    @Query("SELECT * FROM sync_history")
    suspend fun getAllSyncHistoryRaw(): List<SyncHistoryEntity>

    @Query("SELECT * FROM sync_history WHERE userId = :userId")
    suspend fun getAllSyncHistoryForUserRaw(userId: String): List<SyncHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecisions(decisions: List<SwipeDecisionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncHistoryList(history: List<SyncHistoryEntity>)
}


data class AlbumDecisionCount(
    val albumId: String,
    val totalCount: Int,
    val unsyncedCount: Int
)


data class UnsyncedDecisionCounts(
    val keptCount: Int,
    val archivedCount: Int
)
