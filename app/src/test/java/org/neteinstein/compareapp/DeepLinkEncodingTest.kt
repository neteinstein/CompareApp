package org.neteinstein.compareapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URLDecoder

/**
 * Tests for URL encoding in deep links
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DeepLinkEncodingTest {

    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .get()
    }

    @Test
    fun testUberDeepLink_encodesSpaces() {
        // Given
        val pickup = "Times Square New York"
        val dropoff = "Central Park"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then
        // Spaces should be encoded
        assertTrue("Deep link should contain encoded content", 
            deepLink.contains("+") || deepLink.contains("%20"))
    }

    @Test
    fun testUberDeepLink_encodesAmpersand() {
        // Given
        val pickup = "Street & Avenue"
        val dropoff = "Location & Place"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then
        // Ampersand should be encoded as %26
        assertTrue("Ampersand should be encoded", deepLink.contains("%26"))
    }

    @Test
    fun testUberDeepLink_encodesCommas() {
        // Given
        val pickup = "123 Main St, New York, NY"
        val dropoff = "456 Park Ave, NY, NY"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then - Commas should be encoded
        assertTrue("Link should contain encoded commas", deepLink.contains("%2C"))
    }

    @Test
    fun testUberDeepLink_decodableAddresses() {
        // Given
        val pickup = "Special Café & Restaurant"
        val dropoff = "Zürich, Switzerland"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)
        val decoded = URLDecoder.decode(deepLink, "UTF-8")

        // Then - Decoded link should contain original text
        assertTrue("Decoded link should contain pickup", decoded.contains(pickup))
        assertTrue("Decoded link should contain dropoff", decoded.contains(dropoff))
    }

    @Test
    fun testUberDeepLink_encodesHashSymbol() {
        // Given
        val pickup = "Apartment #123"
        val dropoff = "Building #456"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then - Hash should be encoded
        assertTrue("Hash symbol should be encoded", deepLink.contains("%23"))
    }

    @Test
    fun testUberDeepLink_encodesQuestionMark() {
        // Given
        val pickup = "Where am I?"
        val dropoff = "Where to go?"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then - Question mark should be encoded
        assertTrue("Question mark should be encoded", deepLink.contains("%3F"))
    }

    @Test
    fun testUberDeepLink_encodesEquals() {
        // Given
        val pickup = "x=123"
        val dropoff = "y=456"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then - Equals should be encoded in addresses
        assertTrue("Equals should be encoded", deepLink.contains("%3D"))
    }

    @Test
    fun testUberDeepLink_preservesProtocolAndParams() {
        // Given
        val pickup = "Start"
        val dropoff = "End"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then - Protocol and parameter structure should be intact
        assertTrue("Should have uber protocol", deepLink.startsWith("uber://"))
        assertTrue("Should have action parameter", deepLink.contains("action=setPickup"))
        assertTrue("Should have pickup parameter", deepLink.contains("pickup[formatted_address]="))
        assertTrue("Should have dropoff parameter", deepLink.contains("dropoff[formatted_address]="))
    }

    @Test
    fun testUberDeepLink_unicodeCharacters() {
        // Given
        val pickup = "北京市 Beijing"
        val dropoff = "東京 Tokyo"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)
        val decoded = URLDecoder.decode(deepLink, "UTF-8")

        // Then
        assertTrue("Should encode Unicode", deepLink.contains("%"))
        assertTrue("Decoded should contain original text", decoded.contains("北京市"))
        assertTrue("Decoded should contain original text", decoded.contains("東京"))
    }

    @Test
    fun testUberDeepLink_slashesEncoded() {
        // Given
        val pickup = "123/456 Street"
        val dropoff = "N/A"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then
        assertTrue("Slashes should be encoded", deepLink.contains("%2F"))
    }

    @Test
    fun testUberDeepLink_bracketsEncoded() {
        // Given
        val pickup = "Suite [100]"
        val dropoff = "Building (A)"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then - Brackets and parentheses should be encoded
        assertTrue("Brackets should be encoded", 
            deepLink.contains("%5B") || deepLink.contains("%28"))
    }

    @Test
    fun testUberDeepLink_percentEncoded() {
        // Given
        val pickup = "100% Street"
        val dropoff = "50% Avenue"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then - Percent should be encoded
        assertTrue("Percent should be encoded", deepLink.contains("%25"))
    }

    @Test
    fun testUberDeepLink_plusSignEncoded() {
        // Given
        val pickup = "A+B Street"
        val dropoff = "C+D Avenue"

        // When
        val deepLink = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then - Plus should be encoded
        assertTrue("Plus should be encoded", deepLink.contains("%2B"))
    }

    @Test
    fun testUberDeepLink_consistentEncoding() {
        // Given
        val pickup = "Test Location"
        val dropoff = "Test Location"

        // When - Create link twice
        val deepLink1 = activity.createUberDeepLink(pickup, dropoff, null, null)
        val deepLink2 = activity.createUberDeepLink(pickup, dropoff, null, null)

        // Then - Should be identical (consistent encoding)
        assertTrue("Links should be identical", deepLink1 == deepLink2)
    }
}
