package org.neteinstein.compareapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.neteinstein.compareapp.ui.screens.CompareScreen
import org.neteinstein.compareapp.ui.screens.SettingsRoute
import org.neteinstein.compareapp.ui.theme.CompareAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        // Delay to ensure split screen mode is ready before launching second app
        private const val SPLIT_SCREEN_DELAY_MS = 500L
    }

    // No Navigation-Compose in this app: opening Settings (the top-right button on the main
    // screen) is just a local flag flipped back by Settings' own back arrow or the system back
    // gesture/button (see BackHandler below).
    private enum class Screen { MAIN, SETTINGS }

    // Holds the data URI of a location deep link (e.g. "geo:..." shared from Maps).
    // Re-parsed on each onCreate/onNewIntent; the screen tracks which one it already consumed.
    private val incomingLocationUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display to handle window insets properly
        WindowCompat.setDecorFitsSystemWindows(window, false)

        incomingLocationUri.value = intent?.data

        setContent {
            CompareAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
                    BackHandler(enabled = screen != Screen.MAIN) { screen = Screen.MAIN }

                    when (screen) {
                        Screen.SETTINGS -> SettingsRoute(onBack = { screen = Screen.MAIN })
                        Screen.MAIN -> {
                            val locationUri by incomingLocationUri
                            CompareScreen(
                                incomingLocationUri = locationUri,
                                onOpenSettings = { screen = Screen.SETTINGS },
                                onOpenDeepLinks = { uberDeepLink, boltDeepLink ->
                                    openInSplitScreen(uberDeepLink, boltDeepLink)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingLocationUri.value = intent.data
    }

    private fun openInSplitScreen(uberDeepLink: String, boltDeepLink: String) {
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
    }
}
