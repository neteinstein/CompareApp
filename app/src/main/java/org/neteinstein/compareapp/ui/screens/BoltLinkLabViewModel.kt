package org.neteinstein.compareapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.neteinstein.compareapp.data.repository.LocationRepository
import org.neteinstein.compareapp.utils.BoltDeepLinkCandidates
import org.neteinstein.compareapp.utils.BoltLinkCandidate
import javax.inject.Inject

data class BoltLinkLabUiState(
    val pickup: String = "",
    val dropoff: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val candidates: List<BoltLinkCandidate> = emptyList()
)

/**
 * Backs [BoltLinkLabScreen]: geocodes the two addresses typed in, then hands them to
 * [BoltDeepLinkCandidates] to build the list of guessed Bolt deep-link formats to try on-device.
 * See [org.neteinstein.compareapp.utils.BoltLinkCandidate] for why this exists at all.
 */
@HiltViewModel
class BoltLinkLabViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoltLinkLabUiState())
    val uiState = _uiState.asStateFlow()

    fun updatePickup(value: String) {
        _uiState.update { it.copy(pickup = value) }
    }

    fun updateDropoff(value: String) {
        _uiState.update { it.copy(dropoff = value) }
    }

    fun buildCandidates() {
        val currentState = _uiState.value
        if (currentState.pickup.isBlank() || currentState.dropoff.isBlank()) {
            _uiState.update { it.copy(error = "Enter both a pickup and a dropoff address first") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, candidates = emptyList()) }
        viewModelScope.launch {
            val pickupCoords = locationRepository.geocodeAddress(currentState.pickup)
            val dropoffCoords = locationRepository.geocodeAddress(currentState.dropoff)

            if (pickupCoords == null || dropoffCoords == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Could not geocode one of the addresses")
                }
                return@launch
            }

            val candidates = BoltDeepLinkCandidates.build(
                pickupLat = pickupCoords.first,
                pickupLng = pickupCoords.second,
                destLat = dropoffCoords.first,
                destLng = dropoffCoords.second
            )
            _uiState.update { it.copy(isLoading = false, candidates = candidates) }
        }
    }
}
