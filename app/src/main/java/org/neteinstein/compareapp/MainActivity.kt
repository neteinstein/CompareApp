package org.neteinstein.compareapp

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var geocoder: Geocoder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geocoder = Geocoder(this, Locale.getDefault())
        
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

    @Composable
    fun CompareScreen() {
        var pickup by remember { mutableStateOf("") }
        var dropoff by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        val context = LocalContext.current

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
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = pickup,
                onValueChange = { pickup = it },
                label = { Text(stringResource(R.string.pickup_location)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !isLoading
            )

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
                            openInSplitScreen(pickup, dropoff)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(if (isLoading) R.string.loading else R.string.compare))
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
}
