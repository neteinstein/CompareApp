package org.neteinstein.compareapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
import org.neteinstein.compareapp.data.repository.UpdateCheckResult
import org.neteinstein.compareapp.data.repository.UpdateRepository
import org.neteinstein.compareapp.helpers.FakeComparisonConfigRepository
import org.neteinstein.compareapp.ui.screens.SettingsViewModel
import org.neteinstein.compareapp.utils.AppUpdateInstaller
import org.neteinstein.compareapp.utils.FoodDeliveryProvider
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [SettingsViewModel]'s two Settings-only additions: the hidden Diagnostics tap gesture
 * and the exactly-2-of-3 food provider swap logic. Update-check behavior already predates this and
 * isn't re-tested here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var configRepository: FakeComparisonConfigRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val updateRepository = Mockito.mock(UpdateRepository::class.java)
        runBlocking { `when`(updateRepository.checkForUpdate()).thenReturn(UpdateCheckResult.UpToDate("1.0.0")) }
        configRepository = FakeComparisonConfigRepository()
        val appUpdateInstaller = Mockito.mock(AppUpdateInstaller::class.java)
        viewModel = SettingsViewModel(updateRepository, appUpdateInstaller, configRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialSelectedFoodProviders_matchesRepositoryDefault() {
        assertEquals(
            listOf(FoodDeliveryProvider.UBER_EATS, FoodDeliveryProvider.BOLT_FOOD),
            viewModel.uiState.value.selectedFoodProviders
        )
    }

    @Test
    fun testOnTitleClicked_diagnosticsStayHidden_beforeTenthTap() {
        repeat(9) { viewModel.onTitleClicked() }

        assertFalse(viewModel.uiState.value.diagnosticsUnlocked)
    }

    @Test
    fun testOnTitleClicked_diagnosticsUnlock_onTenthTap() {
        repeat(10) { viewModel.onTitleClicked() }

        assertTrue(viewModel.uiState.value.diagnosticsUnlocked)
    }

    @Test
    fun testOnFoodProviderToggled_alreadySelected_isNoOp() {
        viewModel.onFoodProviderToggled(FoodDeliveryProvider.UBER_EATS)

        assertEquals(
            listOf(FoodDeliveryProvider.UBER_EATS, FoodDeliveryProvider.BOLT_FOOD),
            viewModel.uiState.value.selectedFoodProviders
        )
    }

    @Test
    fun testOnFoodProviderToggled_unselected_swapsOutOldestSelection() {
        viewModel.onFoodProviderToggled(FoodDeliveryProvider.GLOVO)

        val selected = viewModel.uiState.value.selectedFoodProviders
        assertEquals(2, selected.size)
        assertEquals(listOf(FoodDeliveryProvider.BOLT_FOOD, FoodDeliveryProvider.GLOVO), selected)
    }

    @Test
    fun testOnFoodProviderToggled_persistsToRepository() {
        viewModel.onFoodProviderToggled(FoodDeliveryProvider.GLOVO)

        assertEquals(
            setOf(FoodDeliveryProvider.BOLT_FOOD, FoodDeliveryProvider.GLOVO),
            configRepository.selectedFoodProviders.value
        )
    }

    @Test
    fun testOnFoodProviderToggled_secondSwapEvictsNewOldest() {
        viewModel.onFoodProviderToggled(FoodDeliveryProvider.GLOVO) // [BOLT_FOOD, GLOVO]
        viewModel.onFoodProviderToggled(FoodDeliveryProvider.UBER_EATS) // evicts BOLT_FOOD

        assertEquals(
            listOf(FoodDeliveryProvider.GLOVO, FoodDeliveryProvider.UBER_EATS),
            viewModel.uiState.value.selectedFoodProviders
        )
    }
}
