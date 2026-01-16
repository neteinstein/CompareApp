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
 * Test to verify that Bolt deep link coordinates are formatted with exactly 6 decimal places
 * and use the correct decimal separator (period).
 *
 * This addresses the issue where coordinates with more than 6 decimal places
 * would cause the Bolt app to not properly set the pickup/destination locations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class BoltDeepLinkFormatTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        val mockLocationRepo = Mockito.mock(LocationRepository::class.java)
        val mockAppRepo = Mockito.mock(AppRepository::class.java)
        viewModel = TestViewModelFactory.createTestViewModel(mockLocationRepo, mockAppRepo)
    }

    @Test
    fun testBoltDeepLink_formatsCoordinatesWithExactly6Decimals() {
        // Given - coordinates with high precision (like from GPS)
        val pickup = "Start Location"
        val dropoff = "End Location"
        val pickupCoords = Pair(40.123456789012345, -73.987654321098765)
        val dropoffCoords = Pair(41.111111111111111, -74.999999999999999)

        // When
        val deepLink = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

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
    fun testBoltDeepLink_addsTrailingZerosWhenNeeded() {
        // Given - coordinates with few decimal places
        val pickup = "Start"
        val dropoff = "End"
        val pickupCoords = Pair(40.5, -73.9)
        val dropoffCoords = Pair(41.0, -74.0)

        // When
        val deepLink = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

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
    fun testBoltDeepLink_usesPeriodAsDecimalSeparator() {
        // Given - any coordinates
        val pickup = "Start"
        val dropoff = "End"
        val pickupCoords = Pair(40.123456, -73.987654)
        val dropoffCoords = Pair(41.111111, -74.999999)

        // When
        val deepLink = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - should always use period (.) not comma (,)
        assertTrue("Deep link should not contain comma as decimal separator",
            !deepLink.contains(","))
        assertTrue("Deep link should use period as decimal separator",
            deepLink.contains("."))
    }

    @Test
    fun testBoltDeepLink_consistentFormatting() {
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
        assertEquals("Deep links should be consistent", deepLink1, deepLink2)
        assertEquals("Deep links should be consistent", deepLink2, deepLink3)
        assertEquals("Deep links should be consistent", deepLink1, deepLink3)
    }

    @Test
    fun testBoltDeepLink_handlesNegativeCoordinates() {
        // Given - negative coordinates (Southern/Western hemispheres)
        val pickup = "Sydney"
        val dropoff = "Rio de Janeiro"
        val pickupCoords = Pair(-33.8688197654321, 151.2092955123456)
        val dropoffCoords = Pair(-22.9068467890123, -43.1728965432109)

        // When
        val deepLink = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

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
    fun testBoltDeepLink_matchesDocumentedFormat() {
        // Given - example from documentation
        val pickup = "Times Square"
        val dropoff = "Central Park"
        val pickupCoords = Pair(40.758896, -73.985130)
        val dropoffCoords = Pair(40.785091, -73.968285)

        // When
        val deepLink = viewModel.createBoltDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then - should match the format from docs/ARCHITECTURE.md
        val expectedFormat = "bolt://ride?pickup_lat=40.758896&pickup_lng=-73.985130&destination_lat=40.785091&destination_lng=-73.968285"
        assertEquals("Deep link should match documented format", expectedFormat, deepLink)
    }
}
