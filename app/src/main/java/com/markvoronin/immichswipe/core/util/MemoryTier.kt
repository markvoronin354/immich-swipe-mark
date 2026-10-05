package com.markvoronin.immichswipe.core.util

import android.app.ActivityManager
import android.content.Context

enum class MemoryTier {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        /**
         * Evaluates device memory tier based on ActivityManager low RAM flag and total RAM size.
         */
        fun getMemoryTier(context: Context): MemoryTier {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (activityManager?.isLowRamDevice == true) {
                return LOW
            }

            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)

            val totalRamGb = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)

            return when {
                totalRamGb < 4.0 -> LOW
                totalRamGb < 8.0 -> MEDIUM
                else -> HIGH
            }
        }
    }
}
