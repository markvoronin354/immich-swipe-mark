package com.markvoronin.immichswipe.feature.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.markvoronin.immichswipe.R
import kotlin.math.roundToInt

@Composable
fun TutorialOverlay(
    controller: TutorialController,
    modifier: Modifier = Modifier
) {
    if (!controller.isVisible) return

    val currentStep = controller.currentStep() ?: return
    val totalSteps = controller.activeSteps.size
    val stepIndex = controller.currentStepIndex

    val targetBoundsInWindow = controller.getBoundsForTarget(currentStep.targetKey)
    var overlayWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val density = LocalDensity.current
    val minExpansionPx = with(density) { 2.dp.toPx() }
    val maxExpansionPx = with(density) { 5.dp.toPx() }
    val minScreenMargin = with(density) { 8.dp.toPx() }
    val strokeMargin = with(density) { 4.dp.toPx() }

    val pulseExpansionPx by infiniteTransition.animateFloat(
        initialValue = minExpansionPx,
        targetValue = maxExpansionPx,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseExpansion"
    )

    // Calculate local target bounds relative to the overlay Box position
    val localTargetRect = if (targetBoundsInWindow != null) {
        val padding = with(density) {
            if (currentStep.targetKey == "swipe_header") 4.dp.toPx() else 6.dp.toPx()
        }
        val rawLeft = targetBoundsInWindow.left - overlayWindowOffset.x - padding
        val rawRight = targetBoundsInWindow.right - overlayWindowOffset.x + padding

        Rect(
            left = rawLeft.coerceAtLeast(minScreenMargin),
            top = targetBoundsInWindow.top - overlayWindowOffset.y - padding,
            right = rawRight,
            bottom = targetBoundsInWindow.bottom - overlayWindowOffset.y + padding
        )
    } else {
        null
    }

    val animatedCutoutRect by animateRectAsState(
        targetValue = localTargetRect ?: Rect.Zero,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "CutoutAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(999f)
            .onGloballyPositioned { coords ->
                overlayWindowOffset = coords.positionInWindow()
                overlaySize = coords.size
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Consume clicks on overlay background
            }
    ) {
        // Scrim with spotlight cutout
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            if (localTargetRect != null && animatedCutoutRect.width > 0f) {
                val boundedCutoutRect = Rect(
                    left = animatedCutoutRect.left.coerceAtLeast(minScreenMargin),
                    top = animatedCutoutRect.top.coerceAtLeast(minScreenMargin),
                    right = animatedCutoutRect.right.coerceAtMost(canvasWidth - minScreenMargin),
                    bottom = animatedCutoutRect.bottom.coerceAtMost(canvasHeight - minScreenMargin)
                )

                val cutoutPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = boundedCutoutRect,
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    )
                }

                clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                    drawRect(Color.Black.copy(alpha = 0.75f))
                }

                // Glowing pulse frame tightly surrounding the cutout
                val strokeWidth = 3.dp.toPx()
                val frameRect = Rect(
                    left = (boundedCutoutRect.left - pulseExpansionPx).coerceAtLeast(strokeMargin),
                    top = (boundedCutoutRect.top - pulseExpansionPx).coerceAtLeast(strokeMargin),
                    right = (boundedCutoutRect.right + pulseExpansionPx).coerceAtMost(canvasWidth - strokeMargin),
                    bottom = (boundedCutoutRect.bottom + pulseExpansionPx).coerceAtMost(canvasHeight - strokeMargin)
                )

                drawRoundRect(
                    color = Color(0xFF38BDF8).copy(alpha = pulseAlpha),
                    topLeft = frameRect.topLeft,
                    size = frameRect.size,
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
            } else {
                drawRect(Color.Black.copy(alpha = 0.8f))
            }
        }

        // Floating Tooltip Card
        val isTargetAtBottom = currentStep.targetKey == "swipe_actions"

        val targetBottomPaddingDp = if (isTargetAtBottom && localTargetRect != null && overlaySize.height > 0) {
            val bottomGapPx = overlaySize.height - localTargetRect.top + with(density) { 12.dp.toPx() }
            with(density) { bottomGapPx.toDp() }.coerceAtLeast(24.dp)
        } else {
            24.dp
        }

        val animatedBottomPaddingDp by animateDpAsState(
            targetValue = targetBottomPaddingDp,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            label = "TooltipBottomPaddingAnimation"
        )

        val targetVerticalBias = when (currentStep.placement) {
            TooltipPlacement.TOP -> -1f
            TooltipPlacement.BOTTOM -> 1f
            TooltipPlacement.AUTO -> 1f
        }

        val animatedVerticalBias by animateFloatAsState(
            targetValue = targetVerticalBias,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            label = "TooltipVerticalBiasAnimation"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = animatedBottomPaddingDp, top = 4.dp)
        ) {
            AnimatedContent(
                targetState = currentStep to stepIndex,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                label = "TooltipContentTransition",
                modifier = Modifier
                    .align(BiasAlignment(0f, animatedVerticalBias))
                    .fillMaxWidth()
            ) { (step, index) ->
                TooltipCard(
                    step = step,
                    stepIndex = index,
                    totalSteps = totalSteps,
                    onNext = { controller.nextStep() },
                    onPrevious = { controller.previousStep() },
                    onSkip = { controller.skipTutorial() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TooltipCard(
    step: TutorialStep,
    stepIndex: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.animateContentSize(
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            // Header: Step Badge & Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = stringResource(R.string.tutorial_step_counter, stepIndex + 1, totalSteps),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                TextButton(
                    onClick = onSkip,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tutorial_skip),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Title
            Text(
                text = stringResource(step.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            // Description
            Text(
                text = stringResource(step.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Optional Gesture Visualizer
            if (step.gestureAnimation == GestureAnimationType.SWIPE_HORIZONTAL) {
                Spacer(Modifier.height(10.dp))
                AnimatedSwipeGestureVisualizer()
            }

            Spacer(Modifier.height(12.dp))

            // Footer Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stepIndex > 0) {
                    OutlinedButton(
                        onClick = onPrevious,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.tutorial_previous),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (stepIndex == totalSteps - 1) {
                            stringResource(R.string.tutorial_finish)
                        } else {
                            stringResource(R.string.tutorial_next)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (stepIndex < totalSteps - 1) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedSwipeGestureVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "SwipeHandTransition")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -40f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SwipeOffsetX"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.SwipeLeft,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.swipe_delete),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }

            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Swipe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .offset { IntOffset(offsetX.roundToInt(), 0) }
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.SwipeRight,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.swipe_keep),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
