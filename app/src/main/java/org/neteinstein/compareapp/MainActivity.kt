package org.neteinstein.compareapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var geocoder: Geocoder
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geocoder = Geocoder(this, Locale.getDefault())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Enable edge-to-edge display to handle window insets properly
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            CompareAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompareScreen()
                }
            }
        }
    }

    internal fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("MainActivity", "App not found: $packageName")
            false
        }
    }

    internal fun checkRequiredApps(): Pair<Boolean, Boolean> {
        val isUberInstalled = isAppInstalled("com.ubercab")
        val isBoltInstalled = isAppInstalled("ee.mtakso.client")
        return Pair(isUberInstalled, isBoltInstalled)
    }

    internal fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    internal suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.w("MainActivity", "Location permission not granted")
            return null
        }

        return withContext(Dispatchers.IO) @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) {
            try {
                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error getting current location", e)
                null
            }
        }
    }

    internal suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    // Build a formatted address string
                    buildString {
                        address.getAddressLine(0)?.let { append(it) }
                    }.takeIf { it.isNotEmpty() }
                } else {
                    Log.w("MainActivity", "No results found for coordinates: $latitude, $longitude")
                    null
                }
            } catch (e: IOException) {
                Log.e("MainActivity", "Reverse geocoding failed", e)
                null
            }
        }
    }

    private suspend fun handleLocationRetrieval(
        onLocationReceived: (latitude: Double, longitude: Double, address: String) -> Unit,
        onError: () -> Unit
    ) {
        val location = getCurrentLocation()
        if (location != null) {
            // Reverse geocode for display in pickup field
            val address = reverseGeocode(location.latitude, location.longitude)
                ?: "Lat: ${location.latitude}, Lng: ${location.longitude}"
            onLocationReceived(location.latitude, location.longitude, address)
        } else {
            onError()
        }
    }

    private fun fetchAndSetLocation(
        context: android.content.Context,
        setGettingLocation: (Boolean) -> Unit,
        onLocationReceived: (latitude: Double, longitude: Double, address: String) -> Unit
    ) {
        setGettingLocation(true)
        lifecycleScope.launch {
            try {
                handleLocationRetrieval(
                    onLocationReceived = onLocationReceived,
                    onError = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.location_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } finally {
                setGettingLocation(false)
            }
        }
    }

    @Composable
    fun CompareScreen() {
        var pickup by remember { mutableStateOf("") }
        var dropoff by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var isUberInstalled by remember { mutableStateOf(false) }
        var isBoltInstalled by remember { mutableStateOf(false) }
        var isUsingDeviceLocation by remember { mutableStateOf(false) }
        var isGettingLocation by remember { mutableStateOf(false) }
        var pickupCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
        val context = LocalContext.current
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

        // Location permission launcher
        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                         permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            
            if (granted) {
                // Permission granted, get location
                fetchAndSetLocation(
                    context = context,
                    setGettingLocation = { isGettingLocation = it },
                    onLocationReceived = { lat, lng, address ->
                        pickupCoordinates = Pair(lat, lng)
                        pickup = address
                        isUsingDeviceLocation = true
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
            // Initial check
            val (uberInstalled, boltInstalled) = checkRequiredApps()
            isUberInstalled = uberInstalled
            isBoltInstalled = boltInstalled
            
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val (uberInstalled, boltInstalled) = checkRequiredApps()
                    isUberInstalled = uberInstalled
                    isBoltInstalled = boltInstalled
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        val areBothAppsInstalled = isUberInstalled && isBoltInstalled
        Log.d("MainActivity", "Uber installed: $isUberInstalled, Bolt installed: $isBoltInstalled")
        val warningMessage = remember(isUberInstalled, isBoltInstalled) {
            if (isUberInstalled && isBoltInstalled) {
                null
            } else {
                val missingApps = buildList {
                    if (!isUberInstalled) add("Uber")
                    if (!isBoltInstalled) add("Bolt")
                }
                "Warning: ${missingApps.joinToString(" and ")} ${if (missingApps.size == 1) "app is" else "apps are"} required for this to work"
            }
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
                        value = pickup,
                        onValueChange = { 
                            pickup = it
                            // If user starts typing, disable device location mode
                            if (isUsingDeviceLocation) {
                                isUsingDeviceLocation = false
                                pickupCoordinates = null
                            }
                        },
                        label = { Text(stringResource(R.string.pickup_location)) },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && !isUsingDeviceLocation && !isGettingLocation
                    )
                    
                    IconButton(
                        onClick = {
                            if (hasLocationPermission()) {
                                // Permission already granted, get location directly
                                fetchAndSetLocation(
                                    context = context,
                                    setGettingLocation = { isGettingLocation = it },
                                    onLocationReceived = { lat, lng, address ->
                                        pickupCoordinates = Pair(lat, lng)
                                        pickup = address
                                        isUsingDeviceLocation = true
                                    }
                                )
                            } else {
                                // Request permission
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        enabled = !isLoading && !isGettingLocation
                    ) {
                        if (isGettingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = stringResource(R.string.use_current_location),
                                tint = if (isUsingDeviceLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dropoff,
                    onValueChange = { dropoff = it },
                    label = { Text(stringResource(R.string.dropoff_location)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    enabled = !isLoading
                )

                Button(
                    onClick = {
                        if (pickup.isEmpty() || dropoff.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.validation_message),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        isLoading = true
                        lifecycleScope.launch {
                            try {
                                openInSplitScreen(pickup, dropoff, pickupCoordinates)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && areBothAppsInstalled
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isLoading) loadingText else compareText)
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

    @Composable
    fun CompareAppTheme(content: @Composable () -> Unit) {
        val darkTheme = isSystemInDarkTheme()
        val colorScheme = if (darkTheme) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
        
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }

    private suspend fun openInSplitScreen(pickup: String, dropoff: String, pickupCoordinates: Pair<Double, Double>? = null) {
        // Open Uber deep link (always uses address string)
        val uberDeepLink = createUberDeepLink(pickup, dropoff)
        val uberIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uberDeepLink))
        uberIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT

        // Open Bolt deep link (uses coordinates if available, otherwise geocodes the address)
        val boltDeepLink = createBoltDeepLink(pickup, dropoff, pickupCoordinates)
        val boltIntent = Intent(Intent.ACTION_VIEW, Uri.parse(boltDeepLink))
        boltIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT

        try {
            // Start Uber first
            startActivity(uberIntent)
            
            // Small delay to ensure split screen is ready
            kotlinx.coroutines.delay(500)
            try {
                startActivity(boltIntent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Could not open Bolt app: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, getString(R.string.error_bolt), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Could not open Uber app: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, getString(R.string.error_uber), Toast.LENGTH_SHORT).show()
            }
        }
    }

    internal fun createUberDeepLink(pickup: String, dropoff: String): String {
        val pickupEncoded = URLEncoder.encode(pickup, "UTF-8")
        val dropoffEncoded = URLEncoder.encode(dropoff, "UTF-8")
        // Uber deep link format
        return "uber://?action=setPickup&pickup[formatted_address]=$pickupEncoded&dropoff[formatted_address]=$dropoffEncoded"
    }

    internal suspend fun createBoltDeepLink(pickup: String, dropoff: String, pickupCoordinates: Pair<Double, Double>? = null): String {
        // If pickup coordinates are provided (from device location), use them directly
        val pickupCoords = pickupCoordinates ?: geocodeAddress(pickup)
        val dropoffCoords = geocodeAddress(dropoff)
        
        return if (pickupCoords != null && dropoffCoords != null) {
            //Bolt supports only up to 6 decimal cases.
            val pickupLat = BigDecimal(pickupCoords.first).setScale(6, RoundingMode.HALF_UP).toDouble()
            val pickupLng = BigDecimal(pickupCoords.second).setScale(6, RoundingMode.HALF_UP).toDouble()
            val dropoffLat = BigDecimal(dropoffCoords.second).setScale(6, RoundingMode.HALF_UP).toDouble()
            val dropoffLng = BigDecimal(dropoffCoords.second).setScale(6, RoundingMode.HALF_UP).toDouble()
            // Use coordinate-based deep link format
            //Log.d("MainActivity", "Opening -> bolt://ride?pickup_lat=${pickupLat}&pickup_lng=${pickupLng}&destination_lat=${dropoffLat}&destination_lng=${dropoffLng}")
            "bolt://ride?pickup_lat=${pickupLat}&pickup_lng=${pickupLng}&destination_lat=${dropoffLat}&destination_lng=${dropoffLng}"
        } else {
            // Fallback to address-based format if geocoding fails
            //val pickupEncoded = URLEncoder.encode(pickup, "UTF-8")
            //val dropoffEncoded = URLEncoder.encode(dropoff, "UTF-8")
            Log.w("MainActivity", "Geocoding failed, using fallback Bolt deep link format")
            "bolt://ride?pickup=0&destination=0"
        }
    }
    
    internal suspend fun geocodeAddress(address: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                // Note: getFromLocationName is deprecated on API 33+, but the new async API
                // requires different handling. For now, we use the deprecated method in IO dispatcher
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(address, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val location = addresses[0]
                    Pair(location.latitude, location.longitude)
                } else {
                    Log.w("MainActivity", "No results found for address: $address")
                    null
                }
            } catch (e: IOException) {
                Log.e("MainActivity", "Geocoding failed for address: $address", e)
                null
            }
        }
    }
}
