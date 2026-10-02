package com.markvoronin.immichswipe

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.markvoronin.immichswipe.core.cache.CacheManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ImmichSwipeApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        CacheManager.schedulePeriodicMaintenance(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
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
