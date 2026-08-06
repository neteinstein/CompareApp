package org.neteinstein.compareapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
 * Tests for prepopulating the dropoff location from an incoming location deep link,
 * and for reusing those coordinates (instead of re-geocoding) when comparing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelDeepLinkTest {

    private lateinit var locationRepository: LocationRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        locationRepository = Mockito.mock(LocationRepository::class.java)
        runBlocking { `when`(locationRepository.searchAddresses(Mockito.anyString())).thenReturn(emptyList()) }
        val appRepository = Mockito.mock(AppRepository::class.java)
        `when`(appRepository.checkRequiredApps()).thenReturn(Pair(true, true))
        viewModel = TestViewModelFactory.createTestViewModel(locationRepository, appRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testApplyIncomingDropoffLocation_withLabelSetsDropoffImmediately() {
        viewModel.applyIncomingDropoffLocation(48.8566, 2.3522, "Paris")

        val state = viewModel.uiState.value
        assertEquals("Paris", state.dropoff)
        assertEquals(48.8566, state.dropoffCoordinates?.first!!, 0.0000001)
        assertEquals(2.3522, state.dropoffCoordinates?.second!!, 0.0000001)
    }

    @Test
    fun testApplyIncomingDropoffLocation_withoutLabelReverseGeocodes() = runTest {
        `when`(locationRepository.reverseGeocode(48.8566, 2.3522)).thenReturn("Paris, France")

        viewModel.applyIncomingDropoffLocation(48.8566, 2.3522, null)

        val state = viewModel.uiState.value
        assertEquals("Paris, France", state.dropoff)
        assertEquals(48.8566, state.dropoffCoordinates?.first!!, 0.0000001)
    }

    @Test
    fun testApplyIncomingDropoffLocation_withAddressOnlySetsTextWithoutCoordinates() {
        viewModel.applyIncomingDropoffLocation(null, null, "Some Address")

        val state = viewModel.uiState.value
        assertEquals("Some Address", state.dropoff)
        assertNull(state.dropoffCoordinates)
    }

    @Test
    fun testUpdateDropoff_clearsPreviouslySetDropoffCoordinates() {
        viewModel.applyIncomingDropoffLocation(48.8566, 2.3522, "Paris")

        viewModel.updateDropoff("New text")

        val state = viewModel.uiState.value
        assertEquals("New text", state.dropoff)
        assertNull(state.dropoffCoordinates)
    }

    @Test
    fun testPrepareDeepLinks_reusesStoredDropoffCoordinatesWithoutReGeocoding() = runTest {
        `when`(locationRepository.geocodeAddress("Start")).thenReturn(Pair(40.7128, -74.0060))

        viewModel.updatePickup("Start")
        viewModel.applyIncomingDropoffLocation(48.8566, 2.3522, "Paris")

        var boltLink: String? = null
        viewModel.prepareDeepLinks(onSuccess = { _, bolt, _ -> boltLink = bolt })

        // If the dropoff coordinates from the deep link weren't reused, "Paris" would have
        // been re-geocoded via the mock (unstubbed -> null), forcing the fallback format.
        assertTrue(boltLink?.contains("pickup_lat=40.712800") == true)
        assertTrue(boltLink?.contains("destination_lat=48.856600") == true)
        assertTrue(boltLink?.contains("destination_lng=2.352200") == true)
    }
}
