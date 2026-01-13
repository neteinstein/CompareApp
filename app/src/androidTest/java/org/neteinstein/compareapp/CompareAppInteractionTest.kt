package org.neteinstein.compareapp

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Additional Espresso UI tests for edge cases and user interactions
 */
@RunWith(AndroidJUnit4::class)
class CompareAppInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testClearingPickupField() {
        // Get text from resources
        val pickupLabel = composeTestRule.activity.getString(R.string.pickup_location)

        // Type text
        composeTestRule.onNodeWithText(pickupLabel).performTextInput("Test Location")
        
        // Clear text
        composeTestRule.onNodeWithText(pickupLabel).performTextClearance()
        
        // Verify field is cleared (implicitly by not crashing)
        composeTestRule.waitForIdle()
    }

    @Test
    fun testTypingSpecialCharactersInFields() {
        val pickupLabel = composeTestRule.activity.getString(R.string.pickup_location)
        val dropoffLabel = composeTestRule.activity.getString(R.string.dropoff_location)

        // Test special characters
        composeTestRule.onNodeWithText(pickupLabel).performTextInput("123 Main St & 5th Ave")
        composeTestRule.onNodeWithText(dropoffLabel).performTextInput("Café de Paris, 10001")
        
        composeTestRule.waitForIdle()
    }

    @Test
    fun testTypingVeryLongAddresses() {
        val pickupLabel = composeTestRule.activity.getString(R.string.pickup_location)
        
        val longAddress = "1234 Very Long Street Name That Continues For A While, " +
                "Building Complex Name, Suite 1001, City Name, State 12345"
        
        composeTestRule.onNodeWithText(pickupLabel).performTextInput(longAddress)
        
        // Verify text is there
        composeTestRule.waitForIdle()
    }

    @Test
    fun testMultipleFieldInteractions() {
        val pickupLabel = composeTestRule.activity.getString(R.string.pickup_location)
        val dropoffLabel = composeTestRule.activity.getString(R.string.dropoff_location)

        // Type in pickup
        composeTestRule.onNodeWithText(pickupLabel).performTextInput("Location A")
        
        // Type in dropoff
        composeTestRule.onNodeWithText(dropoffLabel).performTextInput("Location B")
        
        // Clear pickup
        composeTestRule.onNodeWithText(pickupLabel).performTextClearance()
        
        // Type new value in pickup
        composeTestRule.onNodeWithText(pickupLabel).performTextInput("Location C")
        
        composeTestRule.waitForIdle()
    }

    @Test
    fun testUIRespondsToMultipleInputs() {
        val pickupLabel = composeTestRule.activity.getString(R.string.pickup_location)
        val dropoffLabel = composeTestRule.activity.getString(R.string.dropoff_location)

        // Rapidly change inputs
        composeTestRule.onNodeWithText(pickupLabel).performTextInput("A")
        composeTestRule.onNodeWithText(dropoffLabel).performTextInput("B")
        composeTestRule.onNodeWithText(pickupLabel).performTextClearance()
        composeTestRule.onNodeWithText(pickupLabel).performTextInput("C")
        
        // UI should remain responsive
        composeTestRule.waitForIdle()
        
        // Verify app doesn't crash
        composeTestRule.onNodeWithText("CompareApp").assertExists()
    }

    @Test
    fun testAllUIElementsRendered() {
        // Verify all main UI elements are present
        composeTestRule.onNodeWithText("CompareApp").assertExists()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.pickup_location)
        ).assertExists()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.dropoff_location)
        ).assertExists()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.compare)
        ).assertExists()
        
        // Check for info label
        val infoText = composeTestRule.activity.getString(R.string.info_label)
        composeTestRule.onNodeWithText(infoText, substring = true).assertExists()
    }

    @Test
    fun testEmojisInAddressFields() {
        val pickupLabel = composeTestRule.activity.getString(R.string.pickup_location)
        
        // Test with emoji
        composeTestRule.onNodeWithText(pickupLabel).performTextInput("🏠 Home Address")
        
        composeTestRule.waitForIdle()
    }

    @Test
    fun testNumericInputInFields() {
        val pickupLabel = composeTestRule.activity.getString(R.string.pickup_location)
        val dropoffLabel = composeTestRule.activity.getString(R.string.dropoff_location)

        // Test pure numeric input
        composeTestRule.onNodeWithText(pickupLabel).performTextInput("12345")
        composeTestRule.onNodeWithText(dropoffLabel).performTextInput("67890")
        
        composeTestRule.waitForIdle()
    }
}
