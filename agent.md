# Agent Instructions — Mindful Tennis

## Project Overview

**Mindful Tennis** is an Android app for self-tracking and journaling tennis sessions and performance. Users log sessions, rate 8 technique aspects (forehand, backhand, serve, return, volley, slice, movement, mindset), record set scores, and view trends over time. All data syncs to the cloud across devices.

Refer to `plan.md` for the full implementation plan, screen specs, data model, and milestone breakdown.

## Tech Stack (strict — do not deviate)

| Layer | Technology |
|---|---|
| Platform | Android, minSdk 29, targetSdk 36 |
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3 (BOM `2024.09.00`) |
| Build | Gradle Kotlin DSL, AGP 9.0.1 |
| Architecture | MVVM + Unidirectional Data Flow |
| DI | Hilt (`com.google.dagger:hilt-android`) |
| Local DB | Room |
| Preferences | DataStore (Preferences) |
| Cloud Auth | Supabase Auth (Google Sign-In via OAuth) |
| Cloud DB | Supabase (PostgREST + Realtime) |
| Background Work | WorkManager |
| Navigation | Navigation Compose |
| Charts | `io.github.ehsannarmani:compose-charts` (or custom Canvas) |
| Paging | Paging Compose |
| Testing | JUnit4, Compose UI Test, Espresso, Turbine, MockK |
| Java compat | Java 11 source/target |

## Package Structure

All code lives under `com.ashutosh.mindfultennis`. Follow this layout:

```
com.ashutosh.mindfultennis/
├── MindfulTennisApp.kt              // @HiltAndroidApp Application
├── MainActivity.kt                   // Single Activity, hosts NavHost
├── navigation/
│   ├── NavGraph.kt
│   └── Route.kt                      // sealed class/interface for routes
├── di/
│   ├── AppModule.kt                  // @Module: Room DB, Supabase Client, DataStore providers
│   ├── RepositoryModule.kt           // @Binds for repository interfaces → impls
│   └── ServiceModule.kt
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── MindfulDatabase.kt    // @Database
│   │   │   ├── dao/                  // SessionDao, RatingDao, etc.
│   │   │   └── entity/              // @Entity classes (SessionEntity, etc.)
│   │   └── datastore/
│   │       └── UserPreferences.kt
│   ├── remote/
│   │   ├── SupabaseSessionDataSource.kt
│   │   ├── SupabaseUserDataSource.kt
│   │   └── model/                    // Supabase DTOs (@Serializable)
│   ├── repository/                   // Interface + Impl pairs
│   │   ├── AuthRepository.kt / AuthRepositoryImpl.kt
│   │   ├── SessionRepository.kt / SessionRepositoryImpl.kt
│   │   ├── FocusPointRepository.kt / FocusPointRepositoryImpl.kt
│   │   └── OpponentRepository.kt / OpponentRepositoryImpl.kt
│   └── sync/
│       ├── SyncManager.kt
│       └── SyncWorker.kt            // WorkManager worker
├── domain/
│   ├── model/                        // Clean domain models (no Room/Supabase annotations)
│   │   ├── Session.kt, Rating.kt, FocusPoint.kt, Opponent.kt,
│   │   │   SetScore.kt, Aspect.kt, PerformanceTrend.kt
│   └── usecase/
│       ├── StartSessionUseCase.kt
│       ├── EndSessionUseCase.kt
│       ├── SubmitRatingsUseCase.kt
│       ├── GetPerformanceTrendUseCase.kt
│       ├── GetWinLossRecordUseCase.kt
│       ├── GetAspectAveragesUseCase.kt
│       └── GetSessionsUseCase.kt
├── ui/
│   ├── login/          → LoginScreen.kt, LoginViewModel.kt
│   ├── home/           → HomeScreen.kt, HomeViewModel.kt, components/
│   ├── startsession/   → StartSessionScreen.kt, StartSessionViewModel.kt
│   ├── endsession/     → EndSessionScreen.kt, EndSessionViewModel.kt, components/
│   ├── sessions/       → SessionsListScreen.kt, SessionDetailScreen.kt, ViewModels
│   └── components/     → Shared composables (LoadingShimmer, ErrorRetryCard, etc.)
├── service/
│   └── ActiveSessionService.kt       // Foreground service for active session
├── notification/
│   └── SessionNotificationManager.kt
└── util/
    ├── DateTimeUtils.kt
    ├── ScoreCalculator.kt
    └── Extensions.kt
```

## Architecture Rules

1. **MVVM + UDF**: Every screen has a `ViewModel` exposing `StateFlow<XxxUiState>` and accepting `XxxUiEvent` sealed interface events. Composables observe state and emit events — never call repository/use-case directly from composables.

2. **Repository pattern**: ViewModels call UseCases (for complex logic) or Repositories (for simple CRUD). Repositories abstract Room + Supabase behind a single interface. Read from Room (single source of truth); write to Room first, then sync to Supabase.

3. **Domain models are annotation-free**: `data/local/db/entity/` has Room `@Entity` classes, `data/remote/model/` has Supabase DTOs (`@Serializable`), and `domain/model/` has clean Kotlin data classes. Map between layers explicitly.

