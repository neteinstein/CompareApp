package org.neteinstein.compareapp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.neteinstein.compareapp.data.repository.AppRepository
import org.neteinstein.compareapp.data.repository.LocationRepository
import org.neteinstein.compareapp.helpers.TestViewModelFactory
import org.neteinstein.compareapp.ui.screens.MainViewModel
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for createBoltDeepLinkWeb, the HTTPS fallback link used to set the destination
 * once the Bolt app is already open via the bolt:// scheme.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class BoltDeepLinkWebFormatTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        val mockLocationRepo = Mockito.mock(LocationRepository::class.java)
        val mockAppRepo = Mockito.mock(AppRepository::class.java)
        `when`(mockAppRepo.checkRequiredApps()).thenReturn(Pair(true, true))
        viewModel = TestViewModelFactory.createTestViewModel(mockLocationRepo, mockAppRepo)
    }

    @Test
    fun testBoltDeepLinkWeb_usesHttpsScheme() {
        val pickupCoords = Pair(40.758896, -73.985130)
        val dropoffCoords = Pair(40.785091, -73.968285)

        val deepLink = viewModel.createBoltDeepLinkWeb("Times Square", "Central Park", pickupCoords, dropoffCoords)

        assertTrue("Deep link should start with https://bolt.eu/ride/?", deepLink.startsWith("https://bolt.eu/ride/?"))
    }

    @Test
    fun testBoltDeepLinkWeb_formatsCoordinatesWithExactly6Decimals() {
        val pickupCoords = Pair(40.123456789012345, -73.987654321098765)
        val dropoffCoords = Pair(41.111111111111111, -74.999999999999999)

        val deepLink = viewModel.createBoltDeepLinkWeb("Start Location", "End Location", pickupCoords, dropoffCoords)

        assertTrue("Deep link should contain pickup_lat with 6 decimals",
            deepLink.contains("pickup_lat=40.123457"))
        assertTrue("Deep link should contain pickup_lng with 6 decimals",
            deepLink.contains("pickup_lng=-73.987654"))
        assertTrue("Deep link should contain destination_lat with 6 decimals",
            deepLink.contains("destination_lat=41.111111"))
        assertTrue("Deep link should contain destination_lng with 6 decimals",
            deepLink.contains("destination_lng=-75.000000"))
    }

    @Test
    fun testBoltDeepLinkWeb_containsAllRequiredParameters() {
        val pickupCoords = Pair(40.7128, -74.0060)
        val dropoffCoords = Pair(40.7589, -73.9851)

        val deepLink = viewModel.createBoltDeepLinkWeb("Pickup", "Dropoff", pickupCoords, dropoffCoords)

        assertTrue(deepLink.contains("pickup_lat="))
        assertTrue(deepLink.contains("pickup_lng="))
        assertTrue(deepLink.contains("destination_lat="))
        assertTrue(deepLink.contains("destination_lng="))
    }

    @Test
    fun testBoltDeepLinkWeb_usesPeriodAsDecimalSeparator() {
        val pickupCoords = Pair(40.123456, -73.987654)
        val dropoffCoords = Pair(41.111111, -74.999999)

        val deepLink = viewModel.createBoltDeepLinkWeb("Start", "End", pickupCoords, dropoffCoords)

        assertFalse("Deep link should not contain comma as decimal separator", deepLink.contains(","))
        assertTrue("Deep link should use period as decimal separator", deepLink.contains("."))
    }

    @Test
    fun testBoltDeepLinkWeb_fallsBackToPlainAddressesWhenCoordsMissing() {
        val deepLink = viewModel.createBoltDeepLinkWeb("Some Pickup", "Some Dropoff", null, null)

        assertTrue(deepLink.startsWith("https://bolt.eu/ride/?"))
        assertTrue(deepLink.contains("pickup=Some+Pickup"))
        assertTrue(deepLink.contains("destination=Some+Dropoff"))
    }

    @Test
    fun testBoltDeepLinkWeb_consistentFormatting() {
        val pickupCoords = Pair(40.123456789, -73.987654321)
        val dropoffCoords = Pair(41.111111111, -74.999999999)

        val deepLink1 = viewModel.createBoltDeepLinkWeb("Test", "Test", pickupCoords, dropoffCoords)
        val deepLink2 = viewModel.createBoltDeepLinkWeb("Test", "Test", pickupCoords, dropoffCoords)

        assertEquals(deepLink1, deepLink2)
    }
}
