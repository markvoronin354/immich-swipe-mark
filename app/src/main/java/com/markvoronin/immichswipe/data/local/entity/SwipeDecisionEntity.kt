package com.markvoronin.immichswipe.data.local.entity

import androidx.room.Entity
import androidx.room.Index


@Entity(
    tableName = "swipe_decisions",
    primaryKeys = ["assetId", "userId"],
    indices = [Index(value = ["userId", "albumId"])]
)
data class SwipeDecisionEntity(
    val assetId: String,
    val albumId: String,
    val userId: String,
    val decision: String,
    val fileSize: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val wasSyncedSkip: Boolean = false
)
