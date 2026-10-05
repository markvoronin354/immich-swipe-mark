package com.markvoronin.immichswipe.core.cache

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.markvoronin.immichswipe.core.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Gère la maintenance du cache de l'application.
 */
object CacheManager {
    private const val TAG = "CacheManager"
    private const val WORK_NAME = "CacheMaintenanceWork"
    private const val MAX_CACHE_SIZE = 1024 * 1024 * 1024L // 1 GB
    private const val SHARED_ASSETS_DIR = "shared_assets"
    private const val EXPIRATION_TIME_MS = 24 * 60 * 60 * 1000L // 24 Hours

    /**
     * Planifie la maintenance périodique du cache via WorkManager (1 fois par jour quand la batterie n'est pas faible).
     */
    fun schedulePeriodicMaintenance(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val maintenanceWork = PeriodicWorkRequestBuilder<CacheCleanupWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            maintenanceWork
        )
    }

    /**
     * Effectue une maintenance complète du cache.
     * Call on app startup.
     */
    suspend fun performMaintenance(context: Context) = withContext(Dispatchers.IO) {
        try {
            AppLogger.d(TAG, "Starting cache maintenance")
            
            // 1. Clean expired shared assets
            cleanSharedAssets(context)
            
            // 2. Check global size and clean if necessary
            checkAndLimitGlobalCache(context)
            
            AppLogger.d(TAG, "Cache maintenance finished")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during cache maintenance", e)
        }
    }

    /**
     * Supprime les fichiers temporaires de partage datant de plus de 24h.
     */
    private fun cleanSharedAssets(context: Context) {
        val sharedDir = File(context.cacheDir, SHARED_ASSETS_DIR)
        if (!sharedDir.exists()) return

        val now = System.currentTimeMillis()
        var deletedCount = 0
        var deletedSize = 0L

        sharedDir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > EXPIRATION_TIME_MS) {
                val size = file.length()
                if (file.delete()) {
                    deletedCount++
                    deletedSize += size
                }
            }
        }

        if (deletedCount > 0) {
            AppLogger.i(TAG, "Shared assets cleanup: $deletedCount files deleted (${deletedSize / 1024} KB freed)")
        }
    }

    /**
     * Vérifie si le cache dépasse la limite autorisée et supprime les fichiers les plus vieux.
     * Exclut video_cache et image_cache car ils gèrent leur propre éviction LRU.
     */
    private fun checkAndLimitGlobalCache(context: Context) {
        val cacheDir = context.cacheDir
        val totalSize = getFolderSize(cacheDir)

        if (totalSize > MAX_CACHE_SIZE) {
            AppLogger.i(TAG, "Cache too large (${totalSize / (1024 * 1024)} MB). Launching aggressive cleanup...")
            
            // On récupère tous les fichiers (récursif) et on les trie par date
            val allFiles = getAllFiles(cacheDir).sortedBy { it.lastModified() }
            
            var currentSize = totalSize
            var deletedCount = 0
            
            // On supprime les fichiers les plus anciens jusqu'à redescendre à 70% du max
            val targetSize = (MAX_CACHE_SIZE * 0.7).toLong()
            
            for (file in allFiles) {
                if (currentSize <= targetSize) break
                
                // On ne touche pas aux fichiers critiques (ex: logs de la session actuelle)
                if (file.name == "current_logs.txt") continue
                
                val fileSize = file.length()
                if (file.delete()) {
                    currentSize -= fileSize
                    deletedCount++
                }
            }
            AppLogger.i(TAG, "Aggressive cleanup finished: $deletedCount files deleted. New size: ${currentSize / (1024 * 1024)} MB")
        }
    }

    private fun getFolderSize(file: File): Long {
        var size = 0L
        if (file.isDirectory) {
            if (file.name == "video_cache" || file.name == "image_cache") return 0L
            file.listFiles()?.forEach { size += getFolderSize(it) }
        } else {
            size = file.length()
        }
        return size
    }

    private fun getAllFiles(file: File): List<File> {
        val result = mutableListOf<File>()
        if (file.isDirectory) {
            if (file.name == "video_cache" || file.name == "image_cache") return emptyList()
            file.listFiles()?.forEach { result.addAll(getAllFiles(it)) }
        } else {
            result.add(file)
        }
        return result
    }

    /**
     * Vide intégralement le cache (déclenché manuellement par l'utilisateur).
     */
    fun clearAllCache(context: Context) {
        // Safe clear for video cache
        VideoCache.releaseAndClear(context)

        val cacheDir = context.cacheDir
        cacheDir.listFiles()?.forEach { file ->
            if (file.name != "video_cache") {
                deleteRecursive(file)
            }
        }
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { deleteRecursive(it) }
        }
        fileOrDirectory.delete()
    }
}
