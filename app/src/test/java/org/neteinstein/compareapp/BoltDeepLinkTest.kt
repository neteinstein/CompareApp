package org.neteinstein.compareapp

import android.location.Address
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowGeocoder
import java.net.URLDecoder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class BoltDeepLinkTest {

    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .get()
    }

    @Test
    fun testCreateBoltDeepLink_withSuccessfulGeocoding() = runTest {
        // Given
        val pickup = "Times Square, New York"
        val dropoff = "Central Park, New York"
        
        // Setup mock geocoder
        val pickupAddress = Address(java.util.Locale.US).apply {
            latitude = 40.758896
            longitude = -73.985130
        }
        val dropoffAddress = Address(java.util.Locale.US).apply {
            latitude = 40.785091
            longitude = -73.968285
        }
        
        ShadowGeocoder.setFromLocationName(pickup, listOf(pickupAddress))
        ShadowGeocoder.setFromLocationName(dropoff, listOf(dropoffAddress))

        // When
        val deepLink = activity.createBoltDeepLink(pickup, dropoff, dropoffCoords = dropoffCoords)

        // Then
        assertTrue(deepLink.startsWith("bolt://ride?"))
        assertTrue(deepLink.contains("pickup_lat="))
        assertTrue(deepLink.contains("pickup_lng="))
        assertTrue(deepLink.contains("destination_lat="))
        assertTrue(deepLink.contains("destination_lng="))
    }

    @Test
    fun testCreateBoltDeepLink_withFailedGeocoding() = runTest {
        // Given
        val pickup = "Invalid Address 12345"
        val dropoff = "Another Invalid Address 67890"
        
        // Setup geocoder to return empty list (geocoding fails)
        ShadowGeocoder.setFromLocationName(pickup, emptyList())
        ShadowGeocoder.setFromLocationName(dropoff, emptyList())

        // When
        val deepLink = activity.createBoltDeepLink(pickup, dropoff, dropoffCoords = dropoffCoords)

        // Then
        // Should fallback to address-based format
        assertTrue(deepLink.startsWith("bolt://ride?"))
        assertTrue(deepLink.contains("pickup="))
        assertTrue(deepLink.contains("destination="))
        
        // Verify addresses are encoded
        val decodedLink = URLDecoder.decode(deepLink, "UTF-8")
        assertTrue(decodedLink.contains(pickup))
        assertTrue(decodedLink.contains(dropoff))
    }

    @Test
    fun testCreateBoltDeepLink_withPartialGeocoding() = runTest {
        // Given
        val pickup = "Valid Address"
        val dropoff = "Invalid Address"
        
        // Only pickup succeeds
        val pickupAddress = Address(java.util.Locale.US).apply {
            latitude = 40.758896
            longitude = -73.985130
        }
        ShadowGeocoder.setFromLocationName(pickup, listOf(pickupAddress))
        ShadowGeocoder.setFromLocationName(dropoff, emptyList())

        // When
        val deepLink = activity.createBoltDeepLink(pickup, dropoff, dropoffCoords = dropoffCoords)

        // Then
        // Should fallback to address-based format if any geocoding fails
        assertTrue(deepLink.startsWith("bolt://ride?"))
        assertTrue(deepLink.contains("pickup="))
        assertTrue(deepLink.contains("destination="))
    }

    @Test
    fun testCreateBoltDeepLink_withCoordinates() = runTest {
        // Given
        val pickup = "Test Location 1"
        val dropoff = "Test Location 2"
        val expectedLat1 = 12.345678
        val expectedLng1 = -98.765432
        val expectedLat2 = 23.456789
        val expectedLng2 = -87.654321
        
        val pickupAddress = Address(java.util.Locale.US).apply {
            latitude = expectedLat1
            longitude = expectedLng1
        }
        val dropoffAddress = Address(java.util.Locale.US).apply {
            latitude = expectedLat2
            longitude = expectedLng2
        }
        
        ShadowGeocoder.setFromLocationName(pickup, listOf(pickupAddress))
        ShadowGeocoder.setFromLocationName(dropoff, listOf(dropoffAddress))

        // When
        val deepLink = activity.createBoltDeepLink(pickup, dropoff, dropoffCoords = dropoffCoords)

        // Then
        assertTrue(deepLink.contains("pickup_lat=$expectedLat1"))
        assertTrue(deepLink.contains("pickup_lng=$expectedLng1"))
        assertTrue(deepLink.contains("destination_lat=$expectedLat2"))
        assertTrue(deepLink.contains("destination_lng=$expectedLng2"))
    }
}
