package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SuccessAnimationOverlay() {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(1f, tween(400))
        }
        delay(200.milliseconds)
        iconScale.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
        )
        iconScale.animateTo(1f, spring())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .padding(24.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer {
                                scaleX = iconScale.value * 1.1f
                                scaleY = iconScale.value * 1.1f
                            }
                            .background(MaterialGreen.copy(alpha = 0.15f), CircleShape)
                    )

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialGreen,
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer {
                                scaleX = iconScale.value
                                scaleY = iconScale.value
                            }
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.swipe_sync_success),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.swipe_sync_success_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
