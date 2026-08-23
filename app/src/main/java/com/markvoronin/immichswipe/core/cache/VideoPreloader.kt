package com.markvoronin.immichswipe.core.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.markvoronin.immichswipe.core.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Utility to pre-cache the first segments of video files to ensure instant playback.
 */
@OptIn(UnstableApi::class)
object VideoPreloader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = mutableMapOf<String, Job>()
    private const val PRELOAD_SIZE = 2 * 1024 * 1024L // Preload first 2MB

    /**
     * Starts pre-caching the first segment of a video.
     */
    fun preload(context: Context, assetId: String, videoUrl: String, apiKey: String) {
        if (activeJobs.containsKey(assetId)) return

        val job = scope.launch {
            try {
                AppLogger.d("VideoPreloader", "Starting preload for $assetId")
                
                val dataSourceFactory = VideoCache.getCacheDataSourceFactory(context, apiKey)
                val dataSpec = DataSpec.Builder()
                    .setUri(videoUrl)
                    .setKey(assetId) // Match the cache key used in ExoPlayer
                    .setLength(PRELOAD_SIZE)
                    .build()

                val cacheWriter = CacheWriter(
                    dataSourceFactory.createDataSource(),
                    dataSpec,
                    null, // temporaryBuffer
                    null // Progress listener
                )

                cacheWriter.cache()
                AppLogger.d("VideoPreloader", "Finished preload for $assetId")
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    AppLogger.e("VideoPreloader", "Failed to preload $assetId: ${e.message}")
                }
            } finally {
                activeJobs.remove(assetId)
            }
        }
        activeJobs[assetId] = job
    }

    /**
     * Cancels any active preload for the given asset.
     */
    fun cancel(assetId: String) {
        activeJobs[assetId]?.cancel()
        activeJobs.remove(assetId)
    }

    /**
     * Cancels all active preloads.
     */
    fun cancelAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }
}
