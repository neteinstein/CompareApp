package org.neteinstein.compareapp

import android.location.Address
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowGeocoder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class LocationTest {

    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .get()
    }

    @Test
    fun testReverseGeocode_withValidCoordinates() = runTest {
        // Given
        val latitude = 40.758896
        val longitude = -73.985130
        val expectedAddress = "Times Square, New York, NY 10036, USA"
        
        val mockAddress = Address(java.util.Locale.US).apply {
            setAddressLine(0, expectedAddress)
        }
        ShadowGeocoder.setFromLocation(latitude, longitude, listOf(mockAddress))

        // When
        val result = activity.reverseGeocode(latitude, longitude)

        // Then
        assertNotNull(result)
        assertEquals(expectedAddress, result)
    }

    @Test
    fun testReverseGeocode_withInvalidCoordinates() = runTest {
        // Given
        val latitude = 999.0
        val longitude = 999.0
        ShadowGeocoder.setFromLocation(latitude, longitude, emptyList())

        // When
        val result = activity.reverseGeocode(latitude, longitude)

        // Then
        assertNull(result)
    }

    @Test
    fun testReverseGeocode_withNullResult() = runTest {
        // Given
        val latitude = 37.7749
        val longitude = -122.4194
        ShadowGeocoder.setFromLocation(latitude, longitude, null)

        // When
        val result = activity.reverseGeocode(latitude, longitude)

        // Then
        assertNull(result)
    }

    @Test
    fun testReverseGeocode_withMultipleResults() = runTest {
        // Given
        val latitude = 40.7128
        val longitude = -74.0060
        val expectedAddress = "New York, NY, USA"
        
        val address1 = Address(java.util.Locale.US).apply {
            setAddressLine(0, expectedAddress)
        }
        val address2 = Address(java.util.Locale.US).apply {
            setAddressLine(0, "Alternative Address")
        }
        ShadowGeocoder.setFromLocation(latitude, longitude, listOf(address1, address2))

        // When
        val result = activity.reverseGeocode(latitude, longitude)

        // Then
        assertNotNull(result)
        // Should return the first result
        assertEquals(expectedAddress, result)
    }

    @Test
    fun testCreateBoltDeepLink_withProvidedPickupCoordinates() = runTest {
        // Given
        val pickup = "Current Location"
        val dropoff = "Central Park, New York"
        val pickupCoords = Pair(40.758896, -73.985130)
        
        // Setup mock geocoder for dropoff
        val dropoffAddress = Address(java.util.Locale.US).apply {
            latitude = 40.785091
            longitude = -73.968285
        }
        ShadowGeocoder.setFromLocationName(dropoff, listOf(dropoffAddress))

        // When
        val deepLink = activity.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        // Should use provided coordinates for pickup
        assert(deepLink.contains("pickup_lat=${pickupCoords.first}"))
        assert(deepLink.contains("pickup_lng=${pickupCoords.second}"))
        // Should geocode dropoff
        assert(deepLink.contains("destination_lat=40.785091"))
        assert(deepLink.contains("destination_lng=-73.968285"))
    }

    @Test
    fun testCreateBoltDeepLink_withProvidedPickupCoordinates_andGeocodingFailsForDropoff() = runTest {
        // Given
        val pickup = "Current Location"
        val dropoff = "Invalid Dropoff"
        val pickupCoords = Pair(40.758896, -73.985130)
        
        // Setup geocoder to fail for dropoff
        ShadowGeocoder.setFromLocationName(dropoff, emptyList())

        // When
        val deepLink = activity.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        // Should fallback to address-based format
        assert(deepLink.startsWith("bolt://ride?"))
        assert(deepLink.contains("pickup="))
        assert(deepLink.contains("destination="))
    }

    @Test
    fun testCreateBoltDeepLink_withoutProvidedCoordinates_shouldGeocodePickup() = runTest {
        // Given
        val pickup = "Times Square, New York"
        val dropoff = "Central Park, New York"
        
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
        // Should geocode both pickup and dropoff
        assert(deepLink.contains("pickup_lat=40.758896"))
        assert(deepLink.contains("pickup_lng=-73.985130"))
        assert(deepLink.contains("destination_lat=40.785091"))
        assert(deepLink.contains("destination_lng=-73.968285"))
    }

    @Test
    fun testHasLocationPermission_returnsFalseByDefault() {
        // When
        val result = activity.hasLocationPermission()

        // Then
        // In test environment without permissions granted
        assertEquals(false, result)
    }
}
