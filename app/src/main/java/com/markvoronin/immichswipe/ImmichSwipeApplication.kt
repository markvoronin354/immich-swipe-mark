package com.markvoronin.immichswipe

import android.app.Application
import com.markvoronin.immichswipe.core.cache.CacheManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ImmichSwipeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CacheManager.schedulePeriodicMaintenance(this)
    }
}
