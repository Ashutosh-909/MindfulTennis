-- ============================================================
-- Mindful Tennis — Sample Data for ashutoshpriyadarshiardor@gmail.com
-- Run this in the Supabase SQL Editor AFTER the schema has been applied.
--
-- NOTE: Replace '<SUPABASE_AUTH_UID>' below with the actual
--       Supabase Auth UID for this user. You can find it in
--       Supabase Dashboard → Authentication → Users.
-- ============================================================

-- ── Configuration ─────────────────────────────────────────────
-- Set the user's Auth UID here (one place to change)
DO $$
DECLARE
    uid TEXT := 'sample uid';  -- ← REPLACE with real UID

    -- Opponent IDs
    opp1 TEXT := uuid_generate_v4()::TEXT;
    opp2 TEXT := uuid_generate_v4()::TEXT;
    opp3 TEXT := uuid_generate_v4()::TEXT;
    opp4 TEXT := uuid_generate_v4()::TEXT;

    -- Partner IDs
    par1 TEXT := uuid_generate_v4()::TEXT;
    par2 TEXT := uuid_generate_v4()::TEXT;

    -- Session IDs (10 sessions spread over ~6 weeks)
    s1 TEXT := uuid_generate_v4()::TEXT;
    s2 TEXT := uuid_generate_v4()::TEXT;
    s3 TEXT := uuid_generate_v4()::TEXT;
    s4 TEXT := uuid_generate_v4()::TEXT;
    s5 TEXT := uuid_generate_v4()::TEXT;
    s6 TEXT := uuid_generate_v4()::TEXT;
    s7 TEXT := uuid_generate_v4()::TEXT;
    s8 TEXT := uuid_generate_v4()::TEXT;
    s9 TEXT := uuid_generate_v4()::TEXT;
    s10 TEXT := uuid_generate_v4()::TEXT;

    -- Epoch millis helper: 2026-01-20 09:00 UTC as base
    base_ms BIGINT := 1768990800000;

    -- Convenience: 1 day in ms
    day_ms BIGINT := 86400000;

BEGIN

-- ── User ──────────────────────────────────────────────────────
INSERT INTO users (id, email, display_name, photo_url, created_at, time_zone)
VALUES (
    uid,
    'ashutoshpriyadarshiardor@gmail.com',
    'Ashutosh Priyadarshi',
    NULL,
    base_ms - (30 * day_ms),  -- account created ~30 days before first session
    'Asia/Kolkata'
)
ON CONFLICT (id) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    time_zone    = EXCLUDED.time_zone;

-- ── Opponents ─────────────────────────────────────────────────
INSERT INTO opponents (id, user_id, name, created_at) VALUES
    (opp1, uid, 'Rahul Sharma',   base_ms - (25 * day_ms)),
    (opp2, uid, 'Vikram Patel',   base_ms - (20 * day_ms)),
    (opp3, uid, 'Arjun Mehta',    base_ms - (15 * day_ms)),
    (opp4, uid, 'Karan Singh',    base_ms - (10 * day_ms));

-- ── Partners (for doubles) ────────────────────────────────────
INSERT INTO partners (id, user_id, name, created_at) VALUES
    (par1, uid, 'Amit Desai',     base_ms - (20 * day_ms)),
    (par2, uid, 'Rohan Gupta',    base_ms - (10 * day_ms));

-- ── Focus Points ──────────────────────────────────────────────
INSERT INTO focus_points (user_id, text, category, created_at) VALUES
    (uid, 'Stay low on groundstrokes',      'Technique',  base_ms - (25 * day_ms)),
    (uid, 'Follow through on forehand',     'Technique',  base_ms - (22 * day_ms)),
    (uid, 'First serve percentage',         'Serve',      base_ms - (18 * day_ms)),
    (uid, 'Approach the net more',          'Strategy',   base_ms - (14 * day_ms)),
    (uid, 'Stay calm during tight points',  'Mindset',    base_ms - (10 * day_ms)),
    (uid, 'Watch the ball at contact',      'Technique',  base_ms - (7 * day_ms)),
    (uid, 'Use slice to change pace',       'Tactics',    base_ms - (3 * day_ms));

-- ── Sessions ──────────────────────────────────────────────────
-- Session 1: Great singles session (score 78)
INSERT INTO sessions (id, user_id, focus_note, started_at, ended_at, time_zone_id, notes, match_type, opponent1_id, is_active, overall_score, created_at, updated_at)
VALUES (s1, uid, 'Focus on forehand follow-through', base_ms, base_ms + (90 * 60000), 'Asia/Kolkata', 'Played really well today. Forehand was on fire.', 'SINGLES', opp1, FALSE, 78, base_ms, base_ms + (90 * 60000));

