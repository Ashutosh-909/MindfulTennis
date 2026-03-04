package com.ashutosh.mindfultennis.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    // Repository bindings will be added as implementations are created
    // e.g.:
    // @Binds
    // @Singleton
    // abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
