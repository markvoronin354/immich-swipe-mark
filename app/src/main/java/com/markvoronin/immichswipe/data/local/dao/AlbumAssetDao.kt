package com.markvoronin.immichswipe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.markvoronin.immichswipe.data.local.entity.AlbumAssetEntity

@Dao
interface AlbumAssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumAssets(relations: List<AlbumAssetEntity>)

    @Query("DELETE FROM album_assets WHERE albumId = :albumId AND userId = :userId")
    suspend fun clearAlbumRelations(albumId: String, userId: String)

    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY fileCreatedAt DESC")
    suspend fun getAssetsForAlbum(albumId: String, userId: String): List<AlbumAssetEntity>

    // Sorted Queries for Pagination/Windowing
    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY fileCreatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAssetsChronologicalDesc(albumId: String, userId: String, limit: Int, offset: Int): List<AlbumAssetEntity>

    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY fileCreatedAt ASC LIMIT :limit OFFSET :offset")
    suspend fun getAssetsChronologicalAsc(albumId: String, userId: String, limit: Int, offset: Int): List<AlbumAssetEntity>

    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY fileSizeInBytes DESC LIMIT :limit OFFSET :offset")
    suspend fun getAssetsSizeDesc(albumId: String, userId: String, limit: Int, offset: Int): List<AlbumAssetEntity>

    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY fileSizeInBytes ASC LIMIT :limit OFFSET :offset")
    suspend fun getAssetsSizeAsc(albumId: String, userId: String, limit: Int, offset: Int): List<AlbumAssetEntity>

    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY CASE WHEN type = 'VIDEO' THEN 0 ELSE 1 END ASC, fileCreatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAssetsTypeVideoFirstDesc(albumId: String, userId: String, limit: Int, offset: Int): List<AlbumAssetEntity>

    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY CASE WHEN type = 'IMAGE' THEN 0 ELSE 1 END ASC, fileCreatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getAssetsTypePhotoFirstDesc(albumId: String, userId: String, limit: Int, offset: Int): List<AlbumAssetEntity>

    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY CASE WHEN type = 'VIDEO' THEN 0 ELSE 1 END ASC, fileCreatedAt ASC LIMIT :limit OFFSET :offset")
    suspend fun getAssetsTypeVideoFirstAsc(albumId: String, userId: String, limit: Int, offset: Int): List<AlbumAssetEntity>

    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY CASE WHEN type = 'IMAGE' THEN 0 ELSE 1 END ASC, fileCreatedAt ASC LIMIT :limit OFFSET :offset")
    suspend fun getAssetsTypePhotoFirstAsc(albumId: String, userId: String, limit: Int, offset: Int): List<AlbumAssetEntity>

    // Pseudo-random shuffling using SQLite built-in hash on assetId string + seed
    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY (length(assetId) * :seed) % 1000 ASC LIMIT :limit OFFSET :offset")
    suspend fun getAssetsShuffled(albumId: String, userId: String, seed: Long, limit: Int, offset: Int): List<AlbumAssetEntity>

    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY CASE WHEN type = 'VIDEO' THEN 0 ELSE 1 END ASC, (length(assetId) * :seed) % 1000 ASC LIMIT :limit OFFSET :offset")
    suspend fun getAssetsTypeVideoFirstShuffled(albumId: String, userId: String, seed: Long, limit: Int, offset: Int): List<AlbumAssetEntity>

    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY CASE WHEN type = 'IMAGE' THEN 0 ELSE 1 END ASC, (length(assetId) * :seed) % 1000 ASC LIMIT :limit OFFSET :offset")
    suspend fun getAssetsTypePhotoFirstShuffled(albumId: String, userId: String, seed: Long, limit: Int, offset: Int): List<AlbumAssetEntity>

    @Query("SELECT * FROM album_assets WHERE albumId = :albumId AND userId = :userId ORDER BY fileCreatedAt DESC")
    fun getAssetsForAlbumFlow(albumId: String, userId: String): kotlinx.coroutines.flow.Flow<List<AlbumAssetEntity>>

    @Query("DELETE FROM album_assets WHERE albumId = :albumId AND userId = :userId AND assetId IN (:assetIds)")
    suspend fun deleteAlbumAssets(albumId: String, userId: String, assetIds: List<String>)

    @Query("DELETE FROM album_assets WHERE assetId IN (:assetIds)")
    suspend fun deleteAssets(assetIds: List<String>)

    @Query("SELECT COUNT(*) FROM album_assets WHERE albumId = :albumId AND userId = :userId")
    suspend fun getAssetCountForAlbum(albumId: String, userId: String): Int

    @Query("DELETE FROM album_assets WHERE userId = :userId")
    suspend fun deleteAllAlbumAssetsForUser(userId: String)
}
