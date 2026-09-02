package org.neteinstein.compareapp.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.neteinstein.compareapp.data.repository.AppRepository
import org.neteinstein.compareapp.data.repository.AppRepositoryImpl
import org.neteinstein.compareapp.data.repository.ComparisonConfigRepository
import org.neteinstein.compareapp.data.repository.ComparisonConfigRepositoryImpl
import org.neteinstein.compareapp.data.repository.LocationRepository
import org.neteinstein.compareapp.data.repository.LocationRepositoryImpl
import org.neteinstein.compareapp.data.repository.UpdateRepository
import org.neteinstein.compareapp.data.repository.UpdateRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindAppRepository(
        appRepositoryImpl: AppRepositoryImpl
    ): AppRepository

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(
        updateRepositoryImpl: UpdateRepositoryImpl
    ): UpdateRepository

    @Binds
    @Singleton
    abstract fun bindComparisonConfigRepository(
        comparisonConfigRepositoryImpl: ComparisonConfigRepositoryImpl
    ): ComparisonConfigRepository
}
