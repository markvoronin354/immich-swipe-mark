package com.markvoronin.immichswipe.feature.swipe

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.markvoronin.immichswipe.core.IconPosition

val MaterialGreen = Color(0xFF2E7D32)
val MaterialRed = Color(0xFFC62828)

/**
 * Helper pour trouver l'Activity à partir du Context.
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun IconPosition.toHorizontalAlignment(): Alignment.Horizontal = when (this) {
    IconPosition.TOP_LEFT, IconPosition.BOTTOM_LEFT -> Alignment.Start
    else -> Alignment.End
}
