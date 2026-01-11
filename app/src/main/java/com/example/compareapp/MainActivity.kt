package com.example.compareapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var pickupEditText: EditText
    private lateinit var dropoffEditText: EditText
    private lateinit var compareButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pickupEditText = findViewById(R.id.pickupEditText)
        dropoffEditText = findViewById(R.id.dropoffEditText)
        compareButton = findViewById(R.id.compareButton)

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
        val pickupEncoded = URLEncoder.encode(pickup, "UTF-8")
        val dropoffEncoded = URLEncoder.encode(dropoff, "UTF-8")
        // Bolt deep link format
        return "bolt://rideplanning?pickup=$pickupEncoded&destination=$dropoffEncoded"
    }
}
