# Plan: Offline-First Sync with Manual Push to Supabase

## TL;DR
Change the app from bidirectional periodic sync to: (1) full pull on first login per device, (2) all reads/writes are local-only (Room), (3) push+pull to Supabase only when user manually triggers sync from a new Settings screen. Replace the logout button in HomeScreen TopAppBar with a Settings icon.

---

## Phase 1: Add Initial Full Sync on First Login

**Goal**: When a user logs in on a new device, pull ALL their data from Supabase into Room.

**Steps**:
1. Add `hasCompletedInitialSync` boolean preference to `UserPreferences` (data/local/datastore/UserPreferences.kt) — alongside existing `lastSyncTimestamp` and `cachedUserId`
2. Create `InitialSyncManager` class (data/sync/InitialSyncManager.kt) that does a full pull:
   - Fetch ALL focus_points, opponents, partners for the user via `SupabaseSessionDataSource`
   - Fetch ALL sessions for the user
   - Fetch ALL self_ratings, partner_ratings, set_scores for those sessions
   - Upsert everything into Room with `SyncStatus.SYNCED`
   - Set `hasCompletedInitialSync = true` in UserPreferences
3. Trigger initial sync after authentication succeeds — in `HomeViewModel` or via a dedicated loading/sync screen shown between Login and Home. Check `hasCompletedInitialSync` flag; if false, run `InitialSyncManager.fullPull(userId)` before showing Dashboard.
4. Show a loading indicator during initial sync (could be a full-screen spinner or a progress dialog in HomeScreen).

**Files to modify/create**:
- `data/local/datastore/UserPreferences.kt` — add `hasCompletedInitialSync` preference
- `data/sync/InitialSyncManager.kt` — **new file**, full pull logic
- `di/AppModule.kt` — provide InitialSyncManager if needed (or @Inject constructor)
- `ui/home/HomeViewModel.kt` — check flag on init, trigger initial sync
- `ui/home/HomeUiState.kt` — add `isInitialSyncing` state if showing loading

---

## Phase 2: Remove Best-Effort Supabase Push from Repositories

**Goal**: Write operations go to Room only. No Supabase interaction during normal use.

