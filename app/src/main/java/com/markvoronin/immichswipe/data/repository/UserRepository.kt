package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.api.ImmichApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor() {
    private val api: ImmichApi
        get() = SessionManager.api ?: error("No active API session")

    suspend fun getCurrentUser() = api.getCurrentUser()
}
