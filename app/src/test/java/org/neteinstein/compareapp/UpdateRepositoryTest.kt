package org.neteinstein.compareapp

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.neteinstein.compareapp.data.repository.parseGitHubReleaseResponse
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [parseGitHubReleaseResponse] - uses Robolectric (rather than plain JUnit) because
 * `org.json` classes are Android SDK stubs on the plain unit-test classpath and throw unless
 * backed by Robolectric's real framework implementation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UpdateRepositoryTest {

    @Test
    fun testParseGitHubReleaseResponse_extractsVersionAndDownloadUrl() {
        val json = """
            {
              "tag_name": "v1.0.0.123",
              "assets": [
                { "name": "CompareApp-1.0.0.123.apk", "browser_download_url": "https://example.com/CompareApp-1.0.0.123.apk" }
              ]
            }
        """.trimIndent()

        val update = parseGitHubReleaseResponse(json)

        assertEquals("1.0.0.123", update.versionName)
        assertEquals(123, update.versionCode)
        assertEquals("https://example.com/CompareApp-1.0.0.123.apk", update.apkDownloadUrl)
    }

    @Test
    fun testParseGitHubReleaseResponse_picksApkAssetAmongOthers() {
        val json = """
            {
              "tag_name": "v1.0.0.456",
              "assets": [
                { "name": "checksums.txt", "browser_download_url": "https://example.com/checksums.txt" },
                { "name": "CompareApp-1.0.0.456.apk", "browser_download_url": "https://example.com/app.apk" }
              ]
            }
        """.trimIndent()

        val update = parseGitHubReleaseResponse(json)

        assertEquals("https://example.com/app.apk", update.apkDownloadUrl)
    }

    @Test
    fun testParseGitHubReleaseResponse_throwsWhenNoApkAsset() {
        val json = """
            {
              "tag_name": "v1.0.0.789",
              "assets": [
                { "name": "checksums.txt", "browser_download_url": "https://example.com/checksums.txt" }
              ]
            }
        """.trimIndent()

        assertThrows(JSONException::class.java) { parseGitHubReleaseResponse(json) }
    }

    @Test
    fun testParseGitHubReleaseResponse_throwsWhenTagHasNoBuildNumber() {
        val json = """
            {
              "tag_name": "v1.0.0-beta",
              "assets": [
                { "name": "CompareApp.apk", "browser_download_url": "https://example.com/app.apk" }
              ]
            }
        """.trimIndent()

        assertThrows(JSONException::class.java) { parseGitHubReleaseResponse(json) }
    }
}
