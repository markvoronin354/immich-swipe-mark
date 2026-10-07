package com.markvoronin.immichswipe.feature.swipe.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.markvoronin.immichswipe.R
import java.util.Locale


@Composable
fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> stringResource(R.string.size_unit_gb, gb)
        mb >= 1.0 -> stringResource(R.string.size_unit_mb, mb)
        else -> stringResource(R.string.size_unit_kb, kb)
    }
}

fun formatMediaTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
