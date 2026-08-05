package org.neteinstein.compareapp.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [UpdateRepository] backed by the public GitHub Releases REST API for this project's own repo -
 * see `.github/workflows/release.yml` for how each release and its APK asset are produced. Uses a
 * plain [HttpURLConnection] + Android's built-in `org.json` rather than pulling in an HTTP/JSON
 * dependency, matching this app's existing repositories' style.
 *
 * The endpoint is unauthenticated (no API key needed to read public release metadata), but GitHub
 * 403s any request with no `User-Agent` header, so [fetchLatestReleaseJson] always sets one.
 */
@Singleton
class UpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UpdateRepository {

    companion object {
        private const val TAG = "UpdateRepository"
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/neteinstein/CompareUberVsBoltPriceApp/releases/latest"
        private const val UPDATE_CACHE_DIR_NAME = "updates"
    }

    override suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val update = parseGitHubReleaseResponse(fetchLatestReleaseJson())
            val currentVersionCode = currentVersionCode()
            if (update.versionCode > currentVersionCode) {
                UpdateCheckResult.UpdateAvailable(update)
            } else {
                UpdateCheckResult.UpToDate(currentVersionName())
            }
        } catch (e: IOException) {
            Log.e(TAG, "Update check failed", e)
            UpdateCheckResult.Failed(e.message ?: "Network error")
        } catch (e: JSONException) {
            Log.e(TAG, "Could not parse GitHub's release response", e)
            UpdateCheckResult.Failed(e.message ?: "Could not parse GitHub's release response")
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            UpdateCheckResult.Failed(e.message ?: "Update check failed")
        }
    }

    override suspend fun downloadUpdate(update: AppUpdate): File? = withContext(Dispatchers.IO) {
        try {
            val updatesDir = updatesDir()
            // Only one downloaded update is ever "current" - clear out anything left over from a
            // previous check before writing the new one, rather than letting stale APKs
            // accumulate in the cache dir forever.
            updatesDir.deleteRecursively()
            updatesDir.mkdirs()

            val apkFile = File(updatesDir, "CompareApp-${update.versionName}.apk")
            downloadToFile(url = update.apkDownloadUrl, destination = apkFile)
            apkFile
        } catch (e: IOException) {
            Log.e(TAG, "APK download failed", e)
            null
        }
    }

    override fun clearDownloadedUpdate() {
        updatesDir().deleteRecursively()
    }

    private fun updatesDir(): File = File(context.cacheDir, UPDATE_CACHE_DIR_NAME)

    // getPackageInfo(String, Int) is deprecated in favor of the PackageInfoFlags overload added
    // in API 33, but minSdk is 24 - there's no non-deprecated way to read this below API 33.
    @Suppress("DEPRECATION")
    private fun currentVersionCode(): Int =
        context.packageManager.getPackageInfo(context.packageName, 0).versionCode

    @Suppress("DEPRECATION")
    private fun currentVersionName(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"

    private fun fetchLatestReleaseJson(): String {
        val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "CompareApp-Android")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()
                throw IOException("GitHub API error $responseCode: $errorBody")
            }

            return connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadToFile(url: String, destination: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("APK download failed with HTTP $responseCode")
            }
            connection.inputStream.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Parses a GitHub "get the latest release" API response
 * (https://docs.github.com/en/rest/releases/releases#get-the-latest-release) into an [AppUpdate].
 * Kept as a standalone top-level function (rather than a private method) so it's directly
 * unit-testable without a fake HTTP layer.
 *
 * This project's release tags are `"v<versionName>.<buildNumber>"` (see
 * `.github/workflows/release.yml`'s `release_tag` step) - [AppUpdate.versionCode] is parsed from
 * the tag's trailing numeric segment (the CI run number, which is exactly what `versionCode` is
 * set to at build time - see `app/build.gradle`'s `BUILD_NUMBER`), since `versionName` itself is
 * static and can't be compared release over release. The APK asset is identified by filename
 * suffix (`.apk`) rather than by position, since the release's `assets` array could in principle
 * list other files first.
 */
internal fun parseGitHubReleaseResponse(json: String): AppUpdate {
    val root = JSONObject(json)
    val tagName = root.getString("tag_name")
    val versionName = tagName.removePrefix("v")
    val versionCode = versionName.substringAfterLast('.').toIntOrNull()
        ?: throw JSONException("Release tag \"$tagName\" has no trailing build number")

    val assets = root.optJSONArray("assets") ?: JSONArray()
    val apkAsset = (0 until assets.length())
        .map { index -> assets.getJSONObject(index) }
        .firstOrNull { asset -> asset.optString("name").endsWith(".apk", ignoreCase = true) }
        ?: throw JSONException("Latest release ($versionName) has no APK attached")

    return AppUpdate(
        versionName = versionName,
        versionCode = versionCode,
        apkDownloadUrl = apkAsset.getString("browser_download_url")
    )
}
