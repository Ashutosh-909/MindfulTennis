-- ============================================================
-- Mindful Tennis — 150 Sessions over 6 Months (Screenshot Data)
-- Sep 15 2025 → Mar 15 2026
--
-- Shows gradual performance improvement:
--   Month 1: Scores ~20-50  (beginner, lots of losses)
--   Month 2: Scores ~30-55  (finding rhythm)
--   Month 3: Scores ~40-65  (noticeable improvement)
--   Month 4: Scores ~50-72  (solid intermediate)
--   Month 5: Scores ~55-80  (winning regularly)
--   Month 6: Scores ~65-90  (strong confident play)
--
-- ⚠️  WARNING: This script DELETES all existing data for the
--     given user before inserting. Run only for demo purposes.
--
-- NOTE: Replace 'sample uid' with the actual Supabase Auth UID.
-- ============================================================

DO $$
DECLARE
    uid TEXT := 'a53959d1-fc26-4315-84fc-53039008916a';   -- ← REPLACE with real Supabase Auth UID

    -- ── Opponent & Partner IDs ──
    opp_ids TEXT[6];
    par_ids TEXT[3];

    -- ── Epoch helpers ──
    start_epoch  BIGINT;        -- Sep 15 2025 00:00 IST
    day_ms       BIGINT := 86400000;

    -- ── Session loop variables ──
    i            INTEGER;
    sess_id      TEXT;
    progress     FLOAT;         -- 0.0 → 1.0 across 150 sessions
    smooth_prog  FLOAT;         -- S-curve smoothstep of progress
    day_offset   FLOAT;
    hour_offset  INTEGER;
    session_start BIGINT;
    session_end  BIGINT;
    duration_min INTEGER;

    -- ── Rating variables ──
    base_skill   FLOAT;
    noise        FLOAT;
    r_forehand   INTEGER;
    r_backhand   INTEGER;
    r_serve      INTEGER;
    r_return     INTEGER;
    r_volley     INTEGER;
    r_slice      INTEGER;
    r_movement   INTEGER;
    r_mindset    INTEGER;
    overall      INTEGER;

    -- ── Match metadata ──
    is_doubles   BOOLEAN;
    is_practice  BOOLEAN;       -- ~8% sessions are practice (no sets)
    opp1_idx     INTEGER;
    opp2_idx     INTEGER;
    par_idx      INTEGER;
    match_type   TEXT;

    -- ── Focus note & session note ──
    focus_note   TEXT;
    sess_note    TEXT;

    -- Focus note pools per phase
    fn_phase1 TEXT[] := ARRAY[
        'Watch the ball all the way to the racket',
        'Follow through on forehand',
        'Keep feet moving between shots',
        'Stay low and bend knees on groundstrokes',
        'Smooth consistent toss on serve',
        'Step into the ball on forehand',
        'Keep wrist firm at contact point',
        'Use continental grip for serve'
    ];
    fn_phase2 TEXT[] := ARRAY[
        'Work on backhand consistency',
        'Get first serve in play',
        'Deep crosscourt rallying',
        'Stay patient in long rallies',
        'Use the whole court width',
        'Early racket preparation on both sides',
        'Hit through the ball not at it',
        'Recover to center after each shot'
    ];
    fn_phase3 TEXT[] := ARRAY[
        'Approach the net on short balls',
        'Mix up pace and spin in rallies',
        'Target the corners on serve',
        'Move the opponent side to side',
        'Use slice as a change of pace',
        'Hit behind the opponent',
        'Develop a kick serve',
        'Work on split step timing'
    ];
    fn_phase4 TEXT[] := ARRAY[
        'Stay mentally strong in tight games',
        'Construct points methodically',
        'Aggressive second serve return',
        'Control the baseline rally tempo',
        'Quick recovery to ready position',
        'Focus on process not the score',
        'Play each point on its own merits',
        'Be comfortable at the net'
    ];
    fn_phase5 TEXT[] := ARRAY[
        'Dictate play with the forehand',
        'Serve and volley on big points',
        'Read the opponents patterns early',
        'Vary serve placement and speed',
        'Dominate the net in doubles',
        'Use drop shot when opponent is deep',
        'Hit with more margin on important points',
        'Commit fully to every shot'
    ];
    fn_phase6 TEXT[] := ARRAY[
        'Dominate the first 3 shots of every rally',
        'Play aggressively from the very first point',
        'Trust your shots under pressure',
        'Execute the game plan consistently',
        'Championship mindset on every point',
        'Serve plus one pattern to finish points',
        'Read body language and anticipate',
        'Enjoy the competition and stay relaxed'
    ];

    -- Session note pools
    notes_great TEXT[] := ARRAY[
        'Everything clicked today. Felt confident and aggressive throughout.',
        'Best session in a while! Forehand was on fire and placement was excellent.',
        'Solid all-round performance. Serve was really dialed in today.',
        'Great movement and court coverage. Felt very fit.',
        'Dominated from the baseline. Opponent had no answer to my depth.',
        'Net play was excellent today. Volleys were crisp and well-placed.',
        'Mentally sharp throughout the entire match. Stayed focused on every point.',
        'Aggressive returns kept the pressure on from the start.',
        'Clean winners from both sides. Very satisfying session overall.',
        'Strong finish to a competitive match. Clutch play when it mattered.'
    ];
    notes_avg TEXT[] := ARRAY[
        'Decent session but inconsistent at key moments.',
        'Had moments of brilliance mixed with unforced errors.',
        'Serve worked well but groundstrokes were slightly off today.',
        'Good first set but lost focus in the second. Need to stay present.',
        'Improving steadily but still making too many unforced errors.',
        'Rallied well from the back but net game was rusty.',
        'Competitive match. Could have won with more mental discipline.',
        'Average day. Nothing spectacular but kept fighting to the end.',
        'Mixed bag today. Some things worked really well, others need more practice.',
        'Played okay overall but need to work on finishing points cleanly.'
    ];
    notes_bad TEXT[] := ARRAY[
        'Tough day on court. Nothing seemed to click.',
        'Too many double faults today. Serve was terrible and cost me.',
        'Lost focus early in the match and could not recover mentally.',
        'Movement was sluggish and legs felt heavy throughout.',
        'Got frustrated with errors and it snowballed. Must stay calmer.',
        'Opponent played well but I also gifted too many free points.',
        'Bad day at the office. Will analyze and come back stronger.',
        'Mentally checked out after falling behind. Big area to improve.',
        'Everything felt off. Timing was completely wrong on every shot.',
        'Struggled with consistency on every stroke. Back to basics in practice.'
    ];
    notes_practice TEXT[] := ARRAY[
        'Good practice session. Worked on specific drills and technique.',
        'Focused practice on serves and returns. Felt productive.',
        'Rally practice with focus on consistency and placement.',
        'Drill session working on volleys and approaches.',
        'Technical practice. Broke down the backhand mechanics.',
        'Footwork drills and movement patterns. Good workout.',
        'Practiced serve placement targeting all four corners.',
        'Point play practice with emphasis on constructing rallies.'
    ];

    -- ── Set score helpers ──
    win_prob     FLOAT;
    u_score      INTEGER;
    o_score      INTEGER;

    -- ── Temp ──
    fn_pool      TEXT[];
    fn_len       INTEGER;
    n_idx        INTEGER;

