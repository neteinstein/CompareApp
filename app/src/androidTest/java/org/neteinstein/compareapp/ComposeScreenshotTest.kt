package org.neteinstein.compareapp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot tests for UI components using Roborazzi
 * These tests capture screenshots of the UI for visual regression testing
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28])
class ComposeScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCompareScreen_initialState() {
        // Given - Create activity and set content
        val activity = MainActivity()
        
        composeTestRule.setContent {
            activity.CompareAppTheme {
                activity.CompareScreen()
            }
        }

        // When/Then - Capture screenshot of initial state
        composeTestRule.onRoot().captureRoboImage("screenshots/compare_screen_initial.png")
    }

    @Test
    fun testCompareAppTheme_lightMode() {
        val activity = MainActivity()
        
        composeTestRule.setContent {
            activity.CompareAppTheme {
                androidx.compose.material3.Surface {
                    androidx.compose.material3.Text("Theme Test")
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage("screenshots/theme_light.png")
    }

    @Test
    fun testCompareScreen_layout() {
        val activity = MainActivity()
        
        composeTestRule.setContent {
            activity.CompareAppTheme {
                activity.CompareScreen()
            }
        }

        // Wait for composition to complete
        composeTestRule.waitForIdle()

        // Capture the composed screen
        composeTestRule.onRoot().captureRoboImage("screenshots/compare_screen_layout.png")
    }
}
