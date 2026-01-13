package org.neteinstein.compareapp.data.repository

import android.location.Location

interface LocationRepository {
    suspend fun getCurrentLocation(): Location?
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String?
    suspend fun geocodeAddress(address: String): Pair<Double, Double>?
    fun hasLocationPermission(): Boolean
}
