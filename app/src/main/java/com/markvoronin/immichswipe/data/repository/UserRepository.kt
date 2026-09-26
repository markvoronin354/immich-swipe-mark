package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.data.api.ImmichApi

class UserRepository(
    private val api: ImmichApi
) {
    suspend fun getCurrentUser() = api.getCurrentUser()
}