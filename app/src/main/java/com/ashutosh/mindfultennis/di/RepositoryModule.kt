package com.ashutosh.mindfultennis.di

import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthRepositoryImpl
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    // Additional repository bindings will be added in later milestones:
    // abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
    // abstract fun bindFocusPointRepository(impl: FocusPointRepositoryImpl): FocusPointRepository
    // abstract fun bindOpponentRepository(impl: OpponentRepositoryImpl): OpponentRepository
    // abstract fun bindPartnerRepository(impl: PartnerRepositoryImpl): PartnerRepository
}
