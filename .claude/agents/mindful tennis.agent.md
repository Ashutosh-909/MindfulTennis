# Agent Instructions — Mindful Tennis

> **Mode: Incremental Development** — This is a mature, shipped Android app (v1.3, versionCode 4).
> All milestones (1–8) are complete. Changes are now bug fixes, feature enhancements, UI polish,
> performance optimizations, and refactors. Always explore existing code before making changes.
> Never rewrite or scaffold from scratch — build on what exists.

## Project Overview

**Mindful Tennis** is a production Android app for self-tracking and journaling tennis sessions and performance. Users log sessions (singles or doubles), rate 8 technique aspects, record set scores, optionally receive partner feedback, and view analytics trends over time. All data syncs offline-first via Room → Supabase.

The app has been released and is in active use. Refer to `plan.md` for the original design spec (milestones are all complete).

## Development History

The app was built through 8 milestones and numerous polish commits:

| Commit | Description |
|---|---|
| `9fd4eca` – `d13ab35` | Milestones 1–4: Project setup, auth, data layer, session CRUD |
| `e050261` – `b52908f` | Milestones 5–8: Dashboard analytics, sync, session list/detail |
| `b6085e1` | Feature: Cancel session |
| `47d302b` – `a168f1b` | Aspect performance filtering, UI design upgrades, radar chart |
| `069ec24` – `d6b15c4` | Sync fixes, perf improvements, foreground service removal, first release |
| `4f526f8` – `3ff9470` | KMP conversion attempt (dormant — `composeApp/` and `shared/` exist but are unused) |
| `3a0a944` | Latest: Android release crash fixes |

**Current branch:** `synclogicoverhaul` (HEAD), tracks `origin/main`.

## Incremental Change Guidelines

1. **Explore before editing**: Always read existing files before modifying them. The codebase has 101+ Kotlin files — don't guess at implementations.
2. **Preserve working patterns**: The app compiles and runs. Don't break existing functionality when adding/changing features.
3. **Follow established conventions**: Match the existing code style, naming patterns, and architecture decisions already in the codebase.
4. **Room migrations**: Database is at version 2. Any schema changes MUST include a Room migration.
5. **Sync awareness**: Any new or modified entity must respect the `syncStatus` column (`PENDING`/`SYNCED`) and be handled by `SyncManager`.
6. **Build validation**: After changes, verify the build succeeds. The app uses KSP for code generation (Room, Hilt) — annotation changes can cascade.
7. **No KMP changes**: The `composeApp/` and `shared/` modules are dormant. Do not modify them unless explicitly asked.

## Tech Stack (strict — do not deviate)

| Layer | Technology | Version |
|---|---|---|
| Platform | Android | minSdk 29, targetSdk 36, compileSdk 36 |
| Language | Kotlin | 2.1.10 |
| UI | Jetpack Compose, Material 3 | BOM `2025.01.01` |
| Build | Gradle Kotlin DSL, AGP | 8.9.1 |
| Architecture | MVVM + UDF | — |
| DI | Hilt | 2.55 |
| Local DB | Room | 2.7.1 |
| Preferences | DataStore | 1.1.4 |
| Cloud Auth | Supabase Auth (Google OAuth + Email) | 3.1.1 |
| Cloud DB | Supabase (PostgREST + Realtime) | 3.1.1 |
| Networking | Ktor (OkHttp engine) | 3.1.1 |
| Background Work | WorkManager | 2.10.0 |
| Navigation | Navigation Compose | 2.8.5 |
| Charts | Custom Canvas (RadarChart) | — |
| Serialization | kotlinx-serialization | 1.7.3 |
| Logging | Kermit | 2.0.5 |
| Code Gen | KSP | 2.1.10-1.0.29 |
| Testing | JUnit4, Compose UI Test, Turbine, MockK | — |
| Java compat | Java 11 source/target | — |

Dependencies are managed in `gradle/libs.versions.toml`. Build config injects `SUPABASE_URL` and `SUPABASE_ANON_KEY` from `local.properties`.

## Current Package Structure (what exists today)

