package com.markvoronin.immichswipe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.markvoronin.immichswipe.data.local.entity.UserAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts ORDER BY lastActive DESC")
    fun getAllAccounts(): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM user_accounts WHERE userId = :userId")
    suspend fun getAccountById(userId: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: UserAccountEntity)

    @Delete
    suspend fun deleteAccount(account: UserAccountEntity)

    @Query("DELETE FROM user_accounts")
    suspend fun deleteAllAccounts()

    @Query("UPDATE user_accounts SET lastActive = :timestamp WHERE userId = :userId")
    suspend fun updateLastActive(userId: String, timestamp: Long = System.currentTimeMillis())
}