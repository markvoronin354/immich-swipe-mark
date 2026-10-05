package com.markvoronin.immichswipe

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.markvoronin.immichswipe.core.cache.CacheManager
import com.markvoronin.immichswipe.core.util.MemoryTier
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ImmichSwipeApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        CacheManager.schedulePeriodicMaintenance(this)
    }

    override fun newImageLoader(): ImageLoader {
        val memoryCachePercent = when (MemoryTier.getMemoryTier(this)) {
            MemoryTier.LOW -> 0.15
            MemoryTier.MEDIUM -> 0.25
            MemoryTier.HIGH -> 0.35
        }

        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(memoryCachePercent)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250 * 1024 * 1024L) // 250MB cap
                    .build()
            }
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }
}
