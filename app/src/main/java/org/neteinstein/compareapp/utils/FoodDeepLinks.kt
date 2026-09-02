package org.neteinstein.compareapp.utils

import java.net.URLEncoder

/**
 * What to search for on each food app: a restaurant/place name, or a specific dish.
 * Both modes hit the same search endpoint - only the query text differs - since neither
 * app's search exposes a separate "restaurant vs dish" deep-link parameter.
 */
enum class FoodSearchMode {
    RESTAURANT,
    DISH
}

/**
 * Best-effort search deep links for Uber Eats and Bolt Food.
 *
 * Neither company publishes a documented deep-link API for search (same situation as Bolt's ride
 * app - see [BoltDeepLinkCandidates]), so these are unverified: they're built from Uber Eats'
 * public website URL structure and from Bolt Food's own AndroidManifest.xml
 * (`com.bolt.deliveryclient`), which declares a verified `https://food.bolt.eu` App Link with a
 * `/search` path. Both links target the app's package explicitly so they open the app directly
 * rather than a browser tab (same trick as [org.neteinstein.compareapp.openBoltWebLink]), but
 * whether the app actually pre-fills the search box from the `q` param hasn't been confirmed on
 * a device - if it doesn't, the app still opens, just not pre-searched.
 */
object FoodDeepLinks {

    const val UBER_EATS_PACKAGE = "com.ubercab.eats"
    const val BOLT_FOOD_PACKAGE = "com.bolt.deliveryclient"

    fun createUberEatsSearchLink(query: String, location: String): String {
        val encoded = URLEncoder.encode(combinedQuery(query, location), "UTF-8")
        return "https://www.ubereats.com/search?q=$encoded"
    }

    fun createBoltFoodSearchLink(query: String, location: String): String {
        val encoded = URLEncoder.encode(combinedQuery(query, location), "UTF-8")
        return "https://food.bolt.eu/search?q=$encoded"
    }

    private fun combinedQuery(query: String, location: String): String {
        val trimmedQuery = query.trim()
        val trimmedLocation = location.trim()
        return if (trimmedLocation.isEmpty()) trimmedQuery else "$trimmedQuery $trimmedLocation"
    }
}
