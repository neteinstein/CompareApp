package org.neteinstein.compareapp.helpers

import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.neteinstein.compareapp.data.repository.AppRepository
import org.neteinstein.compareapp.data.repository.LocationRepository
import org.neteinstein.compareapp.ui.screens.MainViewModel

object TestViewModelFactory {
    fun createTestViewModel(
        locationRepository: LocationRepository? = null,
        appRepository: AppRepository? = null
    ): MainViewModel {
        val mockLocationRepo = locationRepository
            ?: Mockito.mock(LocationRepository::class.java)
        // MainViewModel.init calls checkRequiredApps() and destructures the result, so a
        // caller-supplied mock must stub it; when we create the mock ourselves, give it a
        // harmless default so callers that don't care about installed-app state don't NPE.
        val mockAppRepo = appRepository ?: Mockito.mock(AppRepository::class.java).also {
            `when`(it.checkRequiredApps()).thenReturn(Pair(true, true))
        }

        return MainViewModel(mockLocationRepo, mockAppRepo)
    }
}
