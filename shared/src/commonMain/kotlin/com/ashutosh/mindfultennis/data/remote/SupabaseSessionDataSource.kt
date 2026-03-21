package com.ashutosh.mindfultennis.data.remote

import com.ashutosh.mindfultennis.data.remote.model.FocusPointDto
import com.ashutosh.mindfultennis.data.remote.model.OpponentDto
import com.ashutosh.mindfultennis.data.remote.model.PartnerDto
import com.ashutosh.mindfultennis.data.remote.model.PartnerRatingDto
import com.ashutosh.mindfultennis.data.remote.model.SelfRatingDto
import com.ashutosh.mindfultennis.data.remote.model.SessionDto
import com.ashutosh.mindfultennis.data.remote.model.SetScoreDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

/**
 * Data source for Supabase session-related table operations.
 * Handles sessions, self_ratings, partner_ratings, set_scores,
 * focus_points, opponents, and partners tables.
 */
class SupabaseSessionDataSource(
    private val supabaseClient: SupabaseClient,
) {
    // ── Sessions ──────────────────────────────────────────────────────────

    private val sessions get() = supabaseClient.postgrest["sessions"]

    suspend fun upsertSession(session: SessionDto) {
        sessions.upsert(session)
    }

    suspend fun getSession(sessionId: String): SessionDto? {
        return sessions
            .select { filter { eq("id", sessionId) } }
            .decodeSingleOrNull<SessionDto>()
    }

    suspend fun getSessionsForUser(userId: String): List<SessionDto> {
        return sessions
            .select { filter { eq("user_id", userId) } }
            .decodeList<SessionDto>()
    }

    suspend fun getSessionsUpdatedAfter(userId: String, afterMs: Long): List<SessionDto> {
        return sessions
            .select {
                filter {
                    eq("user_id", userId)
                    gt("updated_at", afterMs)
                }
            }
            .decodeList<SessionDto>()
    }

    suspend fun deleteSession(sessionId: String) {
        sessions.delete { filter { eq("id", sessionId) } }
    }

    // ── Self Ratings ──────────────────────────────────────────────────────

    private val selfRatings get() = supabaseClient.postgrest["self_ratings"]

    suspend fun upsertSelfRatings(ratings: List<SelfRatingDto>) {
        if (ratings.isEmpty()) return
        selfRatings.upsert(ratings)
    }

    suspend fun getSelfRatingsForSession(sessionId: String): List<SelfRatingDto> {
        return selfRatings
            .select { filter { eq("session_id", sessionId) } }
            .decodeList<SelfRatingDto>()
    }

    suspend fun getSelfRatingsForSessions(sessionIds: List<String>): List<SelfRatingDto> {
        if (sessionIds.isEmpty()) return emptyList()
        return selfRatings
            .select {
                filter { isIn("session_id", sessionIds) }
                limit(sessionIds.size.toLong() * 8 + 100)
            }
            .decodeList<SelfRatingDto>()
    }

    suspend fun deleteSelfRatingsForSession(sessionId: String) {
        selfRatings.delete { filter { eq("session_id", sessionId) } }
    }

    // ── Partner Ratings ───────────────────────────────────────────────────

    private val partnerRatings get() = supabaseClient.postgrest["partner_ratings"]

    suspend fun upsertPartnerRatings(ratings: List<PartnerRatingDto>) {
        if (ratings.isEmpty()) return
        partnerRatings.upsert(ratings)
    }

    suspend fun getPartnerRatingsForSession(sessionId: String): List<PartnerRatingDto> {
        return partnerRatings
            .select { filter { eq("session_id", sessionId) } }
            .decodeList<PartnerRatingDto>()
    }

    suspend fun getPartnerRatingsForSessions(sessionIds: List<String>): List<PartnerRatingDto> {
        if (sessionIds.isEmpty()) return emptyList()
        return partnerRatings
            .select {
                filter { isIn("session_id", sessionIds) }
                limit(sessionIds.size.toLong() * 8 + 100)
            }
            .decodeList<PartnerRatingDto>()
    }

    suspend fun deletePartnerRatingsForSession(sessionId: String) {
        partnerRatings.delete { filter { eq("session_id", sessionId) } }
    }

    // ── Set Scores ────────────────────────────────────────────────────────

    private val setScores get() = supabaseClient.postgrest["set_scores"]

    suspend fun upsertSetScores(scores: List<SetScoreDto>) {
        if (scores.isEmpty()) return
        setScores.upsert(scores)
    }

    suspend fun getSetScoresForSession(sessionId: String): List<SetScoreDto> {
        return setScores
            .select { filter { eq("session_id", sessionId) } }
            .decodeList<SetScoreDto>()
    }

    suspend fun getSetScoresForSessions(sessionIds: List<String>): List<SetScoreDto> {
        if (sessionIds.isEmpty()) return emptyList()
        return setScores
            .select {
                filter { isIn("session_id", sessionIds) }
                limit(sessionIds.size.toLong() * 5 + 100)
            }
            .decodeList<SetScoreDto>()
    }

    suspend fun deleteSetScoresForSession(sessionId: String) {
        setScores.delete { filter { eq("session_id", sessionId) } }
    }

    // ── Focus Points ──────────────────────────────────────────────────────

    private val focusPoints get() = supabaseClient.postgrest["focus_points"]

    suspend fun upsertFocusPoint(focusPoint: FocusPointDto) {
        focusPoints.upsert(focusPoint)
    }

    suspend fun getFocusPointsForUser(userId: String): List<FocusPointDto> {
        return focusPoints
            .select { filter { eq("user_id", userId) } }
            .decodeList<FocusPointDto>()
    }

    suspend fun deleteFocusPoint(focusPointId: String) {
        focusPoints.delete { filter { eq("id", focusPointId) } }
    }

    // ── Opponents ─────────────────────────────────────────────────────────

    private val opponents get() = supabaseClient.postgrest["opponents"]

    suspend fun upsertOpponent(opponent: OpponentDto) {
        opponents.upsert(opponent)
    }

    suspend fun getOpponentsForUser(userId: String): List<OpponentDto> {
        return opponents
            .select { filter { eq("user_id", userId) } }
            .decodeList<OpponentDto>()
    }

    suspend fun deleteOpponent(opponentId: String) {
        opponents.delete { filter { eq("id", opponentId) } }
    }

    // ── Partners ──────────────────────────────────────────────────────────

    private val partners get() = supabaseClient.postgrest["partners"]

    suspend fun upsertPartner(partner: PartnerDto) {
        partners.upsert(partner)
    }

    suspend fun getPartnersForUser(userId: String): List<PartnerDto> {
        return partners
            .select { filter { eq("user_id", userId) } }
            .decodeList<PartnerDto>()
    }

    suspend fun deletePartner(partnerId: String) {
        partners.delete { filter { eq("id", partnerId) } }
    }
}
