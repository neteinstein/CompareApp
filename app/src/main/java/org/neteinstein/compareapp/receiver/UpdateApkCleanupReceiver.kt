package org.neteinstein.compareapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.neteinstein.compareapp.data.repository.UpdateRepository
import javax.inject.Inject

/**
 * Deletes the APK `UpdateRepositoryImpl` downloaded to `context.cacheDir` once this app finishes
 * being updated through it, so it doesn't sit there wasting space indefinitely.
 *
 * Listens for [Intent.ACTION_MY_PACKAGE_REPLACED] specifically: it's one of the few broadcasts
 * exempted from Android 8+'s implicit-broadcast background restrictions (delivered only to the
 * app that was just updated, and only via a manifest-declared receiver like this one - a
 * dynamically registered one wouldn't survive the process being killed during install, which is
 * the common case), making it the only reliable signal for "the update this app itself downloaded
 * just finished installing". Deleting the file any earlier - e.g. right after firing the install
 * `Intent` in `AppUpdateInstaller.installPackage` - would be unsafe, since the system Package
 * Installer may still be reading it via the `FileProvider` `content://` URI at that point.
 */
@AndroidEntryPoint
class UpdateApkCleanupReceiver : BroadcastReceiver() {

    @Inject
    lateinit var updateRepository: UpdateRepository

    override fun onReceive(context: Context, intent: Intent) {
        // Hilt's @AndroidEntryPoint bytecode transform injects fields for BroadcastReceivers by
        // instrumenting the start of this method directly - unlike Activity/Service/Fragment,
        // BroadcastReceiver.onReceive() is abstract with no super implementation to call into.
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateRepository.clearDownloadedUpdate()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
