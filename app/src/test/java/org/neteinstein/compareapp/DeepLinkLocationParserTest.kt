package org.neteinstein.compareapp

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.neteinstein.compareapp.utils.DeepLinkLocationParser
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DeepLinkLocationParserTest {

    @Test
    fun testGeoUri_withPlainCoordinates() {
        val uri = Uri.parse("geo:37.4219999,-122.0862462")

        val location = DeepLinkLocationParser.parse(uri)!!

        assertEquals(37.4219999, location.latitude!!, 0.0000001)
        assertEquals(-122.0862462, location.longitude!!, 0.0000001)
        assertNull(location.label)
    }

    @Test
    fun testGeoUri_withLabeledCoordinatesInQuery() {
        // Format commonly produced by Google Maps "Share" -> "Open with"
        val uri = Uri.parse("geo:0,0?q=37.4219999%2C-122.0862462%28Googleplex%29")

        val location = DeepLinkLocationParser.parse(uri)!!

        assertEquals(37.4219999, location.latitude!!, 0.0000001)
        assertEquals(-122.0862462, location.longitude!!, 0.0000001)
        assertEquals("Googleplex", location.label)
    }

    @Test
    fun testGeoUri_withAddressOnlyQuery() {
        val uri = Uri.parse("geo:0,0?q=1600+Amphitheatre+Parkway%2C+Mountain+View")

        val location = DeepLinkLocationParser.parse(uri)!!

        assertNull(location.latitude)
        assertNull(location.longitude)
        assertEquals("1600 Amphitheatre Parkway, Mountain View", location.label)
    }

    @Test
    fun testGeoUri_withCoordinatesAndZoomParam() {
        val uri = Uri.parse("geo:40.7589,-73.9851?z=15")

        val location = DeepLinkLocationParser.parse(uri)!!

        assertEquals(40.7589, location.latitude!!, 0.0000001)
        assertEquals(-73.9851, location.longitude!!, 0.0000001)
    }

    @Test
    fun testGeoUri_withNoUsableData_returnsNull() {
        val uri = Uri.parse("geo:0,0")

        val result = DeepLinkLocationParser.parse(uri)

        assertNull(result)
    }

    @Test
    fun testAppUri_withLatLngAndAddress() {
        val uri = Uri.parse("compareapp://dropoff?lat=48.8566&lng=2.3522&address=Paris")

        val location = DeepLinkLocationParser.parse(uri)!!

        assertEquals(48.8566, location.latitude!!, 0.0000001)
        assertEquals(2.3522, location.longitude!!, 0.0000001)
        assertEquals("Paris", location.label)
    }

    @Test
    fun testAppUri_withCoordinatesOnly() {
        val uri = Uri.parse("compareapp://dropoff?lat=48.8566&lng=2.3522")

        val location = DeepLinkLocationParser.parse(uri)!!

        assertEquals(48.8566, location.latitude!!, 0.0000001)
        assertEquals(2.3522, location.longitude!!, 0.0000001)
        assertNull(location.label)
    }

    @Test
    fun testAppUri_withWrongHost_returnsNull() {
        val uri = Uri.parse("compareapp://pickup?lat=48.8566&lng=2.3522")

        val result = DeepLinkLocationParser.parse(uri)

        assertNull(result)
    }

    @Test
    fun testUnsupportedScheme_returnsNull() {
        val uri = Uri.parse("mailto:someone@example.com")

        val result = DeepLinkLocationParser.parse(uri)

        assertNull(result)
    }

    @Test
    fun testGoogleMapsWebUri_withQueryCoordinates() {
        val uri = Uri.parse("https://maps.google.com/?q=48.8566,2.3522")

        val location = DeepLinkLocationParser.parse(uri)!!

        assertEquals(48.8566, location.latitude!!, 0.0000001)
        assertEquals(2.3522, location.longitude!!, 0.0000001)
    }

    @Test
    fun testGoogleMapsWebUri_withPlaceAndAtCoordinates() {
        val uri = Uri.parse(
            "https://www.google.com/maps/place/Golden+Gate+Bridge/@37.8199286,-122.4802945,17z/data=xyz"
        )

        val location = DeepLinkLocationParser.parse(uri)!!

        assertEquals(37.8199286, location.latitude!!, 0.0000001)
        assertEquals(-122.4802945, location.longitude!!, 0.0000001)
        assertEquals("Golden Gate Bridge", location.label)
    }

    @Test
    fun testGoogleMapsWebUri_withDaddrCoordinates() {
        val uri = Uri.parse("https://maps.google.com/maps?daddr=40.7589,-73.9851")

        val location = DeepLinkLocationParser.parse(uri)!!

        assertEquals(40.7589, location.latitude!!, 0.0000001)
        assertEquals(-73.9851, location.longitude!!, 0.0000001)
    }

    @Test
    fun testGoogleMapsWebUri_withNoUsableData_returnsNull() {
        val uri = Uri.parse("https://www.google.com/maps/search/coffee+near+me")

        val result = DeepLinkLocationParser.parse(uri)

        assertNull(result)
    }

    @Test
    fun testHttpUri_withNonGoogleHost_returnsNull() {
        val uri = Uri.parse("https://evilgoogle.com/maps?q=48.8566,2.3522")

        val result = DeepLinkLocationParser.parse(uri)

        assertNull(result)
    }

    @Test
    fun testHttpUri_shortLink_returnsNull() {
        // Short links (maps.app.goo.gl) never carry coordinates themselves - they must be
        // resolved via MapsShareLinkResolver first.
        val uri = Uri.parse("https://maps.app.goo.gl/AbCdEfGh")

        val result = DeepLinkLocationParser.parse(uri)

        assertNull(result)
    }
}
