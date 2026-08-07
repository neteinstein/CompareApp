package org.neteinstein.compareapp.utils

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves the short link Google Maps' "Share" button actually sends (e.g.
 * `https://maps.app.goo.gl/xxxx`) to its final `https://www.google.com/maps/...` URL, which -
 * unlike the short link - encodes the place's coordinates. [DeepLinkLocationParser] can't do
 * anything with the short link itself.
 *
 * Uses a plain [HttpURLConnection] rather than pulling in an HTTP dependency, matching
 * [org.neteinstein.compareapp.data.repository.UpdateRepositoryImpl]'s style. Redirects are
 * followed manually (`instanceFollowRedirects = false`) since the loop needs to inspect each
 * hop's host to know when it has reached a non-short-link destination.
 *
 * Google gates the redirect behind a `consent.google.com` GDPR interstitial for requests it
 * deems EU-originated - that hop has no coordinates, so left unhandled the chain would stop
 * there and [DeepLinkLocationParser] would silently find nothing to parse. [CONSENT_COOKIE]
 * short-circuits that hop for most requests; [continueParamOf] is a fallback for when it still
 * happens.
 */
object MapsShareLinkResolver {

    private const val TAG = "MapsShareLinkResolver"
    private const val MAX_REDIRECTS = 5
    private const val TIMEOUT_MS = 5000

    private val SHORT_LINK_HOSTS = setOf("maps.app.goo.gl", "goo.gl")

    // Any value starting with "YES" makes Google treat consent as already given.
    private const val CONSENT_COOKIE = "CONSENT=YES+cb"

    fun isShortLink(uri: Uri): Boolean = uri.host?.lowercase() in SHORT_LINK_HOSTS

    private fun isConsentInterstitial(uri: Uri): Boolean =
        uri.host?.lowercase() == "consent.google.com"

    /** The interstitial's `continue` query param is the destination the user was headed to. */
    private fun continueParamOf(consentUri: Uri): Uri? =
        consentUri.getQueryParameter("continue")?.let(Uri::parse)

    /** Follows redirects until they stop pointing at a short-link host, or [MAX_REDIRECTS] is hit. */
    suspend fun resolve(uri: Uri): Uri = withContext(Dispatchers.IO) {
        var current = uri
        for (hop in 0 until MAX_REDIRECTS) {
            val location = fetchRedirectLocation(current) ?: break
            current = Uri.parse(location)
            if (isConsentInterstitial(current)) {
                current = continueParamOf(current) ?: current
                break
            }
            if (!isShortLink(current)) break
        }
        current
    }

    private fun fetchRedirectLocation(uri: Uri): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "HEAD"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", "CompareApp-Android")
                setRequestProperty("Cookie", CONSENT_COOKIE)
            }
            val responseCode = connection.responseCode
            if (responseCode in 300..399) connection.getHeaderField("Location") else null
        } catch (e: IOException) {
            Log.w(TAG, "Could not resolve maps share link $uri", e)
            null
        } finally {
            connection?.disconnect()
        }
    }
}
