package com.markvoronin.immichswipe.data.api

import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.domain.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ImmichApi {

    @GET("api/users/me")
    suspend fun getCurrentUser(): User

    @GET("api/albums")
    suspend fun getAlbums(): List<Album>

    @PUT("api/albums/{id}/assets")
    suspend fun addAssetsToAlbum(
        @Path("id") albumId: String,
        @Body request: AddAssetsToAlbumRequest
    ): Response<Unit>

    @GET("api/duplicates")
    suspend fun getDuplicates(): List<DuplicateCluster>


    @POST("api/search/metadata")
    suspend fun searchAssets(@Body request: SearchAssetsRequest): SearchResponse

    @POST("api/search/statistics")
    suspend fun getSearchStatistics(@Body request: SearchAssetsRequest): SearchStatisticsResponse

    @GET("api/assets/{id}")
    suspend fun getAssetDetail(@Path("id") assetId: String): Asset


    @HTTP(method = "DELETE", path = "api/assets", hasBody = true)
    suspend fun deleteAssets(@Body request: DeleteAssetsRequest)

    @PUT("api/assets")
    suspend fun updateAssets(@Body request: UpdateAssetsRequest)

    @PUT("api/assets/{id}/edits")
    suspend fun editAsset(
        @Path("id") assetId: String,
        @Body request: EditAssetRequest
    )

    @POST("api/assets/{id}/rotate")
    suspend fun rotateAsset(
        @Path("id") assetId: String,
        @Body request: RotateAssetRequest
    )
}


data class EditAssetRequest(
    val edits: List<AssetEditActionItem>
)

data class AssetEditActionItem(
    val action: AssetEditAction,
    val parameters: Map<String, Any>
)

enum class AssetEditAction {
    crop, rotate,
}


data class RotateAssetRequest(
    val direction: String = "cw"
)


data class UpdateAssetsRequest(
    val ids: List<String>,
    val isFavorite: Boolean? = null,
    val visibility: String? = null, // archive, timeline, hidden, locked
    val rotation: Int? = null
)


data class DeleteAssetsRequest(
    val ids: List<String>,
    val force: Boolean = false
)


data class SearchAssetsRequest(
    val albumIds: List<String>? = null,
    val ids: List<String>? = null,
    val isNotInAlbum: Boolean? = null,
    val size: Int = 500,
    val page: Int = 1,
    val visibility: String? = null, // archive, timeline, hidden, locked
    val type: String? = null, // IMAGE, VIDEO
    val withExif: Boolean = true,
    val isFavorite: Boolean? = null,
    val order: String? = null // asc, desc, random
)

data class SearchResponse(
    val assets: SearchAssetResult
)

data class SearchStatisticsResponse(
    val total: Int
)


data class SearchAssetResult(
    val items: List<Asset>,
    val total: Int,
    val nextPage: String? = null
)


data class DuplicateCluster(
    val assets: List<Asset>
)


data class AddAssetsToAlbumRequest(
    val ids: List<String>
)
