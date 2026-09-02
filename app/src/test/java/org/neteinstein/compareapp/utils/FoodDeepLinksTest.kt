package org.neteinstein.compareapp.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodDeepLinksTest {

    @Test
    fun testCreateSearchLink_uberEats() {
        val link = FoodDeepLinks.createSearchLink(FoodDeliveryProvider.UBER_EATS, "Sushi Place")

        assertEquals("https://www.ubereats.com/search?q=Sushi+Place", link)
    }

    @Test
    fun testCreateSearchLink_boltFood() {
        val link = FoodDeepLinks.createSearchLink(FoodDeliveryProvider.BOLT_FOOD, "Pizza")

        assertEquals("https://food.bolt.eu/search?q=Pizza", link)
    }

    @Test
    fun testCreateSearchLink_glovo() {
        val link = FoodDeepLinks.createSearchLink(FoodDeliveryProvider.GLOVO, "Burger")

        assertEquals("https://glovoapp.com/search/?query=Burger", link)
    }

    @Test
    fun testPackageNames_matchRealPackageNames() {
        assertEquals("com.ubercab.eats", FoodDeliveryProvider.UBER_EATS.packageName)
        assertEquals("com.bolt.deliveryclient", FoodDeliveryProvider.BOLT_FOOD.packageName)
        assertEquals("com.glovo", FoodDeliveryProvider.GLOVO.packageName)
    }
}
