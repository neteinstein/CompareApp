package org.neteinstein.compareapp.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.neteinstein.compareapp.data.repository.AddressSuggestion
import org.neteinstein.compareapp.data.repository.AppRepository
import org.neteinstein.compareapp.data.repository.LocationRepository
import org.neteinstein.compareapp.utils.FoodDeepLinks
import org.neteinstein.compareapp.utils.FoodSearchMode
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState = _uiState.asStateFlow()

    private var pickupSuggestionsJob: Job? = null
    private var dropoffSuggestionsJob: Job? = null

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

    // Separate from checkInstalledApps() so existing tests that construct MainViewModel with a
    // custom AppRepository mock (stubbing only checkRequiredApps(), called from init) don't need
    // to also stub checkFoodApps() - callers that care about food app state opt in explicitly.
    fun checkFoodAppsInstalled() {
        val (isUberEatsInstalled, isBoltFoodInstalled) = appRepository.checkFoodApps()
        _uiState.update {
            it.copy(
                isUberEatsInstalled = isUberEatsInstalled,
                isBoltFoodInstalled = isBoltFoodInstalled
            )
        }
        Log.d("MainViewModel", "Uber Eats installed: $isUberEatsInstalled, Bolt Food installed: $isBoltFoodInstalled")
    }

    fun updatePickup(value: String) {
        _uiState.update {
            it.copy(
                pickup = value,
                isUsingDeviceLocation = false,
                pickupCoordinates = null,
                pickupSuggestions = emptyList()
            )
        }
        searchPickupSuggestions(value)
    }

    fun updateDropoff(value: String) {
        _uiState.update { it.copy(dropoff = value, dropoffCoordinates = null, dropoffSuggestions = emptyList()) }
        searchDropoffSuggestions(value)
    }

    fun selectPickupSuggestion(suggestion: AddressSuggestion) {
        pickupSuggestionsJob?.cancel()
        _uiState.update {
            it.copy(
                pickup = suggestion.fullAddress,
                pickupCoordinates = Pair(suggestion.latitude, suggestion.longitude),
                isUsingDeviceLocation = false,
                pickupSuggestions = emptyList()
            )
        }
    }

    fun selectDropoffSuggestion(suggestion: AddressSuggestion) {
        dropoffSuggestionsJob?.cancel()
        _uiState.update {
            it.copy(
                dropoff = suggestion.fullAddress,
                dropoffCoordinates = Pair(suggestion.latitude, suggestion.longitude),
                dropoffSuggestions = emptyList()
            )
        }
    }

    private fun searchPickupSuggestions(query: String) {
        pickupSuggestionsJob?.cancel()
        if (query.trim().length < MIN_SUGGESTION_QUERY_LENGTH) {
            return
        }
        pickupSuggestionsJob = viewModelScope.launch {
            delay(SUGGESTION_DEBOUNCE_MS)
            val results = locationRepository.searchAddresses(query)
            _uiState.update { it.copy(pickupSuggestions = results) }
        }
    }

    private fun searchDropoffSuggestions(query: String) {
        dropoffSuggestionsJob?.cancel()
        if (query.trim().length < MIN_SUGGESTION_QUERY_LENGTH) {
            return
        }
        dropoffSuggestionsJob = viewModelScope.launch {
            delay(SUGGESTION_DEBOUNCE_MS)
            val results = locationRepository.searchAddresses(query)
            _uiState.update { it.copy(dropoffSuggestions = results) }
        }
    }

    /**
     * Prepopulates the dropoff location from an incoming deep link (e.g. a shared
     * map location). When only coordinates are available, they're reverse geocoded
     * in the background so the field shows a readable address once ready.
     */
    fun applyIncomingDropoffLocation(latitude: Double?, longitude: Double?, label: String?) {
        val trimmedLabel = label?.trim()?.takeIf { it.isNotBlank() }

        dropoffSuggestionsJob?.cancel()
        if (latitude != null && longitude != null) {
            _uiState.update {
                it.copy(
                    dropoff = trimmedLabel ?: "Lat: $latitude, Lng: $longitude",
                    dropoffCoordinates = Pair(latitude, longitude),
                    dropoffSuggestions = emptyList()
                )
            }
            if (trimmedLabel == null) {
                viewModelScope.launch {
                    val address = locationRepository.reverseGeocode(latitude, longitude)
                    if (address != null) {
                        _uiState.update { it.copy(dropoff = address) }
                    }
                }
            }
        } else if (trimmedLabel != null) {
            _uiState.update { it.copy(dropoff = trimmedLabel, dropoffCoordinates = null, dropoffSuggestions = emptyList()) }
        }
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
                    
                    pickupSuggestionsJob?.cancel()
                    _uiState.update {
                        it.copy(
                            pickup = address,
                            pickupCoordinates = Pair(location.latitude, location.longitude),
                            isUsingDeviceLocation = true,
                            pickupSuggestions = emptyList()
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
        onSuccess: (uberDeepLink: String, boltDeepLink: String, boltDeepLinkWeb: String) -> Unit,
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
                val dropoffCoords = currentState.dropoffCoordinates
                    ?: locationRepository.geocodeAddress(currentState.dropoff)

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

                val boltDeepLinkWeb = createBoltDeepLinkWeb(
                    currentState.pickup,
                    currentState.dropoff,
                    pickupCoords,
                    dropoffCoords
                )

                onSuccess(uberDeepLink, boltDeepLink, boltDeepLinkWeb)
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

    /**
     * Formats a coordinate value to exactly 6 decimal places using US locale.
     * This ensures consistent formatting for Bolt's API compatibility.
     */
    private fun formatCoordinate(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }

    // Opens the Bolt app via its custom scheme; setting the destination requires
    // following up with the HTTPS link from createBoltDeepLinkWeb once the app is open.
    internal fun createBoltDeepLink(
        pickup: String,
        dropoff: String,
        pickupCoords: Pair<Double, Double>?,
        dropoffCoords: Pair<Double, Double>?
    ): String {
        Log.d("MainViewModel", "pickup: $pickup, dropoff: $dropoff, pickupCoords: $pickupCoords, dropoffCoords: $dropoffCoords")
        return if (pickupCoords != null && dropoffCoords != null) {

            val pickupLat = formatCoordinate(pickupCoords.first)
            val pickupLng = formatCoordinate(pickupCoords.second)
            val destLat = formatCoordinate(dropoffCoords.first)
            val destLng = formatCoordinate(dropoffCoords.second)

            "bolt://ride?pickup_lat=$pickupLat&pickup_lng=$pickupLng&destination_lat=$destLat&destination_lng=$destLng"
        } else {
            Log.w("MainViewModel", "Geocoding failed, using fallback Bolt deep link format")
            val pickupEncoded = URLEncoder.encode(pickup, "UTF-8")
            val dropoffEncoded = URLEncoder.encode(dropoff, "UTF-8")
            "bolt://ride?pickup=$pickupEncoded&destination=$dropoffEncoded"
        }
    }

    // This deep link should only be triggered when the app is already open,
    // or it will open the web browser. However, it will set the destination properly.
    internal fun createBoltDeepLinkWeb(
        pickup: String,
        dropoff: String,
        pickupCoords: Pair<Double, Double>?,
        dropoffCoords: Pair<Double, Double>?
    ): String {
        Log.d("MainViewModel", "pickup: $pickup, dropoff: $dropoff, pickupCoords: $pickupCoords, dropoffCoords: $dropoffCoords")
        return if (pickupCoords != null && dropoffCoords != null) {
          
            val pickupLat = formatCoordinate(pickupCoords.first)
            val pickupLng = formatCoordinate(pickupCoords.second)
            val destLat = formatCoordinate(dropoffCoords.first)
            val destLng = formatCoordinate(dropoffCoords.second)

            "https://bolt.eu/ride/?pickup_lat=$pickupLat&pickup_lng=$pickupLng&destination_lat=$destLat&destination_lng=$destLng"
        } else {
            Log.w("MainViewModel", "Geocoding failed, using fallback Bolt web link format")
            val pickupEncoded = URLEncoder.encode(pickup, "UTF-8")
            val dropoffEncoded = URLEncoder.encode(dropoff, "UTF-8")
            "https://bolt.eu/ride/?pickup=$pickupEncoded&destination=$dropoffEncoded"
        }
    }

    fun updateFoodQuery(value: String) {
        _uiState.update { it.copy(foodQuery = value) }
    }

    fun updateFoodLocation(value: String) {
        _uiState.update { it.copy(foodLocation = value) }
    }

    fun setFoodSearchMode(mode: FoodSearchMode) {
        _uiState.update { it.copy(foodSearchMode = mode) }
    }

    /**
     * Builds the Uber Eats / Bolt Food search links from the current query + location - see
     * [FoodDeepLinks] for why these are unverified best-effort guesses rather than a confirmed
     * format. No geocoding needed here (unlike [prepareDeepLinks]): both links take free-text
     * search terms, not coordinates.
     */
    fun prepareFoodSearchLinks(
        onSuccess: (uberEatsLink: String, boltFoodLink: String) -> Unit,
        onError: () -> Unit = {}
    ) {
        val currentState = _uiState.value
        if (currentState.foodQuery.isBlank()) {
            onError()
            return
        }

        val uberEatsLink = FoodDeepLinks.createUberEatsSearchLink(currentState.foodQuery, currentState.foodLocation)
        val boltFoodLink = FoodDeepLinks.createBoltFoodSearchLink(currentState.foodQuery, currentState.foodLocation)
        onSuccess(uberEatsLink, boltFoodLink)
    }

    companion object {
        private const val MIN_SUGGESTION_QUERY_LENGTH = 3
        private const val SUGGESTION_DEBOUNCE_MS = 300L
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
    val pickupCoordinates: Pair<Double, Double>? = null,
    val dropoffCoordinates: Pair<Double, Double>? = null,
    val pickupSuggestions: List<AddressSuggestion> = emptyList(),
    val dropoffSuggestions: List<AddressSuggestion> = emptyList(),
    val foodQuery: String = "",
    val foodLocation: String = "",
    val foodSearchMode: FoodSearchMode = FoodSearchMode.RESTAURANT,
    val isUberEatsInstalled: Boolean = false,
    val isBoltFoodInstalled: Boolean = false
)
