-- ============================================================
-- Mindful Tennis — Supabase Table Definitions + Row Level Security
-- Apply this SQL in your Supabase SQL Editor (Dashboard → SQL Editor)
-- ============================================================

-- Enable UUID extension (usually already enabled)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── Users ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id          TEXT PRIMARY KEY,  -- Supabase Auth UID
    email       TEXT NOT NULL,
    display_name TEXT,
    photo_url   TEXT,
    created_at  BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    time_zone   TEXT NOT NULL DEFAULT 'UTC'
);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own profile"
    ON users FOR SELECT
    USING (auth.uid()::TEXT = id);

CREATE POLICY "Users can insert own profile"
    ON users FOR INSERT
    WITH CHECK (auth.uid()::TEXT = id);

CREATE POLICY "Users can update own profile"
    ON users FOR UPDATE
    USING (auth.uid()::TEXT = id);

CREATE POLICY "Users can delete own profile"
    ON users FOR DELETE
    USING (auth.uid()::TEXT = id);

-- ── Opponents ─────────────────────────────────────────────────
-- (created before sessions because sessions has FK references to opponents)
CREATE TABLE IF NOT EXISTS opponents (
    id          TEXT PRIMARY KEY DEFAULT uuid_generate_v4()::TEXT,
    user_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    created_at  BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

ALTER TABLE opponents ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can CRUD own opponents"
    ON opponents FOR ALL
    USING (auth.uid()::TEXT = user_id);

-- ── Partners ──────────────────────────────────────────────────
-- (created before sessions because sessions has FK references to partners)
CREATE TABLE IF NOT EXISTS partners (
    id          TEXT PRIMARY KEY DEFAULT uuid_generate_v4()::TEXT,
    user_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    created_at  BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

ALTER TABLE partners ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can CRUD own partners"
    ON partners FOR ALL
    USING (auth.uid()::TEXT = user_id);

-- ── Sessions ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sessions (
    id              TEXT PRIMARY KEY DEFAULT uuid_generate_v4()::TEXT,
    user_id         TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    focus_note      TEXT NOT NULL DEFAULT '',
    started_at      BIGINT NOT NULL,
    ended_at        BIGINT,
    time_zone_id    TEXT NOT NULL DEFAULT 'UTC',
    notes           TEXT,
    match_type      TEXT NOT NULL DEFAULT 'SINGLES',
    opponent1_id    TEXT REFERENCES opponents(id) ON DELETE SET NULL,
    opponent2_id    TEXT REFERENCES opponents(id) ON DELETE SET NULL,
    partner_id      TEXT REFERENCES partners(id) ON DELETE SET NULL,
    is_active       BOOLEAN NOT NULL DEFAULT FALSE,
    overall_score   INTEGER,
    created_at      BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at      BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    schema_version  INTEGER NOT NULL DEFAULT 1
);

ALTER TABLE sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can CRUD own sessions"
    ON sessions FOR ALL
    USING (auth.uid()::TEXT = user_id);

-- ── Self Ratings ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS self_ratings (
    id          TEXT PRIMARY KEY DEFAULT uuid_generate_v4()::TEXT,
    session_id  TEXT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    aspect      TEXT NOT NULL,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5)
);

ALTER TABLE self_ratings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can CRUD own self_ratings"
    ON self_ratings FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM sessions s
            WHERE s.id = self_ratings.session_id
              AND s.user_id = auth.uid()::TEXT
        )
    );

-- ── Partner Ratings ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS partner_ratings (
    id          TEXT PRIMARY KEY DEFAULT uuid_generate_v4()::TEXT,
    session_id  TEXT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    aspect      TEXT NOT NULL,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5)
);

ALTER TABLE partner_ratings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can CRUD own partner_ratings"
    ON partner_ratings FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM sessions s
            WHERE s.id = partner_ratings.session_id
              AND s.user_id = auth.uid()::TEXT
        )
    );

-- ── Focus Points ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS focus_points (
    id          TEXT PRIMARY KEY DEFAULT uuid_generate_v4()::TEXT,
    user_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    text        TEXT NOT NULL,
    category    TEXT,
    created_at  BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

ALTER TABLE focus_points ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can CRUD own focus_points"
    ON focus_points FOR ALL
    USING (auth.uid()::TEXT = user_id);

-- ── Set Scores ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS set_scores (
    id              TEXT PRIMARY KEY DEFAULT uuid_generate_v4()::TEXT,
    session_id      TEXT NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    set_number      INTEGER NOT NULL,
    user_score      INTEGER NOT NULL,
    opponent_score  INTEGER NOT NULL,
    opponent_id     TEXT REFERENCES opponents(id) ON DELETE SET NULL
);

ALTER TABLE set_scores ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can CRUD own set_scores"
    ON set_scores FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM sessions s
            WHERE s.id = set_scores.session_id
              AND s.user_id = auth.uid()::TEXT
        )
    );

-- ── Indexes ───────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_updated_at ON sessions(updated_at);
CREATE INDEX IF NOT EXISTS idx_self_ratings_session_id ON self_ratings(session_id);
CREATE INDEX IF NOT EXISTS idx_partner_ratings_session_id ON partner_ratings(session_id);
CREATE INDEX IF NOT EXISTS idx_set_scores_session_id ON set_scores(session_id);
CREATE INDEX IF NOT EXISTS idx_focus_points_user_id ON focus_points(user_id);
CREATE INDEX IF NOT EXISTS idx_opponents_user_id ON opponents(user_id);
CREATE INDEX IF NOT EXISTS idx_partners_user_id ON partners(user_id);
