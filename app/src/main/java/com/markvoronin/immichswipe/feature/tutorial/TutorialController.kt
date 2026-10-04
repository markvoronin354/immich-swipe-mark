package com.markvoronin.immichswipe.feature.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

val LocalTutorialController = staticCompositionLocalOf<TutorialController?> { null }

class TutorialController {
    var isVisible by mutableStateOf(false)
        private set

    var currentStepIndex by mutableIntStateOf(0)
        private set

    var activeSteps by mutableStateOf<List<TutorialStep>>(emptyList())
        private set

    var phase by mutableStateOf<TutorialPhase?>(null)
        private set

    private var onCompleteCallback: (() -> Unit)? = null

    val targetBounds = mutableStateMapOf<String, Rect>()

    fun startTutorial(
        phase: TutorialPhase,
        steps: List<TutorialStep>,
        onComplete: (() -> Unit)? = null
    ) {
        if (steps.isEmpty()) return
        targetBounds.clear()
        this.phase = phase
        this.activeSteps = steps
        this.currentStepIndex = 0
        this.onCompleteCallback = onComplete
        this.isVisible = true
    }

    fun currentStep(): TutorialStep? {
        return activeSteps.getOrNull(currentStepIndex)
    }

    fun nextStep() {
        if (currentStepIndex < activeSteps.lastIndex) {
            currentStepIndex++
        } else {
            finishTutorial()
        }
    }

    fun previousStep() {
        if (currentStepIndex > 0) {
            currentStepIndex--
        }
    }

    fun skipTutorial() {
        finishTutorial()
    }

    private fun finishTutorial() {
        isVisible = false
        val callback = onCompleteCallback
        onCompleteCallback = null
        callback?.invoke()
    }

    fun registerTargetBounds(key: String, bounds: Rect) {
        targetBounds[key] = bounds
    }

    fun getBoundsForTarget(key: String?): Rect? {
        if (key == null) return null
        return targetBounds[key]
    }
}

@Composable
fun rememberTutorialController(): TutorialController {
    return remember { TutorialController() }
}

@Composable
fun ProvideTutorialController(
    controller: TutorialController,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTutorialController provides controller) {
        content()
    }
}

fun Modifier.tutorialTarget(
    key: String,
    controller: TutorialController? = null
): Modifier = this.then(
    Modifier.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        controller?.registerTargetBounds(key, bounds)
    }
)
