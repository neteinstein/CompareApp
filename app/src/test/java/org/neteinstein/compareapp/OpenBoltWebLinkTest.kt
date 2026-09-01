package org.neteinstein.compareapp

import android.content.ActivityNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression coverage for the Bolt web link opening a browser instead of the Bolt app: an
 * `https://bolt.eu/...` intent with no package set only routes into Bolt when Android has
 * verified Bolt's App Link domain, which is unreliable in practice - on a device where that
 * verification hasn't happened, the link silently opens a browser tab that can't set the
 * ride's pickup/destination, even though Bolt is installed and would happily handle the link
 * if targeted explicitly. [openBoltWebLink] fixes this by trying the Bolt-targeted intent
 * first and only falling back to an unrestricted (browser) intent if that one doesn't resolve.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OpenBoltWebLinkTest {

    @Test
    fun testBrowserFallback_notUsedWhenBoltAppHandlesTheLink() {
        var browserStarted = false

        openBoltWebLink(
            startWebApp = { /* Bolt resolves the link */ },
            startWebBrowser = { browserStarted = true }
        )

        assertFalse("The browser fallback must not run when Bolt itself opened the link", browserStarted)
    }

    @Test
    fun testBrowserFallback_runsWhenBoltAppDoesNotHandleTheLink() {
        var browserStarted = false

        openBoltWebLink(
            startWebApp = { throw ActivityNotFoundException("No Activity found to handle Intent") },
            startWebBrowser = { browserStarted = true }
        )

        assertTrue("The browser fallback must run when the Bolt-targeted intent fails to resolve", browserStarted)
    }

    @Test
    fun testBoltAppIntent_isAttemptedBeforeBrowserFallback() {
        val callOrder = mutableListOf<String>()

        openBoltWebLink(
            startWebApp = {
                callOrder.add("app")
                throw ActivityNotFoundException("No Activity found to handle Intent")
            },
            startWebBrowser = { callOrder.add("browser") }
        )

        assertEquals(listOf("app", "browser"), callOrder)
    }

    @Test(expected = ActivityNotFoundException::class)
    fun testBrowserFallbackFailure_isNotSwallowed() {
        // startWebBrowser failing too (e.g. no browser installed either) is a genuine
        // "we couldn't open Bolt at all" case, and must propagate so the caller's
        // "Could not open Bolt" error Toast / logging still runs.
        openBoltWebLink(
            startWebApp = { throw ActivityNotFoundException("No Activity found to handle Intent") },
            startWebBrowser = { throw ActivityNotFoundException("No Activity found to handle Intent") }
        )
    }

    @Test(expected = RuntimeException::class)
    fun testNonActivityNotFoundFailure_isNotCaught() {
        // Only "nothing declares this intent filter" should trigger the browser fallback - any
        // other failure from starting the Bolt-targeted intent is a genuine error and must
        // propagate rather than being masked by a silent fallback.
        openBoltWebLink(
            startWebApp = { throw RuntimeException("unexpected failure") },
            startWebBrowser = { throw AssertionError("browser fallback should not run for this failure") }
        )
    }
}
