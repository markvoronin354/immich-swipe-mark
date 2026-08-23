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
            // Démarrer l'application (Splash Screen -> Login -> Home)
            pressHome()
            startActivityAndWait()
            
            // Note: C'est un test très basique car l'application nécessite une connexion.
            // Dans un vrai environnement, on mockerait l'API ou on insérerait des données de test
            // avant de générer le profil.
            
            // Attendre que l'UI soit stable
            device.waitForIdle()
        }
    )
}