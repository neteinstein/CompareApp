package org.neteinstein.compareapp.utils

import android.net.Uri
import java.net.URLDecoder

/**
 * A location extracted from an incoming deep link.
 * Either [latitude]/[longitude] or [label] (or both) will be non-null.
 */
data class DeepLinkLocation(
    val latitude: Double?,
    val longitude: Double?,
    val label: String?
)

/**
 * Parses incoming "share a location with this app" deep links.
 *
 * Supports three formats:
 * - Standard Android `geo:` URIs (e.g. produced by Google Maps "Share" -> "Open with"),
 *   in the `geo:lat,lng`, `geo:lat,lng?q=...` and `geo:0,0?q=lat,lng(Label)` shapes.
 * - The app's own `compareapp://dropoff?lat=..&lng=..&address=..` scheme for direct linking.
 * - Google Maps `https://www.google.com/maps/...` / `https://maps.google.com/...` web URLs
 *   (e.g. resolved from the maps.app.goo.gl short link Google Maps' "Share" button sends -
 *   see [org.neteinstein.compareapp.utils.MapsShareLinkResolver]).
 */
object DeepLinkLocationParser {

    private val COORDS_REGEX = Regex("""^(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)$""")
    private val LABELED_COORDS_REGEX = Regex("""^(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)\((.*)\)$""")
    private val AT_COORDS_REGEX = Regex("""/@(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)""")
    private val MAPS_QUERY_COORD_PARAMS = listOf("q", "ll", "daddr")

    fun parse(uri: Uri): DeepLinkLocation? {
        return when (uri.scheme?.lowercase()) {
            "geo" -> parseGeoUri(uri)
            "compareapp" -> parseAppUri(uri)
            "http", "https" -> parseGoogleMapsWebUri(uri)
            else -> null
        }
    }

    private fun parseGoogleMapsWebUri(uri: Uri): DeepLinkLocation? {
        val host = uri.host?.lowercase() ?: return null
        if (host != "google.com" && !host.endsWith(".google.com")) return null

        AT_COORDS_REGEX.find(uri.encodedPath.orEmpty())?.let { match ->
            val latitude = match.groupValues[1].toDouble()
            val longitude = match.groupValues[2].toDouble()
            return DeepLinkLocation(latitude, longitude, extractPlaceLabel(uri))
        }

        for (param in MAPS_QUERY_COORD_PARAMS) {
            val value = uri.getQueryParameter(param) ?: continue
            COORDS_REGEX.find(value)?.let { match ->
                val latitude = match.groupValues[1].toDouble()
                val longitude = match.groupValues[2].toDouble()
                return DeepLinkLocation(latitude, longitude, null)
            }
        }

        return null
    }

    // Google Maps "place" URLs embed the label as a path segment, e.g.
    // https://www.google.com/maps/place/Golden+Gate+Bridge/@37.81,-122.47,17z/...
    private fun extractPlaceLabel(uri: Uri): String? {
        val segments = uri.pathSegments
        val placeIndex = segments.indexOf("place")
        if (placeIndex == -1 || placeIndex + 1 >= segments.size) return null
        return segments[placeIndex + 1].replace('+', ' ').takeIf { it.isNotBlank() }
    }

    private fun parseAppUri(uri: Uri): DeepLinkLocation? {
        if (!uri.host.equals("dropoff", ignoreCase = true)) return null

        val latitude = uri.getQueryParameter("lat")?.toDoubleOrNull()
        val longitude = uri.getQueryParameter("lng")?.toDoubleOrNull()
        val address = uri.getQueryParameter("address")?.takeIf { it.isNotBlank() }

        if (latitude == null && longitude == null && address == null) return null
        return DeepLinkLocation(latitude, longitude, address)
    }

    private fun parseGeoUri(uri: Uri): DeepLinkLocation? {
        // Use the encoded (raw) form so percent-encoding and "+" (space) are still
        // intact for us to decode consistently below.
        val schemeSpecificPart = uri.encodedSchemeSpecificPart ?: return null
        val (coordsPart, queryPart) = schemeSpecificPart.split("?", limit = 2)
            .let { it[0] to it.getOrNull(1) }

        val queryValue = queryPart?.let { extractQueryParam(it, "q") }

        COORDS_REGEX.find(coordsPart)?.let { match ->
            val latitude = match.groupValues[1].toDouble()
            val longitude = match.groupValues[2].toDouble()
            if (latitude != 0.0 || longitude != 0.0) {
                val label = queryValue?.takeUnless { looksLikeCoordinates(it) }
                return DeepLinkLocation(latitude, longitude, label)
            }
        }

        val value = queryValue ?: return null

        LABELED_COORDS_REGEX.find(value)?.let { match ->
            val latitude = match.groupValues[1].toDouble()
            val longitude = match.groupValues[2].toDouble()
            val label = match.groupValues[3].takeIf { it.isNotBlank() }
            return DeepLinkLocation(latitude, longitude, label)
        }

        COORDS_REGEX.find(value)?.let { match ->
            val latitude = match.groupValues[1].toDouble()
            val longitude = match.groupValues[2].toDouble()
            return DeepLinkLocation(latitude, longitude, null)
        }

        return value.takeIf { it.isNotBlank() }?.let { DeepLinkLocation(null, null, it) }
    }

    private fun looksLikeCoordinates(value: String): Boolean =
        COORDS_REGEX.matches(value) || LABELED_COORDS_REGEX.matches(value)

    private fun extractQueryParam(queryPart: String, key: String): String? {
        return queryPart.split("&")
            .firstNotNullOfOrNull { pair ->
                val separatorIndex = pair.indexOf("=")
                if (separatorIndex == -1) return@firstNotNullOfOrNull null
                val paramKey = pair.substring(0, separatorIndex)
                if (paramKey != key) return@firstNotNullOfOrNull null
                decodeComponent(pair.substring(separatorIndex + 1))
            }
    }

    private fun decodeComponent(value: String): String {
        return try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
    }
}
