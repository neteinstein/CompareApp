package org.neteinstein.compareapp

import android.content.ActivityNotFoundException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression coverage for the "Bolt app not opening" bug: [MainActivity.openInSplitScreen]
 * used to only fall back to the HTTPS bolt.eu web link if the native `bolt://` intent
 * succeeded, so a device where that native intent failed to resolve (app not registering the
 * scheme/package combo, not installed, etc.) never opened anything for Bolt at all - Uber would
 * open on one side of the split screen while the other side kept showing this app's own first
 * screen. [launchBoltWithFallback] is the extracted control flow that fixes this; these tests
 * pin down that the web fallback always runs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class LaunchBoltWithFallbackTest {

    @Test
    fun testWebFallback_stillRunsWhenNativeBoltIntentThrows() = runTest {
        var webStarted = false

        launchBoltWithFallback(
            startNative = { throw ActivityNotFoundException("No Activity found to handle Intent") },
            startWeb = { webStarted = true },
            delayMs = 500L,
            delay = { /* no-op in tests */ }
        )

        assertTrue("The HTTPS web fallback must still open even if the native bolt:// intent fails", webStarted)
    }

    @Test
    fun testWebFallback_stillRunsWhenNativeBoltIntentSucceeds() = runTest {
        var webStarted = false

        launchBoltWithFallback(
            startNative = { /* succeeds */ },
            startWeb = { webStarted = true },
            delayMs = 500L,
            delay = { /* no-op in tests */ }
        )

        assertTrue(webStarted)
    }

    @Test
    fun testNativeIntent_isAttemptedBeforeWebFallback() = runTest {
        val callOrder = mutableListOf<String>()

        launchBoltWithFallback(
            startNative = { callOrder.add("native") },
            startWeb = { callOrder.add("web") },
            delayMs = 500L,
            delay = { /* no-op in tests */ }
        )

        assertEquals(listOf("native", "web"), callOrder)
    }

    @Test
    fun testNativeIntentFailure_skipsDelayButStillCallsWebFallback() = runTest {
        // Even when the native intent fails, only the post-native delay is skipped (there is
        // nothing to wait for) - the function must not throw or otherwise abort before startWeb.
        var webStarted = false

        launchBoltWithFallback(
            startNative = { throw RuntimeException("package not found") },
            startWeb = { webStarted = true },
            delayMs = 500L,
            delay = { throw AssertionError("delay should not be called when the native intent already failed") }
        )

        assertTrue(webStarted)
    }

    @Test(expected = ActivityNotFoundException::class)
    fun testWebFallbackFailure_isNotSwallowed() = runTest {
        // startWeb failing (e.g. no browser installed either) is a genuine "we couldn't open
        // Bolt at all" case, and must propagate so the caller's error Toast / logging still runs.
        launchBoltWithFallback(
            startNative = { /* succeeds */ },
            startWeb = { throw ActivityNotFoundException("No Activity found to handle Intent") },
            delayMs = 500L,
            delay = { /* no-op in tests */ }
        )
    }
}
