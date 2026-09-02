package org.neteinstein.compareapp.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodDeepLinksTest {

    @Test
    fun testCreateUberEatsSearchLink_combinesQueryAndLocation() {
        val link = FoodDeepLinks.createUberEatsSearchLink("Sushi Place", "Downtown")

        assertTrue(link.startsWith("https://www.ubereats.com/search?q="))
        assertTrue(link.contains("Sushi+Place+Downtown") || link.contains("Sushi%20Place%20Downtown"))
    }

    @Test
    fun testCreateUberEatsSearchLink_withoutLocationUsesQueryOnly() {
        val link = FoodDeepLinks.createUberEatsSearchLink("Sushi Place", "")

        assertEquals("https://www.ubereats.com/search?q=Sushi+Place", link)
    }

    @Test
    fun testCreateBoltFoodSearchLink_combinesQueryAndLocation() {
        val link = FoodDeepLinks.createBoltFoodSearchLink("Pizza", "Lisbon")

        assertTrue(link.startsWith("https://food.bolt.eu/search?q="))
        assertTrue(link.contains("Pizza+Lisbon"))
    }

    @Test
    fun testCreateBoltFoodSearchLink_withoutLocationUsesQueryOnly() {
        val link = FoodDeepLinks.createBoltFoodSearchLink("Pizza", "  ")

        assertEquals("https://food.bolt.eu/search?q=Pizza", link)
    }

    @Test
    fun testPackageConstants_matchRealPackageNames() {
        assertEquals("com.ubercab.eats", FoodDeepLinks.UBER_EATS_PACKAGE)
        assertEquals("com.bolt.deliveryclient", FoodDeepLinks.BOLT_FOOD_PACKAGE)
    }
}
