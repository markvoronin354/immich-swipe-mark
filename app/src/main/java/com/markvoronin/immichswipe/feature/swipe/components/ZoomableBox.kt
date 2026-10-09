package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    resetOnRelease: Boolean = false,
    enabled: Boolean = true,
    aspectRatio: Float? = null,
    isFillMode: Boolean = false,
    enableDoubleTapZoom: Boolean = true,
    onTap: ((Offset, IntSize) -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    onPress: (suspend PressGestureScope.(Offset, IntSize) -> Unit)? = null,
    onIsZoomedChanged: ((Boolean) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier, content = content)
        return
    }

    val scope = rememberCoroutineScope()
    var fillScale by remember(aspectRatio) { mutableFloatStateOf(1f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val animatedScale = remember { Animatable(1f) }
    val animatedOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    val currentScale = if (resetOnRelease) animatedScale.value else scale
    val isZoomedIn = currentScale > 1.05f
    val currentIsZoomedIn by rememberUpdatedState(isZoomedIn)

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnPress by rememberUpdatedState(onPress)

    LaunchedEffect(isZoomedIn) {
        onIsZoomedChanged?.invoke(isZoomedIn)
    }

    fun resetZoom() {
        if (resetOnRelease) {
            scope.launch {
                launch { animatedScale.animateTo(if (isFillMode) fillScale else 1f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
                launch { animatedOffset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
            }
        } else {
            scope.launch {
                launch { animate(scale, 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) { v, _ -> scale = v } }
                launch { animate(typeConverter = Offset.VectorConverter, initialValue = offset, targetValue = Offset.Zero, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) { v, _ -> offset = v } }
            }
        }
    }

    LaunchedEffect(isFillMode, fillScale) {
        val target = if (isFillMode) fillScale else 1f
        if (resetOnRelease) {
            launch { animatedScale.animateTo(target, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
            launch { animatedOffset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
        } else {
            scale = target
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { intSize ->
                boxSize = intSize
                if (aspectRatio != null && intSize.width > 0 && intSize.height > 0) {
                    val boxRatio = intSize.width.toFloat() / intSize.height.toFloat()
                    fillScale = if (aspectRatio > boxRatio) {
                        aspectRatio / boxRatio
                    } else {
                        boxRatio / aspectRatio
                    }
                }
            }
            .pointerInput(resetOnRelease, isFillMode, fillScale) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = false)
                        val pressedCount = event.changes.count { it.pressed }

                        if (pressedCount >= 2) {
                            // Zooming with 2 fingers
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                val oldScale = if (resetOnRelease) animatedScale.value else scale
                                val newScale = (oldScale * zoomChange).coerceIn(0.7f, 5f)
                                val effectiveZoomChange = if (oldScale > 0.0001f) newScale / oldScale else 1f

                                val oldOffset = if (resetOnRelease) animatedOffset.value else offset
                                val newOffset = (centroid - size.toSize().center) * (1f - effectiveZoomChange) + (oldOffset * effectiveZoomChange) + panChange

                                if (resetOnRelease) {
                                    scope.launch {
                                        animatedScale.snapTo(newScale)
                                        animatedOffset.snapTo(newOffset)
                                    }
                                } else {
                                    scale = newScale
                                    offset = newOffset
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } else if (pressedCount == 1 && (if (resetOnRelease) animatedScale.value else scale) > 1.05f) {
                            // Panning with 1 finger ONLY if zoomed in
                            if (panChange != Offset.Zero) {
                                if (resetOnRelease) {
                                    scope.launch {
                                        animatedOffset.snapTo(animatedOffset.value + panChange)
                                    }
                                } else {
                                    offset += panChange
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (resetOnRelease || scale < 1.01f) {
                        resetZoom()
                    }
                }
            }
            .pointerInput(enableDoubleTapZoom, onDoubleTap != null) {
                val shouldHandleDoubleTap = enableDoubleTapZoom || onDoubleTap != null
                detectTapGestures(
                    onTap = { tapOffset -> currentOnTap?.invoke(tapOffset, boxSize) },
                    onDoubleTap = if (shouldHandleDoubleTap) {
                        { tapOffset ->
                            if (currentIsZoomedIn) {
                                resetZoom()
                            } else if (currentOnDoubleTap != null) {
                                currentOnDoubleTap?.invoke()
                            } else if (!resetOnRelease) {
                                val targetScale = 3f
                                val zoomChange = targetScale / scale
                                val targetOffset = (tapOffset - boxSize.toSize().center) * (1f - zoomChange) + offset * zoomChange
                                scope.launch {
                                    launch { animate(scale, targetScale, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) { v, _ -> scale = v } }
                                    launch { animate(typeConverter = Offset.VectorConverter, initialValue = offset, targetValue = targetOffset, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) { v, _ -> offset = v } }
                                }
                            }
                        }
                    } else null,
                    onPress = { tapOffset ->
                        currentOnPress?.invoke(this, tapOffset, boxSize)
                    }
                )
            }
            .graphicsLayer {
                val s = if (resetOnRelease) animatedScale.value else scale
                val o = if (resetOnRelease) animatedOffset.value else offset
                scaleX = s
                scaleY = s
                translationX = o.x
                translationY = o.y
            },
        content = content
    )
}