-- Session 2: Average singles session (score 55)
INSERT INTO sessions (id, user_id, focus_note, started_at, ended_at, time_zone_id, notes, match_type, opponent1_id, is_active, overall_score, created_at, updated_at)
VALUES (s2, uid, 'Work on serve consistency', base_ms + (3 * day_ms), base_ms + (3 * day_ms) + (75 * 60000), 'Asia/Kolkata', 'Serve was inconsistent. Need to work on toss.', 'SINGLES', opp2, FALSE, 55, base_ms + (3 * day_ms), base_ms + (3 * day_ms) + (75 * 60000));

-- Session 3: Needs work singles (score 33)
INSERT INTO sessions (id, user_id, focus_note, started_at, ended_at, time_zone_id, notes, match_type, opponent1_id, is_active, overall_score, created_at, updated_at)
VALUES (s3, uid, 'Stay mentally tough', base_ms + (7 * day_ms), base_ms + (7 * day_ms) + (60 * 60000), 'Asia/Kolkata', 'Tough day. Lost focus after the first set. Need to work on mindset.', 'SINGLES', opp3, FALSE, 33, base_ms + (7 * day_ms), base_ms + (7 * day_ms) + (60 * 60000));

-- Session 4: Great doubles session (score 83)
INSERT INTO sessions (id, user_id, focus_note, started_at, ended_at, time_zone_id, notes, match_type, opponent1_id, opponent2_id, partner_id, is_active, overall_score, created_at, updated_at)
VALUES (s4, uid, 'Communication with partner', base_ms + (10 * day_ms), base_ms + (10 * day_ms) + (105 * 60000), 'Asia/Kolkata', 'Great doubles match! Net play was excellent.', 'DOUBLES', opp1, opp2, par1, FALSE, 83, base_ms + (10 * day_ms), base_ms + (10 * day_ms) + (105 * 60000));

-- Session 5: Average singles (score 59)
INSERT INTO sessions (id, user_id, focus_note, started_at, ended_at, time_zone_id, notes, match_type, opponent1_id, is_active, overall_score, created_at, updated_at)
VALUES (s5, uid, 'Use slice backhand more', base_ms + (14 * day_ms), base_ms + (14 * day_ms) + (80 * 60000), 'Asia/Kolkata', 'Slice was decent but backhand drive needs work.', 'SINGLES', opp4, FALSE, 59, base_ms + (14 * day_ms), base_ms + (14 * day_ms) + (80 * 60000));

-- Session 6: Great singles (score 72)
INSERT INTO sessions (id, user_id, focus_note, started_at, ended_at, time_zone_id, notes, match_type, opponent1_id, is_active, overall_score, created_at, updated_at)
VALUES (s6, uid, 'Aggressive returns', base_ms + (18 * day_ms), base_ms + (18 * day_ms) + (95 * 60000), 'Asia/Kolkata', 'Solid win. Returns were aggressive and deep.', 'SINGLES', opp2, FALSE, 72, base_ms + (18 * day_ms), base_ms + (18 * day_ms) + (95 * 60000));

-- Session 7: Average doubles (score 50)
INSERT INTO sessions (id, user_id, focus_note, started_at, ended_at, time_zone_id, notes, match_type, opponent1_id, opponent2_id, partner_id, is_active, overall_score, created_at, updated_at)
VALUES (s7, uid, 'Volleys at the net', base_ms + (22 * day_ms), base_ms + (22 * day_ms) + (85 * 60000), 'Asia/Kolkata', 'Volleys were hit or miss. Need more practice at the net.', 'DOUBLES', opp3, opp4, par2, FALSE, 50, base_ms + (22 * day_ms), base_ms + (22 * day_ms) + (85 * 60000));

-- Session 8: Needs work singles (score 38)
INSERT INTO sessions (id, user_id, focus_note, started_at, ended_at, time_zone_id, notes, match_type, opponent1_id, is_active, overall_score, created_at, updated_at)
VALUES (s8, uid, 'Footwork and movement', base_ms + (28 * day_ms), base_ms + (28 * day_ms) + (70 * 60000), 'Asia/Kolkata', 'Legs felt heavy. Movement was sluggish. Should warm up better.', 'SINGLES', opp1, FALSE, 38, base_ms + (28 * day_ms), base_ms + (28 * day_ms) + (70 * 60000));

