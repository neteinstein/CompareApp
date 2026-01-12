package org.neteinstein.compareapp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URLDecoder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityTest {

    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .get()
    }

    @Test
    fun testCreateUberDeepLink_encodesAddressesCorrectly() {
        // Given
        val pickup = "123 Main St, New York, NY"
        val dropoff = "456 Park Ave, New York, NY"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff)

        // Then
        assertTrue(deepLink.startsWith("uber://?action=setPickup"))
        assertTrue(deepLink.contains("pickup[formatted_address]="))
        assertTrue(deepLink.contains("dropoff[formatted_address]="))
        
        // Verify encoding by decoding
        val decodedLink = URLDecoder.decode(deepLink, "UTF-8")
        assertTrue(decodedLink.contains(pickup))
        assertTrue(decodedLink.contains(dropoff))
    }

    @Test
    fun testCreateUberDeepLink_withSpecialCharacters() {
        // Given
        val pickup = "Times Square & 42nd St"
        val dropoff = "Central Park, New York"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff)

        // Then
        assertTrue(deepLink.startsWith("uber://?action=setPickup"))
        assertTrue(deepLink.contains("pickup[formatted_address]="))
        assertTrue(deepLink.contains("dropoff[formatted_address]="))
        // Special characters should be URL-encoded
        assertTrue(deepLink.contains("%"))
    }

    @Test
    fun testCreateUberDeepLink_withEmptyStrings() {
        // Given
        val pickup = ""
        val dropoff = ""

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff)

        // Then
        assertTrue(deepLink.startsWith("uber://?action=setPickup"))
        // Should handle empty strings gracefully
        assertNotNull(deepLink)
    }

    @Test
    fun testCreateUberDeepLink_formatIsCorrect() {
        // Given
        val pickup = "Pickup Location"
        val dropoff = "Dropoff Location"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff)

        // Then
        // Verify the exact format expected by Uber
        val expectedStart = "uber://?action=setPickup&pickup[formatted_address]="
        assertTrue(deepLink.startsWith(expectedStart))
        assertTrue(deepLink.contains("&dropoff[formatted_address]="))
    }

    @Test
    fun testIsAppInstalled_returnsFalseForNonExistentPackage() {
        // Given
        val nonExistentPackage = "com.nonexistent.app"

        // When
        val result = activity.isAppInstalled(nonExistentPackage)

        // Then
        assertFalse(result)
    }

    @Test
    fun testCheckRequiredApps_returnsCorrectStatus() {
        // When
        val (isUberInstalled, isBoltInstalled) = activity.checkRequiredApps()

        // Then
        // In Robolectric test environment, these apps won't be installed
        assertFalse(isUberInstalled)
        assertFalse(isBoltInstalled)
    }

    @Test
    fun testIsAppInstalled_handlesValidPackageName() {
        // Given - using Android system package that should exist in test environment
        val systemPackage = "android"

        // When
        val result = activity.isAppInstalled(systemPackage)

        // Then
        assertTrue(result)
    }
}
