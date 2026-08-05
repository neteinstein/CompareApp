package org.neteinstein.compareapp.data.repository

/**
 * A GitHub release newer than the currently installed build, as identified by
 * [UpdateRepository.checkForUpdate]. [versionCode] is parsed from the release tag's trailing
 * numeric segment (the CI run number baked into `versionCode` at build time - see
 * `app/build.gradle`'s `BUILD_NUMBER`/`.github/workflows/release.yml`), not [versionName]: this
 * app's `versionName` is a static "1.0.0" that never changes between builds, so it can't be used
 * to detect whether a newer build exists - `versionCode` is the only value that reliably increases
 * release over release.
 */
data class AppUpdate(
    val versionName: String,
    val versionCode: Int,
    val apkDownloadUrl: String
)

/** Outcome of [UpdateRepository.checkForUpdate]. */
sealed class UpdateCheckResult {
    /** The installed build is already the latest one published on GitHub Releases. */
    data class UpToDate(val currentVersionName: String) : UpdateCheckResult()

    /** GitHub's latest release is newer than the installed build. */
    data class UpdateAvailable(val update: AppUpdate) : UpdateCheckResult()

    /** The check itself failed (network error, unexpected GitHub response shape, etc). */
    data class Failed(val message: String) : UpdateCheckResult()
}
