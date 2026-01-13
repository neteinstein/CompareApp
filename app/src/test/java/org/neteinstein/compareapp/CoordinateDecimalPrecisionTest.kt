package org.neteinstein.compareapp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.neteinstein.compareapp.data.repository.AppRepository
import org.neteinstein.compareapp.data.repository.LocationRepository
import org.neteinstein.compareapp.helpers.TestViewModelFactory
import org.neteinstein.compareapp.ui.screens.MainViewModel
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test suite to validate that all coordinates have at most 6 decimal places.
 * This ensures precision consistency across the application and prevents
 * excessive precision in deep links and location data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class CoordinateDecimalPrecisionTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        val mockLocationRepo = Mockito.mock(LocationRepository::class.java)
        val mockAppRepo = Mockito.mock(AppRepository::class.java)
        viewModel = TestViewModelFactory.createTestViewModel(mockLocationRepo, mockAppRepo)
    }

    /**
     * Helper function to verify a coordinate has at most the specified decimal places
     * by comparing with a rounded version
     */
    private fun hasAtMostDecimalPlaces(value: Double, maxPlaces: Int): Boolean {
        val multiplier = Math.pow(10.0, maxPlaces.toDouble())
        val rounded = Math.round(value * multiplier) / multiplier
        // Allow small floating point tolerance
        return Math.abs(value - rounded) < 1e-10
    }

    /**
     * Helper function to extract coordinate value from deep link parameter
     */
    private fun extractCoordinateFromDeepLink(deepLink: String, paramName: String): Double {
        val regex = "$paramName=([^&]+)".toRegex()
        val match = regex.find(deepLink)
        return match?.groupValues?.get(1)?.toDouble() ?: 0.0
    }

    @Test
    fun testBoltDeepLink_coordinatesHaveAtMost6DecimalPlaces() {
        // Given - coordinates with excessive precision (more than 6 decimal places)
        val pickup = "Location A"
        val dropoff = "Location B"
        val pickupCoords = Pair(40.123456789012345, -73.987654321098765)
        val dropoffCoords = Pair(41.111111111111111, -74.999999999999999)

        // When
        val deepLink = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - extract coordinates from deep link and verify precision
        val pickupLat = extractCoordinateFromDeepLink(deepLink, "pickup_lat")
        val pickupLng = extractCoordinateFromDeepLink(deepLink, "pickup_lng")
        val destLat = extractCoordinateFromDeepLink(deepLink, "destination_lat")
        val destLng = extractCoordinateFromDeepLink(deepLink, "destination_lng")

        // Verify all coordinates have at most 6 decimal places
        assertTrue("Pickup latitude has more than 6 decimal places", hasAtMostDecimalPlaces(pickupLat, 6))
        assertTrue("Pickup longitude has more than 6 decimal places", hasAtMostDecimalPlaces(pickupLng, 6))
        assertTrue("Destination latitude has more than 6 decimal places", hasAtMostDecimalPlaces(destLat, 6))
        assertTrue("Destination longitude has more than 6 decimal places", hasAtMostDecimalPlaces(destLng, 6))
    }

    @Test
    fun testBoltDeepLink_coordinatesRoundedCorrectly() {
        // Given
        val pickup = "Test Location"
        val dropoff = "Test Destination"
        val pickupCoords = Pair(40.7589648123, -73.9851301456)
        val dropoffCoords = Pair(40.7128975321, -74.0060234567)

        // When
        val deepLink = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        val pickupLat = extractCoordinateFromDeepLink(deepLink, "pickup_lat")
        val pickupLng = extractCoordinateFromDeepLink(deepLink, "pickup_lng")
        val destLat = extractCoordinateFromDeepLink(deepLink, "destination_lat")
        val destLng = extractCoordinateFromDeepLink(deepLink, "destination_lng")

        // Verify rounding to 6 decimal places
        assertEquals(40.758965, pickupLat, 0.0000001)
        assertEquals(-73.985130, pickupLng, 0.0000001)
        assertEquals(40.712898, destLat, 0.0000001)
        assertEquals(-74.006023, destLng, 0.0000001)
    }

    @Test
    fun testBoltDeepLink_extremeCoordinatesRoundedTo6Decimals() {
        // Given - coordinates near extremes
        val pickup = "North Pole"
        val dropoff = "Equator"
        val pickupCoords = Pair(89.999999999999, -179.999999999999)
        val dropoffCoords = Pair(0.000000123456, 0.000000654321)

        // When
        val deepLink = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        val pickupLat = extractCoordinateFromDeepLink(deepLink, "pickup_lat")
        val pickupLng = extractCoordinateFromDeepLink(deepLink, "pickup_lng")
        val destLat = extractCoordinateFromDeepLink(deepLink, "destination_lat")
        val destLng = extractCoordinateFromDeepLink(deepLink, "destination_lng")

        // All should have at most 6 decimal places
        assertTrue(hasAtMostDecimalPlaces(pickupLat, 6))
        assertTrue(hasAtMostDecimalPlaces(pickupLng, 6))
        assertTrue(hasAtMostDecimalPlaces(destLat, 6))
        assertTrue(hasAtMostDecimalPlaces(destLng, 6))
    }

    @Test
    fun testBoltDeepLink_negativeCoordinatesRoundedTo6Decimals() {
        // Given - negative coordinates (Southern/Western hemispheres)
        val pickup = "Southern Location"
        val dropoff = "Western Location"
        val pickupCoords = Pair(-33.8688197654321, 151.2092955123456)
        val dropoffCoords = Pair(-22.9068467890123, -43.1728965432109)

        // When
        val deepLink = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        val pickupLat = extractCoordinateFromDeepLink(deepLink, "pickup_lat")
        val pickupLng = extractCoordinateFromDeepLink(deepLink, "pickup_lng")
        val destLat = extractCoordinateFromDeepLink(deepLink, "destination_lat")
        val destLng = extractCoordinateFromDeepLink(deepLink, "destination_lng")

        assertEquals(-33.868820, pickupLat, 0.0000001)
        assertEquals(151.209296, pickupLng, 0.0000001)
        assertEquals(-22.906847, destLat, 0.0000001)
        assertEquals(-43.172897, destLng, 0.0000001)
    }

    @Test
    fun testBoltDeepLink_coordinatesWithLessThan6DecimalsUnchanged() {
        // Given - coordinates that already have 6 or fewer decimal places
        val pickup = "Simple Location"
        val dropoff = "Another Location"
        val pickupCoords = Pair(40.7589, -73.9851)
        val dropoffCoords = Pair(40.712, -74.006)

        // When
        val deepLink = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        val pickupLat = extractCoordinateFromDeepLink(deepLink, "pickup_lat")
        val pickupLng = extractCoordinateFromDeepLink(deepLink, "pickup_lng")
        val destLat = extractCoordinateFromDeepLink(deepLink, "destination_lat")
        val destLng = extractCoordinateFromDeepLink(deepLink, "destination_lng")

        // Values should be preserved
        assertEquals(40.7589, pickupLat, 0.0000001)
        assertEquals(-73.9851, pickupLng, 0.0000001)
        assertEquals(40.712, destLat, 0.0000001)
        assertEquals(-74.006, destLng, 0.0000001)
    }

    @Test
    fun testBoltDeepLink_coordinatesRoundingHalfUp() {
        // Given - test HALF_UP rounding mode at 7th decimal place
        val pickup = "Test"
        val dropoff = "Test"
        // Values where 7th decimal is 5 (should round up)
        val pickupCoords = Pair(40.1234565, -73.9876545)
        val dropoffCoords = Pair(41.1111115, -74.9999995)

        // When
        val deepLink = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        val pickupLat = extractCoordinateFromDeepLink(deepLink, "pickup_lat")
        val pickupLng = extractCoordinateFromDeepLink(deepLink, "pickup_lng")
        val destLat = extractCoordinateFromDeepLink(deepLink, "destination_lat")
        val destLng = extractCoordinateFromDeepLink(deepLink, "destination_lng")

        // HALF_UP rounding mode should round up when 7th decimal is 5
        assertEquals(40.123457, pickupLat, 0.0000001)
        assertEquals(-73.987655, pickupLng, 0.0000001)
        assertEquals(41.111112, destLat, 0.0000001)
        assertEquals(-75.000000, destLng, 0.0000001)
    }

    @Test
    fun testBoltDeepLink_multipleCallsProduceSamePrecision() {
        // Given - same coordinates called multiple times
        val pickup = "Test"
        val dropoff = "Test"
        val pickupCoords = Pair(40.123456789, -73.987654321)
        val dropoffCoords = Pair(41.111111111, -74.999999999)

        // When - create multiple deep links
        val deepLink1 = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)
        val deepLink2 = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)
        val deepLink3 = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - all should produce identical results
        assertEquals(deepLink1, deepLink2)
        assertEquals(deepLink2, deepLink3)
    }

    @Test
    fun testBoltDeepLink_zeroCoordinatesHandled() {
        // Given - zero coordinates (Null Island)
        val pickup = "Null Island"
        val dropoff = "Origin"
        val pickupCoords = Pair(0.0, 0.0)
        val dropoffCoords = Pair(0.000000000001, 0.000000000001)

        // When
        val deepLink = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        val pickupLat = extractCoordinateFromDeepLink(deepLink, "pickup_lat")
        val pickupLng = extractCoordinateFromDeepLink(deepLink, "pickup_lng")
        val destLat = extractCoordinateFromDeepLink(deepLink, "destination_lat")
        val destLng = extractCoordinateFromDeepLink(deepLink, "destination_lng")

        assertEquals(0.0, pickupLat, 0.0000001)
        assertEquals(0.0, pickupLng, 0.0000001)
        assertEquals(0.0, destLat, 0.0000001)
        assertEquals(0.0, destLng, 0.0000001)
    }
}
