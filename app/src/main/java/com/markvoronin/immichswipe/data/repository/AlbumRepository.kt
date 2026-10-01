package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.api.AddAssetsToAlbumRequest
import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.api.SearchAssetsRequest
import com.markvoronin.immichswipe.domain.model.Album
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Repository gérant la récupération des albums depuis le serveur Immich.
 */
@Singleton
class AlbumRepository @Inject constructor(
    private val sessionManager: SessionManager,
    private val customApi: ImmichApi? = null
) {
    private val api: ImmichApi
        get() = customApi ?: sessionManager.api ?: error("No active API session")

    suspend fun getAlbumsRaw(): List<Album> {
        return api.getAlbums()
    }

    /**
     * Calcule le nombre d'assets archivés pour un album donné.
     */
    suspend fun getAlbumArchiveCount(albumId: String): Int {
        return try {
            api.getSearchStatistics(
                SearchAssetsRequest(albumIds = listOf(albumId), visibility = "archive")
            ).total
        } catch (_: Exception) {
            0
        }
    }

    suspend fun refineAlbumCounts(albums: List<Album>): List<Album> {
        val semaphore = Semaphore(15) // Augmenté un peu pour la performance
        return coroutineScope {
            albums.map { album ->
                async {
                    semaphore.withPermit {
                        val archiveCount = getAlbumArchiveCount(album.id)
                        album.copy(assetCount = (album.assetCount - archiveCount).coerceAtLeast(0))
                    }
                }
            }.awaitAll()
        }
    }

    /**
     * Rafraîchit la liste des albums depuis le serveur.
     * @param includeArchived Si vrai, inclut les photos archivées dans le compte total.
     *                        Si faux, soustrait les archives du compte total via search/statistics.
     */
    suspend fun refreshAlbums(includeArchived: Boolean = false): List<Album> {
        val albums = getAlbumsRaw()
        if (includeArchived) return albums
        return refineAlbumCounts(albums)
    }

    /**
     * Ajoute un asset à un album spécifié.
     */
    suspend fun addAssetToAlbum(albumId: String, assetId: String): Boolean {
        return try {
            val response = api.addAssetsToAlbum(albumId,
                AddAssetsToAlbumRequest(ids = listOf(assetId))
            )
            response.isSuccessful
        } catch (e: Exception) {
            AppLogger.e("AlbumRepository", "Erreur ajout asset à l'album $albumId", e)
            false
        }
    }
}
