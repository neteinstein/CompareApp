package org.neteinstein.compareapp

import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.URLEncoder
import java.util.concurrent.Executors
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

    // A hand-rolled single-connection HTTP server rather than com.sun.net.httpserver.HttpServer:
    // that JDK-internal API isn't reliably resolvable when compiling against this project's
    // jvmTarget/sourceCompatibility 1.8, and matches MapsShareLinkResolver's own choice to use
    // plain java.net APIs instead of pulling in an HTTP dependency.
    private lateinit var serverSocket: ServerSocket
    private lateinit var baseUrl: String

    @Volatile
    private var receivedCookie: String? = null
    private var redirectLocation: String = ""
    private val executor = Executors.newSingleThreadExecutor()

    @Before
    fun setUp() {
        serverSocket = ServerSocket(0)
        baseUrl = "http://127.0.0.1:${serverSocket.localPort}"
        executor.submit {
            try {
                serverSocket.accept().use { respondWithRedirect(it) }
            } catch (e: Exception) {
                // Socket closed during tearDown before a request arrived - nothing to do.
            }
        }
    }

    private fun respondWithRedirect(client: Socket) {
        val reader = BufferedReader(InputStreamReader(client.getInputStream()))
        var line: String?
        while (true) {
            line = reader.readLine() ?: break
            if (line.isEmpty()) break
            if (line.startsWith("Cookie:", ignoreCase = true)) {
                receivedCookie = line.substringAfter(":").trim()
            }
        }

        val writer = PrintWriter(client.getOutputStream(), true)
        writer.print("HTTP/1.1 302 Found\r\n")
        writer.print("Location: $redirectLocation\r\n")
        writer.print("Content-Length: 0\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
    }

    @After
    fun tearDown() {
        serverSocket.close()
        executor.shutdownNow()
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
