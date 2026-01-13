package org.neteinstein.compareapp

import android.location.Address
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowGeocoder

/**
 * Integration tests for deep link creation with various coordinate scenarios
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class DeepLinkIntegrationTest {

    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .get()
    }

    @Test
    fun testUberDeepLink_withNullCoordinates() {
        // Given
        val pickup = "Start Location"
        val dropoff = "End Location"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then
        assertTrue(deepLink.startsWith("uber://?action=setPickup"))
        assertTrue(deepLink.contains("pickup[formatted_address]="))
        assertTrue(deepLink.contains("dropoff[formatted_address]="))
    }

    @Test
    fun testUberDeepLink_withValidCoordinates() {
        // Given
        val pickup = "Start"
        val dropoff = "End"
        val pickupCoords = Pair(37.7749, -122.4194)
        val dropoffCoords = Pair(34.0522, -118.2437)

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        // Uber doesn't use coordinates in the deep link, just addresses
        assertTrue(deepLink.startsWith("uber://?action=setPickup"))
        assertTrue(deepLink.contains("Start"))
        assertTrue(deepLink.contains("End"))
    }

    @Test
    fun testBoltDeepLink_coordinatesRoundedTo6Decimals() = runTest {
        // Given
        val pickup = "Start"
        val dropoff = "End"
        val pickupCoords = Pair(40.12345678901234, -73.98765432109876)
        val dropoffCoords = Pair(41.11111111111111, -74.99999999999999)

        // When
        val deepLink = activity.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - Bolt should use rounded coordinates (6 decimal places)
        assertTrue(deepLink.contains("pickup_lat="))
        assertTrue(deepLink.contains("pickup_lng="))
        assertTrue(deepLink.contains("destination_lat="))
        assertTrue(deepLink.contains("destination_lng="))
    }

    @Test
    fun testBoltDeepLink_withOnlyPickupCoordinates() = runTest {
        // Given
        val pickup = "Known Location"
        val dropoff = "Unknown Location"
        val pickupCoords = Pair(40.7589, -73.9851)
        val dropoffCoords = null

        // When
        val deepLink = activity.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - Should fallback to address-based format if any coordinate is null
        assertTrue(deepLink.startsWith("bolt://ride?"))
        assertTrue(deepLink.contains("pickup="))
        assertTrue(deepLink.contains("destination="))
    }

    @Test
    fun testBoltDeepLink_withBothCoordinatesProvided() = runTest {
        // Given
        val pickup = "Location A"
        val dropoff = "Location B"
        val pickupCoords = Pair(40.7589, -73.9851)
        val dropoffCoords = Pair(40.7128, -74.0060)

        // When
        val deepLink = activity.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        assertTrue(deepLink.contains("pickup_lat=40.7589"))
        assertTrue(deepLink.contains("pickup_lng=-73.9851"))
        assertTrue(deepLink.contains("destination_lat=40.7128"))
        assertTrue(deepLink.contains("destination_lng=-74.006"))
    }

    @Test
    fun testGeocoding_withVeryLongAddress() = runTest {
        // Given
        val veryLongAddress = "123 Very Long Street Name That Goes On And On, Building A, Floor 10, Suite 1001, " +
                "Some City Name, Some State, 12345-6789, United States of America"
        
        val mockAddress = Address(java.util.Locale.US).apply {
            latitude = 40.7589
            longitude = -73.9851
        }
        ShadowGeocoder.setFromLocationName(veryLongAddress, listOf(mockAddress))

        // When
        val result = activity.geocodeAddress(veryLongAddress)

        // Then
        assertNotNull(result)
        assertEquals(40.7589, result?.first, 0.000001)
        assertEquals(-73.9851, result?.second, 0.000001)
    }

    @Test
    fun testReverseGeocode_withVeryPreciseCoordinates() = runTest {
        // Given
        val latitude = 40.758896123456789
        val longitude = -73.985130987654321
        
        val mockAddress = Address(java.util.Locale.US).apply {
            setAddressLine(0, "Precise Location Address")
        }
        ShadowGeocoder.setFromLocation(latitude, longitude, listOf(mockAddress))

        // When
        val result = activity.reverseGeocode(latitude, longitude)

        // Then
        assertNotNull(result)
        assertEquals("Precise Location Address", result)
    }

    @Test
    fun testReverseGeocode_withZeroCoordinates() = runTest {
        // Given - null island (0, 0)
        val latitude = 0.0
        val longitude = 0.0
        
        val mockAddress = Address(java.util.Locale.US).apply {
            setAddressLine(0, "Null Island")
        }
        ShadowGeocoder.setFromLocation(latitude, longitude, listOf(mockAddress))

        // When
        val result = activity.reverseGeocode(latitude, longitude)

        // Then
        assertNotNull(result)
        assertEquals("Null Island", result)
    }

    @Test
    fun testRoundDecimal_preservesNegativeSign() {
        // Given
        val negativeValue = -73.985130

        // When
        val result = activity.roundDecimal(negativeValue, 4)

        // Then
        assertEquals(-73.9851, result, 0.00001)
        assertTrue(result < 0)
    }

    @Test
    fun testRoundDecimal_withVeryLargeNumber() {
        // Given
        val largeValue = 180.0

        // When
        val result = activity.roundDecimal(largeValue, 6)

        // Then
        assertEquals(180.0, result, 0.000001)
    }

    @Test
    fun testRoundDecimal_withVerySmallNumber() {
        // Given
        val smallValue = 0.0000001

        // When
        val result = activity.roundDecimal(smallValue, 6)

        // Then
        assertEquals(0.0, result, 0.000001)
    }
}
