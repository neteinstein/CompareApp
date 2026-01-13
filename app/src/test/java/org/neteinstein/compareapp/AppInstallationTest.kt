package org.neteinstein.compareapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for app installation detection logic
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppInstallationTest {

    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .get()
    }

    @Test
    fun testIsAppInstalled_withSystemPackage() {
        // Given - android system package
        val systemPackage = "android"

        // When
        val result = activity.isAppInstalled(systemPackage)

        // Then
        assertTrue("System package 'android' should be installed", result)
    }

    @Test
    fun testIsAppInstalled_withNonExistentPackage() {
        // Given
        val fakePackage = "com.fake.nonexistent.app.that.doesnt.exist"

        // When
        val result = activity.isAppInstalled(fakePackage)

        // Then
        assertFalse("Fake package should not be installed", result)
    }

    @Test
    fun testIsAppInstalled_withUberPackage() {
        // Given
        val uberPackage = "com.ubercab"

        // When
        val result = activity.isAppInstalled(uberPackage)

        // Then
        // In test environment, Uber won't be installed
        assertFalse("Uber app should not be installed in test environment", result)
    }

    @Test
    fun testIsAppInstalled_withBoltPackage() {
        // Given
        val boltPackage = "ee.mtakso.client"

        // When
        val result = activity.isAppInstalled(boltPackage)

        // Then
        // In test environment, Bolt won't be installed
        assertFalse("Bolt app should not be installed in test environment", result)
    }

    @Test
    fun testIsAppInstalled_withEmptyPackageName() {
        // Given
        val emptyPackage = ""

        // When
        val result = activity.isAppInstalled(emptyPackage)

        // Then
        assertFalse("Empty package name should return false", result)
    }

    @Test
    fun testCheckRequiredApps_bothNotInstalled() {
        // When
        val (isUberInstalled, isBoltInstalled) = activity.checkRequiredApps()

        // Then - In test environment
        assertFalse("Uber should not be installed in test environment", isUberInstalled)
        assertFalse("Bolt should not be installed in test environment", isBoltInstalled)
    }

    @Test
    fun testCheckRequiredApps_returnsPair() {
        // When
        val result = activity.checkRequiredApps()

        // Then
        assertTrue("Result should be a Pair", result is Pair)
        assertEquals("Result should have two elements", 2, listOf(result.first, result.second).size)
    }

    @Test
    fun testCheckRequiredApps_consistentResults() {
        // When - Call multiple times
        val result1 = activity.checkRequiredApps()
        val result2 = activity.checkRequiredApps()
        val result3 = activity.checkRequiredApps()

        // Then - Results should be consistent
        assertEquals("First call should match second", result1, result2)
        assertEquals("Second call should match third", result2, result3)
    }

    @Test
    fun testHasLocationPermission_returnsFalseByDefault() {
        // When
        val result = activity.hasLocationPermission()

        // Then - In test environment without granted permissions
        assertFalse("Location permission should not be granted by default in tests", result)
    }

    @Test
    fun testIsAppInstalled_withSpecialCharactersInPackageName() {
        // Given - package name with dots (valid format)
        val validPackage = "com.test.app"

        // When
        val result = activity.isAppInstalled(validPackage)

        // Then
        assertFalse("Non-existent package should return false", result)
    }

    @Test
    fun testIsAppInstalled_multipleCalls() {
        // Given
        val testPackage = "com.test.app"

        // When - Call multiple times
        val result1 = activity.isAppInstalled(testPackage)
        val result2 = activity.isAppInstalled(testPackage)
        val result3 = activity.isAppInstalled(testPackage)

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

        // When
        val result1 = activity.isAppInstalled(package1)
        val result2 = activity.isAppInstalled(package2)
        val result3 = activity.isAppInstalled(package3)

        // Then - Only exact match should work
        assertTrue("Lowercase 'android' should exist", result1)
        assertFalse("Uppercase should not match", result2)
        assertFalse("Capitalized should not match", result3)
    }
}
