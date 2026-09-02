package org.neteinstein.compareapp.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.neteinstein.compareapp.utils.DeepLinkLocationParser
import org.neteinstein.compareapp.utils.FoodDeliveryProvider
import org.neteinstein.compareapp.utils.FoodSearchMode
import org.neteinstein.compareapp.utils.MapsShareLinkResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    viewModel: MainViewModel = hiltViewModel(),
    incomingLocationUri: Uri? = null,
    onOpenSettings: () -> Unit = {},
    onOpenDeepLinks: (uberDeepLink: String, boltDeepLink: String, boltDeepLinkWeb: String) -> Unit,
    onOpenFoodSearch: (links: Map<FoodDeliveryProvider, String>) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var pickupMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var dropoffMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val locationErrorText = stringResource(R.string.location_error)
    val locationPermissionDeniedText = stringResource(R.string.location_permission_denied)
    val validationMessageText = stringResource(R.string.validation_message)
    val errorPrepareDeeplinksText = stringResource(R.string.error_prepare_deeplinks)
    val foodValidationMessageText = stringResource(R.string.food_search_validation_message)

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
                    Toast.makeText(context, locationErrorText, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, locationPermissionDeniedText, Toast.LENGTH_SHORT).show()
        }
    }

    fun requestOrFetchCurrentLocation() {
        if (viewModel.hasLocationPermission()) {
            viewModel.fetchCurrentLocation(
                onLocationReceived = { _, _, _ -> },
                onError = {
                    Toast.makeText(context, locationErrorText, Toast.LENGTH_SHORT).show()
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
    }

    // Check app installation status when screen is created and when it resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkInstalledApps()
                viewModel.checkFoodAppsInstalled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.checkFoodAppsInstalled()
    }

    // Handle an incoming "share a location with this app" deep link: prepopulate the
    // dropoff field with the shared location and assume the current device location as pickup.
    var lastHandledDeepLinkUri by rememberSaveable { mutableStateOf<String?>(null) }
    val deepLinkToastText = stringResource(R.string.deep_link_dropoff_set)
    LaunchedEffect(incomingLocationUri) {
        val uri = incomingLocationUri ?: return@LaunchedEffect
        if (uri.toString() == lastHandledDeepLinkUri) return@LaunchedEffect
        lastHandledDeepLinkUri = uri.toString()

        val resolvedUri = if (MapsShareLinkResolver.isShortLink(uri)) {
            MapsShareLinkResolver.resolve(uri)
        } else {
            uri
        }

        val location = DeepLinkLocationParser.parse(resolvedUri) ?: return@LaunchedEffect
        viewModel.applyIncomingDropoffLocation(location.latitude, location.longitude, location.label)
        Toast.makeText(context, deepLinkToastText, Toast.LENGTH_SHORT).show()
        requestOrFetchCurrentLocation()
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

    val missingFoodApps = uiState.selectedFoodProviders - uiState.installedFoodProviders
    val foodWarningMessage = if (missingFoodApps.isEmpty()) {
        null
    } else {
        val missingAppNames = FoodDeliveryProvider.entries
            .filter { it in missingFoodApps }
            .map { it.displayName }
        "Warning: ${missingAppNames.joinToString(" and ")} ${if (missingAppNames.size == 1) "app is" else "apps are"} required for this to work"
    }

    val loadingText = stringResource(R.string.loading)
    val compareText = stringResource(R.string.compare)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AppHeader()
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.settings_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            warningMessage?.let { message ->
                WarningBanner(message = message, modifier = Modifier.padding(top = 20.dp))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pickupEnabled = !uiState.isLoading && !uiState.isUsingDeviceLocation && !uiState.isGettingLocation
                        ExposedDropdownMenuBox(
                            expanded = pickupMenuExpanded && uiState.pickupSuggestions.isNotEmpty(),
                            onExpandedChange = { pickupMenuExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = uiState.pickup,
                                onValueChange = {
                                    viewModel.updatePickup(it)
                                    pickupMenuExpanded = true
                                },
                                label = { Text(stringResource(R.string.pickup_location)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.TripOrigin,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = pickupEnabled),
                                enabled = pickupEnabled
                            )
                            ExposedDropdownMenu(
                                expanded = pickupMenuExpanded && uiState.pickupSuggestions.isNotEmpty(),
                                onDismissRequest = { pickupMenuExpanded = false }
                            ) {
                                uiState.pickupSuggestions.forEach { suggestion ->
                                    DropdownMenuItem(
                                        text = { Text(suggestion.fullAddress) },
                                        onClick = {
                                            viewModel.selectPickupSuggestion(suggestion)
                                            pickupMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { requestOrFetchCurrentLocation() },
                            enabled = !uiState.isLoading && !uiState.isGettingLocation,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (uiState.isUsingDeviceLocation) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            if (uiState.isGettingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (uiState.isUsingDeviceLocation) {
                                        Icons.Filled.MyLocation
                                    } else {
                                        Icons.Filled.LocationOn
                                    },
                                    contentDescription = stringResource(R.string.use_current_location),
                                    tint = if (uiState.isUsingDeviceLocation) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ExposedDropdownMenuBox(
                        expanded = dropoffMenuExpanded && uiState.dropoffSuggestions.isNotEmpty(),
                        onExpandedChange = { dropoffMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.dropoff,
                            onValueChange = {
                                viewModel.updateDropoff(it)
                                dropoffMenuExpanded = true
                            },
                            label = { Text(stringResource(R.string.dropoff_location)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = !uiState.isLoading),
                            enabled = !uiState.isLoading
                        )
                        ExposedDropdownMenu(
                            expanded = dropoffMenuExpanded && uiState.dropoffSuggestions.isNotEmpty(),
                            onDismissRequest = { dropoffMenuExpanded = false }
                        ) {
                            uiState.dropoffSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion.fullAddress) },
                                    onClick = {
                                        viewModel.selectDropoffSuggestion(suggestion)
                                        dropoffMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (uiState.pickup.isEmpty() || uiState.dropoff.isEmpty()) {
                                Toast.makeText(context, validationMessageText, Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.prepareDeepLinks(
                                onSuccess = { uberDeepLink, boltDeepLink, boltDeepLinkWeb ->
                                    onOpenDeepLinks(uberDeepLink, boltDeepLink, boltDeepLinkWeb)
                                },
                                onError = {
                                    Toast.makeText(context, errorPrepareDeeplinksText, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = !uiState.isLoading && areBothAppsInstalled
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (uiState.isLoading) loadingText else compareText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            foodWarningMessage?.let { message ->
                WarningBanner(message = message, modifier = Modifier.padding(top = 20.dp))
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.food_search_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = uiState.foodSearchMode == FoodSearchMode.RESTAURANT,
                            onClick = { viewModel.setFoodSearchMode(FoodSearchMode.RESTAURANT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text(stringResource(R.string.food_search_mode_restaurant))
                        }
                        SegmentedButton(
                            selected = uiState.foodSearchMode == FoodSearchMode.DISH,
                            onClick = { viewModel.setFoodSearchMode(FoodSearchMode.DISH) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text(stringResource(R.string.food_search_mode_dish))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = uiState.foodQuery,
                        onValueChange = viewModel::updateFoodQuery,
                        label = {
                            Text(
                                text = if (uiState.foodSearchMode == FoodSearchMode.RESTAURANT) {
                                    stringResource(R.string.food_search_query_restaurant_label)
                                } else {
                                    stringResource(R.string.food_search_query_dish_label)
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = uiState.foodLocation,
                        onValueChange = viewModel::updateFoodLocation,
                        label = { Text(stringResource(R.string.food_search_location_label)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.prepareFoodSearchLinks(
                                onSuccess = { links -> onOpenFoodSearch(links) },
                                onError = {
                                    Toast.makeText(context, foodValidationMessageText, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.food_search_button),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = stringResource(R.string.food_search_disclaimer),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }

        // Information label at the bottom
        Text(
            text = stringResource(R.string.info_label),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}

@Composable
private fun AppHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CompareArrows,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = stringResource(R.string.app_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = stringResource(R.string.app_tagline),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun WarningBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = message,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
