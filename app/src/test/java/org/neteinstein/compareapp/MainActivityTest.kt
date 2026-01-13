package org.neteinstein.compareapp

import android.content.Context
import android.content.pm.PackageManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.neteinstein.compareapp.data.repository.AppRepositoryImpl
import org.neteinstein.compareapp.helpers.TestViewModelFactory
import org.neteinstein.compareapp.ui.screens.MainViewModel
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URLDecoder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityTest {

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
    fun testCreateUberDeepLink_encodesAddressesCorrectly() {
        // Given
        val pickup = "123 Main St, New York, NY"
        val dropoff = "456 Park Ave, New York, NY"
        val pickupCoords = Pair(40.7128, -74.0060)
        val dropoffCoords = Pair(40.7589, -73.9851)

        // Create a minimal ViewModel just for testing the method
        val viewModel = createTestViewModel()

        // When
        val deepLink = viewModel.createUberDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        assertTrue(deepLink.startsWith("uber://?action=setPickup"))
        assertTrue(deepLink.contains("pickup[formatted_address]="))
        assertTrue(deepLink.contains("dropoff[formatted_address]="))
        
        // Verify encoding by decoding
        val decodedLink = URLDecoder.decode(deepLink, "UTF-8")
        assertTrue(decodedLink.contains(pickup))
        assertTrue(decodedLink.contains(dropoff))
    }

    @Test
    fun testCreateUberDeepLink_withSpecialCharacters() {
        // Given
        val pickup = "Times Square & 42nd St"
        val dropoff = "Central Park, New York"
        val pickupCoords = null
        val dropoffCoords = null

        val viewModel = createTestViewModel()

        // When
        val deepLink = viewModel.createUberDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        assertTrue(deepLink.startsWith("uber://?action=setPickup"))
        assertTrue(deepLink.contains("pickup[formatted_address]="))
        assertTrue(deepLink.contains("dropoff[formatted_address]="))
        // Special characters should be URL-encoded
        assertTrue(deepLink.contains("%"))
    }

    @Test
    fun testCreateUberDeepLink_withEmptyStrings() {
        // Given
        val pickup = ""
        val dropoff = ""
        val pickupCoords = null
        val dropoffCoords = null

        val viewModel = createTestViewModel()

        // When
        val deepLink = viewModel.createUberDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        assertTrue(deepLink.startsWith("uber://?action=setPickup"))
        // Should handle empty strings gracefully
        assertNotNull(deepLink)
    }

    @Test
    fun testCreateUberDeepLink_formatIsCorrect() {
        // Given
        val pickup = "Pickup Location"
        val dropoff = "Dropoff Location"
        val pickupCoords = null
        val dropoffCoords = null

        val viewModel = createTestViewModel()

        // When
        val deepLink = viewModel.createUberDeepLink(pickup, dropoff, pickupCoords, dropoffCoords)

        // Then
        // Verify the exact format expected by Uber
        val expectedStart = "uber://?action=setPickup&pickup[formatted_address]="
        assertTrue(deepLink.startsWith(expectedStart))
        assertTrue(deepLink.contains("&dropoff[formatted_address]="))
    }

    @Test
    fun testIsAppInstalled_returnsFalseForNonExistentPackage() {
        // Given
        val nonExistentPackage = "com.nonexistent.app"
        `when`(mockPackageManager.getPackageInfo(nonExistentPackage, PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())

        // When
        val result = appRepository.isAppInstalled(nonExistentPackage)

        // Then
        assertFalse(result)
    }

    @Test
    fun testCheckRequiredApps_returnsCorrectStatus() {
        // Given
        `when`(mockPackageManager.getPackageInfo("com.ubercab", PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())
        `when`(mockPackageManager.getPackageInfo("ee.mtakso.client", PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException())

        // When
        val (isUberInstalled, isBoltInstalled) = appRepository.checkRequiredApps()

        // Then
        // In Robolectric test environment, these apps won't be installed
        assertFalse(isUberInstalled)
        assertFalse(isBoltInstalled)
    }

    @Test
    fun testIsAppInstalled_handlesValidPackageName() {
        // Given - using Android system package that should exist in test environment
        val systemPackage = "android"
        `when`(mockPackageManager.getPackageInfo(systemPackage, PackageManager.GET_ACTIVITIES))
            .thenReturn(android.content.pm.PackageInfo())

        // When
        val result = appRepository.isAppInstalled(systemPackage)

        // Then
        assertTrue(result)
    }

    private fun createTestViewModel(): MainViewModel {
        return TestViewModelFactory.createTestViewModel(appRepository = appRepository)
    }
}
