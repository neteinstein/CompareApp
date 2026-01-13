package org.neteinstein.compareapp.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.neteinstein.compareapp.R

@Composable
fun CompareScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onOpenDeepLinks: (uberDeepLink: String, boltDeepLink: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                     permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            viewModel.fetchCurrentLocation(
                onLocationReceived = { _, _, _ -> },
                onError = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.location_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.location_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Check app installation status when screen is created and when it resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkInstalledApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val areBothAppsInstalled = uiState.isUberInstalled && uiState.isBoltInstalled
    val warningMessage = if (uiState.isUberInstalled && uiState.isBoltInstalled) {
        null
    } else {
        val missingApps = buildList {
            if (!uiState.isUberInstalled) add("Uber")
            if (!uiState.isBoltInstalled) add("Bolt")
        }
        "Warning: ${missingApps.joinToString(" and ")} ${if (missingApps.size == 1) "app is" else "apps are"} required for this to work"
    }

    val loadingText = stringResource(R.string.loading)
    val compareText = stringResource(R.string.compare)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Warning label when apps are not installed
            warningMessage?.let { message ->
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } ?: Spacer(modifier = Modifier.padding(bottom = 16.dp))

            // Pickup location with location button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.pickup,
                    onValueChange = { viewModel.updatePickup(it) },
                    label = { Text(stringResource(R.string.pickup_location)) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading && !uiState.isUsingDeviceLocation && !uiState.isGettingLocation
                )

                IconButton(
                    onClick = {
                        if (viewModel.hasLocationPermission()) {
                            viewModel.fetchCurrentLocation(
                                onLocationReceived = { _, _, _ -> },
                                onError = {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.location_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !uiState.isLoading && !uiState.isGettingLocation
                ) {
                    if (uiState.isGettingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = stringResource(R.string.use_current_location),
                            tint = if (uiState.isUsingDeviceLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.dropoff,
                onValueChange = { viewModel.updateDropoff(it) },
                label = { Text(stringResource(R.string.dropoff_location)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                enabled = !uiState.isLoading
            )

            Button(
                onClick = {
                    if (uiState.pickup.isEmpty() || uiState.dropoff.isEmpty()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.validation_message),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    viewModel.prepareDeepLinks(
                        onSuccess = { uberDeepLink, boltDeepLink ->
                            onOpenDeepLinks(uberDeepLink, boltDeepLink)
                        },
                        onError = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_prepare_deeplinks),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && areBothAppsInstalled
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (uiState.isLoading) loadingText else compareText)
            }
        }

        // Information label at the bottom
        Text(
            text = stringResource(R.string.info_label),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
