package com.ashutosh.mindfultennis.di

import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthRepositoryImpl
import com.ashutosh.mindfultennis.data.repository.FocusPointRepository
import com.ashutosh.mindfultennis.data.repository.FocusPointRepositoryImpl
import com.ashutosh.mindfultennis.data.repository.OpponentRepository
import com.ashutosh.mindfultennis.data.repository.OpponentRepositoryImpl
import com.ashutosh.mindfultennis.data.repository.PartnerRepository
import com.ashutosh.mindfultennis.data.repository.PartnerRepositoryImpl
import com.ashutosh.mindfultennis.data.repository.SessionRepository
import com.ashutosh.mindfultennis.data.repository.SessionRepositoryImpl
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

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindFocusPointRepository(impl: FocusPointRepositoryImpl): FocusPointRepository

    @Binds
    @Singleton
    abstract fun bindOpponentRepository(impl: OpponentRepositoryImpl): OpponentRepository

    @Binds
    @Singleton
    abstract fun bindPartnerRepository(impl: PartnerRepositoryImpl): PartnerRepository
}
