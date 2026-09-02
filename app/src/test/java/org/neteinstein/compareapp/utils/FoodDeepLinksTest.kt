package org.neteinstein.compareapp.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodDeepLinksTest {

    @Test
    fun testCreateSearchLink_uberEats_combinesQueryAndLocation() {
        val link = FoodDeepLinks.createSearchLink(FoodDeliveryProvider.UBER_EATS, "Sushi Place", "Downtown")

        assertTrue(link.startsWith("https://www.ubereats.com/search?q="))
        assertTrue(link.contains("Sushi+Place+Downtown") || link.contains("Sushi%20Place%20Downtown"))
    }

    @Test
    fun testCreateSearchLink_uberEats_withoutLocationUsesQueryOnly() {
        val link = FoodDeepLinks.createSearchLink(FoodDeliveryProvider.UBER_EATS, "Sushi Place", "")

        assertEquals("https://www.ubereats.com/search?q=Sushi+Place", link)
    }

    @Test
    fun testCreateSearchLink_boltFood_combinesQueryAndLocation() {
        val link = FoodDeepLinks.createSearchLink(FoodDeliveryProvider.BOLT_FOOD, "Pizza", "Lisbon")

        assertTrue(link.startsWith("https://food.bolt.eu/search?q="))
        assertTrue(link.contains("Pizza+Lisbon"))
    }

    @Test
    fun testCreateSearchLink_boltFood_withoutLocationUsesQueryOnly() {
        val link = FoodDeepLinks.createSearchLink(FoodDeliveryProvider.BOLT_FOOD, "Pizza", "  ")

        assertEquals("https://food.bolt.eu/search?q=Pizza", link)
    }

    @Test
    fun testCreateSearchLink_glovo_combinesQueryAndLocation() {
        val link = FoodDeepLinks.createSearchLink(FoodDeliveryProvider.GLOVO, "Burger", "Barcelona")

        assertTrue(link.startsWith("https://glovoapp.com/search/?query="))
        assertTrue(link.contains("Burger+Barcelona"))
    }

    @Test
    fun testCreateSearchLink_glovo_withoutLocationUsesQueryOnly() {
        val link = FoodDeepLinks.createSearchLink(FoodDeliveryProvider.GLOVO, "Burger", "")

        assertEquals("https://glovoapp.com/search/?query=Burger", link)
    }

    @Test
    fun testPackageNames_matchRealPackageNames() {
        assertEquals("com.ubercab.eats", FoodDeliveryProvider.UBER_EATS.packageName)
        assertEquals("com.bolt.deliveryclient", FoodDeliveryProvider.BOLT_FOOD.packageName)
        assertEquals("com.glovo", FoodDeliveryProvider.GLOVO.packageName)
    }
}
