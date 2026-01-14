package org.neteinstein.compareapp.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.neteinstein.compareapp.data.repository.AppRepository
import org.neteinstein.compareapp.data.repository.LocationRepository
import org.neteinstein.compareapp.utils.MathUtils
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkInstalledApps()
    }

    fun checkInstalledApps() {
        val (isUberInstalled, isBoltInstalled) = appRepository.checkRequiredApps()
        _uiState.update { 
            it.copy(
                isUberInstalled = isUberInstalled,
                isBoltInstalled = isBoltInstalled
            )
        }
        Log.d("MainViewModel", "Uber installed: $isUberInstalled, Bolt installed: $isBoltInstalled")
    }

    fun updatePickup(value: String) {
        _uiState.update { 
            it.copy(
                pickup = value,
                isUsingDeviceLocation = false,
                pickupCoordinates = null
            )
        }
    }

    fun updateDropoff(value: String) {
        _uiState.update { it.copy(dropoff = value) }
    }

    fun hasLocationPermission(): Boolean {
        return locationRepository.hasLocationPermission()
    }

    fun fetchCurrentLocation(
        onLocationReceived: (latitude: Double, longitude: Double, address: String) -> Unit,
        onError: () -> Unit
    ) {
        _uiState.update { it.copy(isGettingLocation = true) }
        viewModelScope.launch {
            try {
                val location = locationRepository.getCurrentLocation()
                if (location != null) {
                    val address = locationRepository.reverseGeocode(location.latitude, location.longitude)
                        ?: "Lat: ${location.latitude}, Lng: ${location.longitude}"
                    
                    _uiState.update { 
                        it.copy(
                            pickup = address,
                            pickupCoordinates = Pair(location.latitude, location.longitude),
                            isUsingDeviceLocation = true
                        )
                    }
                    onLocationReceived(location.latitude, location.longitude, address)
                } else {
                    onError()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching location", e)
                onError()
            } finally {
                _uiState.update { it.copy(isGettingLocation = false) }
            }
        }
    }

    fun prepareDeepLinks(
        onSuccess: (uberDeepLink: String, boltDeepLink: String) -> Unit,
        onError: () -> Unit = {}
    ) {
        val currentState = _uiState.value
        if (currentState.pickup.isEmpty() || currentState.dropoff.isEmpty()) {
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val pickupCoords = currentState.pickupCoordinates 
                    ?: locationRepository.geocodeAddress(currentState.pickup)
                val dropoffCoords = locationRepository.geocodeAddress(currentState.dropoff)

                val uberDeepLink = createUberDeepLink(
                    currentState.pickup, 
                    currentState.dropoff, 
                    pickupCoords, 
                    dropoffCoords
                )
                val boltDeepLink = createBoltDeepLink(
                    currentState.pickup, 
                    currentState.dropoff, 
                    pickupCoords, 
                    dropoffCoords
                )

                onSuccess(uberDeepLink, boltDeepLink)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error preparing deep links", e)
                onError()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    internal fun createUberDeepLink(
        pickup: String,
        dropoff: String,
        pickupCoords: Pair<Double, Double>?,
        dropoffCoords: Pair<Double, Double>?
    ): String {
        val pickupEncoded = URLEncoder.encode(pickup, "UTF-8")
        val dropoffEncoded = URLEncoder.encode(dropoff, "UTF-8")
        return "uber://?action=setPickup&pickup[formatted_address]=$pickupEncoded&dropoff[formatted_address]=$dropoffEncoded"
    }

    internal fun createBoltDeepLink(
        pickup: String,
        dropoff: String,
        pickupCoords: Pair<Double, Double>?,
        dropoffCoords: Pair<Double, Double>?
    ): String {
        return if (pickupCoords != null && dropoffCoords != null) {
            val pickupLat = MathUtils.roundDecimal(pickupCoords.first)
            val pickupLng = MathUtils.roundDecimal(pickupCoords.second)
            val dropoffLat = MathUtils.roundDecimal(dropoffCoords.first)
            val dropoffLng = MathUtils.roundDecimal(dropoffCoords.second)
            "bolt://ride?pickup_lat=${pickupLat}&pickup_lng=${pickupLng}&destination_lat=${dropoffLat}&destination_lng=${dropoffLng}"
        } else {
            Log.w("MainViewModel", "Geocoding failed, using fallback Bolt deep link format")
            "bolt://ride?pickup=0&destination=0"
        }
    }
}

data class CompareUiState(
    val pickup: String = "",
    val dropoff: String = "",
    val isLoading: Boolean = false,
    val isUberInstalled: Boolean = false,
    val isBoltInstalled: Boolean = false,
    val isUsingDeviceLocation: Boolean = false,
    val isGettingLocation: Boolean = false,
    val pickupCoordinates: Pair<Double, Double>? = null
)
