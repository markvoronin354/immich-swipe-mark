package com.markvoronin.immichswipe.data.local.model

import com.markvoronin.immichswipe.data.local.entity.SwipeDecisionEntity
import com.markvoronin.immichswipe.data.local.entity.SyncHistoryEntity

data class DatabaseExport(
    val swipeDecisions: List<SwipeDecisionEntity>,
    val syncHistory: List<SyncHistoryEntity>,
    val exportDate: Long = System.currentTimeMillis(),
    val scope: String,
    val userId: String? = null
)
