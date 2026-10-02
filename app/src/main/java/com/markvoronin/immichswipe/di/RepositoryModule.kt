package com.markvoronin.immichswipe.di

import android.content.Context
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.local.dao.AlbumAssetDao
import com.markvoronin.immichswipe.data.local.dao.SwipeDecisionDao
import com.markvoronin.immichswipe.data.local.dao.UserAccountDao
import com.markvoronin.immichswipe.data.repository.AccountRepository
import com.markvoronin.immichswipe.data.repository.AlbumRepository
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.AuthRepository
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSessionRepository(
        @ApplicationContext context: Context,
        accountRepository: AccountRepository
    ): SessionRepository {
        return SessionRepository(context, accountRepository)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository {
        return AuthRepository()
    }

    @Provides
    @Singleton
    fun provideAccountRepository(userAccountDao: UserAccountDao): AccountRepository {
        return AccountRepository(userAccountDao)
    }

    @Provides
    @Singleton
    fun provideSwipeDecisionRepository(swipeDecisionDao: SwipeDecisionDao): SwipeDecisionRepository {
        return SwipeDecisionRepository(swipeDecisionDao)
    }

    @Provides
    @Singleton
    fun provideAlbumRepository(sessionManager: SessionManager): AlbumRepository {
        return AlbumRepository(sessionManager = sessionManager)
    }

    @Provides
    @Singleton
    fun provideUserRepository(sessionManager: SessionManager): UserRepository {
        return UserRepository(sessionManager = sessionManager)
    }

    @Provides
    @Singleton
    fun provideAssetRepository(
        @ApplicationContext context: Context,
        sessionManager: SessionManager,
        albumAssetDao: AlbumAssetDao
    ): AssetRepository {
        return AssetRepository(context = context, sessionManager = sessionManager, albumAssetDao = albumAssetDao)
    }
}
