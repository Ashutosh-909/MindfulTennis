package com.ashutosh.mindfultennis.data.remote

import com.ashutosh.mindfultennis.data.remote.model.UserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Supabase `users` table operations.
 */
@Singleton
class SupabaseUserDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient,
) {
    private val table get() = supabaseClient.postgrest["users"]

    /** Upsert a user profile (insert or update if exists). */
    suspend fun upsertUser(user: UserDto) {
        table.upsert(user)
    }

    /** Get a user by their ID. */
    suspend fun getUser(userId: String): UserDto? {
        return table
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<UserDto>()
    }

    /** Delete a user and all their data. */
    suspend fun deleteUser(userId: String) {
        table.delete { filter { eq("id", userId) } }
    }
}
