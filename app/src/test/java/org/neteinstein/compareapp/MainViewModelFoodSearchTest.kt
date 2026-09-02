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
import org.neteinstein.compareapp.helpers.FakeComparisonConfigRepository
import org.neteinstein.compareapp.helpers.TestViewModelFactory
import org.neteinstein.compareapp.ui.screens.MainViewModel
import org.neteinstein.compareapp.utils.FoodDeliveryProvider
import org.neteinstein.compareapp.utils.FoodSearchMode
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for the food search side of [MainViewModel] - the query field, restaurant-vs-dish mode
 * toggle, and building search links for the currently selected pair of food providers. Unlike
 * [MainViewModel.prepareDeepLinks], this never geocodes - all these links take free-text search terms.
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
    fun testUpdateFoodQuery_updateState() {
        viewModel.updateFoodQuery("Ramen")

        val state = viewModel.uiState.value
        assertEquals("Ramen", state.foodQuery)
    }

    @Test
    fun testPrepareFoodSearchLinks_blankQueryCallsOnError() {
        viewModel.updateFoodQuery("")

        var errorCalled = false
        var successCalled = false
        viewModel.prepareFoodSearchLinks(
            onSuccess = { successCalled = true },
            onError = { errorCalled = true }
        )

        assertTrue(errorCalled)
        assertFalse(successCalled)
    }

    @Test
    fun testPrepareFoodSearchLinks_buildsLinksForBothSelectedProviders() {
        viewModel.updateFoodQuery("Tacos")

        var links: Map<FoodDeliveryProvider, String>? = null
        viewModel.prepareFoodSearchLinks(onSuccess = { links = it })

        val uberEatsLink = links?.get(FoodDeliveryProvider.UBER_EATS)
        val boltFoodLink = links?.get(FoodDeliveryProvider.BOLT_FOOD)
        assertEquals(2, links?.size)
        assertEquals("https://www.ubereats.com/search?q=Tacos", uberEatsLink)
        assertEquals("https://food.bolt.eu/search?q=Tacos", boltFoodLink)
    }

    @Test
    fun testPrepareFoodSearchLinks_respectsSelectedProviders() {
        val appRepository = Mockito.mock(AppRepository::class.java)
        `when`(appRepository.checkRequiredApps()).thenReturn(Pair(true, true))
        val configRepository = FakeComparisonConfigRepository(
            initial = setOf(FoodDeliveryProvider.BOLT_FOOD, FoodDeliveryProvider.GLOVO)
        )
        val vm = TestViewModelFactory.createTestViewModel(
            Mockito.mock(LocationRepository::class.java),
            appRepository,
            configRepository
        )
        vm.updateFoodQuery("Sushi")

        var links: Map<FoodDeliveryProvider, String>? = null
        vm.prepareFoodSearchLinks(onSuccess = { links = it })

        assertEquals(setOf(FoodDeliveryProvider.BOLT_FOOD, FoodDeliveryProvider.GLOVO), links?.keys)
    }

    @Test
    fun testCheckFoodAppsInstalled_updatesInstalledProviders() {
        val appRepository = Mockito.mock(AppRepository::class.java)
        `when`(appRepository.checkRequiredApps()).thenReturn(Pair(true, true))
        `when`(appRepository.isAppInstalled(FoodDeliveryProvider.UBER_EATS.packageName)).thenReturn(false)
        `when`(appRepository.isAppInstalled(FoodDeliveryProvider.BOLT_FOOD.packageName)).thenReturn(true)
        val vm = TestViewModelFactory.createTestViewModel(
            Mockito.mock(LocationRepository::class.java),
            appRepository
        )

        vm.checkFoodAppsInstalled()

        val state = vm.uiState.value
        assertFalse(FoodDeliveryProvider.UBER_EATS in state.installedFoodProviders)
        assertTrue(FoodDeliveryProvider.BOLT_FOOD in state.installedFoodProviders)
    }
}
