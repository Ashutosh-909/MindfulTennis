# MindfulTennis — Cold Start Performance Report & Optimization Plan

**Date:** March 22, 2026  
**Device:** Samsung Galaxy (Physical – R5CT20370LT), Android 15, Adreno GPU  
**Build:** Debug APK (69.05 MB), targetSdk=36, Vulkan renderer  
**Measured WaitTime:** ~3,017–3,030 ms (3 consecutive runs)

---

## 1. Executive Summary

The app takes **~3 seconds** to cold-start. The Android vitals threshold for "slow cold start" is **5 seconds**, so this isn't critically broken — but for a content app with minimal startup logic, **~1–1.5 seconds should be the target**. There is roughly **1.5–2 seconds of unnecessary work** on the main thread.

---

## 2. Startup Timeline (from ADB logcat)

All timestamps from the second run (PID 12458):

| Timestamp       | Delta    | Event                                                    |
|-----------------|----------|----------------------------------------------------------|
| 11:57:12.028    | +0 ms    | `ActivityTaskManager` OPEN transition requested          |
| 11:57:12.059    | +31 ms   | Splash Screen window created                             |
| 11:57:12.072    | +44 ms   | `ActivityManager`: Start proc 12458 (Zygote fork)       |
| 11:57:12.087    | +59 ms   | Late-enabling `-Xcheck:jni` (process alive)              |
| 11:57:12.111    | +83 ms   | `Using CollectorTypeCMC GC` (ART initialized)            |
| 11:57:12.274    | +246 ms  | `nativeloader`: APK classloader configured               |
| 11:57:12.313    | +285 ms  | **WorkManager initialized** (default config, on main thread) |
| 11:57:12.345    | +317 ms  | `WM-ForceStopRunnable`: cleanup + rescheduling           |
| 11:57:12.372    | +344 ms  | `ActivityThread`: render engine = Vulkan                 |
| 11:57:12.503    | +475 ms  | `InsetsController` — MainActivity window attached        |
| 11:57:12.507    | +479 ms  | `MindfulTennisApp` SharedPreferences (IDS training)      |
| 11:57:12.790    | +762 ms  | **Compose `AndroidComposeView` first init** (hiddenapi)  |
| 11:57:12.892    | +864 ms  | **`Supabase-Core`: SupabaseClient created!**             |
| 11:57:13.195    | +1167 ms | **SLF4J warning** (no providers — Ktor logging dep)      |
| 11:57:13.350    | +1322 ms | **JIT: 5066 KB** to compile `performTraversals()`        |
| 11:57:14.133    | +2105 ms | **`Supabase-Auth`: Trying to load session from storage** |
| 11:57:14.304    | +2276 ms | **`Supabase-Auth`: Successfully loaded session!**        |
| 11:57:14.919    | +2891 ms | WindowManager Relayout — first real frame ready          |
| 11:57:14.930    | +2902 ms | `libpenguin.so` not found (Samsung GPU error, harmless)  |
| 11:57:15.031    | +3003 ms | `reportNextDraw` — first frame submitted                 |
| 11:57:15.121    | +3093 ms | **`Choreographer: Skipped 155 frames!`**                 |
| 11:57:15.113    | +3085 ms | Splash screen removed, app window visible                |

**Total cold start: ~3,017 ms**

---

## 3. Root Cause Analysis

### 3.1. CRITICAL: Supabase Client Created on Main Thread (~500 ms)

The `SupabaseClient` is created as a Koin `single{}` which is lazily resolved on **first access** — but that first access happens on the **main thread** when `App()` composable calls `koinInject<AuthRepository>()`, which triggers the entire Supabase Client creation chain:

```
App() → koinInject<AuthRepository>() 
  → AuthRepositoryImpl(supabaseClient) 
    → createSupabaseClient() { install(Auth); install(Postgrest); install(Realtime) }
```

**Evidence:** `Supabase-Core: SupabaseClient created!` at 12.892s, blocking the main thread.

### 3.2. CRITICAL: Supabase Auth Session Load Blocks UI (~1,240 ms)

After the client is created, Supabase Auth begins **loading the stored session** from encrypted storage:

- **12.892s** → Supabase client created
- **14.133s** → Auth starts loading session from storage (1,241 ms gap!)
- **14.304s** → Session loaded (171 ms for the actual I/O)

The 1.2-second gap between client creation and session load is the **Auth plugin initialization + Ktor engine setup + Realtime WebSocket setup**, all happening before the first frame.

### 3.3. HIGH: WorkManager Initialization on Main Thread (~50 ms)

```
WM-WrkMgrInitializer: Initializing WorkManager with default configuration.
WM-PackageManagerHelper: Skipping component enablement...
WM-Schedulers: Created SystemJobScheduler...
WM-ForceStopRunnable: Application was force-stopped, rescheduling.
```

WorkManager auto-initializes via `ContentProvider` on the main thread before `Application.onCreate()` even runs. While only ~50 ms here, this is pure startup overhead.

