package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.data.api.RetrofitFactory
import com.markvoronin.immichswipe.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthRepository @Inject constructor() {


    suspend fun checkCredentials(baseUrl: String, apiKey: String): User {
        val config = SessionConfig(baseUrl = baseUrl, apiKey = apiKey)
        
        val tempApi = RetrofitFactory.create(config)
        
        return tempApi.getCurrentUser()
    }
}
