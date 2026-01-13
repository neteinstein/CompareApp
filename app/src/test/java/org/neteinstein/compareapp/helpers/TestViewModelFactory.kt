package org.neteinstein.compareapp.helpers

import org.mockito.Mockito
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
        val mockAppRepo = appRepository 
            ?: Mockito.mock(AppRepository::class.java)
        
        return MainViewModel(mockLocationRepo, mockAppRepo)
    }
}
