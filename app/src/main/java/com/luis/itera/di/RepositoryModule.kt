package com.luis.itera.di

import com.luis.itera.data.repository.ExerciseRepositoryImpl
import com.luis.itera.data.repository.HydrationRepositoryImpl
import com.luis.itera.data.repository.SessionRepositoryImpl
import com.luis.itera.data.repository.StatisticsRepositoryImpl
import com.luis.itera.data.repository.UserPrefsRepositoryImpl
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.HydrationRepository
import com.luis.itera.domain.repository.SessionRepository
import com.luis.itera.domain.repository.StatisticsRepository
import com.luis.itera.domain.repository.UserPrefsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindHydrationRepository(impl: HydrationRepositoryImpl): HydrationRepository

    @Binds
    @Singleton
    abstract fun bindUserPrefsRepository(impl: UserPrefsRepositoryImpl): UserPrefsRepository

    @Binds
    @Singleton
    abstract fun bindStatisticsRepository(impl: StatisticsRepositoryImpl): StatisticsRepository
}