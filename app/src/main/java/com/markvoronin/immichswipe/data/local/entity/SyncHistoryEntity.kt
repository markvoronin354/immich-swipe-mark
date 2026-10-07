package com.markvoronin.immichswipe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "sync_history")
data class SyncHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deletedCount: Int = 0,
    val bytesSaved: Long = 0,
    val keptCount: Int = 0,
    val archivedCount: Int = 0,
    val lockedCount: Int = 0,
    val skippedCount: Int = 0
)
