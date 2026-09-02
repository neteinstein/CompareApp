package org.neteinstein.compareapp.utils

import java.net.URLEncoder

/**
 * What to search for on each food app: a restaurant/place name, or a specific dish.
 * Both modes hit the same search endpoint - only the query text differs - since none of these
 * apps' search exposes a separate "restaurant vs dish" deep-link parameter.
 */
enum class FoodSearchMode {
    RESTAURANT,
    DISH
}

/**
 * Best-effort search deep links for the food delivery apps in [FoodDeliveryProvider].
 *
 * None of these companies publish a documented deep-link API for search (same situation as
 * Bolt's ride app - see [BoltDeepLinkCandidates]), so these are unverified: Uber Eats' and Bolt
 * Food's are built from Uber Eats' public website URL structure and from Bolt Food's own
 * AndroidManifest.xml (`com.bolt.deliveryclient`), which declares a verified `https://food.bolt.eu`
 * App Link with a `/search` path. Glovo's `com.glovo` manifest confirms the `glovoapp.com` host
 * (likely - see docs/DEEP_LINKS.md) routes to the app at all, but - unlike Bolt Food - declares no
 * path constraint at all, so whether `/search/?query=` specifically is recognized by the app's
 * internal router is still unverified.
 * All three links target the app's package explicitly so they open the app directly rather than a
 * browser tab (same trick as [org.neteinstein.compareapp.openBoltWebLink]), but whether the app
 * actually pre-fills the search box from the query param hasn't been confirmed on a device - if it
 * doesn't, the app still opens, just not pre-searched.
 */
object FoodDeepLinks {

    fun createSearchLink(provider: FoodDeliveryProvider, query: String): String {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        return when (provider) {
            FoodDeliveryProvider.UBER_EATS -> "https://www.ubereats.com/search?q=$encoded"
            FoodDeliveryProvider.BOLT_FOOD -> "https://food.bolt.eu/search?q=$encoded"
            FoodDeliveryProvider.GLOVO -> "https://glovoapp.com/search/?query=$encoded"
        }
    }
}
