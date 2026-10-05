package com.markvoronin.immichswipe.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.util.MemoryTier

/**
 * Factory for creating ExoPlayer [LoadControl] instances optimized according to the device [MemoryTier].
 */
@OptIn(UnstableApi::class)
object PlayerLoadControlFactory {

    private const val TAG = "PlayerLoadControl"

    /**
     * Creates a LoadControl tailored for the primary Swipe screen video player.
     */
    fun createSwipeLoadControl(context: Context): LoadControl {
        val tier = MemoryTier.getMemoryTier(context)
        val builder = DefaultLoadControl.Builder()
            .setPrioritizeTimeOverSizeThresholds(true)

        val (minBuffer, maxBuffer, backBuffer) = when (tier) {
            MemoryTier.LOW -> {
                Triple(10_000, 20_000, 15_000)
            }
            MemoryTier.MEDIUM -> {
                Triple(20_000, 60_000, 60_000)
            }
            MemoryTier.HIGH -> {
                Triple(30_000, 120_000, 120_000)
            }
        }

        builder.setBufferDurationsMs(
            minBuffer,
            maxBuffer,
            1_000, // bufferForPlaybackMs
            1_000  // bufferForPlaybackAfterRebufferMs
        )
        if (backBuffer > 0) {
            builder.setBackBuffer(backBuffer, true)
        }

        AppLogger.d(TAG, "Created Swipe LoadControl for tier=$tier (min=${minBuffer}ms, max=${maxBuffer}ms, back=${backBuffer}ms)")
        return builder.build()
    }

    /**
     * Creates a LoadControl tailored for compact/comparison video players (e.g. Duplicates screen).
     */
    fun createDuplicateLoadControl(context: Context): LoadControl {
        val tier = MemoryTier.getMemoryTier(context)
        val builder = DefaultLoadControl.Builder()
            .setPrioritizeTimeOverSizeThresholds(true)

        val (minBuffer, maxBuffer, backBuffer) = when (tier) {
            MemoryTier.LOW -> {
                Triple(5_000, 15_000, 0)
            }
            MemoryTier.MEDIUM -> {
                Triple(10_000, 30_000, 15_000)
            }
            MemoryTier.HIGH -> {
                Triple(15_000, 50_000, 30_000)
            }
        }

        builder.setBufferDurationsMs(
            minBuffer,
            maxBuffer,
            500,  // bufferForPlaybackMs
            1_000 // bufferForPlaybackAfterRebufferMs
        )
        if (backBuffer > 0) {
            builder.setBackBuffer(backBuffer, true)
        }

        AppLogger.d(TAG, "Created Duplicate LoadControl for tier=$tier (min=${minBuffer}ms, max=${maxBuffer}ms, back=${backBuffer}ms)")
        return builder.build()
    }
}
