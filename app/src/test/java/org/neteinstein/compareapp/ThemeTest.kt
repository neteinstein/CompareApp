package org.neteinstein.compareapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCompareAppTheme_rendersWithoutCrashing() {
        // Given & When
        composeTestRule.setContent {
            val activity = MainActivity()
            activity.CompareAppTheme {
                // Empty content to test theme application
            }
        }

        // Then - test passes if no exception is thrown
    }

    @Test
    fun testCompareAppTheme_appliesColorScheme() {
        // Given & When
        var appliedColorScheme: androidx.compose.material3.ColorScheme? = null
        
        composeTestRule.setContent {
            val activity = MainActivity()
            activity.CompareAppTheme {
                appliedColorScheme = MaterialTheme.colorScheme
            }
        }

        // Then
        assert(appliedColorScheme != null) { "Color scheme should be applied" }
    }
}
