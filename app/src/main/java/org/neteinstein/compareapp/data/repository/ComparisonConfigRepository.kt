package org.neteinstein.compareapp.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.neteinstein.compareapp.utils.FoodDeliveryProvider

/**
 * Persists which pair of food delivery apps (see [FoodDeliveryProvider]) "Search Food" compares -
 * set from Settings > Comparison configuration. Always exactly 2 providers; enforced by
 * [setSelectedFoodProviders] rather than left to callers.
 */
interface ComparisonConfigRepository {
    val selectedFoodProviders: StateFlow<Set<FoodDeliveryProvider>>

    fun setSelectedFoodProviders(providers: Set<FoodDeliveryProvider>)
}