All code lives under `com.ashutosh.mindfultennis` in `app/src/main/java/`.

```
com.ashutosh.mindfultennis/
├── MindfulTennisApp.kt                    // @HiltAndroidApp, WorkManager init
├── MainActivity.kt                         // Single Activity, NavHost, OAuth callback
├── navigation/
│   ├── NavGraph.kt                         // Auth guard → Login/Home routing
│   └── Route.kt                            // Sealed class: Login, Home, StartSession,
│                                           //   EndSession(id), SessionsList, SessionDetail(id)
├── di/
│   ├── AppModule.kt                        // SupabaseClient, DataStore, Room DB, all 7 DAOs
│   ├── RepositoryModule.kt                 // @Binds: 5 repo interfaces → impls
│   └── ServiceModule.kt                    // Placeholder
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── MindfulDatabase.kt          // @Database v2, 7 entities
│   │   │   ├── dao/                        // SessionDao, SelfRatingDao, PartnerRatingDao,
│   │   │   │                               //   FocusPointDao, OpponentDao, PartnerDao, SetScoreDao
│   │   │   └── entity/                     // SessionEntity, SelfRatingEntity, PartnerRatingEntity,
│   │   │                                   //   FocusPointEntity, OpponentEntity, PartnerEntity,
│   │   │                                   //   SetScoreEntity, EntityMappers, SyncStatus
│   │   └── datastore/
│   │       └── UserPreferences.kt          // Cached userId
│   ├── remote/
│   │   ├── SupabaseSessionDataSource.kt
│   │   ├── SupabaseUserDataSource.kt
│   │   └── model/                          // SessionDto, UserDto, RatingDto, OpponentDto,
│   │                                       //   SetScoreDto, FocusPointDto
│   ├── repository/                         // 5 interface + impl pairs:
│   │   ├── AuthRepository / AuthRepositoryImpl       // Google OAuth + Email auth
│   │   ├── SessionRepository / SessionRepositoryImpl // Full CRUD + sync
│   │   ├── FocusPointRepository / FocusPointRepositoryImpl
│   │   ├── OpponentRepository / OpponentRepositoryImpl
│   │   └── PartnerRepository / PartnerRepositoryImpl // Doubles partner management
│   └── sync/
│       ├── SyncManager.kt                  // Orchestrates Room ↔ Supabase sync
│       └── SyncWorker.kt                   // Periodic 15-min sync with network constraint
├── domain/
│   ├── model/                              // Clean Kotlin data classes (no annotations):
│   │   ├── Session.kt, Rating.kt, FocusPoint.kt, Opponent.kt, Partner.kt,
│   │   │   SetScore.kt, Aspect.kt, PerformanceTrend.kt, WinLossRecord.kt,
│   │   │   MatchType.kt, RatingType.kt, DurationFilter.kt
│   └── usecase/                            // 8 use cases:
│       ├── StartSessionUseCase, EndSessionUseCase, CancelSessionUseCase,
│       │   SubmitRatingsUseCase, GetSessionsUseCase, GetPerformanceTrendUseCase,
│       │   GetAspectAveragesUseCase, GetWinLossRecordUseCase
├── ui/
│   ├── login/                → LoginScreen, LoginViewModel, LoginUiState
│   ├── home/                 → HomeScreen, HomeViewModel, HomeUiState
│   │   └── components/       → AspectPerformanceCard, WinLossCard, DurationFilterChips,
│   │                           FocusPointsRow, PerformanceChart, RadarChart
│   ├── startsession/         → StartSessionScreen, StartSessionViewModel, StartSessionUiState
│   ├── endsession/           → EndSessionScreen, EndSessionViewModel, EndSessionUiState
│   │   └── components/       → SetScoreSection, PartnerRatingSection
│   ├── sessions/             → SessionsListScreen/ViewModel/UiState,
│   │                           SessionDetailScreen/ViewModel/UiState
│   ├── components/           → StarRatingBar, ErrorRetryCard, LoadingShimmer
│   └── theme/                → Theme.kt, Color.kt, Type.kt, Spacing.kt
└── util/
    ├── DateTimeUtils.kt
    ├── ScoreCalculator.kt
    └── Extensions.kt
```

