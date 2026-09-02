package org.neteinstein.compareapp.data.repository

interface AppRepository {
    fun isAppInstalled(packageName: String): Boolean
    fun checkRequiredApps(): Pair<Boolean, Boolean>
    fun checkFoodApps(): Pair<Boolean, Boolean>
}
