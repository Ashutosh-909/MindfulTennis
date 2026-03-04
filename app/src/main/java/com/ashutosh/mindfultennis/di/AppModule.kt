package com.ashutosh.mindfultennis.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.ashutosh.mindfultennis.BuildConfig
import com.ashutosh.mindfultennis.data.local.db.MindfulDatabase
import com.ashutosh.mindfultennis.data.local.db.dao.FocusPointDao
import com.ashutosh.mindfultennis.data.local.db.dao.OpponentDao
import com.ashutosh.mindfultennis.data.local.db.dao.PartnerDao
import com.ashutosh.mindfultennis.data.local.db.dao.PartnerRatingDao
import com.ashutosh.mindfultennis.data.local.db.dao.SelfRatingDao
import com.ashutosh.mindfultennis.data.local.db.dao.SessionDao
import com.ashutosh.mindfultennis.data.local.db.dao.SetScoreDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                scheme = "com.ashutosh.mindfultennis"
                host = "callback"
            }
            install(Postgrest)
            install(Realtime)
        }
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    // ── Room Database ─────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideMindfulDatabase(@ApplicationContext context: Context): MindfulDatabase {
        return Room.databaseBuilder(
            context,
            MindfulDatabase::class.java,
            MindfulDatabase.DATABASE_NAME,
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // ── DAOs ──────────────────────────────────────────────────────────

    @Provides
    fun provideSessionDao(database: MindfulDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideSelfRatingDao(database: MindfulDatabase): SelfRatingDao = database.selfRatingDao()

    @Provides
    fun providePartnerRatingDao(database: MindfulDatabase): PartnerRatingDao = database.partnerRatingDao()

    @Provides
    fun provideFocusPointDao(database: MindfulDatabase): FocusPointDao = database.focusPointDao()

    @Provides
    fun provideOpponentDao(database: MindfulDatabase): OpponentDao = database.opponentDao()

    @Provides
    fun providePartnerDao(database: MindfulDatabase): PartnerDao = database.partnerDao()

    @Provides
    fun provideSetScoreDao(database: MindfulDatabase): SetScoreDao = database.setScoreDao()
}