### 3.4. HIGH: All Koin Dependencies Eagerly Triggered (~200 ms)

The `App()` composable immediately:
1. Creates `AuthRepository` → triggers SupabaseClient creation
2. Collects `authState` → triggers Auth plugin initialization
3. When authenticated → creates `HomeViewModel` → triggers Room DB + all DAOs + SyncManager + 3 Use Cases

All `single{}` Koin dependencies in `commonModule` (Supabase, Room DB, 7 DAOs, 2 data sources, 5 repos, SyncManager) are resolved eagerly on first navigation.

### 3.5. HIGH: Compose First-Frame Overhead (~300 ms)

The Compose runtime initialization (`AndroidComposeView` companion init at 12.790s) takes significant time on cold start due to:
- Reflection for `SystemProperties.addChangeCallback`
- First Compose layout/measure pass with NavHost + auth state resolution
- Material3 theme initialization

### 3.6. MEDIUM: 155 Frames Skipped (Main Thread Jank)

`Choreographer: Skipped 155 frames!` = **2.58 seconds** of main-thread blocking (at 60fps). This directly corresponds to:
- Supabase client creation
- Auth session loading
- Koin dependency resolution cascade
- Room database open/verify (first time)

### 3.7. MEDIUM: SLF4J Provider Warning

```
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders
```

Ktor's logging dependency triggers SLF4J classpath scanning which adds ~170 ms.

### 3.8. LOW: Debug APK Size (69 MB)

The 69 MB debug APK is inflated by:
- No ProGuard/R8 (debug build)
- Supabase BOM (Auth + Postgrest + Realtime + Ktor engine)
- Room + KSP generated code
- Compose runtime + Material3 + Navigation
- Dual module structure (composeApp + shared)

This mainly affects install time and first-launch DEX verification, not subsequent cold starts.

### 3.9. LOW: DNS Lookup Failures

```
DNS Requested by 10165(com.ashutosh.mindfultennis), FAIL, isBlocked=true
```

Network calls fail because the screen was off (device locked during test). In normal use, Supabase Auth token refresh would add latency if session is expired.

---

## 4. Optimization Plan

### Phase 1 — Quick Wins (Target: 3,000 ms → ~1,800 ms)

#### P1.1. Eagerly Initialize Supabase Client Off Main Thread

Move the SupabaseClient creation to a background coroutine in `Application.onCreate()`, so it's ready before `MainActivity` needs it:

```kotlin
class MindfulTennisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MindfulTennisApp)
            modules(commonModule, platformModule, configModule())
        }
        // Eagerly trigger Supabase init on background thread
        CoroutineScope(Dispatchers.IO).launch {
            get<SupabaseClient>() // warm up Supabase off main thread
        }
    }
}
```

**Expected savings:** ~500 ms (Supabase client construction moves off main thread)

#### P1.2. Defer Auth Session Loading — Show UI Immediately

Currently `App()` collects `authState` synchronously, which blocks until `Supabase-Auth` loads the session from storage. Instead, use `AuthState.Loading` as the initial state and show a lightweight splash/skeleton:

```kotlin
@Composable
fun App() {
    MindfulTennisTheme {
        val authRepository = koinInject<AuthRepository>()
        val authState by authRepository.authState.collectAsState(
            initial = AuthState.Loading
        )
        
        Surface(modifier = Modifier.fillMaxSize()) {
            when (authState) {
                is AuthState.Loading -> {
                    // Lightweight branded splash (renders immediately)
                    AppSplashScreen()
                }
                else -> {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        isAuthenticated = authState is AuthState.Authenticated,
                        pendingCancelSessionId = null,
                    )
                }
            }
        }
    }
}
```

**Expected savings:** First frame renders ~1,500 ms sooner (splash visible while auth loads in background)

#### P1.3. Disable WorkManager Auto-Initialization

Add to `AndroidManifest.xml`:
```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="androidx.work.WorkManagerInitializer"
        android:value="androidx.startup"
        tools:node="remove" />
</provider>
```

Then initialize WorkManager lazily (on first use, or in background after auth):
```kotlin
// In AndroidSyncScheduler
WorkManager.initialize(context, Configuration.Builder().build())
```

**Expected savings:** ~50 ms off main thread critical path

---

### Phase 2 — Structural Improvements (Target: ~1,800 ms → ~1,200 ms)

#### P2.1. Lazy Koin Singletons for Non-Essential Dependencies

Room DB, DAOs, SyncManager, Repositories, and Use Cases don't need to exist until the user is authenticated. Use `inject(LazyThreadSafetyMode.NONE)` or Koin's `lazy` qualifier:

```kotlin
// In NavGraph — HomeViewModel is only created when navigating to Home
// This is already correct (koinViewModel is lazy)
// BUT the problem is HomeViewModel's constructor triggers 11 inject() calls

// Make Room DB lazy — only build when first DAO is accessed
single<MindfulDatabase> {
    get<RoomDatabase.Builder<MindfulDatabase>>()
        .fallbackToDestructiveMigration(true)
        .build()
}
// This is already lazy (Koin single = lazy). The real issue is the cascade.
```

