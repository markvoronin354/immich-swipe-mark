package com.markvoronin.immichswipe.core.cache

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.markvoronin.immichswipe.core.AppLogger

class CacheCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            AppLogger.d("CacheCleanupWorker", "Running scheduled background cache maintenance")
            CacheManager.performMaintenance(applicationContext)
            Result.success()
        } catch (e: Exception) {
            AppLogger.e("CacheCleanupWorker", "Error during cache maintenance", e)
            Result.retry()
        }
    }
}
