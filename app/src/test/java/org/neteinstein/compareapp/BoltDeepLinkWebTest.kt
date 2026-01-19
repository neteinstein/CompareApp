package org.neteinstein.compareapp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import java.net.URLDecoder

/**
 * Test to verify that Bolt web deep link (https://) is properly formatted.
 * 
 * This tests the createBoltDeepLinkWeb function which creates HTTPS deep links
 * to bolt.eu that work when the Bolt app is already open or open a browser.
 * The function uses the same coordinate formatting as createBoltDeepLink (6 decimals)
 * and includes URL encoding for the fallback address format.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class BoltDeepLinkWebTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        val mockLocationRepo = Mockito.mock(LocationRepository::class.java)
        val mockAppRepo = Mockito.mock(AppRepository::class.java)
        viewModel = TestViewModelFactory.createTestViewModel(mockLocationRepo, mockAppRepo)
    }

    @Test
    fun testBoltDeepLinkWeb_usesHttpsScheme() {
        // Given - any coordinates
        val pickup = "Start Location"
        val dropoff = "End Location"
        val pickupCoords = Pair(40.758896, -73.985130)
        val dropoffCoords = Pair(40.785091, -73.968285)

        // When
        val deepLink = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - should use https:// scheme, not bolt://
        assertTrue("Web deep link should start with https://", deepLink.startsWith("https://"))
        assertFalse("Web deep link should not use bolt:// scheme", deepLink.startsWith("bolt://"))
    }

    @Test
    fun testBoltDeepLinkWeb_usesBoltEuDomain() {
        // Given - any coordinates
        val pickup = "Start Location"
        val dropoff = "End Location"
        val pickupCoords = Pair(40.758896, -73.985130)
        val dropoffCoords = Pair(40.785091, -73.968285)

        // When
        val deepLink = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - should use bolt.eu domain
        assertTrue("Web deep link should use bolt.eu domain", 
            deepLink.startsWith("https://bolt.eu/ride/"))
    }

    @Test
    fun testBoltDeepLinkWeb_formatsCoordinatesWithExactly6Decimals() {
        // Given - coordinates with high precision (like from GPS)
        val pickup = "Start Location"
        val dropoff = "End Location"
        val pickupCoords = Pair(40.123456789012345, -73.987654321098765)
        val dropoffCoords = Pair(41.111111111111111, -74.999999999999999)

        // When
        val deepLink = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - verify exactly 6 decimal places in the output
        assertTrue("Deep link should contain pickup_lat with 6 decimals",
            deepLink.contains("pickup_lat=40.123457")) // Rounded to 6 decimals
        assertTrue("Deep link should contain pickup_lng with 6 decimals",
            deepLink.contains("pickup_lng=-73.987654"))
        assertTrue("Deep link should contain destination_lat with 6 decimals",
            deepLink.contains("destination_lat=41.111111"))
        assertTrue("Deep link should contain destination_lng with 6 decimals",
            deepLink.contains("destination_lng=-75.000000"))
    }

    @Test
    fun testBoltDeepLinkWeb_addsTrailingZerosWhenNeeded() {
        // Given - coordinates with few decimal places
        val pickup = "Start"
        val dropoff = "End"
        val pickupCoords = Pair(40.5, -73.9)
        val dropoffCoords = Pair(41.0, -74.0)

        // When
        val deepLink = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - should pad with trailing zeros to reach 6 decimals
        assertTrue("Should pad with zeros: pickup_lat",
            deepLink.contains("pickup_lat=40.500000"))
        assertTrue("Should pad with zeros: pickup_lng",
            deepLink.contains("pickup_lng=-73.900000"))
        assertTrue("Should pad with zeros: destination_lat",
            deepLink.contains("destination_lat=41.000000"))
        assertTrue("Should pad with zeros: destination_lng",
            deepLink.contains("destination_lng=-74.000000"))
    }

    @Test
    fun testBoltDeepLinkWeb_usesPeriodAsDecimalSeparator() {
        // Given - any coordinates
        val pickup = "Start"
        val dropoff = "End"
        val pickupCoords = Pair(40.123456, -73.987654)
        val dropoffCoords = Pair(41.111111, -74.999999)

        // When
        val deepLink = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - should always use period (.) not comma (,) in coordinates
        // Extract just the coordinate values to check
        val coordinatePart = deepLink.substringAfter("pickup_lat=")
        assertFalse("Coordinates should not contain comma as decimal separator",
            coordinatePart.contains(","))
        assertTrue("Coordinates should use period as decimal separator",
            coordinatePart.contains("."))
    }

    @Test
    fun testBoltDeepLinkWeb_handlesNegativeCoordinates() {
        // Given - negative coordinates (Southern/Western hemispheres)
        val pickup = "Sydney"
        val dropoff = "Rio de Janeiro"
        val pickupCoords = Pair(-33.8688197654321, 151.2092955123456)
        val dropoffCoords = Pair(-22.9068467890123, -43.1728965432109)

        // When
        val deepLink = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - should format negative coordinates correctly
        assertTrue("Should handle negative latitude",
            deepLink.contains("pickup_lat=-33.868820"))
        assertTrue("Should handle positive longitude",
            deepLink.contains("pickup_lng=151.209296"))
        assertTrue("Should handle negative latitude",
            deepLink.contains("destination_lat=-22.906847"))
        assertTrue("Should handle negative longitude",
            deepLink.contains("destination_lng=-43.172897"))
    }

    @Test
    fun testBoltDeepLinkWeb_fallbackWithNullCoordinates() {
        // Given - null coordinates (geocoding failed)
        val pickup = "Start Location"
        val dropoff = "End Location"
        val pickupCoords = null
        val dropoffCoords = null

        // When
        val deepLink = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - should fallback to address-based format with URL encoding
        assertTrue("Should still use https:// scheme", deepLink.startsWith("https://bolt.eu/ride/"))
        assertTrue("Should contain pickup parameter", deepLink.contains("pickup="))
        assertTrue("Should contain destination parameter", deepLink.contains("destination="))
        assertFalse("Should not contain coordinate parameters", deepLink.contains("pickup_lat="))
    }

    @Test
    fun testBoltDeepLinkWeb_fallbackEncodesAddresses() {
        // Given - addresses with spaces and special characters
        val pickup = "Times Square, New York"
        val dropoff = "Central Park & Zoo"
        val pickupCoords = null
        val dropoffCoords = null

        // When
        val deepLink = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - addresses should be URL encoded
        assertTrue("Pickup should be URL encoded",
            deepLink.contains("pickup=Times+Square") || deepLink.contains("pickup=Times%20Square"))
        assertTrue("Destination should be URL encoded",
            deepLink.contains("destination=Central+Park") || deepLink.contains("destination=Central%20Park"))
        
        // Verify that decoded values match original
        val decodedLink = URLDecoder.decode(deepLink, "UTF-8")
        assertTrue("Decoded link should contain original pickup", decodedLink.contains(pickup))
        assertTrue("Decoded link should contain original dropoff", decodedLink.contains(dropoff))
    }

    @Test
    fun testBoltDeepLinkWeb_fallbackEncodesSpecialCharacters() {
        // Given - addresses with special characters that need encoding
        val pickup = "Place #1 & Street"
        val dropoff = "Location @ Avenue"
        val pickupCoords = null
        val dropoffCoords = null

        // When
        val deepLink = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - special characters should be properly encoded
        assertFalse("Raw # should not appear in URL", deepLink.contains("#1"))
        assertFalse("Raw & should not appear in URL (except as parameter separator)", 
            deepLink.substringAfter("pickup=").substringBefore("&").contains("&"))
        assertFalse("Raw @ should not appear in URL", deepLink.contains("@"))
        
        // Verify proper decoding
        val decodedLink = URLDecoder.decode(deepLink, "UTF-8")
        assertTrue("Decoded should contain #", decodedLink.contains("#"))
        assertTrue("Decoded should contain @", decodedLink.contains("@"))
    }

    @Test
    fun testBoltDeepLinkWeb_consistentFormatting() {
        // Given - same coordinates called multiple times
        val pickup = "Test"
        val dropoff = "Test"
        val pickupCoords = Pair(40.123456789, -73.987654321)
        val dropoffCoords = Pair(41.111111111, -74.999999999)

        // When - create multiple deep links
        val deepLink1 = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)
        val deepLink2 = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)
        val deepLink3 = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - all should produce identical results
        assertEquals("Deep links should be consistent", deepLink1, deepLink2)
        assertEquals("Deep links should be consistent", deepLink2, deepLink3)
        assertEquals("Deep links should be consistent", deepLink1, deepLink3)
    }

    @Test
    fun testBoltDeepLinkWeb_matchesExpectedWebFormat() {
        // Given - example coordinates
        val pickup = "Times Square"
        val dropoff = "Central Park"
        val pickupCoords = Pair(40.758896, -73.985130)
        val dropoffCoords = Pair(40.785091, -73.968285)

        // When
        val deepLink = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - should match the expected web format
        assertTrue("Should start with correct URL", deepLink.startsWith("https://bolt.eu/ride/?"))
        assertTrue("Should contain pickup_lat", deepLink.contains("pickup_lat=40.758896"))
        assertTrue("Should contain pickup_lng", deepLink.contains("pickup_lng=-73.985130"))
        assertTrue("Should contain destination_lat", deepLink.contains("destination_lat=40.785091"))
        assertTrue("Should contain destination_lng", deepLink.contains("destination_lng=-73.968285"))
    }

    @Test
    fun testBoltDeepLinkWeb_partialNullCoordinates_pickupNull() {
        // Given - only pickup coordinates are null
        val pickup = "Start"
        val dropoff = "End"
        val pickupCoords = null
        val dropoffCoords = Pair(40.785091, -73.968285)

        // When
        val deepLink = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - should fallback to address format if any coordinate is null
        assertTrue("Should use fallback format", deepLink.contains("pickup="))
        assertTrue("Should use fallback format", deepLink.contains("destination="))
        assertFalse("Should not use coordinate format", deepLink.contains("pickup_lat="))
    }

    @Test
    fun testBoltDeepLinkWeb_partialNullCoordinates_dropoffNull() {
        // Given - only dropoff coordinates are null
        val pickup = "Start"
        val dropoff = "End"
        val pickupCoords = Pair(40.758896, -73.985130)
        val dropoffCoords = null

        // When
        val deepLink = viewModel.createBoltDeepLinkWeb(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - should fallback to address format if any coordinate is null
        assertTrue("Should use fallback format", deepLink.contains("pickup="))
        assertTrue("Should use fallback format", deepLink.contains("destination="))
        assertFalse("Should not use coordinate format", deepLink.contains("pickup_lat="))
    }
}
