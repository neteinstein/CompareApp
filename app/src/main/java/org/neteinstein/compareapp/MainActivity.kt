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

        incomingLocationUri.value = locationUriFromIntent(intent)

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
                                onOpenDeepLinks = { uberDeepLink, boltDeepLink, boltDeepLinkWeb ->
                                    openInSplitScreen(uberDeepLink, boltDeepLink, boltDeepLinkWeb)
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
        incomingLocationUri.value = locationUriFromIntent(intent)
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
                    launchBoltWithFallback(
                        startNative = {
                            val boltIntent = Intent(Intent.ACTION_VIEW, Uri.parse(boltDeepLink))
                            boltIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                            boltIntent.setPackage("ee.mtakso.client")
                            startActivity(boltIntent)
                        },
                        startWeb = {
                            val boltIntentWeb = Intent(Intent.ACTION_VIEW, Uri.parse(boltDeepLinkWeb))
                            boltIntentWeb.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                            startActivity(boltIntentWeb)
                        },
                        delayMs = SPLIT_SCREEN_DELAY_MS
                    )
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

/**
 * Drives the Bolt half of [MainActivity.openInSplitScreen]'s split-screen launch: try the native
 * `bolt://` intent first, then always attempt the HTTPS web-link fallback afterward - a failure
 * in [startNative] (e.g. the installed Bolt app doesn't resolve that scheme/package combination)
 * must not skip [startWeb], since that fallback is what still gets Bolt (or a browser) open with
 * the destination set. A failure in [startWeb] itself is NOT caught here - it propagates to the
 * caller so the "Could not open Bolt" error path still runs.
 * Top-level so it's unit-testable without a real Activity/Context.
 */
internal suspend fun launchBoltWithFallback(
    startNative: () -> Unit,
    startWeb: () -> Unit,
    delayMs: Long,
    delay: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) }
) {
    try {
        startNative()
        delay(delayMs)
    } catch (e: Exception) {
        Log.w("MainActivity", "Bolt custom scheme intent failed, falling back to web link: ${e.message}")
    }
    startWeb()
}

// The first http(s) URL found in a shared text block, e.g. Google Maps' "Share" text which
// looks like "Place Name\nhttps://maps.app.goo.gl/xxxx".
private val SHARED_TEXT_URL_REGEX = Regex("""https?://\S+""")

/**
 * Extracts the location URI to hand off to [org.neteinstein.compareapp.utils.DeepLinkLocationParser]
 * from an incoming intent. Handles both "Open with" (ACTION_VIEW, e.g. a `geo:` URI) and Google
 * Maps' "Share" button (ACTION_SEND text/plain, whose [Intent.EXTRA_TEXT] contains a maps URL
 * rather than [Intent.data]). Top-level so it's unit-testable without instantiating the activity.
 */
internal fun locationUriFromIntent(intent: Intent?): Uri? {
    intent ?: return null
    return when (intent.action) {
        Intent.ACTION_VIEW -> intent.data
        Intent.ACTION_SEND -> {
            if (intent.type?.startsWith("text/plain") != true) return null
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
            SHARED_TEXT_URL_REGEX.find(sharedText)?.value?.let(Uri::parse)
        }
        else -> null
    }
}
