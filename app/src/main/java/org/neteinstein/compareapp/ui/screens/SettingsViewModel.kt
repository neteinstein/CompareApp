package org.neteinstein.compareapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.neteinstein.compareapp.data.repository.AppUpdate
import org.neteinstein.compareapp.data.repository.UpdateCheckResult
import org.neteinstein.compareapp.data.repository.UpdateRepository
import org.neteinstein.compareapp.utils.AppUpdateInstaller
import javax.inject.Inject

/**
 * Drives the "Update to latest" button and its status text on [SettingsScreen] - checks GitHub
 * Releases and, if allowed, downloads and installs a newer build.
 */
sealed class UpdateStatus {
    /** Nothing in flight - the button's normal resting state. */
    data object Idle : UpdateStatus()

    data object Checking : UpdateStatus()

    /** The installed build is already the latest one published on GitHub Releases. */
    data class UpToDate(val currentVersionName: String) : UpdateStatus()

    /** A newer build exists and is ready to be downloaded/installed on button tap. */
    data class UpdateAvailable(val update: AppUpdate) : UpdateStatus()

    data object Downloading : UpdateStatus()

    /**
     * A newer release exists, but the OS won't let this app install it yet - see
     * [AppUpdateInstaller.canInstallPackages]. [SettingsScreen] shows a warning banner whose
     * action opens the system "install unknown apps" page for this app
     * ([SettingsViewModel.onEnableSideloadingClicked]); the user is expected to tap
     * "Update to latest" again afterwards, which re-checks and proceeds automatically now that
     * the OS allows it.
     */
    data object SideloadingBlocked : UpdateStatus()

    data class Failed(val message: String) : UpdateStatus()
}

data class SettingsUiState(
    val updateStatus: UpdateStatus = UpdateStatus.Idle
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    private val appUpdateInstaller: AppUpdateInstaller
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Runs a background update check when Settings is entered so the "Update to latest" button can
     * reflect availability immediately (green when a release is found), without auto-downloading.
     */
    fun onScreenEntered() {
        viewModelScope.launch {
            when (val result = updateRepository.checkForUpdate()) {
                is UpdateCheckResult.UpToDate ->
                    _uiState.value = _uiState.value.copy(updateStatus = UpdateStatus.UpToDate(result.currentVersionName))
                is UpdateCheckResult.UpdateAvailable ->
                    _uiState.value = _uiState.value.copy(updateStatus = UpdateStatus.UpdateAvailable(result.update))
                is UpdateCheckResult.Failed -> Unit
            }
        }
    }

    /**
     * Checks GitHub Releases and, if a newer build exists, downloads it and launches the system
     * installer - unless [AppUpdateInstaller.canInstallPackages] says the OS will block that
     * install outright, in which case this stops at [UpdateStatus.SideloadingBlocked] without
     * downloading anything.
     */
    fun onUpdateClicked() {
        val availableUpdate = (_uiState.value.updateStatus as? UpdateStatus.UpdateAvailable)?.update
        if (availableUpdate != null) {
            viewModelScope.launch { downloadAndInstall(availableUpdate) }
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updateStatus = UpdateStatus.Checking)
            when (val result = updateRepository.checkForUpdate()) {
                is UpdateCheckResult.UpToDate ->
                    _uiState.value = _uiState.value.copy(updateStatus = UpdateStatus.UpToDate(result.currentVersionName))
                is UpdateCheckResult.UpdateAvailable -> downloadAndInstall(result.update)
                is UpdateCheckResult.Failed ->
                    _uiState.value = _uiState.value.copy(updateStatus = UpdateStatus.Failed(result.message))
            }
        }
    }

    /** Deep-links to the system "install unknown apps" settings page for this app. */
    fun onEnableSideloadingClicked() {
        appUpdateInstaller.openInstallPermissionSettings()
    }

    private suspend fun downloadAndInstall(update: AppUpdate) {
        if (!appUpdateInstaller.canInstallPackages()) {
            _uiState.value = _uiState.value.copy(updateStatus = UpdateStatus.SideloadingBlocked)
            return
        }

        _uiState.value = _uiState.value.copy(updateStatus = UpdateStatus.Downloading)
        val apkFile = updateRepository.downloadUpdate(update)
        if (apkFile != null) {
            // Fire the installer intent while updateStatus is still Downloading (not Idle) so
            // SettingsScreen's keep-screen-on effect - tied to Checking/Downloading - is still
            // active for this exact call: that intent is a foreground-only activity start,
            // silently dropped by the OS if the screen has already locked by now.
            appUpdateInstaller.installPackage(apkFile)
            _uiState.value = _uiState.value.copy(updateStatus = UpdateStatus.Idle)
        } else {
            _uiState.value = _uiState.value.copy(updateStatus = UpdateStatus.Failed("Download failed"))
        }
    }
}