## Architecture Rules

1. **MVVM + UDF**: Every screen has a `ViewModel` exposing `StateFlow<XxxUiState>` and accepting events. Composables observe state and emit events — never call repository/use-case directly from composables.

2. **Repository pattern**: ViewModels call UseCases (for complex logic) or Repositories (for simple CRUD). Repositories abstract Room + Supabase behind a single interface. Read from Room (single source of truth); write to Room first, then sync to Supabase.

3. **Domain models are annotation-free**: `data/local/db/entity/` has Room `@Entity` classes, `data/remote/model/` has Supabase DTOs (`@Serializable`), and `domain/model/` has clean Kotlin data classes. Map between layers via `EntityMappers.kt`.

4. **Hilt for DI**: `MindfulTennisApp` is `@HiltAndroidApp`, `MainActivity` is `@AndroidEntryPoint`. Singletons for Room DB, SupabaseClient, DataStore in `AppModule`. Bindings in `RepositoryModule`.

5. **Offline-first**: All data writes go to Room first. `SyncManager` observes pending changes (via `syncStatus` column: `PENDING` / `SYNCED`) and pushes to Supabase. `SyncWorker` runs periodically (15 min) with network constraint.

6. **Conflict resolution**: Last-Write-Wins per record using `updatedAt` timestamp.

## Key Domain Concepts

### Aspect Enum (8 self-rating dimensions)
```kotlin
enum class Aspect {
    FOREHAND, BACKHAND, SERVE, RETURN, VOLLEY, SLICE, MOVEMENT, MINDSET
}
```

### Match Types
```kotlin
enum class MatchType { SINGLES, DOUBLES }
```
Sessions support singles (1 opponent) and doubles (2 opponents + 1 partner). The `SessionEntity` has `matchType`, `opponent1Id`, `opponent2Id`, and `partnerId` fields.

### Rating Types
```kotlin
enum class RatingType(val label: String) {
    SELF("Self"), PARTNER("Partner's Feedback"), BOTH("Both")
}
```
Partner ratings = partner's feedback on YOUR game (not rating the partner).

### Duration Filters (analytics time ranges)
```kotlin
enum class DurationFilter(val label: String) {
    ONE_WEEK("1W"), ONE_MONTH("1M"), THREE_MONTHS("3M"),
    SIX_MONTHS("6M"), ONE_YEAR("1Y")
}
```

### Performance Score
Composite score from 8 self-ratings (each 1–5), normalized to 0–100:
```
overallScore = ((mean_of_8_ratings - 1) / 4) * 100
```

### Win/Loss
A session is a "win" if the user won a majority of recorded sets (`userScore > opponentScore`). Sessions without set scores are excluded from W/L records.

### Session Color Coding
| Score Range | Color | Label |
|---|---|---|
| ≥ 70 | Green `#4CAF50` | Great |
| 40–69 | Amber `#FF9800` | Average |
| < 40 | Red `#F44336` | Needs Work |
| null | Grey `#9E9E9E` | Unrated |

Always pair color with a text label and icon for accessibility.

## Navigation Routes

```kotlin
sealed class Route(val route: String) {
    data object Login : Route("login")
    data object Home : Route("home")
    data object StartSession : Route("start_session")
    data class EndSession(val sessionId: String) : Route("end_session/$sessionId")
    data object SessionsList : Route("sessions_list")
    data class SessionDetail(val sessionId: String) : Route("session_detail/$sessionId")
}
```

- Auth guard in `NavGraph.kt`: if Supabase Auth session is null → `Login`.
- OAuth callback deep link: `com.ashutosh.mindfultennis://callback`.

## Data Model (Current Schema — Room v2)

**SessionEntity**: `id` (UUID), `userId`, `focusNote`, `startedAt` (epoch ms), `endedAt` (nullable), `timeZoneId`, `notes`, `matchType` (SINGLES/DOUBLES), `opponent1Id`, `opponent2Id`, `partnerId`, `isActive`, `overallScore` (0–100), `createdAt`, `updatedAt`, `schemaVersion`, `syncStatus`