BEGIN

-- ── Epoch for Sep 15, 2025 00:00 IST (UTC+5:30) ──
start_epoch := (EXTRACT(EPOCH FROM TIMESTAMP WITH TIME ZONE '2025-09-15 00:00:00+05:30') * 1000)::BIGINT;

-- ── Generate unique IDs for opponents and partners ──
FOR i IN 1..6 LOOP
    opp_ids[i] := uuid_generate_v4()::TEXT;
END LOOP;
FOR i IN 1..3 LOOP
    par_ids[i] := uuid_generate_v4()::TEXT;
END LOOP;

-- ══════════════════════════════════════════════════════════════
-- CLEAN EXISTING DATA  (cascades to sessions, ratings, scores)
-- ══════════════════════════════════════════════════════════════
DELETE FROM focus_points WHERE user_id = uid;
DELETE FROM set_scores   WHERE session_id IN (SELECT id FROM sessions WHERE user_id = uid);
DELETE FROM partner_ratings WHERE session_id IN (SELECT id FROM sessions WHERE user_id = uid);
DELETE FROM self_ratings  WHERE session_id IN (SELECT id FROM sessions WHERE user_id = uid);
DELETE FROM sessions      WHERE user_id = uid;
DELETE FROM opponents     WHERE user_id = uid;
DELETE FROM partners      WHERE user_id = uid;

