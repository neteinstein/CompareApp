package org.neteinstein.compareapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands a downloaded APK (see `UpdateRepository.downloadUpdate`) to the system Package Installer,
 * and checks/deep-links into the "install unknown apps" (sideloading) permission screen that gates
 * it - the API 26+ replacement for the old device-wide "Unknown sources" toggle, granted per-app
 * instead.
 */
@Singleton
class AppUpdateInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * True once the user has allowed this app to install packages from outside the Play Store.
     * The per-app permission this checks was introduced in API 26; before that, sideloading was
     * gated only by the device-wide "Unknown sources" toggle, so there's nothing app-specific to
     * check and this app was always allowed to try.
     */
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /** Deep-links into this app's own "install unknown apps" toggle in system Settings. */
    fun openInstallPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Launches the system Package Installer for [apkFile]. Requires [canInstallPackages] to
     * already be true - callers are expected to check that (and route to
     * [openInstallPermissionSettings] instead) before ever calling this.
     *
     * Uses a [FileProvider] `content://` URI rather than a plain `file://` one: a cache-dir-backed
     * file can't be shared as a raw `file://` URI with another app (the Package Installer) under
     * this app's targetSdk - that throws `FileUriExposedException` - so the manifest declares a
     * `FileProvider` whose authority matches [FILE_PROVIDER_AUTHORITY_SUFFIX] below, scoped to
     * exactly the cache subdirectory the update APK is downloaded into (see
     * `update_file_paths.xml`, `UpdateRepositoryImpl`).
     */
    fun installPackage(apkFile: File) {
        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.$FILE_PROVIDER_AUTHORITY_SUFFIX", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private companion object {
        // Must exactly match the FileProvider <provider> authority declared in
        // AndroidManifest.xml (that side prefixes it with "${applicationId}.").
        const val FILE_PROVIDER_AUTHORITY_SUFFIX = "update.fileprovider"
    }
}
