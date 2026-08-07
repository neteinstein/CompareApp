package org.neteinstein.compareapp

import android.net.Uri
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLEncoder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.neteinstein.compareapp.utils.MapsShareLinkResolver
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MapsShareLinkResolverTest {

    private lateinit var server: HttpServer
    private lateinit var baseUrl: String
    private var receivedCookie: String? = null
    private var redirectLocation: String = ""

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/short") { exchange ->
            receivedCookie = exchange.requestHeaders.getFirst("Cookie")
            exchange.responseHeaders.add("Location", redirectLocation)
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.start()
        baseUrl = "http://127.0.0.1:${server.address.port}"
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun resolve_followsRedirectToFinalDestination() = runBlocking {
        redirectLocation = "https://www.google.com/maps/place/Golden+Gate+Bridge/@37.8199286,-122.4802945,17z"

        val result = MapsShareLinkResolver.resolve(Uri.parse("$baseUrl/short"))

        assertEquals(Uri.parse(redirectLocation), result)
    }

    @Test
    fun resolve_sendsConsentBypassCookie() = runBlocking {
        redirectLocation = "https://www.google.com/maps/place/Golden+Gate+Bridge/@37.8199286,-122.4802945,17z"

        MapsShareLinkResolver.resolve(Uri.parse("$baseUrl/short"))

        // Preempts the "consent.google.com" GDPR interstitial some Google-hosted requests get
        // redirected through, so the chain reaches the real maps.google.com destination in one hop.
        assertEquals("CONSENT=YES+cb", receivedCookie)
    }

    @Test
    fun resolve_whenRedirectedToConsentInterstitial_usesContinueParamAsDestination() = runBlocking {
        val destination = "https://www.google.com/maps/place/Golden+Gate+Bridge/@37.8199286,-122.4802945,17z"
        redirectLocation = "https://consent.google.com/m?continue=" +
            URLEncoder.encode(destination, "UTF-8") + "&gl=DE"

        val result = MapsShareLinkResolver.resolve(Uri.parse("$baseUrl/short"))

        // Should recover the real destination from the interstitial's own "continue" param
        // rather than surfacing the consent page (which has no coordinates) as the result.
        assertEquals(Uri.parse(destination), result)
    }
}
