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
