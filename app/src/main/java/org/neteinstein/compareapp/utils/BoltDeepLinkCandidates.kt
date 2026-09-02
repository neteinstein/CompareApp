package org.neteinstein.compareapp.utils

import java.util.Locale

/**
 * One guess at Bolt's undocumented rider deep-link contract: a human-readable [label] plus the
 * `bolt://` scheme URI (nullable - some candidates only make sense as a web link) and the
 * `https://` web URI to try.
 *
 * The **host** is no longer a guess: `ee.mtakso.client`'s own shipped AndroidManifest.xml
 * declares an `autoVerify="true"` intent-filter for `bolt://action` (and `taxify://action`)
 * specifically on `DeeplinkActivity`, while any other host - including the `ride` host every
 * candidate here used before - only matches a generic catch-all filter with no host constraint,
 * meaning it opens the app but can't reach whatever internal logic reads deep-link params. The
 * manifest also confirms `bolt.eu` is not a declared App Link host at all (only
 * `scooters.taxify.eu`, `bolt.sng.link`, and `maps.google.com` are), so the `https://bolt.eu/...`
 * web links below can never resolve inside the app itself - they're kept only for direct
 * comparison against the corrected `action`-host native candidates.
 *
 * What's still an unverified guess is the **query-param contract** `DeeplinkActivity` reads once
 * it's on the `action` host - param names, and whether it expects an explicit action-type
 * parameter at all. [BoltLinkLabScreen][org.neteinstein.compareapp.ui.screens.BoltLinkLabScreen]
 * exists to let a person try each candidate against the real Bolt app and report back which (if
 * any) actually sets the destination, so the working one can replace the guess in
 * [BoltDeepLinkCandidates.build].
 */
data class BoltLinkCandidate(
    val id: Int,
    val label: String,
    val nativeUri: String?,
    val webUri: String
)

object BoltDeepLinkCandidates {

    private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

    fun build(
        pickupLat: Double,
        pickupLng: Double,
        destLat: Double,
        destLng: Double
    ): List<BoltLinkCandidate> {
        val pLat = formatCoordinate(pickupLat)
        val pLng = formatCoordinate(pickupLng)
        val dLat = formatCoordinate(destLat)
        val dLng = formatCoordinate(destLng)

        return listOf(
            BoltLinkCandidate(
                id = 1,
                label = "action host (verified) + destination_lat/destination_lng - current app behavior",
                nativeUri = "bolt://action?pickup_lat=$pLat&pickup_lng=$pLng&destination_lat=$dLat&destination_lng=$dLng",
                webUri = "https://bolt.eu/ride/?pickup_lat=$pLat&pickup_lng=$pLng&destination_lat=$dLat&destination_lng=$dLng"
            ),
            BoltLinkCandidate(
                id = 2,
                label = "action host + dropoff_lat/dropoff_lng instead of destination_lat/destination_lng",
                nativeUri = "bolt://action?pickup_lat=$pLat&pickup_lng=$pLng&dropoff_lat=$dLat&dropoff_lng=$dLng",
                webUri = "https://bolt.eu/ride/?pickup_lat=$pLat&pickup_lng=$pLng&dropoff_lat=$dLat&dropoff_lng=$dLng"
            ),
            BoltLinkCandidate(
                id = 3,
                label = "action host + explicit action_type=order_ride param",
                nativeUri = "bolt://action?action_type=order_ride&pickup_lat=$pLat&pickup_lng=$pLng&destination_lat=$dLat&destination_lng=$dLng",
                webUri = "https://bolt.eu/ride/?action_type=order_ride&pickup_lat=$pLat&pickup_lng=$pLng&destination_lat=$dLat&destination_lng=$dLng"
            ),
            BoltLinkCandidate(
                id = 4,
                label = "action host + explicit type=ride param",
                nativeUri = "bolt://action?type=ride&pickup_lat=$pLat&pickup_lng=$pLng&destination_lat=$dLat&destination_lng=$dLng",
                webUri = "https://bolt.eu/ride/?type=ride&pickup_lat=$pLat&pickup_lng=$pLng&destination_lat=$dLat&destination_lng=$dLng"
            ),
            BoltLinkCandidate(
                id = 5,
                label = "action host + bracket-style params (pickup[lat], dropoff[lat], ...)",
                nativeUri = "bolt://action?pickup%5Blat%5D=$pLat&pickup%5Blng%5D=$pLng&dropoff%5Blat%5D=$dLat&dropoff%5Blng%5D=$dLng",
                webUri = "https://bolt.eu/ride/?pickup%5Blat%5D=$pLat&pickup%5Blng%5D=$pLng&dropoff%5Blat%5D=$dLat&dropoff%5Blng%5D=$dLng"
            ),
            BoltLinkCandidate(
                id = 6,
                label = "action host + comma-joined lat,lng pair under pickup/dropoff",
                nativeUri = "bolt://action?pickup=$pLat,$pLng&dropoff=$dLat,$dLng",
                webUri = "https://bolt.eu/ride/?pickup=$pLat,$pLng&dropoff=$dLat,$dLng"
            ),
            BoltLinkCandidate(
                id = 7,
                label = "ride host (baseline, NOT declared in Bolt's manifest - kept for comparison only)",
                nativeUri = "bolt://ride?pickup_lat=$pLat&pickup_lng=$pLng&destination_lat=$dLat&destination_lng=$dLng",
                webUri = "https://bolt.eu/ride/?pickup_lat=$pLat&pickup_lng=$pLng&destination_lat=$dLat&destination_lng=$dLng"
            )
        )
    }
}
