package org.neteinstein.compareapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
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
import org.neteinstein.compareapp.utils.FoodSearchMode
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for the food search (Uber Eats / Bolt Food) side of [MainViewModel] - the query/location
 * fields, restaurant-vs-dish mode toggle, and building the two search links. Unlike
 * [MainViewModel.prepareDeepLinks], this never geocodes - both links take free-text search terms.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelFoodSearchTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val appRepository = Mockito.mock(AppRepository::class.java)
        `when`(appRepository.checkRequiredApps()).thenReturn(Pair(true, true))
        viewModel = TestViewModelFactory.createTestViewModel(
            Mockito.mock(LocationRepository::class.java),
            appRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDefaultFoodSearchMode_isRestaurant() {
        assertEquals(FoodSearchMode.RESTAURANT, viewModel.uiState.value.foodSearchMode)
    }

    @Test
    fun testSetFoodSearchMode_updatesState() {
        viewModel.setFoodSearchMode(FoodSearchMode.DISH)

        assertEquals(FoodSearchMode.DISH, viewModel.uiState.value.foodSearchMode)
    }

    @Test
    fun testUpdateFoodQueryAndLocation_updateState() {
        viewModel.updateFoodQuery("Ramen")
        viewModel.updateFoodLocation("Porto")

        val state = viewModel.uiState.value
        assertEquals("Ramen", state.foodQuery)
        assertEquals("Porto", state.foodLocation)
    }

    @Test
    fun testPrepareFoodSearchLinks_blankQueryCallsOnError() {
        viewModel.updateFoodQuery("")

        var errorCalled = false
        var successCalled = false
        viewModel.prepareFoodSearchLinks(
            onSuccess = { _, _ -> successCalled = true },
            onError = { errorCalled = true }
        )

        assertTrue(errorCalled)
        assertFalse(successCalled)
    }

    @Test
    fun testPrepareFoodSearchLinks_buildsBothLinksFromQueryAndLocation() {
        viewModel.updateFoodQuery("Tacos")
        viewModel.updateFoodLocation("Madrid")

        var uberEatsLink: String? = null
        var boltFoodLink: String? = null
        viewModel.prepareFoodSearchLinks(
            onSuccess = { uber, bolt ->
                uberEatsLink = uber
                boltFoodLink = bolt
            }
        )

        assertTrue(uberEatsLink?.startsWith("https://www.ubereats.com/search?q=") == true)
        assertTrue(uberEatsLink?.contains("Tacos+Madrid") == true)
        assertTrue(boltFoodLink?.startsWith("https://food.bolt.eu/search?q=") == true)
        assertTrue(boltFoodLink?.contains("Tacos+Madrid") == true)
    }

    @Test
    fun testCheckFoodAppsInstalled_updatesInstalledFlags() {
        val appRepository = Mockito.mock(AppRepository::class.java)
        `when`(appRepository.checkRequiredApps()).thenReturn(Pair(true, true))
        `when`(appRepository.checkFoodApps()).thenReturn(Pair(false, true))
        val vm = TestViewModelFactory.createTestViewModel(
            Mockito.mock(LocationRepository::class.java),
            appRepository
        )

        vm.checkFoodAppsInstalled()

        val state = vm.uiState.value
        assertFalse(state.isUberEatsInstalled)
        assertTrue(state.isBoltFoodInstalled)
    }
}
