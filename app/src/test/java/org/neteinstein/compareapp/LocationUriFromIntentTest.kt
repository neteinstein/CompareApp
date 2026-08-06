package org.neteinstein.compareapp

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocationUriFromIntentTest {

    @Test
    fun testActionView_returnsIntentData() {
        val uri = Uri.parse("geo:37.4219999,-122.0862462")
        val intent = Intent(Intent.ACTION_VIEW, uri)

        assertEquals(uri, locationUriFromIntent(intent))
    }

    @Test
    fun testActionSend_withMapsShareLink_extractsUrlFromSharedText() {
        // The shape Google Maps' "Share" button actually produces: a label line followed by
        // a maps.app.goo.gl short link.
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Golden Gate Bridge\nhttps://maps.app.goo.gl/AbCdEfGh")
        }

        val result = locationUriFromIntent(intent)

        assertEquals(Uri.parse("https://maps.app.goo.gl/AbCdEfGh"), result)
    }

    @Test
    fun testActionSend_withUrlOnly_extractsUrl() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://www.google.com/maps/place/Golden+Gate+Bridge/@37.81,-122.47,17z")
        }

        val result = locationUriFromIntent(intent)

        assertEquals(
            Uri.parse("https://www.google.com/maps/place/Golden+Gate+Bridge/@37.81,-122.47,17z"),
            result
        )
    }

    @Test
    fun testActionSend_withNoUrlInText_returnsNull() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Just some text, no link here")
        }

        assertNull(locationUriFromIntent(intent))
    }

    @Test
    fun testActionSend_withWrongMimeType_returnsNull() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, "https://maps.app.goo.gl/AbCdEfGh")
        }

        assertNull(locationUriFromIntent(intent))
    }

    @Test
    fun testUnsupportedAction_returnsNull() {
        val intent = Intent(Intent.ACTION_MAIN)

        assertNull(locationUriFromIntent(intent))
    }

    @Test
    fun testNullIntent_returnsNull() {
        assertNull(locationUriFromIntent(null))
    }
}
