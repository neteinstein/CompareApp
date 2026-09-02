package org.neteinstein.compareapp.utils

/**
 * The food delivery apps this app knows how to search across. Exactly two of these are active at
 * once - the user picks which pair under Settings > Comparison configuration (see
 * [org.neteinstein.compareapp.data.repository.ComparisonConfigRepository]) - and that pair is what
 * [FoodDeepLinks] builds search links for and [org.neteinstein.compareapp.MainActivity.openFoodSearch]
 * opens when "Search Food" is tapped.
 *
 * [displayName] is used only in unlocalized UI copy (warning banners, error toasts) - same
 * convention as the hardcoded "Uber Eats"/"Bolt Food" strings this replaces in `CompareScreen.kt`.
 */
enum class FoodDeliveryProvider(val packageName: String, val displayName: String) {
    UBER_EATS("com.ubercab.eats", "Uber Eats"),
    BOLT_FOOD("com.bolt.deliveryclient", "Bolt Food"),

    // Package name confirmed via the Play Store listing ("Glovo: Food & Grocery Delivery",
    // id=com.glovo). See docs/DEEP_LINKS.md #5 for what is/isn't confirmed about its deep links.
    GLOVO("com.glovo", "Glovo")
}
