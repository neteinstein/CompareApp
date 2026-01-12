package com.example.compareapp

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var pickupEditText: EditText
    private lateinit var dropoffEditText: EditText
    private lateinit var compareButton: Button
    private lateinit var geocoder: Geocoder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pickupEditText = findViewById(R.id.pickupEditText)
        dropoffEditText = findViewById(R.id.dropoffEditText)
        compareButton = findViewById(R.id.compareButton)
        geocoder = Geocoder(this, Locale.getDefault())

        compareButton.setOnClickListener {
            val pickup = pickupEditText.text.toString()
            val dropoff = dropoffEditText.text.toString()

            if (pickup.isEmpty() || dropoff.isEmpty()) {
                Toast.makeText(this, "Please enter both pickup and dropoff locations", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            openInSplitScreen(pickup, dropoff)
        }
    }

    private fun openInSplitScreen(pickup: String, dropoff: String) {
        // Open Uber deep link
        val uberDeepLink = createUberDeepLink(pickup, dropoff)
        val uberIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uberDeepLink))
        uberIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT

        // Open Bolt deep link
        val boltDeepLink = createBoltDeepLink(pickup, dropoff)
        val boltIntent = Intent(Intent.ACTION_VIEW, Uri.parse(boltDeepLink))
        boltIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT

        try {
            // Start Uber first
            startActivity(uberIntent)
            
            // Small delay to ensure split screen is ready
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    startActivity(boltIntent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Could not open Bolt app: ${e.message}")
                    Toast.makeText(this, "Could not open Bolt app", Toast.LENGTH_SHORT).show()
                }
            }, 500)
        } catch (e: Exception) {
            Log.e("MainActivity", "Could not open Uber app: ${e.message}")
            Toast.makeText(this, "Could not open Uber app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createUberDeepLink(pickup: String, dropoff: String): String {
        val pickupEncoded = URLEncoder.encode(pickup, "UTF-8")
        val dropoffEncoded = URLEncoder.encode(dropoff, "UTF-8")
        // Uber deep link format
        return "uber://?action=setPickup&pickup[formatted_address]=$pickupEncoded&dropoff[formatted_address]=$dropoffEncoded"
    }

    private fun createBoltDeepLink(pickup: String, dropoff: String): String {
        // Try to geocode the addresses to coordinates
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
    
    private fun geocodeAddress(address: String): Pair<Double, Double>? {
        return try {
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
