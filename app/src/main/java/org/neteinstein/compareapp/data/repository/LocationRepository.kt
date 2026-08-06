package org.neteinstein.compareapp.data.repository

import android.location.Location

data class AddressSuggestion(
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double
)

interface LocationRepository {
    suspend fun getCurrentLocation(): Location?
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String?
    suspend fun geocodeAddress(address: String): Pair<Double, Double>?
    suspend fun searchAddresses(query: String): List<AddressSuggestion>
    fun hasLocationPermission(): Boolean
}
