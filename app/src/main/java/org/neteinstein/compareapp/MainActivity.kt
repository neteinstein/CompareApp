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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import java.net.URLEncoder
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        private const val SPLIT_SCREEN_DELAY_MS = 500L
    }

    private lateinit var geocoder: Geocoder
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var pickupLocation: Pair<Double, Double>? = null
    private var pickupAddress: String? = null  // Store the address to avoid redundant geocoding
    
    // State holders for location functionality
    private val _pickupText = mutableStateOf("")
    private val _isUsingLocation = mutableStateOf(false)
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // Fine location access granted
                fetchCurrentLocation()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Coarse location access granted
                fetchCurrentLocation()
            }
            else -> {
                // No location access granted
                Toast.makeText(
                    this,
                    "Location permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

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
            false
        }
    }

    internal fun checkRequiredApps(): Pair<Boolean, Boolean> {
        val isUberInstalled = isAppInstalled("com.ubercab")
        val isBoltInstalled = isAppInstalled("ee.mtakso.client")
        return Pair(isUberInstalled, isBoltInstalled)
    }

    @Composable
    fun CompareScreen() {
        var pickup by _pickupText
        var dropoff by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var isUsingLocation by _isUsingLocation
        var isUberInstalled by remember { mutableStateOf(false) }
        var isBoltInstalled by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

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
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
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

            OutlinedTextField(
                value = pickup,
                onValueChange = { pickup = it },
                label = { Text(stringResource(R.string.pickup_location)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = pickup,
                    onValueChange = { 
                        pickup = it
                        if (it.isNotEmpty()) {
                            isUsingLocation = false
                            pickupLocation = null
                        }
                    },
                    label = { Text("Pickup") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && !isUsingLocation
                )
                
                IconButton(
                    onClick = {
                        requestLocationPermission()
                    },
                    enabled = !isLoading
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location),
                        contentDescription = "Use current location",
                        tint = if (isUsingLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
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
                    if ((pickup.isEmpty() && !isUsingLocation) || dropoff.isEmpty()) {
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
                            if (isUsingLocation && pickupLocation != null) {
                                openInSplitScreenWithLocation(pickupLocation!!, dropoff)
                            } else {
                                openInSplitScreen(pickup, dropoff)
                            }
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
    }

    @Composable
    fun CompareAppTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colorScheme = lightColorScheme(),
            content = content
        )
    }

    private suspend fun openInSplitScreen(pickup: String, dropoff: String) {
        // Open Uber deep link
        val uberDeepLink = createUberDeepLink(pickup, dropoff)
        val uberIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uberDeepLink))
        uberIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT

        // Open Bolt deep link (with async geocoding)
        val boltDeepLink = createBoltDeepLink(pickup, dropoff)
        val boltIntent = Intent(Intent.ACTION_VIEW, Uri.parse(boltDeepLink))
        boltIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT

        try {
            // Start Uber first
            startActivity(uberIntent)
            
            // Small delay to ensure split screen is ready
            kotlinx.coroutines.delay(SPLIT_SCREEN_DELAY_MS)
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

    internal suspend fun createBoltDeepLink(pickup: String, dropoff: String): String {
        // Try to geocode the addresses to coordinates (async)
        val pickupCoords = geocodeAddress(pickup)
        val dropoffCoords = geocodeAddress(dropoff)
        
        return if (pickupCoords != null && dropoffCoords != null) {
            // Use coordinate-based deep link format
            "bolt://ride?pickup_lat=${pickupCoords.first}&pickup_lng=${pickupCoords.second}&destination_lat=${dropoffCoords.first}&destination_lng=${dropoffCoords.second}"
        } else {
            // Fallback to address-based format if geocoding fails
            val pickupEncoded = URLEncoder.encode(pickup, "UTF-8")
            val dropoffEncoded = URLEncoder.encode(dropoff, "UTF-8")
            Log.w("MainActivity", "Geocoding failed, using fallback Bolt deep link format")
            "bolt://ride?pickup=$pickupEncoded&destination=$dropoffEncoded"
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
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestLocationPermission() {
        if (hasLocationPermission()) {
            fetchCurrentLocation()
        } else {
            // Request permission
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    private fun fetchCurrentLocation() {
        if (!hasLocationPermission()) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val cancellationTokenSource = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
                
                if (location != null) {
                    pickupLocation = Pair(location.latitude, location.longitude)
                    
                    // Reverse geocode to get address for display
                    val address = reverseGeocode(location.latitude, location.longitude)
                    pickupAddress = address ?: "Current Location"
                    
                    // Update state instead of recreating UI
                    withContext(Dispatchers.Main) {
                        _pickupText.value = pickupAddress!!
                        _isUsingLocation.value = true
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Unable to get current location",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching location", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error getting location: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
                    // Try to build a nice address string
                    address.getAddressLine(0) ?: "${address.locality ?: ""}, ${address.countryName ?: ""}"
                } else {
                    Log.w("MainActivity", "No address found for coordinates: $latitude, $longitude")
                    null
                }
            } catch (e: IOException) {
                Log.e("MainActivity", "Reverse geocoding failed", e)
                null
            }
        }
    }
    
    private suspend fun openInSplitScreenWithLocation(pickupCoords: Pair<Double, Double>, dropoff: String) {
        // Use the already-geocoded address to avoid redundant geocoding
        val addressToUse = pickupAddress ?: "Current Location"
        
        // Open Uber deep link with address
        val uberDeepLink = createUberDeepLink(addressToUse, dropoff)
        val uberIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uberDeepLink))
        uberIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT

        // Open Bolt deep link with coordinates for pickup
        val boltDeepLink = createBoltDeepLinkWithCoordinates(pickupCoords, dropoff)
        val boltIntent = Intent(Intent.ACTION_VIEW, Uri.parse(boltDeepLink))
        boltIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT

        try {
            // Start Uber first
            startActivity(uberIntent)
            
            // Small delay to ensure split screen is ready
            kotlinx.coroutines.delay(SPLIT_SCREEN_DELAY_MS)
            try {
                startActivity(boltIntent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Could not open Bolt app: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Could not open Bolt app", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Could not open Uber app: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Could not open Uber app", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    internal suspend fun createBoltDeepLinkWithCoordinates(pickupCoords: Pair<Double, Double>, dropoff: String): String {
        // Try to geocode the dropoff address to coordinates
        val dropoffCoords = geocodeAddress(dropoff)
        
        return if (dropoffCoords != null) {
            // Use coordinate-based deep link format with coordinates for both
            "bolt://ride?pickup_lat=${pickupCoords.first}&pickup_lng=${pickupCoords.second}&destination_lat=${dropoffCoords.first}&destination_lng=${dropoffCoords.second}"
        } else {
            // Use coordinates for pickup, fallback to address for destination
            val dropoffEncoded = URLEncoder.encode(dropoff, "UTF-8")
            Log.w("MainActivity", "Dropoff geocoding failed, using mixed format")
            "bolt://ride?pickup_lat=${pickupCoords.first}&pickup_lng=${pickupCoords.second}&destination=$dropoffEncoded"
        }
    }
}
