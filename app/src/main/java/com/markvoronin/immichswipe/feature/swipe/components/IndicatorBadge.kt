package com.markvoronin.immichswipe.feature.swipe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IndicatorBadge(text: String, color: Color, align: Alignment, alpha: () -> Float) {
    Box(
        modifier = Modifier
            .padding(horizontal = 70.dp, vertical = 35.dp)
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha() },
        contentAlignment = align
    ) {
        Surface(
            color = color.copy(alpha = 0.05f),
            contentColor = color,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(2.dp, color.copy(alpha = 0.9f))
        ) {
            Text(
                text = text, fontSize = 32.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    .graphicsLayer { rotationZ = if (align == Alignment.TopStart) -15f else 15f }
            )
        }
    }
}
