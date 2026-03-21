package com.ashutosh.mindfultennis.data.repository

import com.ashutosh.mindfultennis.domain.model.Rating
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.domain.model.SetScore
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for session-related operations.
 * Reading from Room (single source of truth), writing to Room first then syncing to Supabase.
 */
interface SessionRepository {

    // ── Session CRUD ──────────────────────────────────────────────────

    suspend fun createSession(session: Session): Result<Session>

    suspend fun updateSession(session: Session): Result<Unit>

    suspend fun getSession(sessionId: String): Result<Session?>

    fun observeSession(sessionId: String): Flow<Session?>

    fun observeAllSessions(userId: String): Flow<List<Session>>

    fun observeSessionsInRange(userId: String, fromMs: Long, toMs: Long): Flow<List<Session>>

    suspend fun getActiveSession(userId: String): Session?

    fun observeActiveSession(userId: String): Flow<Session?>

    suspend fun endSession(sessionId: String, endedAt: Long, notes: String?): Result<Unit>

    suspend fun deleteSession(sessionId: String): Result<Unit>

    // ── Self Ratings ──────────────────────────────────────────────────

    suspend fun saveSelfRatings(sessionId: String, ratings: List<Rating>): Result<Unit>

    suspend fun getSelfRatings(sessionId: String): Result<List<Rating>>

    fun observeSelfRatings(sessionId: String): Flow<List<Rating>>

    // ── Partner Ratings ───────────────────────────────────────────────

    suspend fun savePartnerRatings(sessionId: String, ratings: List<Rating>): Result<Unit>

    suspend fun getPartnerRatings(sessionId: String): Result<List<Rating>>

    fun observePartnerRatings(sessionId: String): Flow<List<Rating>>

    // ── Set Scores ────────────────────────────────────────────────────

    suspend fun saveSetScores(sessionId: String, scores: List<SetScore>): Result<Unit>

    suspend fun getSetScores(sessionId: String): Result<List<SetScore>>

    fun observeSetScores(sessionId: String): Flow<List<SetScore>>

    // ── Batch queries (for dashboard) ─────────────────────────────────

    suspend fun getSelfRatingsForSessions(sessionIds: List<String>): Result<List<Rating>>

    suspend fun getPartnerRatingsForSessions(sessionIds: List<String>): Result<List<Rating>>

    suspend fun getSetScoresForSessions(sessionIds: List<String>): Result<List<SetScore>>

    // ── Bulk / Cleanup ────────────────────────────────────────────────

    suspend fun deleteAllForUser(userId: String)
}
