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

    companion object {
        private const val UBER_PACKAGE_NAME = "com.ubercab"
        private const val BOLT_PACKAGE_NAME = "ee.mtakso.client"
        private const val UBER_EATS_PACKAGE_NAME = "com.ubercab.eats"
        private const val BOLT_FOOD_PACKAGE_NAME = "com.bolt.deliveryclient"
    }

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
        val isUberInstalled = isAppInstalled(UBER_PACKAGE_NAME)
        val isBoltInstalled = isAppInstalled(BOLT_PACKAGE_NAME)
        return Pair(isUberInstalled, isBoltInstalled)
    }

    override fun checkFoodApps(): Pair<Boolean, Boolean> {
        val isUberEatsInstalled = isAppInstalled(UBER_EATS_PACKAGE_NAME)
        val isBoltFoodInstalled = isAppInstalled(BOLT_FOOD_PACKAGE_NAME)
        return Pair(isUberEatsInstalled, isBoltFoodInstalled)
    }
}