-- Session 9: Great singles (score 88)
INSERT INTO sessions (id, user_id, focus_note, started_at, ended_at, time_zone_id, notes, match_type, opponent1_id, is_active, overall_score, created_at, updated_at)
VALUES (s9, uid, 'Enjoy the game, stay relaxed', base_ms + (35 * day_ms), base_ms + (35 * day_ms) + (100 * 60000), 'Asia/Kolkata', 'Best session in weeks! Everything clicked. Mindset was spot on.', 'SINGLES', opp3, FALSE, 88, base_ms + (35 * day_ms), base_ms + (35 * day_ms) + (100 * 60000));

-- Session 10: Average singles (score 63)
INSERT INTO sessions (id, user_id, focus_note, started_at, ended_at, time_zone_id, notes, match_type, opponent1_id, is_active, overall_score, created_at, updated_at)
VALUES (s10, uid, 'Deep serves, target corners', base_ms + (40 * day_ms), base_ms + (40 * day_ms) + (90 * 60000), 'Asia/Kolkata', 'Good serve day. Corners were dialed in. Rest of the game was okay.', 'SINGLES', opp4, FALSE, 63, base_ms + (40 * day_ms), base_ms + (40 * day_ms) + (90 * 60000));

-- ── Self Ratings (8 aspects per session) ──────────────────────
-- Aspects: FOREHAND, BACKHAND, SERVE, RETURN, VOLLEY, SLICE, MOVEMENT, MINDSET

-- Session 1 (overall 78 → avg ~4.1)
INSERT INTO self_ratings (session_id, aspect, rating) VALUES
    (s1, 'FOREHAND', 5), (s1, 'BACKHAND', 4), (s1, 'SERVE', 4), (s1, 'RETURN', 4),
    (s1, 'VOLLEY', 4),   (s1, 'SLICE', 4),    (s1, 'MOVEMENT', 4), (s1, 'MINDSET', 4);

-- Session 2 (overall 55 → avg ~3.2)
INSERT INTO self_ratings (session_id, aspect, rating) VALUES
    (s2, 'FOREHAND', 3), (s2, 'BACKHAND', 3), (s2, 'SERVE', 2), (s2, 'RETURN', 3),
    (s2, 'VOLLEY', 3),   (s2, 'SLICE', 4),    (s2, 'MOVEMENT', 4), (s2, 'MINDSET', 3);

-- Session 3 (overall 33 → avg ~2.3)
INSERT INTO self_ratings (session_id, aspect, rating) VALUES
    (s3, 'FOREHAND', 2), (s3, 'BACKHAND', 2), (s3, 'SERVE', 3), (s3, 'RETURN', 2),
    (s3, 'VOLLEY', 2),   (s3, 'SLICE', 3),    (s3, 'MOVEMENT', 3), (s3, 'MINDSET', 1);

-- Session 4 (overall 83 → avg ~4.3)
INSERT INTO self_ratings (session_id, aspect, rating) VALUES
    (s4, 'FOREHAND', 4), (s4, 'BACKHAND', 4), (s4, 'SERVE', 5), (s4, 'RETURN', 4),
    (s4, 'VOLLEY', 5),   (s4, 'SLICE', 4),    (s4, 'MOVEMENT', 4), (s4, 'MINDSET', 5);

-- Session 5 (overall 59 → avg ~3.4)
INSERT INTO self_ratings (session_id, aspect, rating) VALUES
    (s5, 'FOREHAND', 3), (s5, 'BACKHAND', 3), (s5, 'SERVE', 4), (s5, 'RETURN', 3),
    (s5, 'VOLLEY', 3),   (s5, 'SLICE', 4),    (s5, 'MOVEMENT', 4), (s5, 'MINDSET', 3);

-- Session 6 (overall 72 → avg ~3.9)
INSERT INTO self_ratings (session_id, aspect, rating) VALUES
    (s6, 'FOREHAND', 4), (s6, 'BACKHAND', 4), (s6, 'SERVE', 4), (s6, 'RETURN', 5),
    (s6, 'VOLLEY', 3),   (s6, 'SLICE', 3),    (s6, 'MOVEMENT', 4), (s6, 'MINDSET', 4);

-- Session 7 (overall 50 → avg ~3.0)
INSERT INTO self_ratings (session_id, aspect, rating) VALUES
    (s7, 'FOREHAND', 3), (s7, 'BACKHAND', 3), (s7, 'SERVE', 3), (s7, 'RETURN', 3),
    (s7, 'VOLLEY', 2),   (s7, 'SLICE', 3),    (s7, 'MOVEMENT', 3), (s7, 'MINDSET', 4);

-- Session 8 (overall 38 → avg ~2.5)
INSERT INTO self_ratings (session_id, aspect, rating) VALUES
    (s8, 'FOREHAND', 3), (s8, 'BACKHAND', 2), (s8, 'SERVE', 3), (s8, 'RETURN', 2),
    (s8, 'VOLLEY', 2),   (s8, 'SLICE', 3),    (s8, 'MOVEMENT', 2), (s8, 'MINDSET', 3);