**SelfRatingEntity**: `id`, `sessionId`, `aspect` (enum), `rating` (1–5)

**PartnerRatingEntity**: `id`, `sessionId`, `aspect`, `rating` (1–5)

**FocusPointEntity**: `id`, `userId`, `text`, `category` (nullable), `createdAt`

**OpponentEntity**: `id`, `userId`, `name`, `createdAt`

**PartnerEntity**: `id`, `userId`, `name`, `createdAt`

**SetScoreEntity**: `id`, `sessionId`, `setNumber`, `userScore`, `opponentScore`, `opponentId` (nullable)

### Supabase Tables (8 — all with RLS policies)
```
users               → User profile (id = Supabase Auth UID)
sessions            → Session rows with matchType, opponent/partner IDs
self_ratings        → Self-rating rows (session_id FK)
partner_ratings     → Partner-rating rows (session_id FK)
focus_points        → Focus point rows (user_id FK)
opponents           → Opponent rows (user_id FK)
partners            → Partner rows (user_id FK)
set_scores          → Set score rows (session_id FK)
```

Full schema with RLS policies is in `supabase_schema.sql`.

## What Was Removed / Changed

- **No foreground service**: `ActiveSessionService` was designed but removed before first release (commit `d6b15c4`). Session tracking relies on Room state + UI.
- **No notification system**: `SessionNotificationManager` was not implemented.
- **No Paging**: The sessions list does not use Paging Compose.
- **Auth is Supabase, not Firebase**: The original spec mentioned Firebase UID — it's actually Supabase Auth UID. Auth supports Google OAuth and email/password.
- **KMP modules are dormant**: `composeApp/` and `shared/` exist from a conversion attempt but contain no source code. Only `app/` is the active module.

## Coding Conventions

- **Kotlin**: Idiomatic Kotlin — data classes, sealed classes/interfaces, extension functions, coroutines with `Flow`.
- **Compose**: Material 3 with dynamic color (Material You on Android 12+). Custom theme in `ui/theme/`.
- **Naming**: `XxxScreen.kt`, `XxxViewModel.kt`, `XxxUiState.kt` (separate file per state).
- **Error handling**: Use `Result` or catch-and-log patterns. Never swallow exceptions silently.
- **Formatting**: Standard Kotlin style (ktlint). 4-space indent. ~120 char line length.
- **State management**: `StateFlow` in ViewModels, `collectAsStateWithLifecycle()` in composables.

## What NOT to Do

- Do NOT use XML layouts or View-based UI. Everything is Compose.
- Do NOT use SharedPreferences — use DataStore.
- Do NOT store auth tokens manually — Supabase SDK handles this.
- Do NOT make network calls from composables or on `Main` dispatcher without switching to `IO`.
- Do NOT use `GlobalScope`. Use `viewModelScope` in ViewModels.
- Do NOT add dependencies not listed in the tech stack without explicit approval.
- Do NOT use `mutableStateOf` for complex screen state — use `StateFlow` + `collectAsStateWithLifecycle()`.
- Do NOT modify `composeApp/` or `shared/` modules unless explicitly asked.
- Do NOT rewrite existing files from scratch — make incremental, targeted edits.

## Testing

- **Unit tests** (`src/test/`): UseCases, ViewModels (with `TestDispatcher` + Turbine), ScoreCalculator, mappers.
- **Instrumented tests** (`src/androidTest/`): Room DAOs, Compose UI flows.
- Fakes over mocks when possible. Use MockK only when faking is impractical.
- New features should include at least one unit test for business logic.

## Reference Files

| File | Purpose |
|---|---|
| `plan.md` | Original design spec — UI wireframes, ER diagram, sync details, milestones |
| `supabase_schema.sql` | Production Supabase schema with RLS policies |
| `perfornaceOptimizationPlan.md` | Performance optimization notes |
| `gradle/libs.versions.toml` | All dependency versions (single source of truth) |
| `local.properties` | Supabase URL + anon key (gitignored) |
| `keystore.properties` | Release signing config (gitignored) |

