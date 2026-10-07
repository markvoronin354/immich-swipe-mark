package com.markvoronin.immichswipe.core.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.util.MemoryTier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
object VideoPreloader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private const val MAX_CONCURRENT_PRELOADS = 1

    private fun getPreloadSize(context: Context): Long {
        return when (MemoryTier.getMemoryTier(context)) {
            MemoryTier.LOW -> 1 * 1024 * 1024L
            MemoryTier.MEDIUM -> 2 * 1024 * 1024L
            MemoryTier.HIGH -> 4 * 1024 * 1024L
        }
    }

    fun preload(context: Context, assetId: String, videoUrl: String, apiKey: String) {
        if (activeJobs.containsKey(assetId)) return

        // Cap concurrent preloads to MAX_CONCURRENT_PRELOADS to avoid I/O disk thrashing
        if (activeJobs.size >= MAX_CONCURRENT_PRELOADS) {
            val oldestKey = activeJobs.keys.firstOrNull()
            if (oldestKey != null) {
                cancel(oldestKey)
            }
        }

        val job = scope.launch {
            try {
                val preloadSize = getPreloadSize(context)
                AppLogger.d("VideoPreloader", "Starting preload for $assetId (size=${preloadSize / (1024 * 1024)}MB)")
                
                val dataSourceFactory = VideoCache.getCacheDataSourceFactory(context, apiKey)
                val dataSpec = DataSpec.Builder()
                    .setUri(videoUrl)
                    .setKey(assetId) // Match the cache key used in ExoPlayer
                    .setLength(preloadSize)
                    .build()

                val cacheWriter = CacheWriter(
                    dataSourceFactory.createDataSource(),
                    dataSpec,
                    ByteArray(128 * 1024), // Reuse 128KB buffer to avoid internal reallocations
                    null // Progress listener
                )

                cacheWriter.cache()
                AppLogger.d("VideoPreloader", "Finished preload for $assetId")
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                AppLogger.e("VideoPreloader", "Failed to preload $assetId: ${e.message}")
            } finally {
                activeJobs.remove(assetId)
            }
        }
        activeJobs[assetId] = job
    }

    fun cancel(assetId: String) {
        activeJobs[assetId]?.cancel()
        activeJobs.remove(assetId)
    }

}
