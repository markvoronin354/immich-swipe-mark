package com.markvoronin.immichswipe

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.markvoronin.immichswipe",
        profileBlock = {
            // Start the application (Splash Screen -> Main Flow)
            pressHome()
            startActivityAndWait()

            device.waitForIdle()

            // Simulate card swipe interactions to pre-compile Compose swipe & gesture code paths
            val displayWidth = device.displayWidth
            val displayHeight = device.displayHeight
            val midX = displayWidth / 2
            val midY = displayHeight / 2

            // Swipe right gesture
            device.swipe(midX, midY, displayWidth - 100, midY, 15)
            device.waitForIdle()

            // Swipe left gesture
            device.swipe(midX, midY, 100, midY, 15)
            device.waitForIdle()

            // Vertical swipe gesture
            device.swipe(midX, midY, midX, displayHeight - 200, 15)
            device.waitForIdle()
        }
    )
}