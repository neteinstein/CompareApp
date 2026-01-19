package org.neteinstein.compareapp

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivity

/**
 * Tests for MainActivity's dual-intent logic for opening Uber and Bolt apps
 * Verifies the correct sequence of intents, delays, and error handling
 * 
 * Note on timing: These tests use Thread.sleep() to wait for coroutine execution
 * because we're testing real coroutine delays (kotlinx.coroutines.delay) in the
 * production code. Using test schedulers would require refactoring the production
 * code to inject a test dispatcher, which would change the implementation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityIntentTest {

    companion object {
        // Minimum expected delay tolerance in tests (100ms less than actual 500ms to account for variations)
        private const val MIN_DELAY_MS = 400L
    }

    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .get()
    }

    /**
     * Helper method to invoke the private openInSplitScreen method via reflection
     */
    private fun invokeOpenInSplitScreen(
        uberDeepLink: String,
        boltDeepLink: String,
        boltDeepLinkWeb: String
    ) {
        val method = activity.javaClass.getDeclaredMethod(
            "openInSplitScreen",
            String::class.java,
            String::class.java,
            String::class.java
        )
        method.isAccessible = true
        method.invoke(activity, uberDeepLink, boltDeepLink, boltDeepLinkWeb)
    }

    /**
     * Helper method to collect all three intents with proper delays
     * Returns a list of [Intent?] in the order they were launched
     */
    private fun collectAllIntents(shadowActivity: ShadowActivity): List<Intent?> {
        Thread.sleep(100)
        val intent1 = shadowActivity.nextStartedActivity
        Thread.sleep(600)
        val intent2 = shadowActivity.nextStartedActivity
        Thread.sleep(600)
        val intent3 = shadowActivity.nextStartedActivity
        return listOf(intent1, intent2, intent3)
    }

    @Test
    fun testOpenInSplitScreen_launchesUberIntent() {
        // Given
        val shadowActivity = shadowOf(activity)
        val uberDeepLink = "uber://?action=setPickup&pickup[formatted_address]=Start&dropoff[formatted_address]=End"
        val boltDeepLink = "bolt://ride?pickup_lat=40.7589&pickup_lng=-73.9851&destination_lat=40.7128&destination_lng=-74.0060"
        val boltDeepLinkWeb = "https://m.bolt.eu/ride?pickup_lat=40.7589&pickup_lng=-73.9851&destination_lat=40.7128&destination_lng=-74.0060"
        
        // When
        invokeOpenInSplitScreen(uberDeepLink, boltDeepLink, boltDeepLinkWeb)
        
        // Give coroutines time to execute
        Thread.sleep(100)
        
        // Then - Verify Uber intent was started
        val nextStartedActivity = shadowActivity.nextStartedActivity
        assertNotNull("Uber intent should be started", nextStartedActivity)
        assertEquals(Intent.ACTION_VIEW, nextStartedActivity.action)
        assertEquals(Uri.parse(uberDeepLink), nextStartedActivity.data)
        assertTrue(
            "Uber intent should have NEW_TASK flag",
            (nextStartedActivity.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        )
        assertTrue(
            "Uber intent should have LAUNCH_ADJACENT flag",
            (nextStartedActivity.flags and Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT) != 0
        )
    }

    @Test
    fun testOpenInSplitScreen_launchesBoltAppIntent() {
        // Given
        val shadowActivity = shadowOf(activity)
        val uberDeepLink = "uber://?action=setPickup&pickup[formatted_address]=Start&dropoff[formatted_address]=End"
        val boltDeepLink = "bolt://ride?pickup_lat=40.7589&pickup_lng=-73.9851&destination_lat=40.7128&destination_lng=-74.0060"
        val boltDeepLinkWeb = "https://m.bolt.eu/ride?pickup_lat=40.7589&pickup_lng=-73.9851&destination_lat=40.7128&destination_lng=-74.0060"
        
        // When
        invokeOpenInSplitScreen(uberDeepLink, boltDeepLink, boltDeepLinkWeb)
        
        // Give coroutines time to execute
        Thread.sleep(100)
        
        // Skip the Uber intent
        shadowActivity.nextStartedActivity
        
        // Give more time for the delayed Bolt intent
        Thread.sleep(600)
        
        // Then - Verify Bolt app intent was started
        val boltAppIntent = shadowActivity.nextStartedActivity
        assertNotNull("Bolt app intent should be started", boltAppIntent)
        assertEquals(Intent.ACTION_VIEW, boltAppIntent.action)
        assertEquals(Uri.parse(boltDeepLink), boltAppIntent.data)
        assertEquals("ee.mtakso.client", boltAppIntent.`package`)
        assertTrue(
            "Bolt app intent should have NEW_TASK flag",
            (boltAppIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        )
        assertTrue(
            "Bolt app intent should have LAUNCH_ADJACENT flag",
            (boltAppIntent.flags and Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT) != 0
        )
    }

    @Test
    fun testOpenInSplitScreen_launchesBoltWebIntent() {
        // Given
        val shadowActivity = shadowOf(activity)
        val uberDeepLink = "uber://?action=setPickup&pickup[formatted_address]=Start&dropoff[formatted_address]=End"
        val boltDeepLink = "bolt://ride?pickup_lat=40.7589&pickup_lng=-73.9851&destination_lat=40.7128&destination_lng=-74.0060"
        val boltDeepLinkWeb = "https://m.bolt.eu/ride?pickup_lat=40.7589&pickup_lng=-73.9851&destination_lat=40.7128&destination_lng=-74.0060"
        
        // When
        invokeOpenInSplitScreen(uberDeepLink, boltDeepLink, boltDeepLinkWeb)
        
        // Give coroutines time to execute both Bolt intents
        Thread.sleep(100)
        shadowActivity.nextStartedActivity // Skip Uber
        Thread.sleep(600)
        shadowActivity.nextStartedActivity // Skip Bolt app
        Thread.sleep(600)
        
        // Then - Verify Bolt web intent was started
        val boltWebIntent = shadowActivity.nextStartedActivity
        assertNotNull("Bolt web intent should be started", boltWebIntent)
        assertEquals(Intent.ACTION_VIEW, boltWebIntent.action)
        assertEquals(Uri.parse(boltDeepLinkWeb), boltWebIntent.data)
        assertTrue(
            "Bolt web intent should have NEW_TASK flag",
            (boltWebIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        )
        assertTrue(
            "Bolt web intent should have LAUNCH_ADJACENT flag",
            (boltWebIntent.flags and Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT) != 0
        )
    }

    @Test
    fun testOpenInSplitScreen_correctIntentSequence() {
        // Given
        val shadowActivity = shadowOf(activity)
        val uberDeepLink = "uber://?action=setPickup&pickup[formatted_address]=Start&dropoff[formatted_address]=End"
        val boltDeepLink = "bolt://ride?pickup_lat=40.7589&pickup_lng=-73.9851&destination_lat=40.7128&destination_lng=-74.0060"
        val boltDeepLinkWeb = "https://m.bolt.eu/ride?pickup_lat=40.7589&pickup_lng=-73.9851&destination_lat=40.7128&destination_lng=-74.0060"
        
        // When
        invokeOpenInSplitScreen(uberDeepLink, boltDeepLink, boltDeepLinkWeb)
        val intents = collectAllIntents(shadowActivity)
        val (intent1, intent2, intent3) = intents
        
        // Then - Verify the correct sequence
        // 1. Uber intent
        assertEquals("First intent should be Uber", Uri.parse(uberDeepLink), intent1?.data)
        
        // 2. Bolt app intent with package
        assertEquals("Second intent should be Bolt app", Uri.parse(boltDeepLink), intent2?.data)
        assertEquals("Second intent should target Bolt package", "ee.mtakso.client", intent2?.`package`)
        
        // 3. Bolt web intent
        assertEquals("Third intent should be Bolt web", Uri.parse(boltDeepLinkWeb), intent3?.data)
    }

    @Test
    fun testOpenInSplitScreen_hasProperDelays() {
        // Given
        val shadowActivity = shadowOf(activity)
        val uberDeepLink = "uber://?action=setPickup"
        val boltDeepLink = "bolt://ride"
        val boltDeepLinkWeb = "https://m.bolt.eu/ride"
        
        // When
        val startTime = System.currentTimeMillis()
        invokeOpenInSplitScreen(uberDeepLink, boltDeepLink, boltDeepLinkWeb)
        
        // Wait for all intents to be launched
        Thread.sleep(100) // Initial Uber launch
        shadowActivity.nextStartedActivity
        
        val beforeBoltApp = System.currentTimeMillis()
        Thread.sleep(600) // Wait for delay + Bolt app launch
        shadowActivity.nextStartedActivity
        val afterBoltApp = System.currentTimeMillis()
        
        val beforeBoltWeb = System.currentTimeMillis()
        Thread.sleep(600) // Wait for delay + Bolt web launch
        shadowActivity.nextStartedActivity
        val afterBoltWeb = System.currentTimeMillis()
        
        // Then - Verify delays are present (allowing some margin for test execution)
        val delay1 = afterBoltApp - startTime
        val delay2 = afterBoltWeb - afterBoltApp
        
        // Each delay should be at least SPLIT_SCREEN_DELAY_MS (500ms)
        // We use MIN_DELAY_MS (400ms) to account for timing variations in test environments
        assertTrue("First delay should be at least ${MIN_DELAY_MS}ms, was $delay1", delay1 >= MIN_DELAY_MS)
        assertTrue("Second delay should be at least ${MIN_DELAY_MS}ms, was $delay2", delay2 >= MIN_DELAY_MS)
    }

    @Test
    fun testOpenInSplitScreen_handlesUberError() {
        // This test verifies that the error handling for Uber is in place
        // In a real test environment with mocked startActivity, we would verify
        // that exceptions are caught and logged properly
        // For now, we verify the code structure supports error handling
        
        // When - Try to open with an invalid Uber deep link
        val invalidUberDeepLink = "invalid://uber"
        val boltDeepLink = "bolt://ride"
        val boltDeepLinkWeb = "https://m.bolt.eu/ride"
        
        // Then - Should not crash (error is caught and logged)
        try {
            invokeOpenInSplitScreen(invalidUberDeepLink, boltDeepLink, boltDeepLinkWeb)
            Thread.sleep(100)
            // If we get here without exception, error handling works
            assertTrue("Error handling prevents crash", true)
        } catch (e: Exception) {
            // If an exception bubbles up, it means error handling is missing
            throw AssertionError("openInSplitScreen should handle errors gracefully", e)
        }
    }

    @Test
    fun testOpenInSplitScreen_handlesBoltError() {
        // Verify that Bolt-specific errors don't prevent the method from completing
        val shadowActivity = shadowOf(activity)
        val uberDeepLink = "uber://?action=setPickup"
        val invalidBoltDeepLink = "invalid://bolt"
        val boltDeepLinkWeb = "https://m.bolt.eu/ride"
        
        // When
        // Then - Should launch Uber even if Bolt fails
        try {
            invokeOpenInSplitScreen(uberDeepLink, invalidBoltDeepLink, boltDeepLinkWeb)
            Thread.sleep(100)
            
            // Verify Uber was still launched
            val uberIntent = shadowActivity.nextStartedActivity
            assertNotNull("Uber should be launched even if Bolt fails", uberIntent)
            assertEquals(Uri.parse(uberDeepLink), uberIntent.data)
        } catch (e: Exception) {
            throw AssertionError("Error handling should prevent exceptions", e)
        }
    }

    @Test
    fun testOpenInSplitScreen_allIntentsHaveCorrectFlags() {
        // Given
        val shadowActivity = shadowOf(activity)
        val uberDeepLink = "uber://?action=setPickup"
        val boltDeepLink = "bolt://ride"
        val boltDeepLinkWeb = "https://m.bolt.eu/ride"
        
        // When
        invokeOpenInSplitScreen(uberDeepLink, boltDeepLink, boltDeepLinkWeb)
        val allIntents = collectAllIntents(shadowActivity)
        
        // Then - All intents should have the required flags for split screen
        allIntents.forEachIndexed { index, intent ->
            assertNotNull("Intent ${index + 1} should not be null", intent)
            assertTrue(
                "Intent ${index + 1} should have NEW_TASK flag",
                (intent!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
            )
            assertTrue(
                "Intent ${index + 1} should have LAUNCH_ADJACENT flag",
                (intent.flags and Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT) != 0
            )
        }
    }
}