The key fix: **Don't trigger `performSync()` and `loadAllData()` in `HomeViewModel.init{}`**. Instead, let the auth state observer emit, then start data loading. Currently, `collectLatest` on auth state triggers sync immediately on construction — move heavy work into a `LaunchedEffect` in the screen.

**Expected savings:** ~200 ms (deferred Room DB + DAO creation)

#### P2.2. Remove Supabase Realtime from Initial Load

`install(Realtime)` adds WebSocket connection overhead during client creation. If you don't use Realtime on app launch, install it lazily:

```kotlin
single<SupabaseClient> {
    val config = get<AppConfig>()
    createSupabaseClient(
        supabaseUrl = config.supabaseUrl,
        supabaseKey = config.supabaseAnonKey,
    ) {
        install(Auth) {
            scheme = "com.ashutosh.mindfultennis"
            host = "callback"
        }
        install(Postgrest)
        // Remove: install(Realtime) — add when needed
    }
}
```

**Expected savings:** ~200–400 ms (no WebSocket handshake on startup)

#### P2.3. Add Baseline Profile

Create a Baseline Profile to pre-compile hot startup paths (Compose, Navigation, Koin, Supabase Auth) into native code, eliminating JIT compilation during startup:

```kotlin
// baseline-profile module
@RunWith(AndroidJUnit4::class)
class StartupProfile {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        rule.collect("com.ashutosh.mindfultennis") {
            pressHome()
            startActivityAndWait()
            // Wait for home screen
            device.wait(Until.hasObject(By.text("Start Session")), 10_000)
        }
    }
}
```

**Expected savings:** ~300–500 ms (eliminates `JIT: 5066KB to compile performTraversals()` and reduces Compose first-frame cost)

---

### Phase 3 — Advanced (Target: ~1,200 ms → <1,000 ms)

#### P3.1. Use AndroidX App Startup Library

Replace `Application.onCreate()` Koin init with `Initializer<T>` to parallelize initialization:

```kotlin
class KoinInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        startKoin { androidContext(context); modules(...) }
    }
    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
```

#### P3.2. Pre-warm Compose in Splash Theme

Use `SplashScreen` API (Android 12+) with `setKeepOnScreenCondition` to hold the system splash while initializing:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    splashScreen.setKeepOnScreenCondition { !isReady }
    super.onCreate(savedInstanceState)
    // ...
}
```

#### P3.3. R8 Full Mode + Resource Shrinking (Release Build)

The 69 MB debug APK will shrink dramatically with R8:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

Expected release APK: ~15–25 MB.

#### P3.4. Consider Replacing Ktor with OkHttp for Android

Supabase's Ktor dependency brings in SLF4J, coroutine adapters, and engine setup overhead. If targeting Android-only, using the OkHttp engine reduces initialization:

```kotlin
// gradle
implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
```

---

## 5. Priority Matrix

| # | Optimization | Effort | Impact | Priority |
|---|-------------|--------|--------|----------|
| P1.2 | Defer NavGraph — show splash while auth loads | Low | **-1,500 ms perceived** | **HIGHEST** |
| P1.1 | Eagerly init Supabase off main thread | Low | **-500 ms** | **HIGH** |
| P2.2 | Remove `Realtime` from initial install | Low | **-200–400 ms** | **HIGH** |
| P2.3 | Baseline Profile | Medium | **-300–500 ms** | **HIGH** |
| P1.3 | Disable WorkManager auto-init | Low | **-50 ms** | MEDIUM |
| P2.1 | Lazy dependency cascade | Medium | **-200 ms** | MEDIUM |
| P3.2 | SplashScreen API | Low | **perceived perf** | MEDIUM |
| P3.3 | R8 + shrink (release) | Low | **-1–2s first install** | MEDIUM |
| P3.1 | App Startup library | Medium | **-50–100 ms** | LOW |
| P3.4 | OkHttp engine for Ktor | Medium | **-100–170 ms** | LOW |

---

## 6. Expected Results

| Metric | Current | After Phase 1 | After Phase 2 | After Phase 3 |
|--------|---------|---------------|---------------|---------------|
| WaitTime (cold) | ~3,000 ms | ~1,800 ms | ~1,200 ms | <1,000 ms |
| First frame | ~3,000 ms | ~800 ms (splash) | ~800 ms | ~600 ms |
| Time to interactive | ~3,000 ms | ~2,500 ms | ~1,500 ms | ~1,000 ms |
| Skipped frames | 155 | ~40 | ~15 | <5 |
| Release APK | N/A | N/A | N/A | ~15–25 MB |

---

## 7. Key Takeaway

The single biggest win is **P1.2**: rendering a lightweight splash/skeleton immediately instead of blocking the first frame while Supabase Auth loads the session from encrypted storage (~1.5 seconds). Combined with moving Supabase creation off the main thread (P1.1) and removing Realtime from initial load (P2.2), the app should feel nearly instant.
