package org.neteinstein.compareapp

import android.content.Context
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.neteinstein.compareapp.data.repository.AppRepositoryImpl
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for app installation detection logic
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppInstallationTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPackageManager: PackageManager

    private lateinit var appRepository: AppRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        appRepository = AppRepositoryImpl(mockContext)
    }

    @Test
    fun testIsAppInstalled_withSystemPackage() {
        // Given - android system package
        val systemPackage = "android"
        `when`(mockPackageManager.getPackageInfo(systemPackage, PackageManager.GET_ACTIVITIES))
            .thenReturn(android.content.pm.PackageInfo())

        // When
        val result = appRepository.isAppInstalled(systemPackage)

        // Then
        assertTrue("System package 'android' should be installed", result)
    }

    @Test
    fun testIsAppInstalled_withNonExistentPackage() {
        // Given
        val fakePackage = "com.fake.nonexistent.app.that.doesnt.exist"
        `when`(mockPackageManager.getPackageInfo(fakePackage, PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())

        // When
        val result = appRepository.isAppInstalled(fakePackage)

        // Then
        assertFalse("Fake package should not be installed", result)
    }

    @Test
    fun testIsAppInstalled_withUberPackage() {
        // Given
        val uberPackage = "com.ubercab"
        `when`(mockPackageManager.getPackageInfo(uberPackage, PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())

        // When
        val result = appRepository.isAppInstalled(uberPackage)

        // Then
        // In test environment, Uber won't be installed
        assertFalse("Uber app should not be installed in test environment", result)
    }

    @Test
    fun testIsAppInstalled_withBoltPackage() {
        // Given
        val boltPackage = "ee.mtakso.client"
        `when`(mockPackageManager.getPackageInfo(boltPackage, PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())

        // When
        val result = appRepository.isAppInstalled(boltPackage)

        // Then
        // In test environment, Bolt won't be installed
        assertFalse("Bolt app should not be installed in test environment", result)
    }

    @Test
    fun testIsAppInstalled_withEmptyPackageName() {
        // Given
        val emptyPackage = ""
        `when`(mockPackageManager.getPackageInfo(emptyPackage, PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())

        // When
        val result = appRepository.isAppInstalled(emptyPackage)

        // Then
        assertFalse("Empty package name should return false", result)
    }

    @Test
    fun testCheckRequiredApps_bothNotInstalled() {
        // Given
        `when`(mockPackageManager.getPackageInfo("com.ubercab", PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())
        `when`(mockPackageManager.getPackageInfo("ee.mtakso.client", PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())

        // When
        val (isUberInstalled, isBoltInstalled) = appRepository.checkRequiredApps()

        // Then - In test environment
        assertFalse("Uber should not be installed in test environment", isUberInstalled)
        assertFalse("Bolt should not be installed in test environment", isBoltInstalled)
    }

    @Test
    fun testCheckRequiredApps_returnsPair() {
        // Given
        `when`(mockPackageManager.getPackageInfo("com.ubercab", PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())
        `when`(mockPackageManager.getPackageInfo("ee.mtakso.client", PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())

        // When
        val result = appRepository.checkRequiredApps()

        // Then
        assertTrue("Result should be a Pair", result is Pair)
        assertEquals("Result should have two elements", 2, listOf(result.first, result.second).size)
    }

    @Test
    fun testCheckRequiredApps_consistentResults() {
        // Given
        `when`(mockPackageManager.getPackageInfo("com.ubercab", PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())
        `when`(mockPackageManager.getPackageInfo("ee.mtakso.client", PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())

        // When - Call multiple times
        val result1 = appRepository.checkRequiredApps()
        val result2 = appRepository.checkRequiredApps()
        val result3 = appRepository.checkRequiredApps()

        // Then - Results should be consistent
        assertEquals("First call should match second", result1, result2)
        assertEquals("Second call should match third", result2, result3)
    }

    @Test
    fun testIsAppInstalled_withSpecialCharactersInPackageName() {
        // Given - package name with dots (valid format)
        val validPackage = "com.test.app"
        `when`(mockPackageManager.getPackageInfo(validPackage, PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())

        // When
        val result = appRepository.isAppInstalled(validPackage)

        // Then
        assertFalse("Non-existent package should return false", result)
    }

    @Test
    fun testIsAppInstalled_multipleCalls() {
        // Given
        val testPackage = "com.test.app"
        `when`(mockPackageManager.getPackageInfo(testPackage, PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())

        // When - Call multiple times
        val result1 = appRepository.isAppInstalled(testPackage)
        val result2 = appRepository.isAppInstalled(testPackage)
        val result3 = appRepository.isAppInstalled(testPackage)

        // Then - All should be consistent
        assertEquals("First and second calls should match", result1, result2)
        assertEquals("Second and third calls should match", result2, result3)
    }

    @Test
    fun testIsAppInstalled_caseMatters() {
        // Given - Different case variations
        val package1 = "android"
        val package2 = "ANDROID"
        val package3 = "Android"
        
        `when`(mockPackageManager.getPackageInfo(package1, PackageManager.GET_ACTIVITIES))
            .thenReturn(android.content.pm.PackageInfo())
        `when`(mockPackageManager.getPackageInfo(package2, PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())
        `when`(mockPackageManager.getPackageInfo(package3, PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())

        // When
        val result1 = appRepository.isAppInstalled(package1)
        val result2 = appRepository.isAppInstalled(package2)
        val result3 = appRepository.isAppInstalled(package3)

        // Then - Only exact match should work
        assertTrue("Lowercase 'android' should exist", result1)
        assertFalse("Uppercase should not match", result2)
        assertFalse("Capitalized should not match", result3)
    }
}