-- ══════════════════════════════════════════════════════════════
-- INSERT USER
-- ══════════════════════════════════════════════════════════════
INSERT INTO users (id, email, display_name, photo_url, created_at, time_zone)
VALUES (
    uid,
    'ashutoshpriyadarshiardor@gmail.com',
    'Ashutosh Priyadarshi',
    NULL,
    start_epoch - (7 * day_ms),
    'Asia/Kolkata'
)
ON CONFLICT (id) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    time_zone    = EXCLUDED.time_zone;

-- ══════════════════════════════════════════════════════════════
-- INSERT OPPONENTS (6 regular hitting partners / rivals)
-- ══════════════════════════════════════════════════════════════
INSERT INTO opponents (id, user_id, name, created_at) VALUES
    (opp_ids[1], uid, 'Rahul Sharma',    start_epoch - (5 * day_ms)),
    (opp_ids[2], uid, 'Vikram Patel',    start_epoch - (4 * day_ms)),
    (opp_ids[3], uid, 'Arjun Mehta',     start_epoch - (3 * day_ms)),
    (opp_ids[4], uid, 'Karan Singh',     start_epoch - (2 * day_ms)),
    (opp_ids[5], uid, 'Pradeep Nair',    start_epoch - (1 * day_ms)),
    (opp_ids[6], uid, 'Sanjay Reddy',    start_epoch);

-- ══════════════════════════════════════════════════════════════
-- INSERT PARTNERS (3 doubles partners)
-- ══════════════════════════════════════════════════════════════
INSERT INTO partners (id, user_id, name, created_at) VALUES
    (par_ids[1], uid, 'Amit Desai',    start_epoch - (3 * day_ms)),
    (par_ids[2], uid, 'Rohan Gupta',   start_epoch - (2 * day_ms)),
    (par_ids[3], uid, 'Nikhil Kapoor', start_epoch - (1 * day_ms));

-- ══════════════════════════════════════════════════════════════
-- INSERT FOCUS POINTS (evolving over the 6 months)
-- ══════════════════════════════════════════════════════════════
INSERT INTO focus_points (user_id, text, category, created_at) VALUES
    -- Month 1: Fundamental technique
    (uid, 'Keep eyes on the ball at contact',         'Technique', start_epoch + (0  * day_ms)),
    (uid, 'Follow through on forehand swing',         'Technique', start_epoch + (4  * day_ms)),
    (uid, 'Bend knees on every groundstroke',         'Technique', start_epoch + (9  * day_ms)),
    (uid, 'Consistent ball toss on serve',            'Serve',     start_epoch + (14 * day_ms)),
    (uid, 'Continental grip for volleys',             'Technique', start_epoch + (20 * day_ms)),
    -- Month 2: Building consistency
    (uid, 'Early racket preparation',                 'Technique', start_epoch + (33 * day_ms)),
    (uid, 'First serve percentage over 50%',          'Serve',     start_epoch + (38 * day_ms)),
    (uid, 'Hit through the ball with full swing',     'Technique', start_epoch + (45 * day_ms)),
    (uid, 'Deep crosscourt as default rally shot',    'Strategy',  start_epoch + (52 * day_ms)),
    -- Month 3: Tactical awareness
    (uid, 'Approach the net on short balls',          'Strategy',  start_epoch + (65 * day_ms)),
    (uid, 'Use slice to change the pace',             'Tactics',   start_epoch + (72 * day_ms)),
    (uid, 'Move opponent with angles',                'Strategy',  start_epoch + (80 * day_ms)),
    (uid, 'Split step before opponent hits',          'Movement',  start_epoch + (85 * day_ms)),
    -- Month 4: Mental game & refinement
    (uid, 'Stay calm during tight points',            'Mindset',   start_epoch + (95 * day_ms)),
    (uid, 'Focus on process not scoreboard',          'Mindset',   start_epoch + (102 * day_ms)),
    (uid, 'Target the backhand on return',            'Strategy',  start_epoch + (112 * day_ms)),
    -- Month 5: Advanced patterns
    (uid, 'Vary serve placement every game',          'Serve',     start_epoch + (125 * day_ms)),
    (uid, 'Construct 3-shot rally patterns',          'Strategy',  start_epoch + (133 * day_ms)),
    (uid, 'Recover to optimal court position',        'Movement',  start_epoch + (140 * day_ms)),
    (uid, 'Use drop shot when opponent is deep',      'Tactics',   start_epoch + (145 * day_ms)),
    -- Month 6: Championship mindset
    (uid, 'Aggressive intent from the first point',   'Mindset',   start_epoch + (155 * day_ms)),
    (uid, 'Read opponent body language early',        'Tactics',   start_epoch + (162 * day_ms)),
    (uid, 'Serve plus one pattern to close points',   'Strategy',  start_epoch + (170 * day_ms)),
    (uid, 'Play each point as its own match',         'Mindset',   start_epoch + (178 * day_ms));


