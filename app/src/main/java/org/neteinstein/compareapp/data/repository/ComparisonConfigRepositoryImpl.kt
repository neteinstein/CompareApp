package org.neteinstein.compareapp.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.neteinstein.compareapp.utils.FoodDeliveryProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComparisonConfigRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : ComparisonConfigRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _selectedFoodProviders = MutableStateFlow(loadSelectedFoodProviders())
    override val selectedFoodProviders: StateFlow<Set<FoodDeliveryProvider>> = _selectedFoodProviders.asStateFlow()

    override fun setSelectedFoodProviders(providers: Set<FoodDeliveryProvider>) {
        require(providers.size == 2) {
            "Exactly 2 food delivery providers must be selected, got ${providers.size}: $providers"
        }
        prefs.edit()
            .putStringSet(KEY_FOOD_PROVIDERS, providers.mapTo(mutableSetOf()) { it.name })
            .apply()
        _selectedFoodProviders.value = providers
    }

    private fun loadSelectedFoodProviders(): Set<FoodDeliveryProvider> {
        val storedNames = prefs.getStringSet(KEY_FOOD_PROVIDERS, null) ?: return DEFAULT_FOOD_PROVIDERS
        val parsed = storedNames.mapNotNullTo(mutableSetOf()) { name ->
            FoodDeliveryProvider.entries.find { it.name == name }
        }
        // Falls back to the default pair if prefs are missing, corrupted, or (after an app update
        // that removes a provider) no longer resolve to exactly 2 valid entries.
        return if (parsed.size == 2) parsed else DEFAULT_FOOD_PROVIDERS
    }

    private companion object {
        const val PREFS_NAME = "comparison_config"
        const val KEY_FOOD_PROVIDERS = "selected_food_providers"
        val DEFAULT_FOOD_PROVIDERS = setOf(FoodDeliveryProvider.UBER_EATS, FoodDeliveryProvider.BOLT_FOOD)
    }
}
