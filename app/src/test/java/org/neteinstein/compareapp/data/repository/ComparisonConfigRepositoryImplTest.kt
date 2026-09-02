package org.neteinstein.compareapp.data.repository

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.neteinstein.compareapp.utils.FoodDeliveryProvider
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ComparisonConfigRepositoryImplTest {

    private fun newRepository() = ComparisonConfigRepositoryImpl(ApplicationProvider.getApplicationContext())

    @Test
    fun testSelectedFoodProviders_defaultsToUberEatsAndBoltFood() {
        val repository = newRepository()

        assertEquals(
            setOf(FoodDeliveryProvider.UBER_EATS, FoodDeliveryProvider.BOLT_FOOD),
            repository.selectedFoodProviders.value
        )
    }

    @Test
    fun testSetSelectedFoodProviders_updatesStateFlow() {
        val repository = newRepository()

        repository.setSelectedFoodProviders(setOf(FoodDeliveryProvider.BOLT_FOOD, FoodDeliveryProvider.GLOVO))

        assertEquals(
            setOf(FoodDeliveryProvider.BOLT_FOOD, FoodDeliveryProvider.GLOVO),
            repository.selectedFoodProviders.value
        )
    }

    @Test
    fun testSetSelectedFoodProviders_persistsAcrossInstances() {
        val repository = newRepository()
        repository.setSelectedFoodProviders(setOf(FoodDeliveryProvider.UBER_EATS, FoodDeliveryProvider.GLOVO))

        val reloaded = newRepository()

        assertEquals(
            setOf(FoodDeliveryProvider.UBER_EATS, FoodDeliveryProvider.GLOVO),
            reloaded.selectedFoodProviders.value
        )
    }

    @Test
    fun testSetSelectedFoodProviders_rejectsWrongCount() {
        val repository = newRepository()

        assertThrows(IllegalArgumentException::class.java) {
            repository.setSelectedFoodProviders(setOf(FoodDeliveryProvider.UBER_EATS))
        }
        assertThrows(IllegalArgumentException::class.java) {
            repository.setSelectedFoodProviders(FoodDeliveryProvider.entries.toSet())
        }
    }
}
