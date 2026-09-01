package org.neteinstein.compareapp.utils

import java.util.Locale

/**
 * One guess at Bolt's undocumented rider deep-link contract: a human-readable [label] plus the
 * `bolt://` scheme URI (nullable - some candidates only make sense as a web link) and the
 * `https://` web URI to try. Bolt has no published deep-link API, so every candidate here is
 * unverified; [BoltLinkLabScreen][org.neteinstein.compareapp.ui.screens.BoltLinkLabScreen] exists
 * to let a person try each one against the real Bolt app and report back which (if any) actually
 * sets the destination, so the working one can replace the guess in [BoltDeepLinkCandidates.build].
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
                label = "destination_lat/destination_lng on bolt.eu/ride/ (current app behavior)",
                nativeUri = "bolt://ride?pickup_lat=$pLat&pickup_lng=$pLng&destination_lat=$dLat&destination_lng=$dLng",
                webUri = "https://bolt.eu/ride/?pickup_lat=$pLat&pickup_lng=$pLng&destination_lat=$dLat&destination_lng=$dLng"
            ),
            BoltLinkCandidate(
                id = 2,
                label = "dropoff_lat/dropoff_lng instead of destination_lat/destination_lng",
                nativeUri = "bolt://ride?pickup_lat=$pLat&pickup_lng=$pLng&dropoff_lat=$dLat&dropoff_lng=$dLng",
                webUri = "https://bolt.eu/ride/?pickup_lat=$pLat&pickup_lng=$pLng&dropoff_lat=$dLat&dropoff_lng=$dLng"
            ),
            BoltLinkCandidate(
                id = 3,
                label = "ride.bolt.eu host instead of bolt.eu/ride/",
                nativeUri = null,
                webUri = "https://ride.bolt.eu/?pickup_lat=$pLat&pickup_lng=$pLng&dropoff_lat=$dLat&dropoff_lng=$dLng"
            ),
            BoltLinkCandidate(
                id = 4,
                label = "bracket-style params (pickup[lat], dropoff[lat], ...)",
                nativeUri = "bolt://ride?pickup%5Blat%5D=$pLat&pickup%5Blng%5D=$pLng&dropoff%5Blat%5D=$dLat&dropoff%5Blng%5D=$dLng",
                webUri = "https://bolt.eu/ride/?pickup%5Blat%5D=$pLat&pickup%5Blng%5D=$pLng&dropoff%5Blat%5D=$dLat&dropoff%5Blng%5D=$dLng"
            ),
            BoltLinkCandidate(
                id = 5,
                label = "comma-joined lat,lng pair under pickup/dropoff",
                nativeUri = "bolt://ride?pickup=$pLat,$pLng&dropoff=$dLat,$dLng",
                webUri = "https://bolt.eu/ride/?pickup=$pLat,$pLng&dropoff=$dLat,$dLng"
            )
        )
    }
}
