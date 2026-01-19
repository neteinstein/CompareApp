package org.neteinstein.compareapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.neteinstein.compareapp.ui.screens.CompareScreen
import org.neteinstein.compareapp.ui.theme.CompareAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        // Delay to ensure split screen mode is ready before launching second app
        private const val SPLIT_SCREEN_DELAY_MS = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display to handle window insets properly
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CompareAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompareScreen(
                        onOpenDeepLinks = { uberDeepLink, boltDeepLink, boltDeepLinkWeb ->
                            openInSplitScreen(uberDeepLink, boltDeepLink, boltDeepLinkWeb)
                        }
                    )
                }
            }
        }
    }

    private fun openInSplitScreen(uberDeepLink: String, boltDeepLink: String, boltDeepLinkWeb: String) {
        lifecycleScope.launch {
            try {
                // Open Uber deep link

                val uberIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uberDeepLink))
                uberIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                startActivity(uberIntent)

                // Small delay to ensure split screen is ready
                kotlinx.coroutines.delay(SPLIT_SCREEN_DELAY_MS)

                
                try {
                    // Open Bolt deep link
                    val boltIntent = Intent(Intent.ACTION_VIEW, Uri.parse(boltDeepLink))
                    boltIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                    boltIntent.setPackage("ee.mtakso.client")
                    startActivity(boltIntent)

                    kotlinx.coroutines.delay(SPLIT_SCREEN_DELAY_MS)

                    val boltIntentWeb = Intent(Intent.ACTION_VIEW, Uri.parse(boltDeepLinkWeb))
                    boltIntentWeb.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                    startActivity(boltIntentWeb)
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
    }
}
