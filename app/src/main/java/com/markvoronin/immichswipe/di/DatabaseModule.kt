package com.markvoronin.immichswipe.di

import android.content.Context
import com.markvoronin.immichswipe.data.local.AppDatabase
import com.markvoronin.immichswipe.data.local.dao.AlbumAssetDao
import com.markvoronin.immichswipe.data.local.dao.SwipeDecisionDao
import com.markvoronin.immichswipe.data.local.dao.UserAccountDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideSwipeDecisionDao(database: AppDatabase): SwipeDecisionDao {
        return database.swipeDecisionDao()
    }

    @Provides
    fun provideAlbumAssetDao(database: AppDatabase): AlbumAssetDao {
        return database.albumAssetDao()
    }

    @Provides
    fun provideUserAccountDao(database: AppDatabase): UserAccountDao {
        return database.userAccountDao()
    }
}
