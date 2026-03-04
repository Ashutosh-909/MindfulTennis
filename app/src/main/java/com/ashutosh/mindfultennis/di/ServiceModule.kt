package com.ashutosh.mindfultennis.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    // Service bindings (notification manager, etc.) will be added later
}
