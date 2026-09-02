package org.neteinstein.compareapp.helpers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.neteinstein.compareapp.data.repository.AppRepository
import org.neteinstein.compareapp.data.repository.ComparisonConfigRepository
import org.neteinstein.compareapp.data.repository.LocationRepository
import org.neteinstein.compareapp.ui.screens.MainViewModel
import org.neteinstein.compareapp.utils.FoodDeliveryProvider

/** In-memory [ComparisonConfigRepository] for tests - avoids needing a real Context/SharedPreferences. */
class FakeComparisonConfigRepository(
    initial: Set<FoodDeliveryProvider> = setOf(FoodDeliveryProvider.UBER_EATS, FoodDeliveryProvider.BOLT_FOOD)
) : ComparisonConfigRepository {
    private val state = MutableStateFlow(initial)
    override val selectedFoodProviders: StateFlow<Set<FoodDeliveryProvider>> = state

    override fun setSelectedFoodProviders(providers: Set<FoodDeliveryProvider>) {
        require(providers.size == 2) { "Exactly 2 food delivery providers must be selected, got ${providers.size}" }
        state.value = providers
    }
}

object TestViewModelFactory {
    fun createTestViewModel(
        locationRepository: LocationRepository? = null,
        appRepository: AppRepository? = null,
        comparisonConfigRepository: ComparisonConfigRepository? = null
    ): MainViewModel {
        val mockLocationRepo = locationRepository
            ?: Mockito.mock(LocationRepository::class.java)
        // MainViewModel.init calls checkRequiredApps() and destructures the result, so a
        // caller-supplied mock must stub it; when we create the mock ourselves, give it a
        // harmless default so callers that don't care about installed-app state don't NPE.
        val mockAppRepo = appRepository ?: Mockito.mock(AppRepository::class.java).also {
            `when`(it.checkRequiredApps()).thenReturn(Pair(true, true))
        }
        val configRepo = comparisonConfigRepository ?: FakeComparisonConfigRepository()

        return MainViewModel(mockLocationRepo, mockAppRepo, configRepo)
    }
}