-- ══════════════════════════════════════════════════════════════
-- GENERATE 150 SESSIONS
-- ══════════════════════════════════════════════════════════════
FOR i IN 1..150 LOOP

    sess_id  := uuid_generate_v4()::TEXT;
    progress := (i - 1)::FLOAT / 149.0;              -- 0.0 → 1.0

    -- S-curve (smoothstep) for realistic improvement arc:
    --   slow start → rapid middle growth → gradual plateau
    smooth_prog := progress * progress * (3.0 - 2.0 * progress);

    -- ── Day offset: spread 150 sessions over ~180 days ──
    day_offset := progress * 179.0;

    -- ── Time of day: 6 AM – 7 PM IST, varying ──
    hour_offset := 6 + floor(random() * 14)::INTEGER;

    session_start := start_epoch
                   + (floor(day_offset) * day_ms)::BIGINT
                   + (hour_offset * 3600000)::BIGINT
                   + (floor(random() * 3600000))::BIGINT;   -- random minutes within the hour

    -- Duration: 55 – 120 min
    duration_min := 55 + floor(random() * 66)::INTEGER;
    session_end  := session_start + (duration_min * 60000)::BIGINT;

    -- ── Match type ──
    is_doubles  := random() < 0.18;                   -- ~18% doubles
    is_practice := (NOT is_doubles) AND random() < 0.08;  -- ~8% practice (no sets)

    IF is_doubles THEN
        match_type := 'DOUBLES';
    ELSE
        match_type := 'SINGLES';
    END IF;

    -- ── Pick opponents ──
    -- Weight toward regulars: Rahul(1) and Vikram(2) are most common
    IF random() < 0.30 THEN
        opp1_idx := 1;       -- Rahul (regular rival)
    ELSIF random() < 0.45 THEN
        opp1_idx := 2;       -- Vikram
    ELSE
        opp1_idx := 3 + floor(random() * 4)::INTEGER;
        IF opp1_idx > 6 THEN opp1_idx := 6; END IF;
    END IF;

    opp2_idx := NULL;
    par_idx  := NULL;

    IF is_doubles THEN
        opp2_idx := 1 + floor(random() * 6)::INTEGER;
        IF opp2_idx > 6 THEN opp2_idx := 6; END IF;
        WHILE opp2_idx = opp1_idx LOOP
            opp2_idx := 1 + floor(random() * 6)::INTEGER;
            IF opp2_idx > 6 THEN opp2_idx := 6; END IF;
        END LOOP;
        par_idx := 1 + floor(random() * 3)::INTEGER;
        IF par_idx > 3 THEN par_idx := 3; END IF;
    END IF;

    -- ══════════════════════════════════════════════════════════
    -- CALCULATE ASPECT RATINGS (gradual improvement with noise)
    -- ══════════════════════════════════════════════════════════

    -- Base skill: 2.0 → 4.3 along S-curve
    base_skill := 2.0 + smooth_prog * 2.3;

    -- Session-level noise (good days / bad days): ±0.7
    noise := (random() - 0.5) * 1.4;

    -- FOREHAND: main weapon, improves steadily, slight edge
    r_forehand := GREATEST(1, LEAST(5,
        round(base_skill + 0.15 + noise + smooth_prog * 0.2)::INTEGER));

    -- BACKHAND: starts weaker (-0.4), catches up around month 3-4
    r_backhand := GREATEST(1, LEAST(5,
        round(base_skill - 0.4 + noise
              + CASE WHEN progress > 0.3 THEN (progress - 0.3) * 0.6 ELSE 0 END
        )::INTEGER));

    -- SERVE: starts weakest (-0.5), big jump months 3-4
    r_serve := GREATEST(1, LEAST(5,
        round(base_skill - 0.5 + noise
              + CASE WHEN progress > 0.35 THEN (progress - 0.35) * 0.9 ELSE 0 END
        )::INTEGER));

    -- RETURN: follows base with slight offset
    r_return := GREATEST(1, LEAST(5,
        round(base_skill - 0.1 + noise)::INTEGER));

    -- VOLLEY: starts lowest (-0.6), slow steady climb
    r_volley := GREATEST(1, LEAST(5,
        round(base_skill - 0.6 + noise + smooth_prog * 0.35)::INTEGER));

    -- SLICE: starts okay (+0.1), modest improvement
    r_slice := GREATEST(1, LEAST(5,
        round(base_skill + 0.1 + noise * 0.8)::INTEGER));

    -- MOVEMENT: follows base, extra random for tired days
    r_movement := GREATEST(1, LEAST(5,
        round(base_skill + noise + (random() - 0.5) * 0.4)::INTEGER));

    -- MINDSET: most volatile (noise×1.3), big late improvement
    r_mindset := GREATEST(1, LEAST(5,
        round(base_skill - 0.35 + noise * 1.3
              + CASE WHEN progress > 0.5 THEN (progress - 0.5) * 1.1 ELSE 0 END
        )::INTEGER));

    -- ── Overall score: mean of 8 ratings → 0-100 ──
    overall := round(
        ( (r_forehand + r_backhand + r_serve + r_return
         + r_volley + r_slice + r_movement + r_mindset)::FLOAT
          / 8.0 - 1.0
        ) / 4.0 * 100.0
    )::INTEGER;
    overall := GREATEST(0, LEAST(100, overall));

    -- ══════════════════════════════════════════════════════════
    -- PICK FOCUS NOTE (phase-appropriate)
    -- ══════════════════════════════════════════════════════════
    IF i <= 25 THEN
        fn_pool := fn_phase1;
    ELSIF i <= 50 THEN
        fn_pool := fn_phase2;
    ELSIF i <= 75 THEN
        fn_pool := fn_phase3;
    ELSIF i <= 100 THEN
        fn_pool := fn_phase4;
    ELSIF i <= 125 THEN
        fn_pool := fn_phase5;
    ELSE
        fn_pool := fn_phase6;
    END IF;
    fn_len := array_length(fn_pool, 1);
    focus_note := fn_pool[1 + floor(random() * fn_len)::INTEGER];

    -- ══════════════════════════════════════════════════════════
    -- PICK SESSION NOTE (score-appropriate)
    -- ══════════════════════════════════════════════════════════
    IF is_practice THEN
        n_idx := 1 + floor(random() * array_length(notes_practice, 1))::INTEGER;
        IF n_idx > array_length(notes_practice, 1) THEN n_idx := array_length(notes_practice, 1); END IF;
        sess_note := notes_practice[n_idx];
    ELSIF overall >= 65 THEN
        n_idx := 1 + floor(random() * array_length(notes_great, 1))::INTEGER;
        IF n_idx > array_length(notes_great, 1) THEN n_idx := array_length(notes_great, 1); END IF;
        sess_note := notes_great[n_idx];
    ELSIF overall >= 40 THEN
        n_idx := 1 + floor(random() * array_length(notes_avg, 1))::INTEGER;
        IF n_idx > array_length(notes_avg, 1) THEN n_idx := array_length(notes_avg, 1); END IF;
        sess_note := notes_avg[n_idx];
    ELSE
        n_idx := 1 + floor(random() * array_length(notes_bad, 1))::INTEGER;
        IF n_idx > array_length(notes_bad, 1) THEN n_idx := array_length(notes_bad, 1); END IF;
        sess_note := notes_bad[n_idx];
    END IF;

    -- ~12% of sessions have no written note (realistic)
    IF (NOT is_practice) AND random() < 0.12 THEN
        sess_note := NULL;
    END IF;

    -- ══════════════════════════════════════════════════════════
    -- INSERT SESSION
    -- ══════════════════════════════════════════════════════════
    IF is_doubles AND opp2_idx IS NOT NULL AND par_idx IS NOT NULL THEN
        INSERT INTO sessions
            (id, user_id, focus_note, started_at, ended_at, time_zone_id,
             notes, match_type, opponent1_id, opponent2_id, partner_id,
             is_active, overall_score, created_at, updated_at)
        VALUES
            (sess_id, uid, focus_note, session_start, session_end, 'Asia/Kolkata',
             sess_note, 'DOUBLES', opp_ids[opp1_idx], opp_ids[opp2_idx], par_ids[par_idx],
             FALSE, overall, session_start, session_end);
    ELSE
        INSERT INTO sessions
            (id, user_id, focus_note, started_at, ended_at, time_zone_id,
             notes, match_type, opponent1_id,
             is_active, overall_score, created_at, updated_at)
        VALUES
            (sess_id, uid, focus_note, session_start, session_end, 'Asia/Kolkata',
             sess_note, 'SINGLES', opp_ids[opp1_idx],
             FALSE, overall, session_start, session_end);
    END IF;

    -- ══════════════════════════════════════════════════════════
    -- INSERT SELF RATINGS (8 aspects)
    -- ══════════════════════════════════════════════════════════
    INSERT INTO self_ratings (session_id, aspect, rating) VALUES
        (sess_id, 'FOREHAND',  r_forehand),
        (sess_id, 'BACKHAND',  r_backhand),
        (sess_id, 'SERVE',     r_serve),
        (sess_id, 'RETURN',    r_return),
        (sess_id, 'VOLLEY',    r_volley),
        (sess_id, 'SLICE',     r_slice),
        (sess_id, 'MOVEMENT',  r_movement),
        (sess_id, 'MINDSET',   r_mindset);

    -- ══════════════════════════════════════════════════════════
    -- INSERT PARTNER RATINGS (doubles only)
    -- ══════════════════════════════════════════════════════════
    IF is_doubles THEN
        INSERT INTO partner_ratings (session_id, aspect, rating) VALUES
            (sess_id, 'FOREHAND',  GREATEST(1, LEAST(5, r_forehand  + (floor(random()*3) - 1)::INTEGER))),
            (sess_id, 'BACKHAND',  GREATEST(1, LEAST(5, r_backhand  + (floor(random()*3) - 1)::INTEGER))),
            (sess_id, 'SERVE',     GREATEST(1, LEAST(5, r_serve     + (floor(random()*3) - 1)::INTEGER))),
            (sess_id, 'RETURN',    GREATEST(1, LEAST(5, r_return    + (floor(random()*3) - 1)::INTEGER))),
            (sess_id, 'VOLLEY',    GREATEST(1, LEAST(5, r_volley    + (floor(random()*3) - 1)::INTEGER))),
            (sess_id, 'SLICE',     GREATEST(1, LEAST(5, r_slice     + (floor(random()*3) - 1)::INTEGER))),
            (sess_id, 'MOVEMENT',  GREATEST(1, LEAST(5, r_movement  + (floor(random()*3) - 1)::INTEGER))),
            (sess_id, 'MINDSET',   GREATEST(1, LEAST(5, r_mindset   + (floor(random()*3) - 1)::INTEGER)));
    END IF;

    -- ══════════════════════════════════════════════════════════
    -- INSERT SET SCORES (skip for practice sessions)
    -- ══════════════════════════════════════════════════════════
    IF NOT is_practice THEN

        -- Win probability scales with overall score
        win_prob := LEAST(0.90, GREATEST(0.10,
                    overall::FLOAT / 120.0 + 0.05));

        IF random() < win_prob THEN
            -- ── USER WINS THE MATCH ──

            IF random() < 0.52 THEN
                -- Straight sets win (2-0)
                -- Set 1: user wins
                u_score := 6;
                o_score := floor(random() * 5)::INTEGER;                -- 0-4
                INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                VALUES (sess_id, 1, u_score, o_score);

                -- Set 2: user wins
                IF random() < 0.15 THEN
                    u_score := 7;
                    o_score := 5 + floor(random() * 2)::INTEGER;       -- 5 or 6
                ELSE
                    u_score := 6;
                    o_score := floor(random() * 5)::INTEGER;            -- 0-4
                END IF;
                INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                VALUES (sess_id, 2, u_score, o_score);

            ELSE
                -- Three-set win (2-1)
                IF random() < 0.5 THEN
                    -- Lose set 1, win sets 2 & 3
                    o_score := 6;
                    u_score := 1 + floor(random() * 4)::INTEGER;       -- 1-4
                    INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                    VALUES (sess_id, 1, u_score, o_score);

                    u_score := 6;
                    o_score := 1 + floor(random() * 4)::INTEGER;
                    INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                    VALUES (sess_id, 2, u_score, o_score);
                ELSE
                    -- Win set 1, lose set 2
                    u_score := 6;
                    o_score := 1 + floor(random() * 4)::INTEGER;
                    INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                    VALUES (sess_id, 1, u_score, o_score);

                    o_score := 6;
                    u_score := 1 + floor(random() * 4)::INTEGER;
                    INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                    VALUES (sess_id, 2, u_score, o_score);
                END IF;

                -- Deciding set 3: user wins
                IF random() < 0.20 THEN
                    u_score := 7;
                    o_score := 5 + floor(random() * 2)::INTEGER;
                ELSE
                    u_score := 6;
                    o_score := 2 + floor(random() * 3)::INTEGER;       -- 2-4
                END IF;
                INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                VALUES (sess_id, 3, u_score, o_score);
            END IF;

        ELSE
            -- ── USER LOSES THE MATCH ──

            IF random() < 0.52 THEN
                -- Straight sets loss (0-2)
                -- Set 1: user loses
                o_score := 6;
                u_score := floor(random() * 5)::INTEGER;                -- 0-4
                INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                VALUES (sess_id, 1, u_score, o_score);

                -- Set 2: user loses
                IF random() < 0.15 THEN
                    o_score := 7;
                    u_score := 5 + floor(random() * 2)::INTEGER;
                ELSE
                    o_score := 6;
                    u_score := floor(random() * 5)::INTEGER;
                END IF;
                INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                VALUES (sess_id, 2, u_score, o_score);

            ELSE
                -- Three-set loss (1-2)
                IF random() < 0.5 THEN
                    -- Win set 1, lose sets 2 & 3
                    u_score := 6;
                    o_score := 1 + floor(random() * 4)::INTEGER;
                    INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                    VALUES (sess_id, 1, u_score, o_score);

                    o_score := 6;
                    u_score := 1 + floor(random() * 4)::INTEGER;
                    INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                    VALUES (sess_id, 2, u_score, o_score);
                ELSE
                    -- Lose set 1, win set 2
                    o_score := 6;
                    u_score := 1 + floor(random() * 4)::INTEGER;
                    INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                    VALUES (sess_id, 1, u_score, o_score);

                    u_score := 6;
                    o_score := 1 + floor(random() * 4)::INTEGER;
                    INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                    VALUES (sess_id, 2, u_score, o_score);
                END IF;

                -- Deciding set 3: user loses
                IF random() < 0.20 THEN
                    o_score := 7;
                    u_score := 5 + floor(random() * 2)::INTEGER;
                ELSE
                    o_score := 6;
                    u_score := 2 + floor(random() * 3)::INTEGER;
                END IF;
                INSERT INTO set_scores (session_id, set_number, user_score, opponent_score)
                VALUES (sess_id, 3, u_score, o_score);
            END IF;
        END IF;

    END IF; -- end set scores (not practice)

END LOOP;

RAISE NOTICE '✅ Inserted 150 sessions with ratings, set scores, and focus points for user %', uid;

END $$;
