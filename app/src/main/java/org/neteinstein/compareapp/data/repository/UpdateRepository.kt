package org.neteinstein.compareapp.data.repository

import java.io.File

/**
 * Boundary between the update-check logic and GitHub Releases (production: [UpdateRepositoryImpl];
 * fakeable in tests).
 */
interface UpdateRepository {
    /**
     * Compares the installed build's `versionCode` against GitHub's latest release and returns
     * which [UpdateCheckResult] case applies.
     */
    suspend fun checkForUpdate(): UpdateCheckResult

    /** Downloads [update]'s APK to local storage, returning the file it was written to, or null on failure. */
    suspend fun downloadUpdate(update: AppUpdate): File?

    /**
     * Deletes any APK previously written by [downloadUpdate] - called once the system finishes
     * installing it (see [org.neteinstein.compareapp.receiver.UpdateApkCleanupReceiver]) so it
     * doesn't sit in the cache directory forever wasting space. A no-op if nothing was downloaded.
     */
    fun clearDownloadedUpdate()
}