-- Session 9 (overall 88 → avg ~4.5)
INSERT INTO self_ratings (session_id, aspect, rating) VALUES
    (s9, 'FOREHAND', 5), (s9, 'BACKHAND', 5), (s9, 'SERVE', 4), (s9, 'RETURN', 5),
    (s9, 'VOLLEY', 4),   (s9, 'SLICE', 4),    (s9, 'MOVEMENT', 5), (s9, 'MINDSET', 5);

-- Session 10 (overall 63 → avg ~3.5)
INSERT INTO self_ratings (session_id, aspect, rating) VALUES
    (s10, 'FOREHAND', 3), (s10, 'BACKHAND', 3), (s10, 'SERVE', 5), (s10, 'RETURN', 3),
    (s10, 'VOLLEY', 3),   (s10, 'SLICE', 3),    (s10, 'MOVEMENT', 4), (s10, 'MINDSET', 4);

-- ── Partner Ratings (only for doubles sessions 4 & 7) ─────────
-- Session 4 partner: Amit Desai
INSERT INTO partner_ratings (session_id, aspect, rating) VALUES
    (s4, 'FOREHAND', 4), (s4, 'BACKHAND', 3), (s4, 'SERVE', 4), (s4, 'RETURN', 4),
    (s4, 'VOLLEY', 5),   (s4, 'SLICE', 3),    (s4, 'MOVEMENT', 4), (s4, 'MINDSET', 4);

-- Session 7 partner: Rohan Gupta
INSERT INTO partner_ratings (session_id, aspect, rating) VALUES
    (s7, 'FOREHAND', 3), (s7, 'BACKHAND', 3), (s7, 'SERVE', 3), (s7, 'RETURN', 2),
    (s7, 'VOLLEY', 2),   (s7, 'SLICE', 3),    (s7, 'MOVEMENT', 3), (s7, 'MINDSET', 3);

-- ── Set Scores ────────────────────────────────────────────────
-- Session 1: Won 2-1 (6-4, 4-6, 6-3)
INSERT INTO set_scores (session_id, set_number, user_score, opponent_score) VALUES
    (s1, 1, 6, 4), (s1, 2, 4, 6), (s1, 3, 6, 3);

-- Session 2: Lost 0-2 (3-6, 5-7)
INSERT INTO set_scores (session_id, set_number, user_score, opponent_score) VALUES
    (s2, 1, 3, 6), (s2, 2, 5, 7);

-- Session 3: Lost 0-2 (2-6, 3-6)
INSERT INTO set_scores (session_id, set_number, user_score, opponent_score) VALUES
    (s3, 1, 2, 6), (s3, 2, 3, 6);

-- Session 4 (doubles): Won 2-0 (6-3, 6-4)
INSERT INTO set_scores (session_id, set_number, user_score, opponent_score) VALUES
    (s4, 1, 6, 3), (s4, 2, 6, 4);

-- Session 5: Lost 1-2 (6-4, 3-6, 4-6)
INSERT INTO set_scores (session_id, set_number, user_score, opponent_score) VALUES
    (s5, 1, 6, 4), (s5, 2, 3, 6), (s5, 3, 4, 6);

-- Session 6: Won 2-0 (6-2, 7-5)
INSERT INTO set_scores (session_id, set_number, user_score, opponent_score) VALUES
    (s6, 1, 6, 2), (s6, 2, 7, 5);

-- Session 7 (doubles): Lost 1-2 (6-7, 6-3, 3-6)
INSERT INTO set_scores (session_id, set_number, user_score, opponent_score) VALUES
    (s7, 1, 6, 7), (s7, 2, 6, 3), (s7, 3, 3, 6);

-- Session 8: Lost 0-2 (2-6, 4-6)
INSERT INTO set_scores (session_id, set_number, user_score, opponent_score) VALUES
    (s8, 1, 2, 6), (s8, 2, 4, 6);

-- Session 9: Won 2-0 (6-1, 6-2)
INSERT INTO set_scores (session_id, set_number, user_score, opponent_score) VALUES
    (s9, 1, 6, 1), (s9, 2, 6, 2);

-- Session 10: Won 2-1 (6-4, 4-6, 7-5)
INSERT INTO set_scores (session_id, set_number, user_score, opponent_score) VALUES
    (s10, 1, 6, 4), (s10, 2, 4, 6), (s10, 3, 7, 5);

RAISE NOTICE 'Sample data inserted successfully for user %', uid;

END $$;