4. **Hilt for DI**: Annotate `Application` with `@HiltAndroidApp`, `MainActivity` with `@AndroidEntryPoint`. Provide singletons for Room DB, Supabase Client, DataStore in `AppModule`. Bind repository interfaces in `RepositoryModule`.

5. **Offline-first**: All data writes go to Room first. `SyncManager` observes pending changes (track via `syncStatus` column: `PENDING` / `SYNCED`) and pushes to Supabase. `SyncWorker` runs periodically (15 min) with network constraint. Supabase Realtime channels update Room when app is in foreground.

6. **Conflict resolution**: Last-Write-Wins per document using `updatedAt` timestamp. Field-level merge for Session documents.

## Key Domain Concepts

### Aspect Enum
```kotlin
enum class Aspect {
    FOREHAND, BACKHAND, SERVE, RETURN, VOLLEY, SLICE, MOVEMENT, MINDSET
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
    // end_session/{sessionId}
    // sessions_list
    // session_detail/{sessionId}
}
```

- Auth guard: if Supabase Auth session is null, navigate to `Login`.
- Deep link from notification: `end_session/{sessionId}`.

## Data Model (Entities)

**User**: `id` (Supabase Auth UID), `email`, `displayName`, `photoUrl`, `createdAt`, `timeZone`

**Session**: `id` (UUID), `userId`, `focusNote`, `startedAt` (epoch ms), `endedAt` (nullable), `timeZoneId`, `notes`, `isActive`, `overallScore` (0–100), `createdAt`, `updatedAt`, `schemaVersion`

**SelfRating**: `id`, `sessionId`, `aspect` (enum), `rating` (1–5)

**PartnerRating**: `id`, `sessionId`, `aspect`, `rating` (1–5)

**FocusPoint**: `id`, `userId`, `text`, `category` (nullable), `createdAt`

**Opponent**: `id`, `userId`, `name`, `createdAt`

**SetScore**: `id`, `sessionId`, `setNumber`, `userScore`, `opponentScore`, `opponentId` (nullable)

### Supabase Table Structure
```
users               → User profile (id = Supabase Auth UID)
sessions            → Session rows (user_id FK → users.id)
self_ratings        → Self-rating rows (session_id FK → sessions.id)
partner_ratings     → Partner-rating rows (session_id FK → sessions.id)
focus_points        → Focus point rows (user_id FK → users.id)
opponents           → Opponent rows (user_id FK → users.id)
set_scores          → Set score rows (session_id FK → sessions.id)
```

## Foreground Service (Active Session)

- `ActiveSessionService` starts when user begins a session and stops when they end it.
- Shows an ongoing (non-dismissible) notification with elapsed time and an "End Session" action.
- Uses `START_STICKY` to survive process death.
- `BOOT_COMPLETED` receiver restarts the service if an active session exists in Room.
- Notification channel: `active_session`, importance `LOW`.

## Coding Conventions

- **Kotlin**: Use idiomatic Kotlin — data classes, sealed classes/interfaces, extension functions, coroutines with `Flow`.
- **Compose**: Use `@Stable`/`@Immutable` on state classes. Use `key()` in `LazyColumn`. Avoid unnecessary lambda allocations — use `remember`.
- **Naming**: Screens are `XxxScreen.kt`, ViewModels are `XxxViewModel.kt`, state is `XxxUiState`, events are `XxxUiEvent`.
- **Error handling**: Use `Result` or sealed `Resource<T>` (`Loading`, `Success(data)`, `Error(message)`) pattern in repositories. Never swallow exceptions silently.
- **Formatting**: Follow standard Kotlin style (ktlint). 4-space indent. Max line length ~120.
- **Tests**: Test files mirror main source structure. Use `FakeXxxRepository` classes for UI/ViewModel tests. Use Turbine for `Flow` assertions. Use `@get:Rule val composeTestRule = createComposeRule()` for Compose UI tests.

## What NOT to Do

- Do NOT use XML layouts or View-based UI. Everything is Compose.
- Do NOT use SharedPreferences directly — use DataStore.
- Do NOT store auth tokens manually — Supabase Auth SDK handles this.
- Do NOT make network calls from composables or on `Main` dispatcher without switching to `IO`.
- Do NOT use `GlobalScope`. Use `viewModelScope` in ViewModels, `CoroutineScope` injected in repositories.
- Do NOT add dependencies not listed in the tech stack table above without explicit approval. If a task requires a new library, flag it.
- Do NOT use `mutableStateOf` for complex screen state — use `StateFlow` in ViewModel and `collectAsStateWithLifecycle()` in composables.

## Testing

- **Unit tests** (`src/test/`): UseCases, ViewModels (with `TestDispatcher` + Turbine), ScoreCalculator, mappers.
- **Instrumented tests** (`src/androidTest/`): Room DAOs, Compose UI flows (login, start/end session, ratings).
- Fakes over mocks when possible. Use MockK only when faking is impractical.
- Every new feature should include at least one unit test for the ViewModel and one for any business logic.

## Reference

See `plan.md` for:
- Full UI/UX wireframes and layout specs (§4)
- Complete ER diagram (§5)
- Sync & conflict resolution details (§7)
- Notification edge cases (§8)
- Chart/analytics specifics (§9)
- Milestone checklist (§13)
- Open questions and assumptions (§14)