**Steps**:
5. In `SessionRepositoryImpl` — remove try/catch Supabase push blocks from: `createSession()`, `updateSession()`, `endSession()`, `saveSelfRatings()`, `savePartnerRatings()`, `saveSetScores()`, `deleteSession()`. All writes just go to Room with `SyncStatus.PENDING`. For deletes, change to soft-delete: mark as `PENDING_DELETE` instead of hard-deleting from Room (so manual sync can push the delete to Supabase later).
6. In `FocusPointRepositoryImpl` — remove try/catch Supabase push from `create()`, `delete()`. Same soft-delete pattern.
7. In `OpponentRepositoryImpl` — same removal from `create()`, `delete()`.
8. In `PartnerRepositoryImpl` — same removal from `create()`, `delete()`.
9. For soft-delete support: add a query filter to all DAO `observe*` and `getAll*` methods to exclude `PENDING_DELETE` rows (so UI doesn't show deleted items). E.g., `WHERE syncStatus != 'PENDING_DELETE'`.

**Files to modify**:
- `data/repository/SessionRepositoryImpl.kt`
- `data/repository/FocusPointRepositoryImpl.kt`
- `data/repository/OpponentRepositoryImpl.kt`
- `data/repository/PartnerRepositoryImpl.kt`
- `data/local/db/dao/SessionDao.kt` — filter out PENDING_DELETE in observe queries
- `data/local/db/dao/FocusPointDao.kt` — same
- `data/local/db/dao/OpponentDao.kt` — same
- `data/local/db/dao/PartnerDao.kt` — same
- (Rating/Score DAOs cascade from session, so no changes needed)

---

## Phase 3: Remove Automatic Background Sync

**Goal**: No more periodic SyncWorker. Sync is manual only.

**Steps**:
10. Remove `SyncWorker.enqueue(application)` call from `HomeViewModel.kt` (line ~87)
11. Optionally delete or gut `SyncWorker.kt` entirely (or keep it for future use but don't enqueue)
12. Remove the `RefreshClicked` event from `HomeUiEvent` and its handler in `HomeViewModel` (the refresh button in HomeScreen) — sync will now live in Settings screen only.

**Files to modify**:
- `ui/home/HomeViewModel.kt` — remove SyncWorker.enqueue() and RefreshClicked handling
- `ui/home/HomeScreen.kt` — remove refresh button if it exists in UI
- `ui/home/HomeUiEvent.kt` — remove RefreshClicked event
- `data/sync/SyncWorker.kt` — can be deleted or kept dormant

---

## Phase 4: Enhance SyncManager for Bidirectional Manual Sync

**Goal**: Manual sync does full push+pull so multi-device changes are reflected.

**Steps**:
13. Enhance existing `sync(userId)` in `SyncManager` to also handle `PENDING_DELETE` items:
    - Push phase (existing + new): push all `PENDING` items (same dependency order), then push all `PENDING_DELETE` items (delete from Supabase, then hard-delete from Room)
    - Pull phase (existing): unchanged — pull sessions updated after `lastSyncTimestamp`, LWW merge, pull focus points/opponents/partners
    - Update `lastSyncTimestamp` after completion
14. Add PENDING_DELETE push methods in SyncManager: for each table, query rows with `syncStatus = PENDING_DELETE`, call `remoteDataSource.delete*()`, then `dao.deleteById()`. Insert these after the existing PENDING push and before the pull phase.
15. The Settings sync button will call the existing `syncManager.sync(userId)` — no new method needed, just the PENDING_DELETE enhancement.

**Files to modify**:
- `data/sync/SyncManager.kt` — add PENDING_DELETE handling to existing `sync()` method

---

## Phase 5: Create Settings Screen

**Goal**: New Settings screen with Sync button and Logout button, accessible from HomeScreen.

**Steps**:
16. Add `Settings` route to `Route.kt`
17. Create `SettingsUiState.kt` — `isSyncing: Boolean`, `lastSyncTime: Long?`, `syncResult: String?`
18. Create `SettingsViewModel.kt`:
    - Inject `SyncManager`, `AuthRepository`, `UserPreferences`
    - `onSyncClicked()` → call `syncManager.sync(userId)` (full push+pull), update UI state with result
    - `onLogoutClicked()` → call `authRepository.signOut()`
19. Create `SettingsScreen.kt` composable:
    - TopAppBar with back navigation and title "Settings"
    - Sync button (with loading indicator when syncing, show last sync time)
    - Logout button
    - Match existing app theme/styling
20. Add Settings composable to `NavGraph.kt` with navigation from Home

**Files to create**:
- `ui/settings/SettingsScreen.kt`
- `ui/settings/SettingsViewModel.kt`
- `ui/settings/SettingsUiState.kt`

**Files to modify**:
- `navigation/Route.kt` — add Settings route
- `navigation/NavGraph.kt` — add Settings destination

---

## Phase 6: Update HomeScreen — Replace Logout with Settings

**Goal**: Replace the logout IconButton with a Settings icon that navigates to the Settings screen.

**Steps**:
21. In `HomeScreen.kt` TopAppBar actions: replace the exit/logout IconButton with a Settings gear icon (`Icons.Default.Settings`) that triggers navigation to Settings route
22. Remove `SignOutClicked` event handling from `HomeViewModel` (signOut now lives in SettingsViewModel)
23. Remove `SignOutClicked` from `HomeUiEvent`
24. Add `onNavigateToSettings: () -> Unit` callback to `HomeScreen` composable params

**Files to modify**:
- `ui/home/HomeScreen.kt` — replace logout with settings icon
- `ui/home/HomeViewModel.kt` — remove signOut handling
- `ui/home/HomeUiEvent.kt` — remove SignOutClicked
- `navigation/NavGraph.kt` — wire up settings navigation from Home

---

## Phase 7: Handle Logout Data Cleanup

**Goal**: On logout, reset `hasCompletedInitialSync` so next login triggers full pull again.

**Steps**:
25. In logout flow (SettingsViewModel.onLogoutClicked or AuthRepositoryImpl.signOut), reset `hasCompletedInitialSync = false` and clear `lastSyncTimestamp` in UserPreferences. Optionally clear Room database for the user.

**Files to modify**:
- `ui/settings/SettingsViewModel.kt` or `data/repository/AuthRepositoryImpl.kt` — reset preferences on logout

---

## Relevant Files

| File | Action |
|------|--------|
| `data/local/datastore/UserPreferences.kt` | Add `hasCompletedInitialSync` preference |
| `data/sync/InitialSyncManager.kt` | **CREATE** — full pull logic for first login |
| `data/sync/SyncManager.kt` | Add PENDING_DELETE handling to existing `sync()` |
| `data/sync/SyncWorker.kt` | Remove or stop enqueuing |
| `data/repository/SessionRepositoryImpl.kt` | Remove Supabase push from all write methods |
| `data/repository/FocusPointRepositoryImpl.kt` | Remove Supabase push from write methods |
| `data/repository/OpponentRepositoryImpl.kt` | Remove Supabase push from write methods |
| `data/repository/PartnerRepositoryImpl.kt` | Remove Supabase push from write methods |
| `data/local/db/dao/SessionDao.kt` | Filter PENDING_DELETE from observe queries |
| `data/local/db/dao/FocusPointDao.kt` | Filter PENDING_DELETE from observe queries |
| `data/local/db/dao/OpponentDao.kt` | Filter PENDING_DELETE from observe queries |
| `data/local/db/dao/PartnerDao.kt` | Filter PENDING_DELETE from observe queries |
| `ui/settings/SettingsScreen.kt` | **CREATE** — Settings UI |
| `ui/settings/SettingsViewModel.kt` | **CREATE** — Sync + Logout logic |
| `ui/settings/SettingsUiState.kt` | **CREATE** — Settings state |
| `ui/home/HomeScreen.kt` | Replace logout with settings icon |
| `ui/home/HomeViewModel.kt` | Remove sync/signout, add initial sync check |
| `ui/home/HomeUiEvent.kt` | Remove SignOutClicked, RefreshClicked |
| `navigation/Route.kt` | Add Settings route |
| `navigation/NavGraph.kt` | Add Settings destination + navigation |

---

## Verification

1. **First login test**: Fresh install → login → verify full Supabase pull populates Room → Dashboard shows all data
2. **Subsequent launch test**: Kill app, relaunch → verify no Supabase queries (only Room reads), data persists
3. **Offline write test**: Turn off network → create session, add focus points → verify data appears in UI from Room
4. **Manual sync test**: Open Settings → tap Sync → verify PENDING items pushed to Supabase AND remote changes pulled into Room
5. **Multi-device test**: Edit on device A → sync A → sync B → verify B has device A's changes
6. **Delete sync test**: Delete an item locally → manual sync → verify deleted from Supabase
7. **Logout/re-login test**: Logout → verify `hasCompletedInitialSync` reset → login again → verify full pull runs again
8. **Settings navigation**: Verify gear icon in HomeScreen navigates to Settings, back button returns to Home
9. **No background sync**: Verify SyncWorker is NOT enqueued, no periodic network calls

---

## Decisions
- **Manual sync is bidirectional** (push local changes → Supabase, then pull remote changes → Room). This supports multi-device use — changes from device A appear on device B after syncing.
- **Soft-delete pattern**: Deletes mark rows as `PENDING_DELETE` in Room (hidden from UI) so manual sync can push the delete to Supabase before hard-deleting.
- **Initial sync location**: Triggered in HomeViewModel on auth, with a loading state — no separate screen needed.
- **SyncWorker**: Removed (not just disabled) to keep codebase clean.
- **Conflict resolution**: Last-Write-Wins (LWW) by `updatedAt` timestamp during pull, same as existing logic.

## Further Considerations
1. **Sync conflict awareness**: If user edits the same item on two devices and syncs, the most recent `updatedAt` wins. This is acceptable for a single-user app but could cause silent data loss in edge cases.
2. **Data size on initial sync**: If a user has hundreds of sessions, the initial pull could take time. Consider showing a progress indicator (e.g., "Syncing sessions... opponents... focus points...").