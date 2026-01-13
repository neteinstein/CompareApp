package org.neteinstein.compareapp.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppRepository {

    override fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AppRepository", "App not found: $packageName")
            false
        }
    }

    override fun checkRequiredApps(): Pair<Boolean, Boolean> {
        val isUberInstalled = isAppInstalled("com.ubercab")
        val isBoltInstalled = isAppInstalled("ee.mtakso.client")
        return Pair(isUberInstalled, isBoltInstalled)
    }
}
